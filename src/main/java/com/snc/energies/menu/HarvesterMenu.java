package com.snc.energies.menu;

import com.snc.energies.entity.HarvesterEntity;
import com.snc.energies.registry.SncItems;
import com.snc.energies.registry.SncMenus;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Harvester supply panel: slot 0 = motor oil (vegetable oil only), slots 1-9 =
 * the first grain-tank row (27 tank slots stay out of the menu). Buttons:
 * 0 = ignition toggle, 1 = header lift toggle. Data: 0 oil, 1 working,
 * 2 header raised, 3 tank fills per row (compact mirror for the panel).
 */
public class HarvesterMenu extends AbstractContainerMenu {
    public static final int BUTTON_IGNITION = 0;
    public static final int BUTTON_HEADER = 1;
    public static final int BUTTON_CART = 2;
    public static final int BUTTON_UNLOAD_CART = 3;
    /** Menu slot index of the oil supply cell (a dedicated 1-slot container). */
    public static final int OIL_SLOT = 0;

    private final HarvesterEntity harvester;
    /** Dedicated oil supply slot container (kept out of the grain tank so
     * every Slot index stays inside its container). */
    private final net.minecraft.world.SimpleContainer supply = new net.minecraft.world.SimpleContainer(1);
    private final ContainerData data;

    public HarvesterMenu(int id, Inventory inventory, HarvesterEntity harvester) {
        super(SncMenus.HARVESTER, id);
        this.harvester = harvester;
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> harvester.getOil();
                    case 1 -> harvester.isWorking() ? 1 : 0;
                    case 2 -> harvester.isHeaderLowered() ? 0 : 1;
                    case 3 -> harvester.hasAttachedCart() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 4; }
        };
        addDataSlots(data);
        mirrorSupply();
        // Motor oil: only the vegetable-oil portion, matching hand refuelling.
        addSlot(new Slot(supply, HarvesterMenu.OIL_SLOT, 36, 47) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(SncItems.VEGETABLE_OIL);
            }
        });
        for (int slot = 0; slot < HarvesterEntity.SLOTS_PER_ROW; slot++) {
            addSlot(new Slot(harvester, slot, 126 + (slot % 3) * 28, 47 - (slot / 3) * 0 + (slot / 3) * 0 + (slot / 3) * 18));
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
    public HarvesterMenu(int id, Inventory inventory) {
        super(SncMenus.HARVESTER, id);
        this.harvester = null;
        this.data = new ContainerData() {
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 4; }
        };
        addDataSlots(data);
        addSlot(new Slot(supply, HarvesterMenu.OIL_SLOT, 36, 47));
        for (int slot = 0; slot < HarvesterEntity.SLOTS_PER_ROW; slot++) {
            addSlot(new Slot(new SimpleContainer(1), 0, 126 + (slot % 3) * 28, 47 + (slot / 3) * 18));
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

    /** Server-authoritative controls, mirroring the tractor panel. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (harvester == null || !stillValid(player)) return false;
        return switch (button) {
            case BUTTON_IGNITION -> harvester.toggleWorking();
            case BUTTON_HEADER -> harvester.cycleHeader();
            case BUTTON_CART -> harvester.hasAttachedCart() ? harvester.detachCart() : harvester.attachCart();
            case BUTTON_UNLOAD_CART -> harvester.unloadIntoCart();
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 10) {
            if (!moveItemStackTo(original, 10, 37, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 10, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return harvester != null && harvester.stillValid(player);
    }

    public int oil() { return data.get(0); }
    public boolean working() { return data.get(1) == 1; }
    public boolean raised() { return data.get(2) == 1; }
    /** True when the SNC 90-C grain cart is coupled to the rear hitch. */
    public boolean cartAttached() { return data.get(3) == 1; }

    /** Oil still waiting in the supply cell (the client mirrors it visually). */
    public int supplyOil() {
        return supply.getItem(HarvesterMenu.OIL_SLOT).getCount();
    }

    /** Server-only supply sync: drains the oil cell into the tank. */
    private void mirrorSupply() {
        ItemStack oil = supply.getItem(HarvesterMenu.OIL_SLOT);
        if (!oil.isEmpty() && oil.is(SncItems.VEGETABLE_OIL)) {
            int accepted = harvester.addOil(oil.getCount() * HarvesterEntity.OIL_PER_ITEM);
            int used = (accepted + HarvesterEntity.OIL_PER_ITEM - 1) / HarvesterEntity.OIL_PER_ITEM;
            if (used > 0) {
                oil.shrink(used);
                if (oil.isEmpty()) supply.setItem(HarvesterMenu.OIL_SLOT, ItemStack.EMPTY);
            }
        }
    }

    /** Drains the supply cell while the panel is open (vanilla furnace pattern). */
    @Override
    public void broadcastChanges() {
        if (harvester != null && !harvester.level().isClientSide()) mirrorSupply();
        super.broadcastChanges();
    }

    /** MenuProvider for the entity supply panel. */
    public record Provider(HarvesterEntity harvester) implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("entity.snc_energies.harvester");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new HarvesterMenu(id, inventory, harvester);
        }
    }
}
