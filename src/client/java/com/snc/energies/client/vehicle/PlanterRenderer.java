package com.snc.energies.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snc.energies.entity.PlanterEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Renders the approved SNC 75-P implement rig in Minecraft's native entity pass. */
public final class PlanterRenderer extends EntityRenderer<PlanterEntity, PlanterRenderState> {
    private final VehicleRig rig;

    public PlanterRenderer(EntityRendererProvider.Context context) {
        super(context);
        rig = VehicleRig.load(Identifier.fromNamespaceAndPath("snc_energies", "vehicle/planter-model.json"),
            context.getResourceManager());
        shadowRadius = 1.8F;
        shadowStrength = 0.7F;
    }

    @Override
    public PlanterRenderState createRenderState() {
        return new PlanterRenderState();
    }

    @Override
    public void extractRenderState(PlanterEntity entity, PlanterRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.vehicleYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.vehicleWheelRotation = entity.getWheelRotation();
        state.vehicleWorking = entity.isWorking();
        state.vehicleRaised = entity.isRaised();
    }

    @Override
    protected AABB getBoundingBoxForCulling(PlanterEntity entity, float partialTick) {
        return entity.getBoundingBox().inflate(1.5, 0.8, 2.5);
    }

    @Override
    public void submit(PlanterRenderState state, PoseStack poses, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.isInvisible) {
            poses.pushPose();
            poses.rotateDegrees(Axis.YP, 180.0F - state.yaw());
            rig.submit(state, poses, collector);
            poses.popPose();
        }
        super.submit(state, poses, collector, camera);
    }
}
