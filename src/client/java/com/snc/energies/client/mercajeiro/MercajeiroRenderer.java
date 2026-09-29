package com.snc.energies.client.mercajeiro;

import com.snc.energies.SncEnergies;
import com.snc.energies.entity.MercajeiroEntity;

import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/**
 * Renders the Mercajeiro with the vanilla humanoid model and a dedicated
 * 64×64 player-style skin (the Gago pattern: standard humanoid rig, own
 * texture). The model layer reuses the vanilla humanoid mesh so proportions
 * match villagers/players exactly.
 */
public final class MercajeiroRenderer extends HumanoidMobRenderer<MercajeiroEntity, HumanoidRenderState, HumanoidModel<HumanoidRenderState>> {
    public static final ModelLayerLocation LAYER =
        new ModelLayerLocation(SncEnergies.id("mercajeiro"), "main");

    private static final Identifier TEXTURE = SncEnergies.id("textures/entity/mercajeiro.png");

    public MercajeiroRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(LAYER)), 0.5F);
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return TEXTURE;
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    /** Registers the layer definition (vanilla humanoid mesh). */
    public static void bootstrap() {
        ModelLayerRegistry.registerModelLayer(LAYER,
            () -> LayerDefinition.create(HumanoidModel.createMesh(
                new net.minecraft.client.model.geom.builders.CubeDeformation(0.0F), 0.0F), 64, 64));
    }
}
