package com.snc.energies.block;

import com.snc.energies.blockentity.WoodStoveBlockEntity;
import com.snc.energies.registry.SncBlocks;
import com.snc.energies.registry.SncItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import static com.snc.energies.block.MachineBlock.FACING;
import static com.snc.energies.block.CoalGeneratorBlock.LIT;

/** Structural block, never a second inventory or generator. */
public final class WoodStovePartBlock extends Block {
    public static final IntegerProperty PART=IntegerProperty.create("part",1,7);
    public WoodStovePartBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING,Direction.NORTH).setValue(LIT,false).setValue(PART,1));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(FACING,LIT,PART); }
    public static BlockPos controllerPos(BlockPos pos, BlockState state) {
        BlockPos delta=WoodStoveBlock.cell(BlockPos.ZERO,state.getValue(FACING),state.getValue(PART));
        return pos.offset(-delta.getX(),-delta.getY(),-delta.getZ());
    }
    public static WoodStoveBlockEntity controller(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos anchor=controllerPos(pos,state);
        BlockState owner=level.getBlockState(anchor);
        if (!owner.is(SncBlocks.WOOD_STOVE) || !owner.getValue(WoodStoveBlock.ASSEMBLED)
                || owner.getValue(FACING)!=state.getValue(FACING)) return null;
        return level.getBlockEntity(anchor) instanceof WoodStoveBlockEntity stove ? stove : null;
    }
    @Override protected MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos) { return controller(level,pos,state); }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        WoodStoveBlockEntity stove=controller(level,pos,state);
        if (stove==null) return InteractionResult.PASS;
        if (!level.isClientSide()) player.openMenu(stove);
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && player.isCreative() && controller(level,pos,state)!=null)
            level.destroyBlock(controllerPos(pos,state),false);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        if (!level.getBlockState(pos).is(this) && controller(level,pos,state)!=null)
            level.destroyBlock(controllerPos(pos,state),true);
    }
    @Override protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        if(state.getValue(PART)<=5) return Shapes.block();
        return Shapes.box(2/16.0,0,2/16.0,14/16.0,1,14/16.0);
    }
    @Override protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) { return getShape(state,level,pos,context); }
    @Override protected ItemStack getCloneItemStack(LevelReader level,BlockPos pos,BlockState state,boolean includeData) { return new ItemStack(SncItems.WOOD_STOVE); }
}
