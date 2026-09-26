package com.snc.energies.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.List;

/** Shared by processing, slot validation, guide and functional tests. */
public final class WorkshopRecipes {
    public record Process(String id, boolean press, Item input, int count, Item output, int outputCount,
                          Item residue, int residueCount, int ticks) {
        public boolean accepts(ItemStack stack) { return stack.is(input); }
        public ItemStack product() { return new ItemStack(output, outputCount); }
        public ItemStack byproduct() { return residueCount == 0 ? ItemStack.EMPTY : new ItemStack(residue, residueCount); }
    }
    private WorkshopRecipes() {}
    public static List<Process> all() {
        return List.of(
            new Process("rice", false, SncItems.RICE_PADDY, 2, SncItems.RICE, 2, SncItems.RICE_HUSK, 1, 80),
            new Process("flour", false, Items.WHEAT, 2, SncItems.FLOUR, 2, Items.AIR, 0, 80),
            new Process("mate", false, SncItems.DRIED_MATE, 2, SncItems.GROUND_MATE, 2, Items.AIR, 0, 80),
            new Process("soy", true, SncItems.SOYBEAN, 4, SncItems.VEGETABLE_OIL, 1, SncItems.SOY_MEAL, 2, 120),
            new Process("briquette", true, SncItems.RICE_HUSK, 4, SncItems.BIOMASS_BRIQUETTE, 1, Items.AIR, 0, 120)
        );
    }
    public static Process find(boolean press, ItemStack input) {
        for (Process process : all()) if (process.press() == press && process.accepts(input)) return process;
        return null;
    }
}
