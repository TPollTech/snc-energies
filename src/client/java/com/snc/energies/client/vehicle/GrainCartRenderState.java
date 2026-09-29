package com.snc.energies.client.vehicle;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Snapshot of synchronized grain-cart state, independent of the live entity. */
public final class GrainCartRenderState extends EntityRenderState {
    public float vehicleYaw;
    public float vehicleWheelRotation;
    /** 0..1 hopper fill for the visible grain line. */
    public float cargoFilled;

    public float yaw() { return vehicleYaw; }
    public float wheelRotation() { return vehicleWheelRotation; }
}
