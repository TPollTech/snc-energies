package com.snc.energies.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snc.energies.entity.HarvesterEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** Renders the approved SNC 90 rig in Minecraft's native entity pass. */
public final class HarvesterRenderer extends EntityRenderer<HarvesterEntity, HarvesterRenderState> {
    private final VehicleRig rig;

    public HarvesterRenderer(EntityRendererProvider.Context context) {
        super(context);
        rig = VehicleRig.load(Identifier.fromNamespaceAndPath("snc_energies", "vehicle/harvester-model.json"),
            context.getResourceManager());
        shadowRadius = 2.2F;
        shadowStrength = 0.8F;
    }

    @Override
    public HarvesterRenderState createRenderState() {
        return new HarvesterRenderState();
    }

    @Override
    public void extractRenderState(HarvesterEntity entity, HarvesterRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.vehicleYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.vehicleSteering = entity.getSteering();
        state.vehicleWheelRotation = entity.getWheelRotation();
        state.vehicleWorking = entity.isWorking();
        state.vehicleRaised = !entity.isHeaderLowered();
    }

    @Override
    protected AABB getBoundingBoxForCulling(HarvesterEntity entity, float partialTick) {
        // The header reaches 4 blocks ahead and the auger folds above the tank.
        return entity.getBoundingBox().inflate(4.5, 1.5, 4.5);
    }

    @Override
    public void submit(HarvesterRenderState state, PoseStack poses, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.isInvisible) {
            poses.pushPose();
            // Golden coordinates: +Y up, -Z forward. Entity yaw 0 faces +Z.
            poses.rotateDegrees(Axis.YP, 180.0F - state.yaw());
            rig.submit(state, poses, collector);
            poses.popPose();
        }
        super.submit(state, poses, collector, camera);
    }
}
