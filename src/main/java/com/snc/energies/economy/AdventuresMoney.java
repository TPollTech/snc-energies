package com.snc.energies.economy;

import com.snc.energies.SncEnergies;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Optional bridge to the SNC Adventures wallet (com.intoxicantes.PlayerMoney).
 * Adventures remains the sole authority over money: this class never stores a
 * second balance. When Adventures is absent every wallet call reports the
 * purchase as unaffordable instead of inventing a parallel currency.
 * All access is reflective so the Energies JAR never loads companion classes.
 */
public final class AdventuresMoney {
    private static final String REAL_ITEM_ID = "intoxicantes:real";
    private static volatile boolean resolved;
    private static volatile java.lang.reflect.Method getMethod;
    private static volatile java.lang.reflect.Method subtractMethod;
    private static volatile boolean usable;

    private AdventuresMoney() {}

    private static synchronized void resolve() {
        if (resolved) return;
        resolved = true;
        try {
            if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("intoxicantes")) {
                SncEnergies.LOGGER.info("SNC Adventures absent; Mercadão runs without wallet purchases");
                return;
            }
            Class<?> money = Class.forName("com.intoxicantes.PlayerMoney");
            getMethod = money.getMethod("get", ServerPlayer.class);
            subtractMethod = money.getMethod("subtrair", ServerPlayer.class, int.class);
            usable = true;
            SncEnergies.LOGGER.info("SNC Adventures wallet bridged for the Mercadão");
        } catch (Throwable error) {
            SncEnergies.LOGGER.warn("SNC Adventures wallet not bridged: {}", error.toString());
        }
    }

    /** Current R$ balance; 0 when Adventures is absent (nothing is purchasable). */
    public static int balance(ServerPlayer player) {
        resolve();
        if (!usable) return 0;
        try {
            return (Integer) getMethod.invoke(null, player);
        } catch (Throwable error) {
            SncEnergies.LOGGER.warn("Wallet read failed: {}", error.toString());
            return 0;
        }
    }

    /** Server-authoritative debit; returns false without charging when short. */
    public static boolean withdraw(ServerPlayer player, int amount) {
        if (amount < 0) return false;
        resolve();
        if (!usable) return false;
        try {
            return (Boolean) subtractMethod.invoke(null, player, amount);
        } catch (Throwable error) {
            SncEnergies.LOGGER.warn("Wallet debit failed: {}", error.toString());
            return false;
        }
    }

    /** True when the Adventures coin item (the "real") is registered. */
    public static boolean hasRealItem() {
        return real() != null;
    }

    /** One coin item for the shelf display; null when Adventures is absent. */
    public static Item real() {
        return net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getOptional(net.minecraft.resources.Identifier.parse(REAL_ITEM_ID)).orElse(null);
    }

    /** Test seam: true when the reflective wallet bridge is live. */
    public static boolean usableForTest() {
        resolve();
        return usable;
    }

    /** Test seam: fund the wallet by debiting a negative amount. */
    public static void creditForTest(ServerPlayer player, int amount) {
        resolve();
        if (!usable || amount <= 0) return;
        try {
            subtractMethod.invoke(null, player, -amount);
        } catch (Throwable ignored) {
            // PlayerMoney.subtrair clamps at zero; test wallets just stay small.
        }
    }
}
