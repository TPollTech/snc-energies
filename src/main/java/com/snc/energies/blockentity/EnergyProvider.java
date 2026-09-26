package com.snc.energies.blockentity;

import com.snc.energies.energy.EnergyStorage;

import net.minecraft.core.Direction;

/**
 * Implemented by block entities that expose an SNC energy storage on their sides.
 */
public interface EnergyProvider {
	/**
	 * @param side the side energy is queried for (never null)
	 * @return the storage for that side, or null if not available
	 */
	EnergyStorage getEnergyStorage(Direction side);
}
