package com.snc.energies.compat;

import com.snc.energies.SncEnergies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Registry-based bridge to the SNC Adventures UV lamp.
 *
 * The lamp block itself is resolved from the global registry by its stable id,
 * so no companion class is ever loaded and the bridge cannot break on internal
 * refactors. Turning the game's original lamp on and off stays the Adventures
 * mod's own job — this bridge only reads its current state for the guide.
 */
public final class AdventuresUvBridge {
    private static final String LAMP_ID = "intoxicantes:lampada_uv";

    private AdventuresUvBridge() {}

    /** The Adventures UV lamp block, or null when the mod is absent. */
    public static Block lamp() {
        if (!AdventuresCompatibility.isLoaded()) return null;
        return BuiltInRegistries.BLOCK.getOptional(Identifier.parse(LAMP_ID)).orElse(null);
    }

    /** True when the game's original lamp is currently lit at the given cell. */
    public static boolean isVanillaLampLit(Level level, BlockPos pos) {
        Block lamp = lamp();
        if (lamp == null) return false;
        BlockState state = level.getBlockState(pos);
        if (!state.is(lamp)) return false;
        BooleanProperty lit = state.hasProperty(LIT_FALLBACK) ? LIT_FALLBACK : null;
        if (lit == null) {
            for (BooleanProperty candidate : UV_LIT_PROPERTIES) {
                if (state.hasProperty(candidate)) { lit = candidate; break; }
            }
        }
        return lit != null && state.getValue(lit);
    }

    /** Property name discovered in the companion bytecode; kept as fallback only. */
    static final BooleanProperty LIT_FALLBACK = BooleanProperty.create("lit");

    private static final BooleanProperty[] UV_LIT_PROPERTIES = {LIT_FALLBACK};
}
