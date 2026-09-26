package com.snc.energies.compat;

import com.snc.energies.SncEnergies;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Optional integration using registry tags; never loads companion classes. */
public final class AdventuresCompatibility {
    public static final TagKey<Item> BIOMASS_FUEL = TagKey.create(
            Registries.ITEM, SncEnergies.id("biomass_fuel"));
    public static final int BIOMASS_BURN_TICKS = 100;

    private AdventuresCompatibility() {}

    public static boolean isLoaded() {
        return FabricLoader.getInstance().isModLoaded("intoxicantes");
    }

    public static int biomassBurnTicks(ItemStack stack) {
        return !stack.isEmpty() && stack.is(BIOMASS_FUEL) ? BIOMASS_BURN_TICKS : 0;
    }

    public static void bootstrap() {
        FabricLoader.getInstance().getModContainer("intoxicantes").ifPresentOrElse(
                mod -> SncEnergies.LOGGER.info("SNC Adventures {} detected; optional biomass integration enabled",
                        mod.getMetadata().getVersion().getFriendlyString()),
                () -> SncEnergies.LOGGER.info("SNC Adventures absent; standalone progression enabled"));
    }
}
