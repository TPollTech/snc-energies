package com.snc.energies.block;

import com.snc.energies.blockentity.ElectricUvLampBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Opt-in electric variant of the Adventures UV lamp.
 *
 * Independent gates, per the cross-mod contract: it emits only while its own
 * SNC energy buffer holds a paid cycle, and (unlike the original) it ignores
 * redstone entirely unless the builder adds an external valve through the
 * machine's own controls. It deliberately emits at light level 11 — below the
 * 12 threshold of the vanilla-growth shortcut — so the ONLY effect is the
 * UV maturation of the Adventures crops under it.
 */
public class ElectricUvLampBlock extends Block implements EntityBlock {
    public static final BooleanProperty EMITTING = BooleanProperty.create("emitting");

    public ElectricUvLampBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(EMITTING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EMITTING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ElectricUvLampBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level, BlockState state,
            net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null : (world, pos, blockState, entity) -> {
            if (entity instanceof com.snc.energies.blockentity.MachineBlockEntity machine) machine.serverTick();
        };
    }
}
