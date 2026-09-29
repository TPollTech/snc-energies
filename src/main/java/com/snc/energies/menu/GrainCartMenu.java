package com.snc.energies.menu;

import com.snc.energies.entity.GrainCartEntity;
import com.snc.energies.registry.SncMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Grain cart tank panel: the fifteen cargo slots (3 rows × 5), opened on the
 * cart itself. No buttons: unloading happens through the harvester panel or
 * by hand from this grid.
 */
public class GrainCartMenu extends AbstractContainerMenu {
    public static final int CARGO_SLOTS = GrainCartEntity.TOTAL_SLOTS;

    private final GrainCartEntity cart;

    public GrainCartMenu(int id, Inventory inventory, GrainCartEntity cart) {
        super(SncMenus.GRAIN_CART, id);
        this.cart = cart;
        for (int row = 0; row < GrainCartEntity.ROWS; row++) {
            for (int col = 0; col < GrainCartEntity.SLOTS_PER_ROW; col++) {
                addSlot(new Slot(cart, row * GrainCartEntity.SLOTS_PER_ROW + col,
                    70 + col * 22, 47 + row * 22));
            }
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 48 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, 48 + col * 18, 162));
        }
    }

    /** Client-side factory constructor with stand-in slots until the server syncs. */
    public GrainCartMenu(int id, Inventory inventory) {
        super(SncMenus.GRAIN_CART, id);
        this.cart = null;
        SimpleContainer standIn = new SimpleContainer(CARGO_SLOTS);
        for (int slot = 0; slot < CARGO_SLOTS; slot++) {
            addSlot(new Slot(standIn, slot, 70 + (slot % GrainCartEntity.SLOTS_PER_ROW) * 22,
                47 + (slot / GrainCartEntity.SLOTS_PER_ROW) * 22));
        }
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 48 + col * 18, 104 + row * 18));
            }
        }
        for (int col = 0; col < 9; ++col) {
            addSlot(new Slot(inventory, col, 48 + col * 18, 162));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < CARGO_SLOTS) {
            if (!moveItemStackTo(original, CARGO_SLOTS, CARGO_SLOTS + 36, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, CARGO_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return cart != null && cart.stillValid(player);
    }

    /** MenuProvider for the cart tank panel. */
    public record Provider(GrainCartEntity cart) implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("entity.snc_energies.grain_cart");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new GrainCartMenu(id, inventory, cart);
        }
    }
}
