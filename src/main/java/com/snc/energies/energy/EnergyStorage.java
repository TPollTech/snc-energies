package com.snc.energies.energy;

/**
 * Minimal energy capability used across the SNC grid. All amounts are in "SNC energy units" (E),
 * transfer rates in E per tick.
 */
public interface EnergyStorage {
	/** @return currently stored energy. */
	long getEnergy();

	/** @return maximum storable energy. */
	long getCapacity();

	/**
	 * Insert energy, returning the amount actually accepted.
	 *
	 * @param amount maximum amount to insert
	 * @param simulate when true the insertion is not persisted
	 */
	long insert(long amount, boolean simulate);

	/**
	 * Extract energy, returning the amount actually removed.
	 *
	 * @param amount maximum amount to extract
	 * @param simulate when true the extraction is not persisted
	 */
	long extract(long amount, boolean simulate);

	/** @return true when nothing can be inserted (full). */
	default boolean isFull() {
		return getEnergy() >= getCapacity();
	}

	/** @return true when no energy is stored. */
	default boolean isEmpty() {
		return getEnergy() <= 0;
	}
}
