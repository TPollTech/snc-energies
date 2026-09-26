package com.snc.energies.block;

import java.util.EnumMap;
import java.util.Map;
import com.snc.energies.blockentity.CableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;

/** Thin cable with six independently connected arms and matching selection/collision. */
public class CableBlock extends Block implements net.minecraft.world.level.block.EntityBlock {
    public static final Map<Direction, BooleanProperty> CONNECTIONS = new EnumMap<>(Direction.class);
    private static final VoxelShape[] SHAPES = new VoxelShape[64];
    static {
        for (Direction direction : Direction.values()) {
            CONNECTIONS.put(direction, BooleanProperty.create(direction.getSerializedName()));
        }
        for (int mask = 0; mask < 64; mask++) {
            VoxelShape shape = Shapes.box(6/16.0, 6/16.0, 6/16.0, 10/16.0, 10/16.0, 10/16.0);
            for (Direction direction : Direction.values()) {
                if ((mask & (1 << direction.ordinal())) == 0) continue;
                double x0=6, y0=6, z0=6, x1=10, y1=10, z1=10;
                switch (direction) {
                    case DOWN -> y0=0;
                    case UP -> y1=16;
                    case NORTH -> z0=0;
                    case SOUTH -> z1=16;
                    case WEST -> x0=0;
                    case EAST -> x1=16;
                }
                shape = Shapes.or(shape, Shapes.box(x0/16, y0/16, z0/16, x1/16, y1/16, z1/16));
            }
            SHAPES[mask] = shape;
        }
    }
    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = stateDefinition.any();
        for (BooleanProperty property : CONNECTIONS.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        for (BooleanProperty property : CONNECTIONS.values()) builder.add(property);
    }
    protected boolean connects(BlockState state) {
        if (state.getBlock() instanceof SteamPipeBlock || state.getBlock() instanceof WorkshopBlock) return false;
        if (state.getBlock() instanceof IndustrialBlock machine) return machine.kind.electric() || machine.kind == com.snc.energies.registry.IndustryKind.TURBINE;
        return state.getBlock() instanceof CableBlock || state.getBlock() instanceof MachineBlock
                || state.getBlock() instanceof EnergyCubeBlock || state.getBlock() instanceof WoodStovePartBlock;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(direction), connects(context.getLevel().getBlockState(context.getClickedPos().relative(direction))));
        }
        return state;
    }
    @Override protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), connects(neighborState));
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction direction : Direction.values()) rotated = rotated.setValue(CONNECTIONS.get(rotation.rotate(direction)), state.getValue(CONNECTIONS.get(direction)));
        return rotated;
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction direction : Direction.values()) mirrored = mirrored.setValue(CONNECTIONS.get(mirror.mirror(direction)), state.getValue(CONNECTIONS.get(direction)));
        return mirrored;
    }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int mask=0;
        for (Direction direction : Direction.values()) if (state.getValue(CONNECTIONS.get(direction))) mask |= 1 << direction.ordinal();
        return SHAPES[mask];
    }
    @Override protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return getShape(state, level, pos, context);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CableBlockEntity(pos, state);
    }
    @Override public <T extends BlockEntity> net.minecraft.world.level.block.entity.BlockEntityTicker<T> getTicker(
            Level level, BlockState state, net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
        return level.isClientSide() ? null : (world, pos, blockState, entity) -> {
            if (entity instanceof CableBlockEntity cable) cable.serverTick();
        };
    }
}
