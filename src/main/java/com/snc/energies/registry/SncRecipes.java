package com.snc.energies.registry;

import com.snc.energies.SncEnergies;

/**
 * Recipe bootstrap. Smelting uses vanilla recipes; crushing is a hard map
 * for v0.1.0 (see CrusherBlockEntity).
 */
public final class SncRecipes {
	private SncRecipes() {
	}

	public static void bootstrap() {
		SncEnergies.LOGGER.info("SNC Energies recipes ready");
	}
}
