package com.snc.energies.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/** A passive duct: no per-pipe tick and no electric cable entity. */
public final class SteamPipeBlock extends CableBlock {
    public SteamPipeBlock(Properties properties) { super(properties); }
    @Override protected boolean connects(BlockState state) {
        return state.getBlock() instanceof SteamPipeBlock || state.getBlock() instanceof IndustrialBlock block
            && (block.kind.steamConsumer() || block.kind==com.snc.energies.registry.IndustryKind.BOILER);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state){return null;}
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type){return null;}
}
