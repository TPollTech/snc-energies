package com.snc.energies;

import com.snc.energies.block.IndustrialBlock;
import com.snc.energies.block.MachineBlock;
import com.snc.energies.blockentity.ElectricFurnaceBlockEntity;
import com.snc.energies.blockentity.IndustrialBlockEntity;
import com.snc.energies.blockentity.ItemPipeBlockEntity;
import com.snc.energies.registry.IndustryKind;
import com.snc.energies.registry.SncBlocks;
import com.snc.energies.registry.SncItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Automation milestone: item duct conservation, blocked network behaviour,
 * machine suction and the redstone gate. Everything lives inside the forced
 * spawn chunk and is torn down by explicit clears.
 */
final class AutomationFunctionalTest {
    private final ServerLevel level;

    private AutomationFunctionalTest(ServerLevel level) { this.level = level; }

    static void run(ServerLevel level) { new AutomationFunctionalTest(level).run(); }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }

    private void clear(int x0, int z0) {
        for (int x = x0 - 1; x <= x0 + 13; x++)
            for (int z = z0 - 1; z <= z0 + 11; z++)
                for (int y = 116; y <= 128; y++)
                    level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    private ItemPipeBlockEntity pipe(BlockPos pos) {
        level.setBlock(pos, SncBlocks.ITEM_PIPE.defaultBlockState(), 3);
        return (ItemPipeBlockEntity)level.getBlockEntity(pos);
    }

    private ElectricFurnaceBlockEntity furnace(BlockPos pos, Direction facing) {
        BlockState state = SncBlocks.ELECTRIC_FURNACE.defaultBlockState().setValue(MachineBlock.FACING, facing);
        level.setBlock(pos, state, 3);
        return (ElectricFurnaceBlockEntity)level.getBlockEntity(pos);
    }

    private IndustrialBlockEntity industry(BlockPos anchor, IndustryKind kind, Direction facing) {
        var block = SncBlocks.INDUSTRY.get(kind);
        BlockState state = block.defaultBlockState().setValue(MachineBlock.FACING, facing);
        level.setBlock(anchor, state, 3);
        block.setPlacedBy(level, anchor, state, null, new ItemStack(block.asItem()));
        return (IndustrialBlockEntity)level.getBlockEntity(anchor);
    }

    private void run() {
        clear(2, 2);

        // 1. Push through two ducts into a furnace input; item arrives intact, buffers end empty.
        BlockPos line = new BlockPos(3, 120, 3);
        ItemPipeBlockEntity a = pipe(line);
        ItemPipeBlockEntity b = pipe(line.east());
        ElectricFurnaceBlockEntity furnace = furnace(line.east().east(), Direction.WEST);
        a.setItem(0, new ItemStack(Items.RAW_IRON));
        for (int i = 0; i < 120 && furnace.getItem(0).isEmpty(); i++) { a.serverTick(); b.serverTick(); }
        check(furnace.getItem(0).is(Items.RAW_IRON) && furnace.getItem(0).getCount() == 1
            && a.getItem(0).isEmpty() && b.getItem(0).isEmpty(),
            "duct network delivers one raw iron into a furnace input without loss or duplication");

        // 2. Hopper semantics: insertion and extraction windows on every face, empty buffer only.
        check(a.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.DOWN)
            && a.canTakeItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.UP),
            "duct buffer exposes standard hopper windows on every face");
        a.setItem(0, new ItemStack(Items.RAW_IRON));
        check(!a.canPlaceItem(0, new ItemStack(Items.RAW_IRON)) && !a.accepts(new ItemStack(Items.RAW_IRON)),
            "duct holding an item rejects further insertion");
        a.setItem(0, ItemStack.EMPTY);

        // 3. Blocked destination: the item parks in the network, nothing created or destroyed.
        BlockPos blockedLine = new BlockPos(3, 120, 8);
        ItemPipeBlockEntity c = pipe(blockedLine);
        ItemPipeBlockEntity d = pipe(blockedLine.east());
        ElectricFurnaceBlockEntity blockedFurnace = furnace(blockedLine.east().east(), Direction.WEST);
        blockedFurnace.setItem(0, new ItemStack(Items.GOLD_INGOT));
        c.setItem(0, new ItemStack(Items.RAW_IRON));
        for (int i = 0; i < 80; i++) { c.serverTick(); d.serverTick(); }
        int parked = c.getItem(0).getCount() + d.getItem(0).getCount();
        boolean goldIntact = blockedFurnace.getItem(0).is(Items.GOLD_INGOT) && blockedFurnace.getItem(0).getCount() == 1;
        check(goldIntact && count(Items.RAW_IRON, c.getItem(0)) + count(Items.RAW_IRON, d.getItem(0)) == 1 && parked == 1,
            "blocked network parks the item in a duct without merging, ejecting or destroying it");
        blockedFurnace.setItem(0, ItemStack.EMPTY);
        for (int i = 0; i < 160 && blockedFurnace.getItem(0).isEmpty(); i++) { c.serverTick(); d.serverTick(); }
        check(blockedFurnace.getItem(0).is(Items.RAW_IRON) && c.getItem(0).isEmpty() && d.getItem(0).isEmpty(),
            "parked item resumes delivery once the destination accepts again");

        // 4. Suction: an empty duct pulls finished output from a native machine window.
        BlockPos suction = new BlockPos(3, 120, 12);
        ItemPipeBlockEntity e = pipe(suction.above());
        ElectricFurnaceBlockEntity hotFurnace = furnace(suction, Direction.NORTH);
        hotFurnace.getEnergyStorage(Direction.UP).insert(10000, false);
        hotFurnace.setItem(0, new ItemStack(SncItems.VOLTAITE_DUST));
        for (int i = 0; i < 200 && hotFurnace.getItem(1).isEmpty(); i++) hotFurnace.serverTick();
        check(hotFurnace.getItem(1).is(SncItems.VOLTAITE_INGOT), "furnace produces before suction check");
        for (int i = 0; i < 60 && e.getItem(0).isEmpty(); i++) e.serverTick();
        check(e.getItem(0).is(SncItems.VOLTAITE_INGOT) && hotFurnace.getItem(1).isEmpty(),
            "empty duct pulls a finished ingot out of a machine output");

        // 5. Industrial outputs: only the controller front face offers an extraction window.
        BlockPos frontAnchor = new BlockPos(11, 121, 3);
        IndustrialBlockEntity laminator = industry(frontAnchor, IndustryKind.LAMINATOR, Direction.NORTH);
        check(level.getBlockState(frontAnchor).getValue((IntegerProperty)IndustrialBlock.PART) == 0
            && level.getBlockEntity(frontAnchor) == laminator,
            "industrial anchor hosts the controller cell");
        check(laminator.getSlotsForFace(Direction.DOWN).length > 0
            && laminator.canTakeItemThroughFace(4, new ItemStack(SncItems.STEEL_PLATE), Direction.DOWN)
            && laminator.canTakeItemThroughFace(6, new ItemStack(Items.BUCKET), Direction.DOWN)
            && !laminator.canTakeItemThroughFace(0, new ItemStack(SncItems.STEEL_INGOT), Direction.DOWN),
            "industrial outputs extract only downwards");
        check(laminator.getSlotsForFace(Direction.UP).length > 0
            && laminator.canPlaceItemThroughFace(0, new ItemStack(SncItems.STEEL_INGOT), Direction.UP)
            && !laminator.canTakeItemThroughFace(4, new ItemStack(SncItems.STEEL_PLATE), Direction.NORTH),
            "side and top faces offer insertion windows without extraction");
        ItemPipeBlockEntity f = pipe(frontAnchor.below());
        for (int i = 0; i < 60 && f.getItem(0).isEmpty(); i++) {
            if (laminator.getItem(4).isEmpty()) laminator.setItem(4, new ItemStack(SncItems.STEEL_PLATE, 2));
            f.serverTick();
        }
        check(f.getItem(0).is(SncItems.STEEL_PLATE) && laminator.getItem(4).getCount() == 1,
            "duct suction removes exactly one plate per step from an industrial output");
        level.destroyBlock(frontAnchor, true);

        // 6. Redstone gate: mode persists, power starts work, unpower finishes the batch only.
        BlockPos gateAnchor = new BlockPos(12, 121, 8);
        IndustrialBlockEntity dryer = industry(gateAnchor, IndustryKind.DRYER, Direction.NORTH);
        check(dryer.runMode() == IndustrialBlockEntity.RUN_ALWAYS, "machines default to always-on operation");
        dryer.toggleRunMode();
        check(dryer.runMode() == IndustrialBlockEntity.RUN_WITH_REDSTONE, "panel toggle switches to redstone mode");
        dryer.getEnergyStorage(Direction.UP).insert(500000, false);
        dryer.setItem(0, new ItemStack(SncItems.RICE_PADDY, 8));
        dryer.serverTick();
        check(dryer.status() == 7, "redstone mode without signal reports the gate status");
        level.setBlock(new BlockPos(11, 121, 8), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        for (int i = 0; i < 40; i++) dryer.serverTick();
        check(dryer.status() == 2 && dryer.getProgress() > 0, "redstone power starts processing");
        level.removeBlock(new BlockPos(11, 121, 8), false);
        for (int i = 0; i < 200; i++) dryer.serverTick();
        check(dryer.getItem(4).is(SncItems.RICE) && dryer.getItem(4).getCount() == 4,
            "in-flight batch completes after the signal drops");
        check(dryer.getProgress() == 0 && dryer.status() == 7 && dryer.getItem(0).getCount() == 4,
            "no new batch starts while unpowered; inputs and gate state are preserved");
        var saved = dryer.saveWithFullMetadata(level.registryAccess());
        var restored = (IndustrialBlockEntity)BlockEntity.loadStatic(
            gateAnchor, dryer.getBlockState(), saved, level.registryAccess());
        check(restored != null && restored.runMode() == IndustrialBlockEntity.RUN_WITH_REDSTONE,
            "run mode survives persistence");
        check(IndustrialBlock.controller(level, gateAnchor, dryer.getBlockState()) == dryer,
            "gate test machine remains the single controller");
        level.destroyBlock(gateAnchor, true);

        // 7. Side configuration: per-face modes keep the 0.2.0 default and the
        // screwdriver cycle reconfigures faces and inverts the redstone gate.
        BlockPos sideAnchor = new BlockPos(3, 121, 6);
        IndustrialBlockEntity sided = industry(sideAnchor, IndustryKind.DRYER, Direction.NORTH);
        check(sided.faceMode(Direction.DOWN) == IndustrialBlockEntity.FACE_OUTPUT
            && sided.faceMode(Direction.UP) == IndustrialBlockEntity.FACE_INPUT,
            "default face modes keep the 0.2.0 contract: outputs below, inputs elsewhere");
        check(sided.canTakeItemThroughFace(4, new ItemStack(SncItems.RICE), Direction.DOWN)
            && !sided.canPlaceItemThroughFace(0, new ItemStack(SncItems.RICE_PADDY), Direction.DOWN)
            && sided.canPlaceItemThroughFace(0, new ItemStack(SncItems.RICE_PADDY), Direction.UP),
            "default windows: extraction only below, insertion above and beside");
        sided.cycleFaceMode(Direction.NORTH); // front: input -> output
        check(sided.faceMode(Direction.NORTH) == IndustrialBlockEntity.FACE_OUTPUT
            && sided.canTakeItemThroughFace(4, new ItemStack(SncItems.RICE), Direction.NORTH)
            && !sided.canPlaceItemThroughFace(0, new ItemStack(SncItems.RICE_PADDY), Direction.NORTH),
            "screwdriver reconfigures the front face to output only");
        sided.cycleFaceMode(Direction.NORTH); // output -> both
        check(sided.faceMode(Direction.NORTH) == IndustrialBlockEntity.FACE_BOTH
            && sided.canPlaceItemThroughFace(0, new ItemStack(SncItems.RICE_PADDY), Direction.NORTH)
            && sided.canTakeItemThroughFace(4, new ItemStack(SncItems.RICE), Direction.NORTH),
            "second cycle turns the front face into a both-ways window");
        sided.cycleRunMode();
        sided.cycleRunMode();
        check(sided.runMode() == IndustrialBlockEntity.RUN_NOT_REDSTONE,
            "screwdriver cycles through to the inverted redstone mode");
        sided.getEnergyStorage(Direction.UP).insert(500000, false);
        sided.setItem(0, new ItemStack(SncItems.RICE_PADDY, 8));
        sided.serverTick();
        check(sided.status() == 2, "inverted mode runs while unpowered");
        int progressBefore = sided.getProgress();
        level.setBlock(new BlockPos(2, 121, 6), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        sided.serverTick();
        check(sided.status() == 8 && sided.getProgress() == progressBefore,
            "power halts an inverted machine with the new gate status without losing progress");
        level.removeBlock(new BlockPos(2, 121, 6), false);
        sided.serverTick();
        check(sided.status() == 2, "inverted machine resumes once the signal drops");
        var sideSaved = sided.saveWithFullMetadata(level.registryAccess());
        var sideRestored = (IndustrialBlockEntity)BlockEntity.loadStatic(
            sideAnchor, sided.getBlockState(), sideSaved, level.registryAccess());
        check(sideRestored != null && sideRestored.runMode() == IndustrialBlockEntity.RUN_NOT_REDSTONE
            && sideRestored.faceMode(Direction.NORTH) == IndustrialBlockEntity.FACE_BOTH
            && sideRestored.faceMode(Direction.DOWN) == IndustrialBlockEntity.FACE_OUTPUT,
            "face modes and the inverted mode persist across save and load");
        level.destroyBlock(sideAnchor, true);

        // 8. Duct whitelist: gates hopper insertion and machine suction, persists, clears.
        BlockPos filterPos = new BlockPos(6, 120, 6);
        ItemPipeBlockEntity duct = pipe(filterPos);
        ElectricFurnaceBlockEntity donor = furnace(filterPos.east(), Direction.WEST);
        check(duct.accepts(new ItemStack(Items.RAW_IRON)) && duct.accepts(new ItemStack(Items.IRON_INGOT)),
            "empty whitelist lets every item into the duct");
        duct.setFilterItem(0, new ItemStack(Items.RAW_IRON));
        check(duct.accepts(new ItemStack(Items.RAW_IRON)) && !duct.accepts(new ItemStack(Items.IRON_INGOT))
            && !duct.canPlaceItemThroughFace(0, new ItemStack(Items.IRON_INGOT), Direction.UP)
            && duct.canPlaceItemThroughFace(0, new ItemStack(Items.RAW_IRON), Direction.UP),
            "whitelist admits only listed items through the insertion windows");
        donor.setItem(1, new ItemStack(Items.IRON_INGOT));
        for (int i = 0; i < 40 && duct.getItem(0).isEmpty(); i++) duct.serverTick();
        check(duct.getItem(0).isEmpty() && donor.getItem(1).is(Items.IRON_INGOT),
            "whitelist blocks suction of an unlisted machine output");
        donor.setItem(1, new ItemStack(Items.RAW_IRON));
        for (int i = 0; i < 60 && duct.getItem(0).isEmpty() && donor.getItem(1).getCount() == 1; i++) duct.serverTick();
        int rawIronTotal = count(Items.RAW_IRON, duct.getItem(0)) + count(Items.RAW_IRON, donor.getItem(0))
            + count(Items.RAW_IRON, donor.getItem(1));
        check(donor.getItem(1).isEmpty() && rawIronTotal == 1,
            "whitelisted output leaves the machine window without loss");
        duct.setItem(0, ItemStack.EMPTY); // free the cell so the persistence check tests the filter, not occupancy
        var ductSaved = duct.saveWithFullMetadata(level.registryAccess());
        var ductRestored = (ItemPipeBlockEntity)BlockEntity.loadStatic(
            filterPos, duct.getBlockState(), ductSaved, level.registryAccess());
        check(ductRestored != null && ductRestored.filterItem(0).is(Items.RAW_IRON)
            && ductRestored.accepts(new ItemStack(Items.RAW_IRON)) && !ductRestored.accepts(new ItemStack(Items.IRON_INGOT)),
            "filter persists across save and load");
        ductRestored.clearFilter();
        check(ductRestored.filterItem(0).isEmpty() && ductRestored.accepts(new ItemStack(Items.IRON_INGOT)),
            "clearing the whitelist restores universal acceptance");
        level.destroyBlock(donor.getBlockPos(), true);
    }

    private static int count(net.minecraft.world.item.Item item, ItemStack stack) {
        return stack.is(item) ? stack.getCount() : 0;
    }
}
