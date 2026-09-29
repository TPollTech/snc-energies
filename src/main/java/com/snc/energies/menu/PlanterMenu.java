package com.snc.energies.menu;

import com.snc.energies.entity.PlanterEntity;
import com.snc.energies.entity.TractorEntity;
import com.snc.energies.registry.SncMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Planter tank menu: the same three 64-slot row tanks the tractor panel
 * mirrors, opened directly on the implement (row 0 shown as the first
 * visible slot per row; the remaining 189 slots stay out of the menu).
 */
public class PlanterMenu extends AbstractContainerMenu {
    public static final int DISPLAYED_SLOTS = 4;

    private final PlanterEntity planter;

    public PlanterMenu(int id, Inventory inventory, PlanterEntity planter) {
        super(SncMenus.PLANTER, id);
        this.planter = planter;
        for (int row = 0; row < PlanterEntity.ROWS; row++) {
            for (int shown = 0; shown < 1; shown++) {
                final int slot = row * PlanterEntity.SEEDS_PER_ROW;
                addSlot(new Slot(planter, slot, 126 + row * 28, 47) {
                    @Override public boolean mayPlace(ItemStack stack) {
                        return TractorEntity.isSeed(stack);
                    }
                });
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
    public PlanterMenu(int id, Inventory inventory) {
        super(SncMenus.PLANTER, id);
        this.planter = null;
        for (int row = 0; row < PlanterEntity.ROWS; row++) {
            addSlot(new Slot(new SimpleContainer(1), 0, 126 + row * 28, 47));
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
        if (index < 3) {
            if (!moveItemStackTo(original, 3, 30, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 3, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return planter != null && planter.stillValid(player);
    }

    /** MenuProvider for the implement tank menu. */
    public record Provider(PlanterEntity planter) implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("entity.snc_energies.planter");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new PlanterMenu(id, inventory, planter);
        }
    }
}
