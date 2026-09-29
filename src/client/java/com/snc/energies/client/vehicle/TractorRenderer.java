package com.snc.energies.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snc.energies.entity.TractorEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Renders the complete approved SNC 75 rig in Minecraft's native entity pass. */
public final class TractorRenderer extends EntityRenderer<TractorEntity, TractorRenderState> {
    private final TractorRig rig;

    public TractorRenderer(EntityRendererProvider.Context context) {
        super(context);
        rig = TractorRig.load(context.getResourceManager());
        shadowRadius = 1.65F;
        shadowStrength = 0.8F;
    }

    @Override
    public TractorRenderState createRenderState() {
        return new TractorRenderState();
    }

    @Override
    public void extractRenderState(TractorEntity entity, TractorRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.steering = entity.getSteering();
        state.wheelRotation = entity.getWheelRotation();
        // The golden model always carries the planter; the lift pivot animates it.
        state.planter = true;
        state.working = entity.isWorking();
        state.planterRaised = entity.isPlanterRaised();
    }

    @Override
    protected AABB getBoundingBoxForCulling(TractorEntity entity, float partialTick) {
        // The implement reaches 3.6 blocks behind the entity origin and the
        // canopy is taller than the chassis collision box. Include both at every yaw.
        return entity.getBoundingBox().inflate(4.0, 1.2, 4.0);
    }

    @Override
    public void submit(TractorRenderState state, PoseStack poses, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.isInvisible) {
            poses.pushPose();
            // Golden coordinates: +Y up, -Z forward. Entity yaw 0 faces +Z.
            poses.rotateDegrees(Axis.YP, 180.0F - state.yaw);
            rig.submit(state, poses, collector);
            poses.popPose();
        }
        super.submit(state, poses, collector, camera);
    }
}
