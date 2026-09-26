package com.snc.energies.block;

import com.snc.energies.blockentity.WoodStoveBlockEntity;
import com.snc.energies.registry.SncBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** One controller plus seven structural cells. Old single-block stoves retain their state. */
public class WoodStoveBlock extends CoalGeneratorBlock {
    public static final BooleanProperty ASSEMBLED = BooleanProperty.create("assembled");
    public static final int[][] CELLS = {{0,0,0},{1,0,0},{2,0,0},{0,0,1},{1,0,1},{2,0,1},{2,1,1},{2,2,1}};
    public WoodStoveBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ASSEMBLED, false));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ASSEMBLED);
    }
    public static BlockPos cell(BlockPos anchor, Direction facing, int index) {
        int[] offset=CELLS[index];
        return anchor.relative(facing.getClockWise(),offset[0]).relative(facing.getOpposite(),offset[2]).above(offset[1]);
    }
    public static boolean canAssemble(Level level, BlockPos anchor, Direction facing) {
        for (int i=1;i<CELLS.length;i++) {
            BlockPos pos=cell(anchor,facing,i);
            if (level.isOutsideBuildHeight(pos) || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.hasChunkAt(pos) || !level.getBlockState(pos).canBeReplaced()) return false;
        }
        return true;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state=super.getStateForPlacement(context);
        return canAssemble(context.getLevel(),context.getClickedPos(),state.getValue(FACING))
                ? state.setValue(ASSEMBLED,true) : null;
    }
    @Override public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level,pos,state,placer,stack);
        if (!level.isClientSide() && state.getValue(ASSEMBLED)) assemble(level,pos,state);
    }
    public static void assemble(Level level, BlockPos pos, BlockState state) {
        if (!canAssemble(level,pos,state.getValue(FACING))) throw new IllegalStateException("Stove footprint is obstructed");
        for (int i=1;i<CELLS.length;i++) level.setBlock(cell(pos,state.getValue(FACING),i),
                SncBlocks.WOOD_STOVE_PART.defaultBlockState().setValue(FACING,state.getValue(FACING))
                    .setValue(LIT,state.getValue(LIT)).setValue(WoodStovePartBlock.PART,i),3);
    }
    public static boolean owns(BlockState part, BlockState controller, int index) {
        return part.is(SncBlocks.WOOD_STOVE_PART) && part.getValue(WoodStovePartBlock.PART)==index
                && part.getValue(FACING)==controller.getValue(FACING);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        if (!state.getValue(ASSEMBLED) || level.getBlockState(pos).is(this)) return;
        for (int i=1;i<CELLS.length;i++) {
            BlockPos partPos=cell(pos,state.getValue(FACING),i);
            if (owns(level.getBlockState(partPos),state,i)) level.removeBlock(partPos,false);
        }
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WoodStoveBlockEntity(pos,state); }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (state.getValue(ASSEMBLED)) return Shapes.block();
        VoxelShape chimney=switch(state.getValue(FACING)) {
            case SOUTH -> Shapes.box(1/16.0,0.75,0,6/16.0,1,5/16.0);
            case WEST -> Shapes.box(11/16.0,0.75,1/16.0,1,1,6/16.0);
            case EAST -> Shapes.box(0,0.75,10/16.0,5/16.0,1,15/16.0);
            default -> Shapes.box(10/16.0,0.75,11/16.0,15/16.0,1,1);
        };
        return Shapes.or(Shapes.box(0,0,0,1,0.75,1),chimney);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state,level,pos,context);
    }
}
