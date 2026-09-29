package com.snc.energies.menu;

import com.snc.energies.registry.SncMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Per-cell whitelist editor for an item duct. Slot 0 of the backing container
 * is the duct buffer shown read-only; slots 1..9 are the whitelist itself.
 * Data slot 0 mirrors the buffered item id (0 when empty) so the title can
 * hint at what the cell currently carries.
 */
public final class ItemPipeFilterMenu extends SncMachineMenu {
    public static final int BUFFER_SLOT = 0, FILTER_FIRST = 1;
    private final Container filter;
    public ItemPipeFilterMenu(int id, Inventory inventory) { this(id, inventory, new SimpleContainer(10), new SimpleContainerData(1)); }
    public ItemPipeFilterMenu(int id, Inventory inventory, Container pipe) {
        this(id, inventory, pipe, new ContainerData() {
            @Override public int get(int index) {
                ItemStack buffer = pipe.getItem(BUFFER_SLOT);
                return buffer.isEmpty() ? 0 : net.minecraft.core.registries.BuiltInRegistries.ITEM.getId(buffer.getItem()) & 0xFFFF;
            }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return 1; }
        });
    }
    private ItemPipeFilterMenu(int id, Inventory inventory, Container pipe, ContainerData data) {
        super(SncMenus.ITEM_PIPE_FILTER, id, pipe, data, 10);
        filter = pipe;
        addSlot(new Slot(pipe, BUFFER_SLOT, 80, 20) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            addSlot(new Slot(pipe, 1 + row * 3 + col, 62 + col * 18, 48 + row * 18));
        }
        addPlayerSlots(inventory);
    }
    public int bufferedItemId() { return data.get(0); }
    /** Clear button: server-side wipe of the whitelist. */
    @Override public boolean clickMenuButton(Player player, int button) {
        if (button == 0 && stillValid(player)) {
            for (int slot = FILTER_FIRST; slot < FILTER_FIRST + 9; slot++) filter.setItem(slot, ItemStack.EMPTY);
            filter.setChanged();
            return true;
        }
        return false;
    }
}
