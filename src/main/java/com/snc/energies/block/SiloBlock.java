package com.snc.energies.block;

import com.snc.energies.blockentity.SiloBlockEntity;
import com.snc.energies.registry.SiloSize;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Agroindustrial bulk storage: a 2x2x3 grain bin (twelve cells, one shared
 * inventory on the controller). Cells are the anchor-local XZ ring over three
 * levels, so the silo has no orientation. Placement rules and teardown mirror
 * IndustrialBlock.
 */
public final class SiloBlock extends Block implements EntityBlock {
    public static final IntegerProperty PART=IntegerProperty.create("part",0,SiloSize.CELLS-1);

    public SiloBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(PART,0));
    }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block,BlockState> builder) { builder.add(PART); }

    /** Anchor-local cell layout: full 2x2 ring on each of the three levels. */
    public static BlockPos cell(BlockPos anchor,int part) {
        int level=part/SiloSize.RING;
        int ring=part%SiloSize.RING;
        int x=(ring%2)*1;
        int z=(ring/2)*1;
        return anchor.offset(x,level,z);
    }

    public static int partOf(BlockPos pos,BlockPos anchor) {
        int x=pos.getX()-anchor.getX(),y=pos.getY()-anchor.getY(),z=pos.getZ()-anchor.getZ();
        if(x<0||x>1||z<0||z>1||y<0||y>=SiloSize.LEVELS) return -1;
        return y*SiloSize.RING+z*2+x;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return canPlace(context.getLevel(),context.getClickedPos()) ? defaultBlockState() : null;
    }

    public boolean canPlace(Level level,BlockPos anchor) {
        for(int i=1;i<SiloSize.CELLS;i++) {
            BlockPos target=cell(anchor,i);
            if(level.isOutsideBuildHeight(target)||!level.hasChunkAt(target)||!level.getWorldBorder().isWithinBounds(target)
                ||!level.getBlockState(target).canBeReplaced()) return false;
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level,BlockPos pos,BlockState state,LivingEntity player,ItemStack stack) {
        if(level.isClientSide()) return;
        if(!canPlace(level,pos)) { level.destroyBlock(pos,true); return; }
        for(int i=1;i<SiloSize.CELLS;i++) level.setBlock(cell(pos,i),state.setValue(PART,i),3);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos,BlockState state) {
        return state.getValue(PART)==0 ? new SiloBlockEntity(pos,state) : null;
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level,BlockState state,BlockEntityType<T> type) {
        return null; // pure storage; no ticking
    }

    @Override
    protected VoxelShape getShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return NativeMachineShapes.shape("silo",state.getValue(PART),Direction.NORTH);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state,BlockGetter level,BlockPos pos,CollisionContext context) {
        return getShape(state,level,pos,context);
    }

    /** The controller answers for the whole structure. */
    public static SiloBlockEntity controller(BlockGetter level,BlockPos pos,BlockState state) {
        if(!(state.getBlock() instanceof SiloBlock)) return null;
        int part=state.getValue(PART);
        if(part>=SiloSize.CELLS) return null;
        BlockPos delta=cell(BlockPos.ZERO,part);
        BlockPos anchor=pos.offset(-delta.getX(),-delta.getY(),-delta.getZ());
        if(level.getBlockState(anchor).getBlock() instanceof SiloBlock
            &&level.getBlockState(anchor).getValue(PART)==0
            &&level.getBlockEntity(anchor) instanceof SiloBlockEntity silo) return silo;
        return null;
    }

    @Override
    protected MenuProvider getMenuProvider(BlockState state,Level level,BlockPos pos) {
        return controller(level,pos,state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state,Level level,BlockPos pos,Player player,BlockHitResult hit) {
        var silo=controller(level,pos,state);
        if(silo==null) return InteractionResult.PASS;
        if(!level.isClientSide()) player.openMenu(silo);
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState playerWillDestroy(Level level,BlockPos pos,BlockState state,Player player) {
        if(!level.isClientSide()&&player.isCreative()&&state.getValue(PART)!=0&&controller(level,pos,state)!=null)
            level.destroyBlock(anchor(pos,state),false);
        return super.playerWillDestroy(level,pos,state,player);
    }

    private static BlockPos anchor(BlockPos pos,BlockState state) {
        BlockPos delta=cell(BlockPos.ZERO,state.getValue(PART));
        return pos.offset(-delta.getX(),-delta.getY(),-delta.getZ());
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state,ServerLevel level,BlockPos pos,boolean moved) {
        super.affectNeighborsAfterRemoval(state,level,pos,moved);
        if(level.getBlockState(pos).is(this)) return;
        if(state.getValue(PART)!=0) {
            if(controller(level,pos,state)!=null) level.destroyBlock(anchor(pos,state),true);
            return;
        }
        for(int i=1;i<SiloSize.CELLS;i++) {
            BlockPos target=cell(pos,i);
            BlockState there=level.getBlockState(target);
            if(there.getBlock()==this&&there.getValue(PART)==i) level.removeBlock(target,false);
        }
    }
}
