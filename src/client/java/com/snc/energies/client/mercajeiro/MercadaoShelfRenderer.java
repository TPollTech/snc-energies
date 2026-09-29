package com.snc.energies.client.mercajeiro;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snc.energies.block.MercadaoShelfBlock;
import com.snc.energies.blockentity.MercadaoShelfBlockEntity;
import com.snc.energies.economy.MercadaoCatalog;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.Level;

/**
 * Shelf goods rendered in the glass display case: the first four offers of
 * the shelf line stand upright on the model's inner shelf (FIXED display
 * context), following the CampfireRenderer contract of the 26.3 submit
 * pipeline (ItemModelResolver + ItemStackRenderState). The display empties
 * with the day: every offer scales down with its remaining daily stock and
 * leaves the shelf entirely once sold out.
 */
public final class MercadaoShelfRenderer implements BlockEntityRenderer<MercadaoShelfBlockEntity, MercadaoShelfRenderer.State> {
    private static final int DISPLAYED = 4;
    /** Inner shelf surface of the display-counter model (y 10/16). */
    private static final float SHELF_Y = 10.0F / 16.0F;
    /** Two alternating sizes for a stocked-market look (full stock). */
    private static final float SCALE_LARGE = 0.375F;
    private static final float SCALE_SMALL = 0.325F;
    /** Sold-out items vanish; the smallest a nearly-empty display gets. */
    private static final float MIN_SIZE_FACTOR = 0.55F;

    private final ItemModelResolver itemModelResolver;

    public MercadaoShelfRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MercadaoShelfBlockEntity shelf, State state, float partialTick,
                                   net.minecraft.world.phys.Vec3 camera,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderState.extractBase(shelf, state, crumbling);
        state.facing = shelf.getBlockState().getValue(MercadaoShelfBlock.FACING);
        Level level = shelf.getLevel();
        List<MercadaoCatalog.Offer> offers = MercadaoCatalog.byShelf(shelf.shelf());
        state.items.clear();
        state.fractions.clear();
        for (int i = 0; i < DISPLAYED; i++) {
            ItemStackRenderState itemState = new ItemStackRenderState();
            float fraction = 0.0F;
            if (level != null && i < offers.size()) {
                MercadaoCatalog.Offer offer = offers.get(i);
                // remaining() is a read-only mirror here (it may restock-and
                // -setChanged on the server tick, never from the render thread).
                int remaining = shelf.remainingReadonly(offer);
                fraction = Math.clamp(remaining / (float) Math.max(1, offer.dailyStock()), 0.0F, 1.0F);
                if (remaining > 0) {
                    itemModelResolver.updateForTopItem(itemState, offer.stack(),
                        ItemDisplayContext.FIXED, level, null, i);
                }
            }
            state.items.add(itemState);
            state.fractions.add(fraction);
        }
    }

    @Override
    public void submit(State state, PoseStack poses, SubmitNodeCollector collector, CameraRenderState camera) {
        for (int i = 0; i < state.items.size(); i++) {
            ItemStackRenderState item = state.items.get(i);
            float fraction = state.fractions.get(i);
            if (item.isEmpty() || fraction <= 0.0F) continue;
            // The display empties with the day: full stock keeps the base
            // size (alternating large/small), sold-down offers shrink.
            float base = (i % 2 == 0) ? SCALE_LARGE : SCALE_SMALL;
            float scale = base * (MIN_SIZE_FACTOR + (1.0F - MIN_SIZE_FACTOR) * fraction);
            poses.pushPose();
            // Pivot at the counter center, then step along the display row.
            poses.translate(0.5, SHELF_Y + scale / 2.0F, 0.5);
            poses.rotateDegrees(Axis.YP, -state.facing.toYRot());
            poses.translate(-0.375F + i * 0.25F, 0, 0);
            // Stand the flat item upright, facing the glass and the aisle.
            poses.rotateDegrees(Axis.XP, 90.0F);
            poses.scale(scale, scale, scale);
            item.submit(poses, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poses.popPose();
        }
    }

    /** Per-shelf render snapshot. */
    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public final List<ItemStackRenderState> items = new ArrayList<>();
        /** Remaining daily stock per displayed offer, 0..1 (drives the size). */
        public final List<Float> fractions = new ArrayList<>();
    }
}
