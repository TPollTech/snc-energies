package com.snc.energies.block;

import com.snc.energies.blockentity.MercadaoShelfBlockEntity;
import com.snc.energies.economy.MercadaoCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The Mercadão counter shelf: a glass display counter facing the aisle. One
 * class, four fixed shelf variants (beverages, seeds, deli, the strong
 * counter) — mirrors the INDUSTRY map pattern. The block entity holds the
 * daily stock of its shelf line; the renderer floats the goods on the inner
 * shelf, behind the front glass. Right-click opens the purchase menu; the
 * block itself never debits money.
 */
public class MercadaoShelfBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<MercadaoShelfVariant> VARIANT = EnumProperty.create("shelf",
        MercadaoShelfVariant.class);

    /**
     * Display-front collision: the counter hugs the wall behind it, leaving
     * the aisle side walkable. The model's north face is the display side;
     * the blockstate rotates the model toward the shelf's facing.
     */
    private static final VoxelShape NORTH = Block.box(0, 0, 14, 16, 16, 16);
    private static final VoxelShape EAST = Block.box(0, 0, 0, 2, 16, 16);
    private static final VoxelShape SOUTH = Block.box(0, 0, 0, 16, 16, 2);
    private static final VoxelShape WEST = Block.box(14, 0, 0, 16, 16, 16);

    public MercadaoShelfBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(VARIANT, MercadaoShelfVariant.BEVERAGES));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.setValue(FACING, mirror.mirror(state.getValue(FACING)));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            default -> EAST;
        };
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        MercadaoShelfBlockEntity shelf = new MercadaoShelfBlockEntity(pos, state);
        shelf.setShelf(state.getValue(VARIANT).shelfId());
        return shelf;
    }

    /** Opens the purchase menu of the Mercadão this shelf belongs to. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof MercadaoShelfBlockEntity shelf) {
            if (!level.isClientSide()) {
                player.openMenu(shelf);
                if (shelf.npcAt() != null) {
                    level.playSound(null, shelf.npcAt(), SoundEvents.VILLAGER_AMBIENT,
                        SoundSource.NEUTRAL, 0.6F, 1.05F);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                     BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type,
            com.snc.energies.registry.SncBlockEntities.MERCADAO_SHELF,
            (world, pos, blockState, entity) -> entity.serverTick());
    }

    /** The four fixed shelf lines of the Mercadão (additive-only ids). */
    public enum MercadaoShelfVariant implements StringRepresentable {
        BEVERAGES(MercadaoCatalog.SHELF_BEVERAGES, "bebidas"),
        SEEDS(MercadaoCatalog.SHELF_SEEDS, "sementes"),
        DELI(MercadaoCatalog.SHELF_DELI, "frios"),
        PREMIUM(MercadaoCatalog.SHELF_PREMIUM, "balcao_forte");

        private final String shelfId;
        private final String name;

        MercadaoShelfVariant(String shelfId, String name) {
            this.shelfId = shelfId;
            this.name = name;
        }

        public String shelfId() {
            return shelfId;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
