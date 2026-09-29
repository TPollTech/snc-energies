package com.snc.energies.client.vehicle;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Per-vehicle visual snapshot shared by every SNC rig: the golden model
 * articulations read these mirrors instead of touching live entities.
 */
public interface VehicleVisualState {
    /** Facing yaw in degrees (entity convention). */
    float yaw();

    /** Steering wheel / knuckle angle in degrees. */
    float steering();

    /** Signed rear-wheel rotation in degrees. */
    float wheelRotation();

    /** Engine/mechanism running flag (wheels and driven parts spin). */
    boolean working();

    /** Implement raised off the ground (planter hitch / harvester header). */
    boolean raised();

    /** Packed light at the entity (mirror of the render state's light coords). */
    int light();

    /** Packed outline color for the hover highlight (0 = none). */
    int outlineColor();
}
