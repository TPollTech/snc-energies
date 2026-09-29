package com.snc.energies.item;

import com.snc.energies.entity.GrainCartEntity;
import com.snc.energies.menu.GrainCartMenu;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Places the SNC 90-C grain cart in the world. Using the item on an existing
 * cart opens its tank panel instead of spawning another one.
 */
public class GrainCartItem extends Item {
    public GrainCartItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawn = clicked.relative(face);
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;

        for (GrainCartEntity cart : level.getEntitiesOfClass(GrainCartEntity.class,
            new AABB(spawn).inflate(2.0))) {
            if (!level.isClientSide() && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                serverPlayer.openMenu(new GrainCartMenu.Provider(cart));
            }
            return InteractionResult.SUCCESS;
        }

        BlockState below = level.getBlockState(clicked);
        if (below.isAir() || !level.getBlockState(spawn).isAir()
            || !level.getEntitiesOfClass(GrainCartEntity.class, new AABB(spawn)).isEmpty()) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide()) {
            GrainCartEntity cart = new GrainCartEntity(level,
                spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
            cart.setYRot(context.getHorizontalDirection().toYRot() + 180.0F);
            level.addFreshEntity(cart);
            level.playSound(null, spawn, SoundEvents.METAL_PLACE, SoundSource.BLOCKS, 0.8F, 0.9F);
            if (!player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("gui.snc_energies.grain_cart.tooltip"));
    }
}
