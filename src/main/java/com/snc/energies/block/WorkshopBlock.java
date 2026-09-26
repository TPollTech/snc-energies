package com.snc.energies.block;

import com.snc.energies.blockentity.WorkshopBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Two wide, one deep, two high. Only cell zero owns inventory and ticks. */
public final class WorkshopBlock extends MachineBlock {
    public static final IntegerProperty PART = IntegerProperty.create("part", 0, 3);
    public WorkshopBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PART);
    }
    public static BlockPos cell(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing.getClockWise(), part % 2).above(part / 2);
    }
    public static BlockPos anchor(BlockPos pos, BlockState state) {
        var delta = cell(BlockPos.ZERO, state.getValue(FACING), state.getValue(PART));
        return pos.offset(-delta.getX(), -delta.getY(), -delta.getZ());
    }
    public static boolean owns(BlockState candidate, BlockState owner, int part) {
        return candidate.is(owner.getBlock()) && candidate.getValue(PART) == part
                && candidate.getValue(FACING) == owner.getValue(FACING);
    }
    public static WorkshopBlockEntity controller(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos anchor = anchor(pos, state);
        BlockState owner = level.getBlockState(anchor);
        if (!owns(owner, state, 0)) return null;
        return level.getBlockEntity(anchor) instanceof WorkshopBlockEntity machine ? machine : null;
    }
    public static boolean canPlace(Level level, BlockPos pos, Direction facing) {
        for (int i = 1; i < 4; i++) {
            var target = cell(pos, facing, i);
            if (!level.hasChunkAt(target) || level.isOutsideBuildHeight(target)
                    || !level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target).canBeReplaced()) return false;
        }
        return true;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return canPlace(context.getLevel(), context.getClickedPos(), state.getValue(FACING)) ? state : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;
        if (!canPlace(level, pos, state.getValue(FACING))) { level.destroyBlock(pos, true); return; }
        for (int i = 1; i < 4; i++) level.setBlock(cell(pos, state.getValue(FACING), i), state.setValue(PART, i), 3);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == 0 ? new WorkshopBlockEntity(pos, state) : null;
    }
    @Override protected MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) { return controller(level, pos, state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        var machine = controller(level, pos, state);
        if (machine == null) return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(machine);
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && state.getValue(PART) != 0 && controller(level, pos, state) != null)
            level.destroyBlock(anchor(pos, state), false);
        return super.playerWillDestroy(level, pos, state, player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state, level, pos, moved);
        if (level.getBlockState(pos).is(this)) return;
        if (state.getValue(PART) != 0) {
            if (controller(level, pos, state) != null) level.destroyBlock(anchor(pos, state), true);
            return;
        }
        for (int i = 1; i < 4; i++) {
            var target = cell(pos, state.getValue(FACING), i);
            if (owns(level.getBlockState(target), state, i)) level.removeBlock(target, false);
        }
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        String model=state.is(com.snc.energies.registry.SncBlocks.SEED_PRESS)?"seed_press":"grain_mill";
        return NativeMachineShapes.shape(model,state.getValue(PART),state.getValue(FACING));
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
}
