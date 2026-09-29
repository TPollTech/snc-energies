package com.snc.energies.client.vehicle;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Snapshot of synchronized harvester state, independent of the live entity. */
public final class HarvesterRenderState extends EntityRenderState implements VehicleVisualState {
    public float vehicleYaw;
    public float vehicleSteering;
    public float vehicleWheelRotation;
    public boolean vehicleWorking;
    public boolean vehicleRaised;

    @Override public float yaw() { return vehicleYaw; }
    @Override public float steering() { return vehicleSteering; }
    @Override public float wheelRotation() { return vehicleWheelRotation; }
    @Override public boolean working() { return vehicleWorking; }
    @Override public boolean raised() { return vehicleRaised; }
    @Override public int light() { return lightCoords; }
    @Override public int outlineColor() { return outlineColor; }
}
