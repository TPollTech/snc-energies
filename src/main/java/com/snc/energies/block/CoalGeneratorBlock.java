package com.snc.energies.block;

import com.snc.energies.blockentity.CoalGeneratorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The coal generator: burns fuel and pushes energy to neighbours.
 */
public class CoalGeneratorBlock extends MachineBlock {
	public static final BooleanProperty LIT = BlockStateProperties.LIT;

	public CoalGeneratorBlock(Properties props) {
		super(props);
		registerDefaultState(stateDefinition.any().setValue(LIT, Boolean.FALSE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(LIT);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CoalGeneratorBlockEntity(pos, state);
	}
}
