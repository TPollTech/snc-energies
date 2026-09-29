package com.snc.energies.block;

import com.snc.energies.blockentity.MercadaoAnchorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Invisible, non-solid structure anchor one block above the shelf row: the
 * Mercadão's manager. Keeps one Mercajeiro alive at the counter (the anchor
 * block entity respawns him), mirroring the Adventures MarketSystem. No
 * collision, no drops, not in the creative tab; moved by structure NBT only.
 */
public class MercadaoAnchorBlock extends BaseEntityBlock {
    private static final VoxelShape NO_COLLISION = Block.box(0, 0, 0, 0, 0, 0);

    public MercadaoAnchorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return NO_COLLISION;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MercadaoAnchorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                     BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type,
            com.snc.energies.registry.SncBlockEntities.MERCADAO_ANCHOR,
            (world, pos, blockState, entity) -> entity.serverTick());
    }
}
