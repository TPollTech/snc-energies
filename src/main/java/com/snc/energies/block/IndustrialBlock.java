package com.snc.energies.block;

import com.snc.energies.blockentity.IndustrialBlockEntity;
import com.snc.energies.registry.IndustryKind;
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

/** Native cell models with a single, stable controller per installation. */
public final class IndustrialBlock extends MachineBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,35);
    public final IndustryKind kind;
    public IndustrialBlock(Properties properties, IndustryKind kind) {
        super(properties);this.kind=kind;registerDefaultState(defaultBlockState().setValue(PART,0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { super.createBlockStateDefinition(builder);builder.add(PART); }
    public BlockPos cell(BlockPos anchor, Direction facing, int part) {
        return anchor.relative(facing.getClockWise(),part%kind.width)
            .relative(facing.getOpposite(),(part/kind.width)%kind.depth).above(part/(kind.width*kind.depth));
    }
    public BlockPos anchor(BlockPos pos,BlockState state) {
        BlockPos delta=cell(BlockPos.ZERO,state.getValue(FACING),state.getValue(PART));
        return pos.offset(-delta.getX(),-delta.getY(),-delta.getZ());
    }
    public boolean owns(BlockState state,BlockState owner,int part) {
        return state.is(this) && state.getValue(PART)==part && state.getValue(FACING)==owner.getValue(FACING);
    }
    public boolean canPlace(Level level,BlockPos pos,Direction facing) {
        for(int i=1;i<kind.cells();i++) {
            BlockPos target=cell(pos,facing,i);
            if(level.isOutsideBuildHeight(target)||!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(target)
                || !level.getBlockState(target).canBeReplaced()) return false;
        }
        return true;
    }
    public static IndustrialBlockEntity controller(BlockGetter level,BlockPos pos,BlockState state) {
        if(!(state.getBlock() instanceof IndustrialBlock block) || state.getValue(PART)>=block.kind.cells()) return null;
        BlockPos anchor=block.anchor(pos,state);
        if(!block.owns(level.getBlockState(anchor),state,0)) return null;
        return level.getBlockEntity(anchor) instanceof IndustrialBlockEntity machine ? machine : null;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state=super.getStateForPlacement(context);
        return canPlace(context.getLevel(),context.getClickedPos(),state.getValue(FACING)) ? state : null;
    }
    @Override public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack) {
        if(level.isClientSide()) return;
        if(!canPlace(level,pos,state.getValue(FACING))) {level.destroyBlock(pos,true);return;}
        for(int i=1;i<kind.cells();i++) level.setBlock(cell(pos,state.getValue(FACING),i),state.setValue(PART,i),3);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos,BlockState state) { return state.getValue(PART)==0 ? new IndustrialBlockEntity(pos,state) : null; }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){
        return NativeMachineShapes.shape(kind.id,state.getValue(PART),state.getValue(FACING));
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,net.minecraft.world.phys.shapes.CollisionContext context){return getShape(state,level,pos,context);}
    @Override protected MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos) { return controller(level,pos,state); }
    @Override protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var machine=controller(level,pos,state);
        if(machine==null)return InteractionResult.PASS;
        if(!level.isClientSide())player.openMenu(machine);
        return InteractionResult.SUCCESS;
    }
    @Override public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide()&&player.isCreative()&&state.getValue(PART)!=0&&controller(level,pos,state)!=null)
            level.destroyBlock(anchor(pos,state),false);
        return super.playerWillDestroy(level,pos,state,player);
    }
    @Override protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved) {
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        if(level.getBlockState(pos).is(this))return;
        if(state.getValue(PART)!=0){if(controller(level,pos,state)!=null)level.destroyBlock(anchor(pos,state),true);return;}
        for(int i=1;i<kind.cells();i++){BlockPos target=cell(pos,state.getValue(FACING),i);if(owns(level.getBlockState(target),state,i))level.removeBlock(target,false);}
    }
}
