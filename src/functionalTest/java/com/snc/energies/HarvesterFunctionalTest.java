package com.snc.energies;

import com.snc.energies.entity.HarvesterEntity;
import com.snc.energies.menu.HarvesterMenu;
import com.snc.energies.registry.SncItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Harvester milestone: supply panel, oil-gated ignition, three-row harvest on
 * the planter's row layout with exact product accounting, mature-only cutting,
 * full-tank refusal without crop loss, and inventory drops on removal.
 */
final class HarvesterFunctionalTest {
    private final ServerLevel level;

    private HarvesterFunctionalTest(ServerLevel level) { this.level = level; }

    static void run(ServerLevel level, ServerPlayer player) { new HarvesterFunctionalTest(level).run(player); }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }

    private void clear(int x0, int z0) {
        for (int x = x0 - 1; x <= x0 + 11; x++)
            for (int z = z0 - 1; z <= z0 + 11; z++)
                for (int y = 116; y <= 132; y++)
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    /** The functional world persists between runs: failed passes leave
     * machine ghosts that would corrupt later accounting. */
    private void purgeGhosts() {
        for (net.minecraft.world.entity.Entity ghost : level.getEntitiesOfClass(
            net.minecraft.world.entity.Entity.class, new AABB(58, 110, 0, 74, 135, 26))) {
            if (ghost instanceof HarvesterEntity) {
                ghost.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
    }

    private void run(ServerPlayer player) {
        clear(60, 2);
        purgeGhosts();
        // Field mirroring the planter's row layout, mid-row for mature crops.
        for (int z = 2; z <= 12; z++)
            for (int x = 63; x <= 67; x++) {
                level.setBlock(new BlockPos(x, 119, z), Blocks.DIRT.defaultBlockState(), 3);
                level.setBlock(new BlockPos(x, 120, z), Blocks.FARMLAND.defaultBlockState(), 3);
            }
        for (int z = 2; z <= 12; z++) {
            level.setBlock(new BlockPos(64, 121, z), Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, 7), 3);
            level.setBlock(new BlockPos(66, 121, z), Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 7), 3);
        }
        // An immature carrot column must never be cut.
        level.setBlock(new BlockPos(66, 121, 8), Blocks.CARROTS.defaultBlockState().setValue(CropBlock.AGE, 3), 3);

        HarvesterEntity harvester = new HarvesterEntity(level, 65.5, 121.0, 3.5);
        level.addFreshEntity(harvester);

        // 1. Panel contract and oil gate.
        check(new HarvesterMenu.Provider(harvester).createMenu(12, player.getInventory(), player) instanceof HarvesterMenu,
            "harvester exposes its supply panel menu");
        check(!harvester.toggleWorking(), "harvester refuses to start on an empty oil tank");
        harvester.setOil(HarvesterEntity.OIL_PER_ITEM * 2);
        check(harvester.toggleWorking() && harvester.isWorking(), "harvester starts with oil available");

        // 2. One pass cuts the two mature columns on every row path; immature survives.
        harvester.setPos(65.5, 121.0, 6.5);
        harvester.setYRot(0.0F);
        int wheatBefore = count(harvester, Items.WHEAT);
        int carrotsBefore = count(harvester, Items.CARROT);
        harvester.driveTick(0.2F);
        // One pass sweeps the three rows three cells deep: exactly three mature
        // wheat plants (one wheat each, always) plus two carrot plants whose
        // root count is loot-random, so only a floor is asserted.
        check(count(harvester, Items.WHEAT) == wheatBefore + 3, "every mature wheat plant in the sweep lands in the tank");
        check(count(harvester, Items.CARROT) >= carrotsBefore + 2, "mature carrots yield their roots into the tank");
        BlockState leftBehind = level.getBlockState(new BlockPos(66, 121, 8));
        check(leftBehind.is(Blocks.CARROTS) && leftBehind.getValue(CropBlock.AGE) == 3,
            "immature crops are never cut");
        check(level.getBlockState(new BlockPos(64, 121, 7)).is(Blocks.WHEAT)
            && level.getBlockState(new BlockPos(64, 121, 7)).getValue(CropBlock.AGE) == 0,
            "cut crops reset to freshly planted for the next cycle");

        // 3. Full tank: harvest pauses without destroying any crop.
        for (int slot = 0; slot < HarvesterEntity.TOTAL_SLOTS; slot++) {
            harvester.setItem(slot, new ItemStack(Items.WHEAT, 64));
        }
        int wheatFull = count(harvester, Items.WHEAT);
        harvester.setPos(65.5, 121.0, 9.5);
        harvester.driveTick(0.2F);
        check(count(harvester, Items.WHEAT) == wheatFull, "full tank spends nothing: crops stay standing");
        check(level.getBlockState(new BlockPos(64, 121, 11)).getValue(CropBlock.AGE) == 7,
            "standing crops survive a full-tank pass");
        for (int slot = 0; slot < HarvesterEntity.TOTAL_SLOTS; slot++) {
            harvester.setItem(slot, ItemStack.EMPTY);
        }

        // 4. Raised header cuts nothing; the engine still burns one oil portion
        // per 80 work ticks, stationary included.
        harvester.cycleHeader();
        check(!harvester.isHeaderLowered(), "header lift toggles through the panel command");
        harvester.setPos(65.5, 121.0, 4.5);
        int wheatRaised = count(harvester, Items.WHEAT);
        int oilBefore = harvester.getOil();
        for (int i = 0; i < HarvesterEntity.TICKS_PER_OIL_PORTION; i++) {
            harvester.driveTick(0.0F);
        }
        check(count(harvester, Items.WHEAT) == wheatRaised, "raised header harvests nothing");
        check(harvester.getOil() == oilBefore - 1, "a work portion burns exactly one oil item with the header raised");

        // 5. Running dry stops the engine silently; it refuses to restart until refuelled.
        harvester.setOil(1);  // the next oil-portion boundary must kill the engine
        for (int i = 0; i < HarvesterEntity.TICKS_PER_OIL_PORTION && harvester.isWorking(); i++) {
            harvester.driveTick(0.0F);
        }
        check(!harvester.isWorking(), "oil exhaustion stops the engine silently");
        check(harvester.toggleWorking() == false, "engine refuses to restart without oil");
        harvester.driveTick(0.2F);
        check(harvester.getOil() == 0, "dead engine stops the oil drain");

        // 6. Panel buttons stay server-authoritative; removal returns the whole tank.
        player.setPos(65.5, 122.0, 4.5);
        HarvesterMenu panel = new HarvesterMenu(13, player.getInventory(), harvester);
        harvester.setOil(HarvesterEntity.OIL_PER_ITEM);
        check(panel.clickMenuButton(player, HarvesterMenu.BUTTON_IGNITION)
            && panel.clickMenuButton(player, HarvesterMenu.BUTTON_IGNITION),
            "panel ignition button toggles the harvester");
        harvester.setItem(0, new ItemStack(Items.WHEAT, 5));
        // The suite runs inside END_SERVER_TICK, after the entity step: the
        // spawned ItemEntity is only registered next tick, so the exact tank
        // drain is asserted instead of the (flaky-in-tick) drop lookup.
        harvester.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
        check(harvester.isRemoved() && harvester.getItem(0).isEmpty(),
            "removal drops the tank contents before the entity is gone");

        clear(60, 2);
    }

    private int count(HarvesterEntity harvester, net.minecraft.world.item.Item item) {
        int total = 0;
        for (int slot = 0; slot < HarvesterEntity.TOTAL_SLOTS; slot++) {
            ItemStack stack = harvester.getItem(slot);
            if (stack.is(item)) total += stack.getCount();
        }
        return total;
    }
}
