package com.snc.energies.block;

import com.snc.energies.blockentity.BeverageMotorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The machine engine: dock it against a supported Adventures machine and it
 * feeds and collects on its own, paying an SNC energy lot per step.
 */
public class BeverageMotorBlock extends MachineBlock {

    public BeverageMotorBlock(Properties props) {
        super(props);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BeverageMotorBlockEntity(pos, state);
    }
}
