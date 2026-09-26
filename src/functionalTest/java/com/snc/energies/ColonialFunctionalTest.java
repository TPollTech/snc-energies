package com.snc.energies;

import com.snc.energies.block.WorkshopBlock;
import com.snc.energies.block.MachineBlock;
import com.snc.energies.blockentity.WorkshopBlockEntity;
import com.snc.energies.blockentity.WoodStoveBlockEntity;
import com.snc.energies.menu.WorkshopMenu;
import com.snc.energies.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

/** Exercises actual server registries, recipes, multiblocks, loot and persistence. */
final class ColonialFunctionalTest {
    private final ServerLevel level;
    private final ServerPlayer player;
    private final BlockPos pos = new BlockPos(8, 110, 8);
    private ColonialFunctionalTest(ServerLevel level, ServerPlayer player) { this.level = level; this.player = player; }
    static void run(ServerLevel level, ServerPlayer player) { new ColonialFunctionalTest(level, player).run(); }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }
    private WorkshopBlockEntity place(Block block, Direction facing) {
        var state = block.defaultBlockState().setValue(MachineBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        block.setPlacedBy(level, pos, state, player, block.asItem().getDefaultInstance());
        return (WorkshopBlockEntity) level.getBlockEntity(pos);
    }
    private int drops(Item item) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(4,108,4,12,115,12))
                .stream().filter(e -> e.getItem().is(item)).mapToInt(e -> e.getItem().getCount()).sum();
    }
    private void run() {
        for (int x=4;x<12;x++) for(int y=110;y<113;y++) for(int z=4;z<12;z++) level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
        for (var block : new Block[]{SncBlocks.GRAIN_MILL, SncBlocks.SEED_PRESS}) {
            for (var facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                BlockPos obstructed = WorkshopBlock.cell(pos, facing, 3);
                level.setBlock(obstructed, Blocks.STONE.defaultBlockState(), 3);
                check(!WorkshopBlock.canPlace(level, pos, facing), "workshop refuses obstructed footprint: " + facing);
                level.removeBlock(obstructed, false);
                var machine = place(block, facing);
                for (int i=0;i<4;i++) {
                    var cell = WorkshopBlock.cell(pos, facing, i);
                    check(WorkshopBlock.controller(level, cell, level.getBlockState(cell)) == machine, "workshop part shares controller: " + facing + "/" + i);
                    check(i == 0 || level.getBlockEntity(cell) == null, "workshop structural parts have no duplicate inventory");
                }
                int before = drops(block.asItem()), beforeRice = drops(SncItems.RICE_PADDY);
                machine.setItem(0, new ItemStack(SncItems.RICE_PADDY, 7));
                level.destroyBlock(WorkshopBlock.cell(pos, facing, 3), true);
                for (int i=0;i<4;i++) check(level.getBlockState(WorkshopBlock.cell(pos, facing, i)).isAir(), "workshop removal cleans every cell");
                check(drops(block.asItem()) == before + 1, "workshop drops exactly one machine; delta=" + (drops(block.asItem()) - before));
                check(drops(SncItems.RICE_PADDY) == beforeRice + 7, "workshop returns stored items exactly once");
            }
        }
        var mill = place(SncBlocks.GRAIN_MILL, Direction.NORTH);
        mill.setItem(0, new ItemStack(SncItems.RICE_PADDY, 2));
        mill.serverTick();
        check(mill.getProgress() == 0 && mill.status() == 1, "manual workshop waits for work");
        check(mill.crank() && !mill.crank(), "crank is server-authoritative and limited to one charge per tick");
        mill.setItem(2, new ItemStack(SncItems.RICE_HUSK, 64));
        mill.serverTick();
        check(mill.work() == 120 && mill.getProgress() == 0 && mill.status() == 3, "full residue slot blocks all input and work consumption");
        mill.setItem(2, ItemStack.EMPTY);
        for(int i=0;i<20;i++) mill.serverTick();
        var saved = mill.saveWithFullMetadata(level.registryAccess());
        var restored = (WorkshopBlockEntity)BlockEntity.loadStatic(pos, mill.getBlockState(), saved, level.registryAccess());
        check(restored != null && restored.work() == 100 && restored.getProgress() == 20 && restored.getItem(0).getCount() == 2,
                "workshop persists partial processing, work and input");
        for(int i=0;i<60;i++) mill.serverTick();
        check(mill.getItem(0).isEmpty() && mill.getItem(1).is(SncItems.RICE) && mill.getItem(1).getCount() == 2
                && mill.getItem(2).is(SncItems.RICE_HUSK) && mill.getItem(2).getCount() == 1 && mill.work() == 40,
                "milling conserves exact ingredient, product, residue and work quantities");
        check(mill.getSlotsForFace(Direction.DOWN).length == 2 && mill.canPlaceItemThroughFace(0, new ItemStack(Items.WHEAT), Direction.UP)
                && !mill.canPlaceItemThroughFace(1, new ItemStack(SncItems.RICE), Direction.UP)
                && !mill.canTakeItemThroughFace(0, new ItemStack(Items.WHEAT), Direction.DOWN), "workshop sided access protects input and output slots");
        var menu = (WorkshopMenu)mill.createMenu(27, player.getInventory(), player);
        check(!menu.clickMenuButton(player, 45), "invalid workshop button is rejected");
        level.destroyBlock(pos, true);
        check(!menu.clickMenuButton(player, 0), "removed workshop cannot be cranked through a stale menu");

        var press = place(SncBlocks.SEED_PRESS, Direction.NORTH);
        press.setItem(0, new ItemStack(SncItems.SOYBEAN, 4));
        press.crank();
        for(int i=0;i<120;i++) press.serverTick();
        check(press.getItem(0).isEmpty() && press.getItem(1).is(SncItems.VEGETABLE_OIL) && press.getItem(1).getCount() == 1
                && press.getItem(2).is(SncItems.SOY_MEAL) && press.getItem(2).getCount() == 2 && press.work() == 0,
                "press produces oil and meal atomically without free containers");
        level.destroyBlock(pos, true);
        press = place(SncBlocks.SEED_PRESS, Direction.NORTH);
        press.setItem(0, new ItemStack(SncItems.RICE_HUSK, 4)); press.crank();
        for(int i=0;i<120;i++) press.serverTick();
        check(press.getItem(1).is(SncItems.BIOMASS_BRIQUETTE) && press.getItem(2).isEmpty(), "four rice husks produce one briquette without a phantom residue");
        check(WoodStoveBlockEntity.woodBurnTicks(new ItemStack(SncItems.BIOMASS_BRIQUETTE)) == 500
                && WoodStoveBlockEntity.woodBurnTicks(new ItemStack(SncItems.VEGETABLE_OIL)) == 400, "regional fuels expose documented burn values");
        level.destroyBlock(pos, true);

        mill = place(SncBlocks.GRAIN_MILL, Direction.NORTH);
        mill.crank(); mill.setItem(0, new ItemStack(SncItems.RICE_PADDY, 2));
        for(int i=0;i<20;i++) mill.serverTick();
        mill.setItem(0, new ItemStack(Items.WHEAT, 2)); mill.serverTick();
        check(mill.getProgress() == 1 && mill.getItem(1).isEmpty(), "changing recipe cannot reuse progress from a different ingredient");
        var namedFlour = new ItemStack(SncItems.FLOUR);
        namedFlour.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Named flour"));
        mill.setItem(1, namedFlour); int work = mill.work(); mill.serverTick();
        check(mill.work() == work && mill.status() == 3, "output stack components are preserved rather than overwritten");
        level.destroyBlock(pos, true);

        crop(SncBlocks.RICE_CROP, SncItems.RICE_SEEDS, SncItems.RICE_PADDY);
        crop(SncBlocks.SOY_CROP, SncItems.SOY_SEEDS, SncItems.SOYBEAN);
        crop(SncBlocks.MATE_CROP, SncItems.MATE_SEEDS, SncItems.MATE_LEAF);
        mill = place(SncBlocks.GRAIN_MILL, Direction.NORTH);
        mill.setItem(0, new ItemStack(SncItems.DRIED_MATE, 2)); mill.crank();
        for (int i=0;i<80;i++) mill.serverTick();
        check(mill.getItem(0).isEmpty() && mill.getItem(1).is(SncItems.GROUND_MATE)
                && mill.getItem(1).getCount() == 2 && mill.getItem(2).isEmpty(), "optional mate milling preserves the batch without phantom residue");
        var drink = new ItemStack(SncItems.MATE_INFUSION).finishUsingItem(level, player);
        check(drink.is(Items.BOWL), "mate infusion returns its bowl after consumption");
        level.destroyBlock(pos, true);
    }
    private void crop(Block block, Item seed, Item product) {
        var young = Block.getDrops(block.defaultBlockState(), level, pos, null);
        var mature = Block.getDrops(block.defaultBlockState().setValue(CropBlock.AGE, 7), level, pos, null);
        check(young.stream().mapToInt(s -> s.is(seed) ? s.getCount() : 0).sum() == 1
                && young.stream().noneMatch(s -> s.is(product)), "young crop returns seed without harvest exploit");
        check(mature.stream().mapToInt(s -> s.is(seed) ? s.getCount() : 0).sum() == 2
                && mature.stream().mapToInt(s -> s.is(product) ? s.getCount() : 0).sum() == 2,
                "mature crop yields documented seeds and produce");
    }
}
