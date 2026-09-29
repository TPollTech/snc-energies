package com.snc.energies.block;

import com.snc.energies.blockentity.ItemPipeBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** A thin item duct: one buffered item per cell, routing decided by each cell's tick. */
public final class ItemPipeBlock extends CableBlock {
    public ItemPipeBlock(Properties properties) { super(properties); }
    /** Right click opens the per-cell whitelist filter. */
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        BlockEntity entity = level.getBlockEntity(pos);
        if (entity instanceof ItemPipeBlockEntity duct) {
            if (!level.isClientSide()) player.openMenu(duct);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
    @Override protected boolean connects(BlockState state) {
        if (state.getBlock() instanceof IndustrialBlock machine) return true;
        if (state.getBlock() instanceof MachineBlock) return true;
        return state.getBlock() instanceof ItemPipeBlock || state.getBlock() instanceof EnergyCubeBlock;
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return new ItemPipeBlockEntity(pos,state);}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){
        return level.isClientSide()?null:(world,pos,blockState,entity)->{
            if(entity instanceof ItemPipeBlockEntity duct)duct.serverTick();
        };
    }
}
