package com.snc.energies.block;

import com.snc.energies.blockentity.EnergyCubeBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The energy cube: a big battery block.
 */
public class EnergyCubeBlock extends Block implements net.minecraft.world.level.block.EntityBlock {

	public EnergyCubeBlock(Properties props) {
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new EnergyCubeBlockEntity(pos, state);
	}
	@Override
	public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
			net.minecraft.world.level.Level level, BlockState state,
			net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		return level.isClientSide() ? null : (world, pos, blockState, entity) -> {
			if (entity instanceof com.snc.energies.blockentity.MachineBlockEntity machine) machine.serverTick();
		};
	}
	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state,
			net.minecraft.world.level.Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.phys.BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof EnergyCubeBlockEntity cube) {
			player.openMenu(cube);
		}
		return net.minecraft.world.InteractionResult.SUCCESS;
	}
}
