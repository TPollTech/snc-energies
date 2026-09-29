package com.snc.energies.blockentity;

import com.snc.energies.block.IndustrialBlock;
import com.snc.energies.block.ItemPipeBlock;
import com.snc.energies.block.MachineBlock;
import com.snc.energies.energy.ItemTransit;
import com.snc.energies.menu.ItemPipeFilterMenu;
import com.snc.energies.registry.SncBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A passive duct cell holding at most one item. The buffer guarantees exact
 * conservation: an item pushed or pulled in stays here until a neighbour
 * accepts one onward. Cells pull finished outputs from machines and push
 * towards machine inputs, hoppers and other ducts.
 */
public final class ItemPipeBlockEntity extends MachineBlockEntity implements WorldlyContainer {
    public static final int FILTER_SLOTS = 9;
    private final NonNullList<ItemStack> filter = NonNullList.withSize(FILTER_SLOTS, ItemStack.EMPTY);
    private int cooldown;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(SncBlockEntities.ITEM_PIPE, pos, state, 1);
    }

    private boolean neighboursLoaded(Level level) {
        for (Direction side : Direction.values()) {
            if (!level.hasChunkAt(worldPosition.relative(side))) return false;
        }
        return true;
    }

    @Override public void serverTick() {
        if (!canTick() || level == null) return;
        if (cooldown > 0) { cooldown--; setChanged(); return; }
        if (getItem(0).isEmpty()) {
            if (neighboursLoaded(level) && pullFromMachine(level)) cooldown = 8;
            return;
        }
        if (neighboursLoaded(level) && ItemTransit.moveFrom(level, worldPosition, this)) cooldown = 8;
        setChanged();
    }

    /**
     * Suction: empty cells take one item from adjacent machine outputs. Only
     * machine windows qualify; hoppers and ducts are never drained.
     */
    private boolean pullFromMachine(Level level) {
        for (Direction side : Direction.values()) {
            BlockPos next = worldPosition.relative(side);
            if (!level.hasChunkAt(next)) continue;
            BlockState state = level.getBlockState(next);
            Direction approach = side.getOpposite();
            Container donor = null;
            if (state.getBlock() instanceof IndustrialBlock) {
                var controller = IndustrialBlock.controller(level, next, state);
                if (controller != null) donor = controller;
            } else if (state.getBlock() instanceof MachineBlock) {
                donor = level.getBlockEntity(next) instanceof Container container ? container : null;
            }
            if (donor == null) continue;
            int[] slots = donor instanceof WorldlyContainer worldly
                ? worldly.getSlotsForFace(approach) : ItemTransit.flatSlots(donor);
            for (int slot : slots) {
                ItemStack stack = donor.getItem(slot);
                if (stack.isEmpty() || !filterAllows(stack)) continue;
                boolean takeable = donor.canTakeItem(this, slot, stack)
                    && (!(donor instanceof WorldlyContainer worldly) || worldly.canTakeItemThroughFace(slot, stack, approach));
                if (!takeable) continue;
                ItemStack moved = donor.removeItem(slot, 1);
                if (moved.isEmpty()) continue;
                setItem(0, moved);
                donor.setChanged();
                cooldown = 8;
                return true;
            }
        }
        return false;
    }

    /** Neighbours (ducts, hoppers or machines) may insert whenever the buffer is empty. */
    public boolean accepts(ItemStack stack) {
        return getItem(0).isEmpty() && !stack.isEmpty() && filterAllows(stack);
    }

    /** Empty whitelist lets everything through; otherwise the item must match one entry. */
    public boolean filterAllows(ItemStack stack) {
        if (stack.isEmpty()) return false;
        boolean any = false;
        for (ItemStack entry : filter) {
            if (entry.isEmpty()) continue;
            any = true;
            if (ItemStack.isSameItemSameComponents(entry, stack)) return true;
        }
        return !any;
    }

    public ItemStack filterItem(int slot) { return filter.get(slot); }

    public void setFilterItem(int slot, ItemStack stack) { filter.set(slot, stack); setChanged(); }

    public void clearFilter() { filter.replaceAll(stack -> ItemStack.EMPTY); setChanged(); }

    /** Container view over the whitelist, backing the filter menu slots. */
    public Container filterView() {
        return new Container() {
            @Override public int getContainerSize() { return FILTER_SLOTS; }
            @Override public boolean isEmpty() { return filter.stream().allMatch(ItemStack::isEmpty); }
            @Override public ItemStack getItem(int slot) { return filter.get(slot); }
            @Override public ItemStack removeItem(int slot, int amount) {
                ItemStack removed = ContainerHelper.removeItem(filter, slot, amount);
                if (!removed.isEmpty()) setChanged();
                return removed;
            }
            @Override public ItemStack removeItemNoUpdate(int slot) {
                ItemStack taken = ContainerHelper.takeItem(filter, slot);
                if (!taken.isEmpty()) setChanged();
                return taken;
            }
            @Override public void setItem(int slot, ItemStack stack) {
                filter.set(slot, stack);
                setChanged();
            }
            @Override public void setChanged() { ItemPipeBlockEntity.this.setChanged(); }
            @Override public boolean stillValid(Player player) { return ItemPipeBlockEntity.this.stillValid(player); }
            @Override public void clearContent() { clearFilter(); }
        };
    }

    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ItemPipeFilterMenu(id, inventory, filterView());
    }

    @Override public Component getDisplayName() { return Component.translatable("gui.snc_energies.pipe_filter.title"); }

    /** One item leaves towards the given face; returns the moved stack or empty. */
    public ItemStack drain(Direction face) {
        ItemStack buffer = getItem(0);
        if (buffer.isEmpty() || !canTakeItemThroughFace(0, buffer, face)) return ItemStack.EMPTY;
        setItem(0, ItemStack.EMPTY);
        return buffer.split(1);
    }

    // ---- container wiring: hopper push/pull and ItemTransit all use this ----

    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && accepts(stack); }

    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0}; }

    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && accepts(stack);
    }

    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 && !stack.isEmpty();
    }

    /** Ducts carry no energy; returning null keeps the cable network away. */
    @Override public com.snc.energies.energy.EnergyStorage getEnergyStorage(Direction side) { return null; }

    @Override protected String defaultLangKey() { return "block.snc_energies.item_pipe"; }

    @Override protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Cooldown", cooldown);
        output.store("Filter", ItemStack.OPTIONAL_CODEC.listOf(), filter);
    }

    @Override protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        cooldown = Math.clamp(input.getIntOr("Cooldown", 0), 0, 64);
        input.read("Filter", ItemStack.OPTIONAL_CODEC.listOf()).ifPresent(saved -> {
            for (int i = 0; i < filter.size() && i < saved.size(); i++) filter.set(i, saved.get(i));
        });
    }
}
