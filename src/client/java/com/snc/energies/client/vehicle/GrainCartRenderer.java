package com.snc.energies.client.vehicle;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.snc.energies.entity.GrainCartEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Vector3f;

/**
 * Renders the SNC 90-C grain cart in Minecraft's native entity pass as a
 * hand-authored cuboid rig (same visual language as the golden vehicles):
 * steel chassis, canvas hopper with the SNC decal, drawbar hitch and four
 * rotating wheels. The hopper fill line rises with cargo.
 */
public final class GrainCartRenderer extends EntityRenderer<GrainCartEntity, GrainCartRenderState> {
    private static final Identifier ENAMEL = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/enamel_orange.png");
    private static final Identifier DARK = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/enamel_dark.png");
    private static final Identifier STEEL = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/steel.png");
    private static final Identifier RUBBER = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/rubber.png");
    private static final Identifier TANK = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/seed_tank.png");
    private static final Identifier DECAL = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/decal_snc.png");
    private static final Identifier RIM = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/planter/rim.png");

    public GrainCartRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 1.4F;
        shadowStrength = 0.8F;
    }

    @Override
    public GrainCartRenderState createRenderState() {
        return new GrainCartRenderState();
    }

    @Override
    public void extractRenderState(GrainCartEntity entity, GrainCartRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.vehicleYaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        state.vehicleWheelRotation = entity.getWheelRotation();
        state.cargoFilled = cargoFraction(entity);
    }

    private static float cargoFraction(GrainCartEntity cart) {
        int total = 0;
        for (int slot = 0; slot < cart.getContainerSize(); slot++) total += cart.getItem(slot).getCount();
        return Math.clamp(total / 320.0F, 0.0F, 1.0F);
    }

    @Override
    protected AABB getBoundingBoxForCulling(GrainCartEntity entity, float partialTick) {
        return entity.getBoundingBox().inflate(1.0, 0.5, 1.0);
    }

    @Override
    public void submit(GrainCartRenderState state, PoseStack poses, SubmitNodeCollector collector,
                       CameraRenderState camera) {
        if (!state.isInvisible) {
            poses.pushPose();
            poses.rotateDegrees(Axis.YP, 180.0F - state.yaw());
            submitBody(state, poses, collector);
            submitWheels(state, poses, collector);
            poses.popPose();
        }
        super.submit(state, poses, collector, camera);
    }

    /** Pixels: +Y up, -Z forward, 16 units per block. Origin at the drawbar eye. */
    private static void submitBody(GrainCartRenderState state, PoseStack poses, SubmitNodeCollector collector) {
        poses.pushPose();
        poses.translate(0.0, 12.0 / 16.0, -20.0 / 16.0);
        // Drawbar from the hitch eye to the chassis.
        box(poses, collector, STEEL, -1, -1, -12, 1, 1, 14);
        // Chassis frame.
        box(poses, collector, DARK, -20, -2, 4, 20, 2, 22);
        // Hopper walls tapering wider at the top (canvas look with the SNC decal).
        box(poses, collector, TANK, -17, 0, -9, 17, 14, 9);
        box(poses, collector, ENAMEL, -18, 14, -10, 18, 16, 10);
        // Decal strip on both long sides.
        box(poses, collector, DECAL, -17.2f, 5, 8.8f, 17.2f, 9, 9.2f);
        box(poses, collector, DECAL, -17.2f, 5, -9.2f, 17.2f, 9, -8.8f);
        // Grain fill visible above the walls once cargo starts piling up.
        float fill = state.cargoFilled;
        if (fill > 0.05F) {
            float height = 2.0F + fill * 11.0F;
            box(poses, collector, TANK, -16, 14, -8, 16, 14 + height, 8);
        }
        poses.popPose();
    }

    private static void submitWheels(GrainCartRenderState state, PoseStack poses, SubmitNodeCollector collector) {
        float spin = state.wheelRotation();
        for (int side = -1; side <= 1; side += 2) {
            for (int axle = 0; axle < 2; axle++) {
                poses.pushPose();
                poses.translate(side * 20.0 / 16.0, 6.0 / 16.0, (6 + axle * 12) / 16.0);
                poses.rotateDegrees(Axis.XP, spin);
                // Tire + rim: two crossing boxes read as a spoked wheel at this scale.
                box(poses, collector, RUBBER, -2.5f, -5.5f, -5.5f, 2.5f, 5.5f, 5.5f);
                box(poses, collector, RIM, -2.9f, -3.2f, -3.2f, 2.9f, 3.2f, 3.2f);
                poses.popPose();
            }
        }
    }

    /** Full-texture cuboid, matching the VehicleRig face winding. */
    private static void box(PoseStack poses, SubmitNodeCollector collector, Identifier texture,
                            double x0, double y0, double z0, double x1, double y1, double z1) {
        Vector3f[] corners = {
            new Vector3f((float) x0, (float) y0, (float) z0), new Vector3f((float) x1, (float) y0, (float) z0),
            new Vector3f((float) x1, (float) y0, (float) z1), new Vector3f((float) x0, (float) y0, (float) z1),
            new Vector3f((float) x0, (float) y1, (float) z0), new Vector3f((float) x1, (float) y1, (float) z0),
            new Vector3f((float) x1, (float) y1, (float) z1), new Vector3f((float) x0, (float) y1, (float) z1),
        };
        int[][] faces = {
            {5, 6, 2, 1}, {0, 3, 7, 4}, {4, 7, 6, 5}, {1, 2, 3, 0}, {7, 3, 2, 6}, {0, 1, 5, 4},
        };
        for (int[] face : faces) {
            Vector3f[] quad = new Vector3f[4];
            for (int i = 0; i < 4; i++) quad[i] = corners[face[i]];
            collector.submitCustomGeometry(poses, RenderTypes.entitySolid(texture), (pose, buffer) -> {
                float[][] uvs = {{0, 1}, {1, 1}, {1, 0}, {0, 0}};
                for (int i = 0; i < 4; i++) {
                    buffer.addVertex(pose, quad[i].x() / 16.0F, quad[i].y() / 16.0F, quad[i].z() / 16.0F)
                        .setColor(-1).setUv(uvs[i][0], uvs[i][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(0xF000F0)
                        .setNormal(pose, 0, 1, 0);
                }
            });
        }
    }
}
