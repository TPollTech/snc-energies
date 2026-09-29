package com.snc.energies.item;

import com.snc.energies.block.IndustrialBlock;
import com.snc.energies.blockentity.IndustrialBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;


/**
 * The side configuration tool. Use on an industrial cell to cycle that face
 * between both, input and output; sneak-use toggles inverted redstone
 * (runs only while unpowered). The face is the clicked one, mapped through
 * the machine's facing so it stays stable when the block is rotated.
 */
public final class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties properties) { super(properties); }

    @Override public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (state.getBlock() instanceof IndustrialBlock) {
            BlockEntity entity = level.getBlockEntity(pos);
            if (entity instanceof IndustrialBlockEntity machine) {
                if (!level.isClientSide()) {
                    Direction face = context.getClickedFace();
                    Direction facing = state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                        ? state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)
                        : Direction.NORTH;
                    if (context.isSecondaryUseActive()) {
                        int mode = machine.cycleRunMode();
                        machine.setChanged();
                        player.sendOverlayMessage(Component.translatable(
                            mode == IndustrialBlockEntity.RUN_ALWAYS ? "gui.snc_energies.screwdriver.run_always"
                            : mode == IndustrialBlockEntity.RUN_WITH_REDSTONE ? "gui.snc_energies.screwdriver.run_with"
                            : "gui.snc_energies.screwdriver.run_not"));
                    } else {
                        Direction side = face.getAxis().isVertical() ? face : face == facing ? Direction.NORTH
                            : face == facing.getOpposite() ? Direction.SOUTH
                            : face == facing.getClockWise() ? Direction.EAST : Direction.WEST;
                        int mode = machine.cycleFaceMode(side);
                        machine.setChanged();
                        player.sendOverlayMessage(Component.translatable(
                            "gui.snc_energies.screwdriver.face",
                            Component.translatable("gui.snc_energies.screwdriver.side." + (side == Direction.NORTH ? "front"
                                : side == Direction.SOUTH ? "back" : side == Direction.EAST ? "right" : "left")),
                            Component.translatable("gui.snc_energies.screwdriver.mode." + (mode == IndustrialBlockEntity.FACE_BOTH ? "both"
                                : mode == IndustrialBlockEntity.FACE_INPUT ? "input" : "output"))));
                    }
                    level.playSound(null, pos, SoundEvents.WOODEN_TRAPDOOR_CLOSE, SoundSource.BLOCKS, .5f, 1.4f);
                }
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display, java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("gui.snc_energies.screwdriver.tooltip"));
    }
}
