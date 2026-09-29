package com.snc.energies.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;

/**
 * The painted "Mercadão" sign above the front door: three blocks
 * (left/center/right) render as one band thanks to the shared 16×8 px top
 * texture split across the part property. Structure-only block — no item,
 * creative tab or loot table entry; the sign survives with the building.
 */
public class MercadaoSignBlock extends HorizontalDirectionalBlock {
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /** Wall-solid: the sign block stands in for the wall cell it decorates. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 16, 16);

    public MercadaoSignBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
            .setValue(FACING, net.minecraft.core.Direction.SOUTH)
            .setValue(PART, Part.CENTER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        // Signs hang with their face toward the reader (opposite the wall).
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** Left/center/right thirds of the painted band. */
    public enum Part implements net.minecraft.util.StringRepresentable {
        LEFT("left"), CENTER("center"), RIGHT("right");

        private final String name;

        Part(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }
}
