package com.snc.energies.compat;

import java.util.ArrayList;
import java.util.List;

import com.snc.energies.SncEnergies;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Read-only view of the SNC Adventures beverage processing contract
 * (com.intoxicantes.ProcessosBebida, the canonical recipe source).
 *
 * Access is reflective so the Energies JAR never loads companion classes and
 * never touches private state: only the published static query methods
 * (receitasPrima + public record accessors) are read. When Adventures is
 * absent the step list stays empty and the motor idles — both mods keep
 * working standalone.
 *
 * Recipes are keyed by the machine-side enum (MaquinaPrimaBlock$Tipo with ids
 * moenda_cana / prensa_uvas / caldeirao_mostura). Feed/collect slots come from
 * the machine instance itself (tipoMaquina + idxInsumo/idxOut), so the bridge
 * never hardcodes another mod's internals.
 */
public final class AdventuresBeverages {
    /** One motorizable batch: put inputCount of input in, expect outputCount of output. */
    public record Step(String machine, int inputSlot, int outputSlot, int extraSlot,
                       Item input, int inputCount, Item output, int outputCount,
                       Item extraOut, int extraCount) {
        /** A one-item sample of the batch input, used for filter checks. */
        public ItemStack sample() { return new ItemStack(input, 1); }
    }

    private static final String[] MOTORIZED = {"moenda_cana", "prensa_uvas"};
    private static volatile boolean resolved;
    private static volatile List<Step> steps = List.of();

    private AdventuresBeverages() {}

    /** Motorizable batches across the beverage chain; empty without Adventures. */
    public static List<Step> steps() {
        resolve();
        return steps;
    }

    /** Slot layout for a docked machine, read from the machine itself. */
    public record Layout(int inputSlot, int outputSlot, int extraSlot) {}

    /** Read the published slot layout of a docked prima machine (null when unsupported). */
    public static Layout layoutOf(BlockEntity machine) {
        try {
            Class<?> entity = machine.getClass();
            if (!entity.getName().equals("com.intoxicantes.MaquinaPrimaBlockEntity")) return null;
            Object kind = entity.getMethod("tipoMaquina").invoke(machine);
            Class<?> kindClass = kind.getClass();
            String id = (String) kindClass.getField("id").get(kind);
            boolean motorized = false;
            for (String name : MOTORIZED) motorized |= name.equals(id);
            if (!motorized) return null;
            // The GUI enum publishes idxInsumo() as a method; idxOut/idxExtra are fields.
            Object guiKind = null;
            try {
                Class<?> guiEnumLookup = Class.forName("com.intoxicantes.TipoMaquina");
                for (Object constant : guiEnumLookup.getEnumConstants()) {
                    if (((String) guiEnumLookup.getMethod("id").invoke(constant)).equals(id)) { guiKind = constant; break; }
                }
            } catch (Throwable ignored) {
                // GUI enum absent or reshaped: fall back to the machine instance below.
            }
            Class<?> guiEnum = null;
            try { guiEnum = Class.forName("com.intoxicantes.TipoMaquina"); } catch (Throwable ignored) {}
            int inputSlot = guiKind != null
                    ? (Integer) guiEnum.getMethod("idxInsumo").invoke(guiKind)
                    : inputSlotFallback(machine);
            int outputSlot = guiKind != null
                    ? (Integer) guiEnum.getField("idxOut").get(guiKind)
                    : outputSlotFallback(machine);
            int extraSlot = guiKind != null
                    ? (Integer) guiEnum.getField("idxExtra").get(guiKind)
                    : -1;
            return new Layout(inputSlot, outputSlot, extraSlot);
        } catch (Throwable error) {
            SncEnergies.LOGGER.warn("Beverage machine layout unavailable: {}", error.toString());
            return null;
        }
    }

    /** Fallback when the GUI enum is unavailable: probe the machine's own slots. */
    private static int inputSlotFallback(BlockEntity machine) {
        try { return (Integer) machine.getClass().getMethod("idxInsumo").invoke(machine); }
        catch (Throwable ignored) { return 0; }
    }

    private static int outputSlotFallback(BlockEntity machine) {
        try { return (Integer) machine.getClass().getMethod("idxOut").invoke(machine); }
        catch (Throwable ignored) { return 1; }
    }

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        if (!AdventuresCompatibility.isLoaded()) return;
        try {
            Class<?> processes = Class.forName("com.intoxicantes.ProcessosBebida");
            Class<?> prima = Class.forName("com.intoxicantes.ProcessosBebida$Prima");
            Class<?> kindType = Class.forName("com.intoxicantes.MaquinaPrimaBlock$Tipo");
            var receitasPrima = processes.getMethod("receitasPrima", kindType);
            Object[] kinds = kindType.getEnumConstants();
            List<Step> found = new ArrayList<>();
            for (Object kind : kinds) {
                String id = (String) kindType.getField("id").get(kind);
                boolean motorized = false;
                for (String name : MOTORIZED) motorized |= name.equals(id);
                if (!motorized) continue;
                for (Object recipe : (List<?>) receitasPrima.invoke(null, kind)) {
                    Item extraOut = (Item) prima.getMethod("extraOut").invoke(recipe);
                    int extraCount = (Integer) prima.getMethod("extraQtd").invoke(recipe);
                    found.add(new Step(id, -1, -1, extraSlot(extraOut, extraCount),
                            (Item) prima.getMethod("input").invoke(recipe),
                            (Integer) prima.getMethod("qtdIn").invoke(recipe),
                            (Item) prima.getMethod("output").invoke(recipe),
                            (Integer) prima.getMethod("qtdOut").invoke(recipe),
                            extraOut, extraOut != null ? extraCount : 0));
                }
            }
            steps = List.copyOf(found);
            SncEnergies.LOGGER.info("SNC Adventures beverage contract read: {} motorized batches", found.size());
        } catch (Throwable error) {
            steps = List.of();
            SncEnergies.LOGGER.warn("SNC Adventures beverage contract unavailable: {}", error.toString());
        }
    }

    /** Extra output slot: -1 when the recipe has no second product. */
    private static int extraSlot(Item extraOut, int extraCount) {
        return extraOut != null && extraCount > 0 ? 1 : -1;
    }
}
