package com.snc.energies;

import java.util.List;

import com.snc.energies.block.MachineBlock;
import com.snc.energies.blockentity.BeverageMotorBlockEntity;
import com.snc.energies.blockentity.ElectricUvLampBlockEntity;
import com.snc.energies.compat.AdventuresBeverages;
import com.snc.energies.registry.SncBlocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Optional Adventures bridges: the beverage motor (transactional feed/collect)
 * and the electric UV lamp (energy-gated opt-in variant). In the standalone
 * profile the engine must idle safely; with Adventures the full chain runs.
 */
final class AdventuresBridgeFunctionalTest {
    private final ServerLevel level;
    // Proven ticking band (chunk 1,0 — the agro suite runs its machines here),
    // kept clear of the agro footprint by staying at z <= 3.
    private final BlockPos base = new BlockPos(16, 130, 2);
    private final BlockPos lampPos = new BlockPos(18, 130, 2);

    private AdventuresBridgeFunctionalTest(ServerLevel level) {
        this.level = level;
    }

    static void run(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        new AdventuresBridgeFunctionalTest(level).run();
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }

    private void clear() {
        for (int x = 14; x < 31; x++) for (int z = 0; z < 4; z++) for (int y = 126; y < 140; y++)
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
    }

    private void placeMotor(BlockPos pos, Direction facing) {
        BlockState state = SncBlocks.BEVERAGE_MOTOR.defaultBlockState().setValue(MachineBlock.FACING, facing);
        level.setBlock(pos, state, 3);
    }

    /** Ticks any block-attached entity through its own block ticker, exactly as the world would. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void tickThroughBlock(ServerLevel level, BlockPos pos, Block block, BlockEntity entity) {
        BlockEntityTicker ticker = ((EntityBlock) block).getTicker(level, level.getBlockState(pos), entity.getType());
        if (ticker != null) ticker.tick(level, pos, level.getBlockState(pos), entity);
    }

    private void run() {
        clear();

        // ---- Standalone contract: the engine is a normal SNC machine ----
        BlockPos motorPos = base;
        placeMotor(motorPos, Direction.NORTH);
        var motor = (BeverageMotorBlockEntity) level.getBlockEntity(motorPos);
        check(motor != null, "beverage motor installs with its own block entity");

        motor.getEnergyStorage(Direction.UP).insert(BeverageMotorBlockEntity.CAPACITY, false);
        motor.serverTick();
        check(motor.getEnergy() == BeverageMotorBlockEntity.CAPACITY,
            "engine without a docked machine conserves its buffer");

        // Plain chest docked in front: the engine must leave unknown machines intact.
        BlockPos chestPos = motorPos.relative(Direction.NORTH);
        level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);
        if (!(level.getBlockEntity(chestPos) instanceof Container chest)) throw new AssertionError("docked chest missing");
        chest.setItem(0, new ItemStack(Items.WHEAT, 30));
        motor.setItem(BeverageMotorBlockEntity.FEED, new ItemStack(Items.WHEAT, 8));
        for (int i = 0; i < BeverageMotorBlockEntity.STEP_INTERVAL * 2; i++) motor.serverTick();
        check(chest.getItem(0).getCount() == 30 && motor.getItem(BeverageMotorBlockEntity.FEED).getCount() == 8,
            "engine ignores machines outside the beverage contract");
        motor.setItem(BeverageMotorBlockEntity.FEED, ItemStack.EMPTY);
        chest.setItem(0, ItemStack.EMPTY);

        // Persistence round trip.
        motor.getEnergyStorage(Direction.UP).insert(BeverageMotorBlockEntity.CAPACITY, false);
        var saved = motor.saveWithFullMetadata(level.registryAccess());
        var restored = (BeverageMotorBlockEntity) BlockEntity.loadStatic(motorPos, motor.getBlockState(), saved, level.registryAccess());
        check(restored != null && restored.getEnergy() == motor.getEnergy(),
            "engine energy and inventory survive serialization");
        clear();

        // ---- Electric UV lamp: opt-in variant with its own energy ----
        level.setBlock(lampPos, SncBlocks.ELECTRIC_UV_LAMP.defaultBlockState(), 3);
        var lamp = (ElectricUvLampBlockEntity) level.getBlockEntity(lampPos);
        check(lamp != null, "electric UV lamp installs with its own block entity");
        check(!lamp.isEmitting() && !level.getBlockState(lampPos).getValue(com.snc.energies.block.ElectricUvLampBlock.EMITTING),
            "lamp starts dark without energy");

        lamp.getEnergyStorage(Direction.UP).insert(ElectricUvLampBlockEntity.CAPACITY, false);
        lamp.serverTick();
        check(lamp.isEmitting() && level.getBlockState(lampPos).getValue(com.snc.energies.block.ElectricUvLampBlock.EMITTING),
            "a paid cycle lights the lamp through its own energy buffer");

        long before = lamp.getEnergy();
        int cyclesBefore = lamp.getProgress();
        lamp.serverTick();
        check(lamp.getEnergy() == before - ElectricUvLampBlockEntity.DRAIN_PER_TICK
            && lamp.getProgress() == cyclesBefore - 1,
            "every lit tick burns exactly its drain and counts down the cycle");

        // Starve it: the lamp goes dark on its own, no redstone involved.
        lamp.getEnergyStorage(Direction.UP).extract(Long.MAX_VALUE, false);
        int leftover = lamp.getProgress();
        for (int i = 0; i <= leftover + 2; i++) lamp.serverTick();
        check(!lamp.isEmitting() && !level.getBlockState(lampPos).getValue(com.snc.energies.block.ElectricUvLampBlock.EMITTING),
            "running out of energy turns the lamp off by itself");

        var lampSaved = lamp.saveWithFullMetadata(level.registryAccess());
        var lampRestored = (ElectricUvLampBlockEntity) BlockEntity.loadStatic(lampPos, lamp.getBlockState(), lampSaved, level.registryAccess());
        check(lampRestored != null && lampRestored.getEnergy() == lamp.getEnergy(),
            "lamp energy and cycle survive serialization");
        clear();

        // ---- Adventures profile: full motorization chain ----
        if (!com.snc.energies.compat.AdventuresCompatibility.isLoaded()) {
            check(AdventuresBeverages.steps().isEmpty(),
                "standalone profile exposes no motorizable batches");
            clear();
            return;
        }
        List<AdventuresBeverages.Step> steps = AdventuresBeverages.steps();
        check(!steps.isEmpty(), "Adventures beverage contract publishes motorizable batches");

        var cane = BuiltInRegistries.ITEM.getOptional(Identifier.parse("intoxicantes:cana_de_acucar")).orElseThrow(
            () -> new AssertionError("cana missing from registry"));
        AdventuresBeverages.Step caneStep = steps.stream()
            .filter(step -> step.input() == cane).findFirst()
            .orElseThrow(() -> new AssertionError("cane batch missing from the contract"));
        check(caneStep.machine().equals("moenda_cana") && caneStep.inputCount() == 4,
            "cane batch matches the published moenda contract (4 cana -> caldo + bagaco)");

        placeMotor(base, Direction.NORTH);
        var caneMotor = (BeverageMotorBlockEntity) level.getBlockEntity(base);
        BlockPos machinePos = base.relative(Direction.NORTH);
        Block moendaBlock = BuiltInRegistries.BLOCK.getOptional(Identifier.parse("intoxicantes:moenda_cana")).orElseThrow(
            () -> new AssertionError("moenda missing from registry"));
        level.setBlock(machinePos, moendaBlock.defaultBlockState(), 3);
        BlockEntity moendaEntity = level.getBlockEntity(machinePos);
        if (!(moendaEntity instanceof Container moenda)) throw new AssertionError("moenda BE missing");

        caneMotor.setItem(BeverageMotorBlockEntity.FEED, new ItemStack(cane, 8));
        caneMotor.getEnergyStorage(Direction.UP).insert(BeverageMotorBlockEntity.CAPACITY, false);
        var layout = AdventuresBeverages.layoutOf(moendaEntity);
        if (layout == null) throw new AssertionError("moenda layout unavailable to the engine");
        // Run engine + machine until the first batch is delivered; the engine
        // waits its natural STEP_INTERVAL between steps, exactly like in-game.
        int delivered = 0;
        for (int i = 0; i < 400 && delivered == 0; i++) {
            tickThroughBlock(level, machinePos, moendaBlock, moendaEntity);
            caneMotor.serverTick();
            delivered = moenda.getItem(layout.inputSlot()).getCount();
        }
        int inFeed = caneMotor.getItem(BeverageMotorBlockEntity.FEED).getCount();
        check(delivered + inFeed == 8,
            "engine delivered cane without loss (machine=" + delivered + " feed=" + inFeed + ")");
        check(delivered == 4, "engine delivers exactly one batch per step");

        // The moenda finishes the batch on its own tick; the engine then collects.
        caneMotor.getEnergyStorage(Direction.UP).insert(BeverageMotorBlockEntity.CAPACITY, false);
        boolean collected = false;
        for (int i = 0; i < 600 && !collected; i++) {
            tickThroughBlock(level, machinePos, moendaBlock, moendaEntity);
            caneMotor.serverTick();
            collected = !caneMotor.getItem(BeverageMotorBlockEntity.COLLECT).isEmpty();
        }
        check(collected, "engine collected the finished moenda output");
        ItemStack stack = caneMotor.getItem(BeverageMotorBlockEntity.COLLECT);
        var juice = BuiltInRegistries.ITEM.getOptional(Identifier.parse("intoxicantes:caldo_de_cana")).orElse(null);
        var bagaco = BuiltInRegistries.ITEM.getOptional(Identifier.parse("intoxicantes:bagaco_de_cana")).orElse(null);
        check(juice == null || stack.is(juice) || stack.is(bagaco),
            "collected stack is a real moenda product");
        clear();
    }
}
