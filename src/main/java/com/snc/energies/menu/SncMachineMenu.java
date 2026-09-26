package com.snc.energies.menu;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Base menu for SNC machines.
 *
 * Data slots layout (ContainerData of length 7):
 * 0-1 energy lo/hi, 2-3 capacity lo/hi, 4 progress, 5 burn, 6 burn/max.
 *
 * Slot layout: machine slots first (0..machineSlotCount-1), then the 36 player slots.
 */
public abstract class SncMachineMenu extends AbstractContainerMenu {
	protected final Container machine;
	protected final ContainerData data;
	protected final int machineSlotCount;

	protected SncMachineMenu(MenuType<?> type, int id, Container machine, ContainerData data, int machineSlotCount) {
		super(type, id);
		this.machine = machine;
		this.data = data;
		this.machineSlotCount = machineSlotCount;
		addDataSlots(data);
	}

	@Override
	public boolean stillValid(Player player) {
		return this.machine.stillValid(player);
	}

	/** Adds the standard player inventory + hotbar slots at their GUI positions. */
	protected void addPlayerSlots(Inventory inv) {
		for (int row = 0; row < 3; ++row) {
			for (int col = 0; col < 9; ++col) {
				addSlot(new Slot(inv, col + row * 9 + 9, 48 + col * 18, 156 + row * 18));
			}
		}
		for (int col = 0; col < 9; ++col) {
			addSlot(new Slot(inv, col, 48 + col * 18, 214));
		}
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (!slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack original = slot.getItem();
		ItemStack copy = original.copy();
		if (index < machineSlotCount) {
			if (!moveItemStackTo(original, machineSlotCount, machineSlotCount + 36, true)) {
				return ItemStack.EMPTY;
			}
		} else {
			if (!moveItemStackTo(original, 0, machineSlotCount, false)) {
				return ItemStack.EMPTY;
			}
		}
		if (original.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	// ---- client-synced values ----

	public long getSyncedEnergy() {
		return (data.get(0) & 0xFFFFL) | ((data.get(1) & 0xFFFFL) << 16);
	}

	public long getSyncedCapacity() {
		return (data.get(2) & 0xFFFFL) | ((data.get(3) & 0xFFFFL) << 16);
	}

	public int getSyncedProgress() {
		return data.get(4);
	}

	public int getSyncedBurn() {
		return data.get(5);
	}

	public int getSyncedBurnTotal() {
		return data.get(6);
	}

	/** Stand-in data used by the client-side factory until the server syncs. */
	public static ContainerData dummyData() {
		return new net.minecraft.world.inventory.SimpleContainerData(7);
	}
}
