package com.snc.energies.menu;

import com.snc.energies.entity.TractorEntity;
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
 * Tractor supply panel: slot 0 = motor oil, slots 1-3 = one seed stack per
 * planter row (the other 189 tank slots stay out of the menu). Buttons:
 * 0 = ignition toggle, 1 = planter lift toggle, 2 = implement hitch toggle.
 * Data: 0 oil, 1 working, 2 planter raised, 3-5 row status mirrors.
 */
public class TractorMenu extends AbstractContainerMenu {
    public static final int BUTTON_IGNITION = 0;
    public static final int BUTTON_LIFT = 1;
    public static final int BUTTON_HITCH = 2;
    /** Menu slot index of the oil supply cell (a dedicated 1-slot container). */
    public static final int OIL_SLOT = 0;

    private final TractorEntity tractor;
    /** Dedicated oil supply slot container (kept out of the seed tanks so
     * every Slot index stays inside its container). */
    private final net.minecraft.world.SimpleContainer supply = new net.minecraft.world.SimpleContainer(1);
    private final ContainerData data;

    public TractorMenu(int id, Inventory inventory, TractorEntity tractor) {
        super(SncMenus.TRACTOR, id);
        this.tractor = tractor;
        this.data = new ContainerData() {
            @Override public int get(int index) {
                return switch (index) {
                    case 0 -> tractor.getOil();
                    case 1 -> tractor.isWorking() ? 1 : 0;
                    case 2 -> tractor.isPlanterRaised() ? 1 : 0;
                    case 3 -> tractor.rowHasSeeds(0) ? 1 : 0;
                    case 4 -> tractor.rowHasSeeds(1) ? 1 : 0;
                    case 5 -> tractor.rowHasSeeds(2) ? 1 : 0;
                    case 6 -> tractor.hasAttachedPlanter() ? 1 : 0;
                    default -> 0;
                };
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 7; }
        };
        addDataSlots(data);
        mirrorSupply();
        // Motor oil: only the vegetable-oil portion, matching hand refuelling.
        addSlot(new Slot(supply, TractorMenu.OIL_SLOT, 36, 47) {
            @Override public boolean mayPlace(ItemStack stack) {
                return stack.is(SncItems.VEGETABLE_OIL);
            }
        });
        for (int row = 0; row < TractorEntity.ROWS; row++) {
            final int slot = row * TractorEntity.SEEDS_PER_ROW;
            addSlot(new Slot(tractor, slot, 126 + row * 28, 47) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return TractorEntity.isSeed(stack);
                }
            });
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

    /** Client-side factory constructor with stand-in data until the server syncs. */
    /** Client-side factory constructor with stand-in slots until the server syncs. */
    public TractorMenu(int id, Inventory inventory) {
        super(SncMenus.TRACTOR, id);
        this.tractor = null;
        this.data = new ContainerData() {
            @Override public int get(int index) { return 0; }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 7; }
        };
        addDataSlots(data);
        addSlot(new Slot(supply, TractorMenu.OIL_SLOT, 36, 47));
        for (int row = 0; row < TractorEntity.ROWS; row++) {
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

    /** Server-authoritative controls, mirroring the industrial redstone valve. */
    @Override
    public boolean clickMenuButton(Player player, int button) {
        if (tractor == null || !stillValid(player)) return false;
        return switch (button) {
            case BUTTON_IGNITION -> tractor.toggleWorking();
            case BUTTON_LIFT -> tractor.cyclePlanterLift();
            case BUTTON_HITCH -> tractor.hasAttachedPlanter() ? tractor.detachPlanter() : tractor.attachPlanter();
            default -> false;
        };
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack copy = original.copy();
        if (index < 4) {
            if (!moveItemStackTo(original, 4, 40, true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(original, 0, 4, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return tractor != null && tractor.stillValid(player);
    }

    public int oil() { return data.get(0); }
    public boolean working() { return data.get(1) == 1; }
    public boolean raised() { return data.get(2) == 1; }
    public boolean rowSeeded(int row) { return data.get(3 + row) == 1; }
    /** True when the SNC 75-P implement is coupled to the three-point hitch. */
    public boolean attached() { return data.get(6) == 1; }

    /** Oil still waiting in the supply cell (the client mirrors it visually). */
    public int supplyOil() {
        return supply.getItem(TractorMenu.OIL_SLOT).getCount();
    }

    /** Server-only supply sync: drains the oil cell into the tank. */
    private void mirrorSupply() {
        ItemStack oil = supply.getItem(TractorMenu.OIL_SLOT);
        if (!oil.isEmpty() && oil.is(SncItems.VEGETABLE_OIL)) {
            int accepted = tractor.addOil(oil.getCount() * TractorEntity.OIL_PER_ITEM);
            int used = (accepted + TractorEntity.OIL_PER_ITEM - 1) / TractorEntity.OIL_PER_ITEM;
            if (used > 0) {
                oil.shrink(used);
                if (oil.isEmpty()) supply.setItem(TractorMenu.OIL_SLOT, ItemStack.EMPTY);
            }
        }
    }

    /** Drains the supply cell while the panel is open (vanilla furnace pattern). */
    @Override
    public void broadcastChanges() {
        if (tractor != null && !tractor.level().isClientSide()) mirrorSupply();
        super.broadcastChanges();
    }

    /** MenuProvider for the entity supply panel. */
    public record Provider(TractorEntity tractor) implements net.minecraft.world.MenuProvider {
        @Override
        public Component getDisplayName() {
            return Component.translatable("entity.snc_energies.tractor");
        }

        @Override
        public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
            return new TractorMenu(id, inventory, tractor);
        }
    }
}
