package com.snc.energies.economy;

import com.snc.energies.SncEnergies;
import com.snc.energies.registry.SncItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Optional;
/**
 * The Mercadão catalog: real items from SNC Adventures (beverages, seed
 * packets and the intoxicants it already sells through the Gago) plus SNC
 * Energies produce, priced in R$ on the same scale the Gago uses.
 * Prices and stock follow the Adventures gago() catalog (decompiled contract);
 * the premium intoxicant shelf mirrors its rarer end (stock 2-4 per day).
 * Adventures items are resolved by identifier on first query — deferred past
 * every mod's registry freeze, so entrypoint order cannot drop them; entries
 * whose item is absent (standalone profile) simply do not exist here — the mod
 * never registers content on behalf of another.
 */
public final class MercadaoCatalog {
    /** One purchasable line: item, unit price (R$), daily restock per shelf. */
    public record Offer(String id, String shelf, Item item, int price, int dailyStock, boolean premium) {
        public ItemStack stack() {
            return new ItemStack(item);
        }
    }

    public static final String SHELF_BEVERAGES = "bebidas";
    public static final String SHELF_SEEDS = "sementes";
    public static final String SHELF_DELI = "frios";
    public static final String SHELF_PREMIUM = "balcao_forte";

    /**
     * Memoized on first query — always after every mod's registry froze — so
     * cross-mod items resolve regardless of Fabric entrypoint order.
     */
    private static volatile List<Offer> cache;

    private static List<Offer> OFFERS() {
        List<Offer> offers = cache;
        if (offers == null) {
            synchronized (MercadaoCatalog.class) {
                offers = cache;
                if (offers == null) {
                    offers = build();
                    cache = offers;
                    long adventures = offers.stream()
                        .filter(o -> BuiltInRegistries.ITEM.getKey(o.item()).getNamespace()
                            .equals("intoxicantes")).count();
                    SncEnergies.LOGGER.info("Mercadão catalog ready: {} offers ({} from Adventures)",
                        offers.size(), adventures);
                }
            }
        }
        return offers;
    }

    private static List<Offer> build() {
        return java.util.stream.Stream.of(
        // Bebidas — same ids and prices the Gago charges (TradeCatalog.gago).
        offer("cerveja", SHELF_BEVERAGES, "intoxicantes:cerveja", 15, 8, false),
        offer("vinho", SHELF_BEVERAGES, "intoxicantes:vinho", 25, 8, false),
        offer("hidromel", SHELF_BEVERAGES, "intoxicantes:hidromel", 20, 8, false),
        offer("cachaca", SHELF_BEVERAGES, "intoxicantes:cachaca", 35, 8, false),
        offer("rum", SHELF_BEVERAGES, "intoxicantes:rum", 40, 8, false),
        offer("mate_infusion", SHELF_BEVERAGES, SncItems.MATE_INFUSION, 12, 8, false),
        // Sementes — the Gago sells packets of 4 for R$ 4.
        offer("semente_lupulo", SHELF_SEEDS, "intoxicantes:semente_lupulo", 4, 8, false),
        offer("semente_uva", SHELF_SEEDS, "intoxicantes:semente_uva", 4, 8, false),
        offer("semente_cafe", SHELF_SEEDS, "intoxicantes:semente_cafe", 4, 8, false),
        offer("semente_maconha", SHELF_SEEDS, "intoxicantes:semente_maconha", 4, 8, false),
        offer("semente_papoula", SHELF_SEEDS, "intoxicantes:semente_papoula", 4, 8, false),
        offer("rice_seeds", SHELF_SEEDS, SncItems.RICE_SEEDS, 2, 12, false),
        offer("soy_seeds", SHELF_SEEDS, SncItems.SOY_SEEDS, 2, 12, false),
        // Frios e mantimentos — vanilla items the Gago also stocks.
        offer("bread", SHELF_DELI, "minecraft:bread", 5, 8, false),
        offer("glass_bottle", SHELF_DELI, "minecraft:glass_bottle", 2, 16, false),
        offer("rice", SHELF_DELI, SncItems.RICE, 3, 12, false),
        offer("soybean", SHELF_DELI, SncItems.SOYBEAN, 3, 12, false),
        offer("vegetable_oil", SHELF_DELI, SncItems.VEGETABLE_OIL, 6, 8, false),
        // Balcão forte — intoxicants, rarer stock mirroring the Gago (4/day)
        // and the Traficante's street prices.
        offer("baseado", SHELF_PREMIUM, "intoxicantes:baseado", 42, 4, true),
        offer("cocaina", SHELF_PREMIUM, "intoxicantes:cocaina", 51, 4, true),
        offer("heroina", SHELF_PREMIUM, "intoxicantes:heroina", 44, 4, true),
        offer("lsd", SHELF_PREMIUM, "intoxicantes:lsd", 56, 4, true),
        offer("opio", SHELF_PREMIUM, "intoxicantes:opio", 14, 6, true),
        offer("extrato_cafeina", SHELF_PREMIUM, "intoxicantes:extrato_cafeina", 25, 6, true),
        offer("po_estelar", SHELF_PREMIUM, "intoxicantes:po_estelar", 60, 4, true))
        .filter(java.util.Objects::nonNull)
        .toList();
    }

    private static final int SLOTS_PER_SHELF = 9;

    private MercadaoCatalog() {}

    private static Offer offer(String id, String shelf, String itemId, int price, int stock, boolean premium) {
        Item item = BuiltInRegistries.ITEM.getOptional(Identifier.parse(itemId)).orElse(null);
        if (item == null) return null;
        return new Offer(id, shelf, item, price, stock, premium);
    }

    private static Offer offer(String id, String shelf, Item item, int price, int stock, boolean premium) {
        return new Offer(id, shelf, item, price, stock, premium);
    }

    /** Offers grouped by shelf, in registration order; nulls dropped. */
    public static List<Offer> byShelf(String shelf) {
        return OFFERS().stream().filter(o -> o != null && o.shelf().equals(shelf)).toList();
    }

    /** Every resolvable offer (shelf order). */
    public static List<Offer> all() {
        return OFFERS().stream().filter(o -> o != null).toList();
    }

    public static Optional<Offer> byId(String id) {
        return all().stream().filter(o -> o.id().equals(id)).findFirst();
    }

    public static int slotsPerShelf() {
        return SLOTS_PER_SHELF;
    }

    /**
     * Mod-init hook: clears any memo so the first query resolves after all
     * registries froze (log lands with the first in-game use).
     */
    public static void bootstrap() {
        cache = null;
    }
}
