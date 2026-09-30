package com.snc.energies.compat;

import com.snc.energies.SncEnergies;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Transactional stock service for motorized Adventures machines.
 *
 * Every move is all-or-nothing: the destination is only written after a
 * capacity check proves it can keep the whole amount, and a snapshot compare
 * rolls anything unexpected back. An interrupted tick can never duplicate or
 * destroy items — the same conservation rule the item ducts follow.
 *
 * No fake player is created and no private state is touched: the machine is
 * reached through its public {@link Container} interface, the same surface
 * hoppers use. Slot selection is decided by the caller (for the motorized
 * beverage machines the input/output slots are published by TipoMaquina and
 * carried in {@link AdventuresBeverages.Step}).
 */
public final class AdventuresAutomation {
    private AdventuresAutomation() {}

    private static ItemStack[] snapshot(Container machine) {
        ItemStack[] copy = new ItemStack[machine.getContainerSize()];
        for (int slot = 0; slot < copy.length; slot++) copy[slot] = machine.getItem(slot).copy();
        return copy;
    }

    /** True when every slot outside {@code changedSlot} is exactly as before. */
    private static boolean intact(Container machine, ItemStack[] before, int changedSlot) {
        for (int slot = 0; slot < before.length; slot++) {
            if (slot == changedSlot) continue;
            if (!ItemStack.matches(machine.getItem(slot), before[slot])) return false;
        }
        // The changed slot is the move itself: same item kind must remain,
        // with components intact; the count changed on purpose.
        ItemStack now = machine.getItem(changedSlot);
        ItemStack was = before[changedSlot];
        if (now.isEmpty() && was.isEmpty()) return true;
        if (now.isEmpty() || was.isEmpty()) return true; // filled an empty slot / drained to empty
        return ItemStack.isSameItemSameComponents(now, was);
    }

    private static void rollback(Container machine, ItemStack[] before) {
        for (int slot = 0; slot < before.length; slot++) machine.setItem(slot, before[slot]);
        SncEnergies.LOGGER.warn("SNC Adventures automation rolled back a partially applied move");
    }

    /**
     * Move up to {@code amount} of the buffer's stack into the machine's input
     * slot, simulating capacity first. Returns false (and changes nothing)
     * when the slot is full, holds a different item, or the offer is empty.
     */
    public static boolean feed(Container machine, int inputSlot, Container buffer, int bufferSlot, int amount) {
        ItemStack offering = buffer.getItem(bufferSlot);
        if (offering.isEmpty() || amount <= 0) return false;
        ItemStack input = machine.getItem(inputSlot);
        boolean sameStack = !input.isEmpty() && ItemStack.isSameItemSameComponents(input, offering);
        int free = input.isEmpty() ? offering.getMaxStackSize()
                : sameStack ? input.getMaxStackSize() - input.getCount() : 0;
        int moving = Math.min(Math.min(amount, free), offering.getCount());
        if (moving <= 0) return false;
        ItemStack[] before = snapshot(machine);
        if (input.isEmpty()) machine.setItem(inputSlot, offering.copyWithCount(moving));
        else input.grow(moving);
        if (!intact(machine, before, inputSlot)) {
            rollback(machine, before);
            return false;
        }
        offering.shrink(moving);
        if (offering.isEmpty()) buffer.setItem(bufferSlot, ItemStack.EMPTY);
        machine.setChanged();
        buffer.setChanged();
        return true;
    }

    /**
     * Move the stacks of the machine's output slot (and its extra slot when
     * set) into the buffer. The buffer is measured before anything is removed,
     * so a full buffer simply leaves the machine untouched.
     */
    public static boolean harvest(Container machine, int outputSlot, int extraSlot,
                                  Container buffer, int bufferSlot) {
        boolean moved = drain(machine, outputSlot, buffer, bufferSlot);
        if (extraSlot >= 0 && extraSlot != outputSlot) moved |= drain(machine, extraSlot, buffer, bufferSlot);
        return moved;
    }

    private static boolean drain(Container machine, int fromSlot, Container buffer, int bufferSlot) {
        ItemStack out = machine.getItem(fromSlot);
        if (out.isEmpty()) return false;
        ItemStack into = buffer.getItem(bufferSlot);
        boolean sameStack = !into.isEmpty() && ItemStack.isSameItemSameComponents(into, out);
        int free = into.isEmpty() ? out.getMaxStackSize()
                : sameStack ? into.getMaxStackSize() - into.getCount() : 0;
        int moving = Math.min(free, out.getCount());
        if (moving <= 0) return false;
        ItemStack[] before = snapshot(machine);
        if (into.isEmpty()) buffer.setItem(bufferSlot, out.copyWithCount(moving));
        else into.grow(moving);
        out.shrink(moving);
        if (out.isEmpty()) machine.setItem(fromSlot, ItemStack.EMPTY);
        if (!intact(machine, before, fromSlot)) {
            rollback(machine, before);
            return false;
        }
        machine.setChanged();
        buffer.setChanged();
        return true;
    }
}
