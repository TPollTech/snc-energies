package com.snc.energies;

import com.snc.energies.registry.SncBlockEntities;
import com.snc.energies.registry.SncBlocks;
import com.snc.energies.registry.SncItems;
import com.snc.energies.registry.SncMenus;
import com.snc.energies.registry.SncRecipes;

import com.snc.energies.world.SncBiomeModifications;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SNC Energies entrypoint. Registration order matters: blocks first,
 * then block entities, items, menus and recipes.
 */
public class SncEnergies implements ModInitializer {
	public static final String MOD_ID = "snc_energies";
	public static final Logger LOGGER = LoggerFactory.getLogger("SNC Energies");

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {

		SncBlocks.bootstrap();
		SncBlockEntities.bootstrap();
		SncItems.bootstrap();
		SncMenus.bootstrap();
		SncRecipes.bootstrap();
		SncBiomeModifications.bootstrap();
		com.snc.energies.compat.AdventuresCompatibility.bootstrap();
		LOGGER.info("SNC Energies initialized: the grid is live!");
	}
}
