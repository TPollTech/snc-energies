package com.snc.energies.energy;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Simple internal buffer implementation used by machines, cables and the energy cube.
 * Persistence is handled by each block entity through {@link #save} / {@link #load}.
 */
public class SimpleEnergyStorage implements EnergyStorage {
	private final long capacity;
	private long energy;

	public SimpleEnergyStorage(long capacity) {
		this.capacity = capacity;
	}

	@Override
	public long getEnergy() {
		return energy;
	}

	@Override
	public long getCapacity() {
		return capacity;
	}

	@Override
	public long insert(long amount, boolean simulate) {
		if (amount <= 0) {
			return 0;
		}
		long accepted = Math.min(amount, capacity - energy);
		if (!simulate && accepted > 0) {
			energy += accepted;
		}
		return accepted;
	}

	@Override
	public long extract(long amount, boolean simulate) {
		if (amount <= 0) {
			return 0;
		}
		long removed = Math.min(amount, energy);
		if (!simulate && removed > 0) {
			energy -= removed;
		}
		return removed;
	}

	public void save(ValueOutput output) {
		output.putLong("Energy", energy);
		output.putLong("Capacity", capacity);
	}

	public void load(ValueInput input) {
		this.energy = Math.clamp(input.getLongOr("Energy", 0L), 0L, capacity);
	}
}
