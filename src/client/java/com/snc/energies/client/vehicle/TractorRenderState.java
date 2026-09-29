package com.snc.energies.client.vehicle;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Snapshot of synchronized vehicle state, independent of the live entity. */
public final class TractorRenderState extends EntityRenderState implements VehicleVisualState {
    public float yaw;
    public float steering;
    public float wheelRotation;
    public boolean planter;
    public boolean working;
    /** True when the three-point hitch has the implement lifted off the ground. */
    public boolean planterRaised;

    @Override public float yaw() { return yaw; }
    @Override public float steering() { return steering; }
    @Override public float wheelRotation() { return wheelRotation; }
    @Override public boolean working() { return working; }
    @Override public boolean raised() { return planterRaised; }
    @Override public int light() { return lightCoords; }
    @Override public int outlineColor() { return outlineColor; }
}
