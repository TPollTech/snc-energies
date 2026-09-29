package com.snc.energies.menu;

import com.snc.energies.blockentity.MercadaoShelfBlockEntity;
import com.snc.energies.economy.MercadaoCatalog;
import com.snc.energies.registry.SncMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The Mercadão counter menu: no item slots — the buyer points at a shelf
 * position and the attendant (server) debits the Adventures wallet, consumes
 * the shelf stock and hands the item over. Container data layout:
 * 0..9 = stock remaining per position, 10..19 = price per position,
 * 20 = shelf line index, 21 = the buyer's Adventures wallet balance.
 * Purchase = clickMenuButton(position 0..9), following the project's
 * TractorMenu button contract (no custom networking).
 */
public class MercadaoMenu extends AbstractContainerMenu {
    public static final int POSITIONS = 10;
    public static final int DATA_SHELF = 20;
    public static final int DATA_MONEY = 21;
    public static final int DATA_COUNT = 22;

    private final MercadaoShelfBlockEntity shelf;
    private final ContainerData data;

    public MercadaoMenu(int id, Inventory inventory, MercadaoShelfBlockEntity shelf) {
        super(SncMenus.MERCADAO, id);
        this.shelf = shelf;
        this.data = new ContainerData() {
            @Override public int get(int index) {
                if (index == DATA_SHELF) return shelfIndex(shelf.shelf());
                if (index == DATA_MONEY) {
                    return inventory.player instanceof net.minecraft.server.level.ServerPlayer buyer
                        ? com.snc.energies.economy.AdventuresMoney.balance(buyer) : 0;
                }
                List<MercadaoCatalog.Offer> offers = offers();
                if (index < POSITIONS) {
                    return index < offers.size() ? shelf.remaining(offers.get(index)) : 0;
                }
                int offerIndex = index - POSITIONS;
                return offerIndex < offers.size() ? offers.get(offerIndex).price() : 0;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(data);
    }

    /** Client-side factory: display data until the server syncs the rest. */
    public MercadaoMenu(int id, Inventory inventory) {
        super(SncMenus.MERCADAO, id);
        this.shelf = null;
        this.data = new ContainerData() {
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        };
        addDataSlots(data);
    }

    private static int shelfIndex(String shelf) {
        return switch (shelf) {
            case MercadaoCatalog.SHELF_SEEDS -> 1;
            case MercadaoCatalog.SHELF_DELI -> 2;
            case MercadaoCatalog.SHELF_PREMIUM -> 3;
            default -> 0;
        };
    }

    private static String shelfName(int index) {
        return switch (Math.floorMod(index, 4)) {
            case 1 -> MercadaoCatalog.SHELF_SEEDS;
            case 2 -> MercadaoCatalog.SHELF_DELI;
            case 3 -> MercadaoCatalog.SHELF_PREMIUM;
            default -> MercadaoCatalog.SHELF_BEVERAGES;
        };
    }

    private List<MercadaoCatalog.Offer> offers() {
        return MercadaoCatalog.byShelf(shelfName(data.get(DATA_SHELF)));
    }

    /** Display model for one counter position (client and server). */
    public record Entry(MercadaoCatalog.Offer offer, int price, int stock) {}

    /** Live wallet balance (server-computed, synced to the screen). */
    public int money() {
        return data.get(DATA_MONEY);
    }

    public List<Entry> entries() {
        List<MercadaoCatalog.Offer> offers = offers();
        java.util.List<Entry> result = new java.util.ArrayList<>(POSITIONS);
        for (int i = 0; i < POSITIONS; i++) {
            if (i < offers.size()) {
                MercadaoCatalog.Offer offer = offers.get(i);
                result.add(new Entry(offer, data.get(POSITIONS + i), data.get(i)));
            } else {
                result.add(null);
            }
        }
        return result;
    }

    public Component shelfTitle() {
        int index = shelf == null ? 0 : shelfIndex(shelf.shelf());
        return Component.translatable("gui.snc_energies.mercadao.shelf." + shelfName(index));
    }

    /**
     * Server-authoritative purchase; returns false so the client plays the
     * villager "no" sound, mirroring the EsquinaoNetworking feedback.
     */
    @Override
    public boolean clickMenuButton(Player player, int position) {
        if (shelf == null || position < 0 || position >= POSITIONS) return false;
        if (!stillValid(player)) return false;
        List<MercadaoCatalog.Offer> offers = offers();
        if (position >= offers.size()) return false;
        return shelf.trySell((net.minecraft.server.level.ServerPlayer) player, offers.get(position));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return shelf != null && shelf.stillValid(player);
    }
}
