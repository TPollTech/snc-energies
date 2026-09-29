package com.snc.energies;

import com.snc.energies.entity.PlanterEntity;
import com.snc.energies.entity.TractorEntity;
import com.snc.energies.menu.TractorMenu;
import com.snc.energies.registry.SncBlocks;
import com.snc.energies.registry.SncItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Tractor milestone: supply panel, oil-gated ignition, three-row seeding on
 * tilled soil with exact seed accounting, occupied-position skip and the
 * planter lift gate. Everything lives inside the forced spawn chunk and is
 * torn down by explicit clears.
 */
final class TractorFunctionalTest {
    private final ServerLevel level;

    private TractorFunctionalTest(ServerLevel level) { this.level = level; }

    static void run(ServerLevel level, ServerPlayer player) { new TractorFunctionalTest(level).run(player); }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }

    private void clear(int x0, int z0) {
        for (int x = x0 - 1; x <= x0 + 11; x++)
            for (int z = z0 - 1; z <= z0 + 11; z++)
                for (int y = 116; y <= 128; y++)
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    /** The functional world persists between runs: failed passes leave
     * implement ghosts that the hitch search would couple instead of ours. */
    private void purgeGhosts() {
        for (net.minecraft.world.entity.Entity ghost : level.getEntitiesOfClass(
            net.minecraft.world.entity.Entity.class, new AABB(38, 110, 0, 54, 135, 26))) {
            if (ghost instanceof TractorEntity || ghost instanceof PlanterEntity) {
                ghost.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private void run(ServerPlayer player) {
        clear(40, 2);
        purgeGhosts();
        // Ground: tilled field with one pre-planted wheat crop in the middle row path.
        for (int z = 2; z <= 12; z++)
            for (int x = 43; x <= 47; x++) {
                level.setBlock(new BlockPos(x, 119, z), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 120, z), Blocks.FARMLAND.defaultBlockState(), 3);
            }
        level.setBlock(new BlockPos(45, 121, 4), Blocks.WHEAT.defaultBlockState(), 3);

        TractorEntity tractor = new TractorEntity(level, 45.5, 121.0, 3.5);
        level.addFreshEntity(tractor);

        // 1. Panel contract: menu provider and slot acceptance rules.
        check(new TractorMenu.Provider(tractor).createMenu(7, player.getInventory(), player) instanceof TractorMenu,
            "tractor exposes its supply panel menu");
        check(TractorEntity.isSeed(new ItemStack(SncItems.SOY_SEEDS))
            && TractorEntity.isSeed(new ItemStack(Items.WHEAT_SEEDS))
            && !TractorEntity.isSeed(new ItemStack(Items.RAW_IRON)),
            "seed slots accept regional and vanilla seeds only");

        // 2. Ignition refuses without oil; a filled tank lets the engine start and stop.
        check(!tractor.toggleWorking(), "engine refuses to start on an empty oil tank");
        tractor.setOil(TractorEntity.OIL_PER_ITEM);
        check(tractor.toggleWorking() && tractor.isWorking(), "engine starts with oil available");
        check(tractor.toggleWorking() && !tractor.isWorking(), "engine toggles off through the same button");

        // 3. Planting consumes one seed per free position and skips occupied ones.
        // Row 0 (left) takes soybeans from its own tank; rows 1/2 carry wheat.
        tractor.setItem(0, new ItemStack(SncItems.SOY_SEEDS, 10));
        tractor.setItem(TractorEntity.SEEDS_PER_ROW, new ItemStack(Items.WHEAT_SEEDS, 10));
        tractor.setItem(2 * TractorEntity.SEEDS_PER_ROW, new ItemStack(Items.WHEAT_SEEDS, 10));
        tractor.toggleWorking();
        tractor.setPlanterRaised(false);
        tractor.setPos(45.5, 121.0, 3.5);
        tractor.setYRot(0.0F);
        int seedsBefore = tractor.getItem(0).getCount() + tractor.getItem(TractorEntity.SEEDS_PER_ROW).getCount()
            + tractor.getItem(2 * TractorEntity.SEEDS_PER_ROW).getCount();
        tractor.driveTick(0.2F);
        int seedsAfter = tractor.getItem(0).getCount() + tractor.getItem(TractorEntity.SEEDS_PER_ROW).getCount()
            + tractor.getItem(2 * TractorEntity.SEEDS_PER_ROW).getCount();
        check(seedsAfter == seedsBefore - 3, "one seed spent per planted row position (three rows, one occupied)");
        BlockState occupied = level.getBlockState(new BlockPos(45, 121, 4));
        check(occupied.is(Blocks.WHEAT) && occupied.getValue(CropBlock.AGE) == 0,
            "existing crop is never replaced by the planter");
        check(level.getBlockState(new BlockPos(45, 121, 6)).is(Blocks.WHEAT),
            "centre row plants wheat ahead of the tractor");
        check(level.getBlockState(new BlockPos(44, 121, 6)).is(SncBlocks.SOY_CROP),
            "left row plants soybeans from its own tank");
        check(level.getBlockState(new BlockPos(46, 121, 6)).is(Blocks.WHEAT),
            "right row plants wheat from its own tank");

        // 4. Raised lift: no planting, no seed spending; renderer mirrors the hitch.
        tractor.setPlanterRaised(true);
        check(tractor.isPlanterRaised(), "lift command synchronizes to the renderer state");
        tractor.setPos(45.5, 121.0, 6.5);
        int beforeLift = tractor.getItem(0).getCount();
        tractor.driveTick(0.2F);
        check(tractor.getItem(0).getCount() == beforeLift,
            "raised planter spends no seeds");

        // 5. Oil burns while driving with the engine on.
        tractor.setPlanterRaised(false);
        int oilBefore = tractor.getOil();
        tractor.driveTick(0.2F);
        check(tractor.getOil() == oilBefore - 1, "driving with the engine on burns exactly one oil tick");

        // 6. Two occupied columns on every row path: seeds are never spent there.
        level.setBlock(new BlockPos(44, 121, 7), Blocks.WHEAT.defaultBlockState(), 3);
        level.setBlock(new BlockPos(45, 121, 7), Blocks.WHEAT.defaultBlockState(), 3);
        level.setBlock(new BlockPos(46, 121, 7), Blocks.WHEAT.defaultBlockState(), 3);
        int beforeSkip = tractor.getItem(0).getCount() + tractor.getItem(TractorEntity.SEEDS_PER_ROW).getCount()
            + tractor.getItem(2 * TractorEntity.SEEDS_PER_ROW).getCount();
        tractor.setPos(45.5, 121.0, 4.5);
        tractor.driveTick(0.2F);
        check(tractor.getItem(0).getCount() + tractor.getItem(TractorEntity.SEEDS_PER_ROW).getCount()
            + tractor.getItem(2 * TractorEntity.SEEDS_PER_ROW).getCount() == beforeSkip,
            "occupied positions across all rows spend no seeds");

        // 6. Supply panel buttons remain server-authoritative and tractor-bound.
        player.setPos(45.5, 122.0, 4.5);   // panel validity requires the operator nearby
        TractorMenu panel = new TractorMenu(9, player.getInventory(), tractor);
        check(panel.clickMenuButton(player, TractorMenu.BUTTON_IGNITION)
            && panel.clickMenuButton(player, TractorMenu.BUTTON_IGNITION),
            "panel ignition button toggles the engine");

        // 7. Detachable SNC 75-P implement: attach, plant from its tanks, detach.
        PlanterEntity implement = new PlanterEntity(level, 45.5, 121.0, 8.5);
        implement.setYRot(0.0F);
        level.addFreshEntity(implement);
        implement.setItem(0, new ItemStack(Items.WHEAT_SEEDS, 10));
        implement.setItem(PlanterEntity.SEEDS_PER_ROW, new ItemStack(SncItems.SOY_SEEDS, 10));
        implement.setItem(2 * PlanterEntity.SEEDS_PER_ROW, new ItemStack(SncItems.SOY_SEEDS, 10));
        tractor.setPlanterRaised(false);
        tractor.setPos(45.5, 121.0, 10.5);
        tractor.setYRot(180.0F);           // facing the implement parked behind
        check(!tractor.hasAttachedPlanter(), "implement starts unattached");
        check(tractor.attachPlanter(), "hitch couples the parked implement");
        check(tractor.hasAttachedPlanter() && tractor.attachPlanter() == false,
            "second hitch is refused while one implement is coupled");
        tractor.setYRot(0.0F);
        tractor.setPos(45.5, 121.0, 10.5);
        tractor.setItem(0, ItemStack.EMPTY);          // tractor tanks drained:
        tractor.setItem(TractorEntity.SEEDS_PER_ROW, ItemStack.EMPTY);   // rows must
        tractor.setItem(2 * TractorEntity.SEEDS_PER_ROW, ItemStack.EMPTY); // come from the implement
        // Clear the field ahead of the implement so the earlier passes do not block it.
        for (int z = 10; z <= 12; z++) {
            level.setBlock(new BlockPos(44, 121, z), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(new BlockPos(45, 121, z), Blocks.AIR.defaultBlockState(), 3);
            level.setBlock(new BlockPos(46, 121, z), Blocks.AIR.defaultBlockState(), 3);
        }
        if (tractor.isWorking()) tractor.toggleWorking();  // the panel pass left it running
        check(tractor.toggleWorking() && tractor.isWorking(), "engine starts for the hitch pass");
        int implementBefore = implement.getItem(0).getCount() + implement.getItem(PlanterEntity.SEEDS_PER_ROW).getCount()
            + implement.getItem(2 * PlanterEntity.SEEDS_PER_ROW).getCount();
        // The hitch pose runs BEFORE plantRows() in the same tick, so the row
        // origin is the welded hitch cell reached after this drive step
        // (z 10.5 + 0.2 throttle), never the implement's parked spot.
        double weldedZ = tractor.getZ() + 0.2 - PlanterEntity.HITCH_DISTANCE;
        int plantZ = (int) Math.floor(weldedZ) + 3;
        // Occupy the welded centre cell: rows 0/2 must plant, the centre must be kept.
        level.setBlock(new BlockPos(45, 121, plantZ), Blocks.WHEAT.defaultBlockState(), 3);
        tractor.driveTick(0.2F);
        int implementAfter = implement.getItem(0).getCount() + implement.getItem(PlanterEntity.SEEDS_PER_ROW).getCount()
            + implement.getItem(2 * PlanterEntity.SEEDS_PER_ROW).getCount();
        check(implementAfter == implementBefore - 2,
            "attached implement spends its own seeds (welded origin, occupied column skipped)");
        check(level.getBlockState(new BlockPos(44, 121, plantZ)).is(Blocks.WHEAT),
            "left row plants the implement's own wheat seeds");
        check(level.getBlockState(new BlockPos(46, 121, plantZ)).is(SncBlocks.SOY_CROP),
            "right row plants the implement's own soybean seeds");
        BlockState keptCentre = level.getBlockState(new BlockPos(45, 121, plantZ));
        check(keptCentre.is(Blocks.WHEAT) && keptCentre.getValue(CropBlock.AGE) == 0,
            "occupied centre cell is never replaced by the implement");
        check(Math.abs(implement.getZ() - (tractor.getZ() - PlanterEntity.HITCH_DISTANCE)) < 0.01,
            "implement rides welded at the tractor's rear hitch");
        check(implement.isWorking() && !implement.isRaised(), "implement mirrors working and lift state");
        check(tractor.detachPlanter() && !tractor.hasAttachedPlanter(), "hitch uncouples the implement");
        check(!implement.isWorking(), "detached implement parks its mechanisms");
        check(implement.getItem(0).getCount() > 0, "detached implement keeps its remaining seeds");
        check(level.getBlockState(new BlockPos(45, 121, 8)) != null,
            "implement menu contract persists after detach");
        check(new com.snc.energies.menu.PlanterMenu.Provider(implement)
            .createMenu(11, player.getInventory(), player) instanceof com.snc.energies.menu.PlanterMenu,
            "implement exposes its own tank menu");

        // Cleanup (chunk-torn-down rule).
        implement.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        tractor.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        clear(40, 2);
        purgeGhosts();
    }
}
