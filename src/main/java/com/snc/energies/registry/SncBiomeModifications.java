package com.snc.energies.world;

import com.snc.energies.SncEnergies;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/**
 * Registers the Voltaite ore feature into world generation. Only affects chunks
 * generated after this point (standard behaviour for added features).
 */
public final class SncBiomeModifications {
	private static final ResourceKey<PlacedFeature> VOLTAITE_ORE =
			ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, SncEnergies.id("voltaite_ore"));

	private SncBiomeModifications() {
	}

	public static void bootstrap() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
            ResourceKey.create(net.minecraft.core.registries.Registries.PLACED_FEATURE, SncEnergies.id("tin_ore")));
		BiomeModifications.addFeature(
				BiomeSelectors.foundInOverworld(),
				GenerationStep.Decoration.UNDERGROUND_ORES,
				VOLTAITE_ORE);
	}
}
