package com.snc.energies.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Client registers the opener, keeping client classes out of the server source set. */
public final class FieldGuideItem extends Item {
    private static Runnable opener = () -> {};
    public FieldGuideItem(Properties properties) { super(properties); }
    public static void registerOpener(Runnable action) { opener = action; }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) opener.run();
        return InteractionResult.SUCCESS;
    }
}
