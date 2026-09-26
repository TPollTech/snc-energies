package com.snc.energies.menu;

import com.snc.energies.blockentity.ElectricFurnaceBlockEntity;
import com.snc.energies.registry.SncMenus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Electric furnace menu: slot 0 = input, slot 1 = output.
 */
public class ElectricFurnaceMenu extends SncMachineMenu {
	public ElectricFurnaceMenu(int id, Inventory inv, ElectricFurnaceBlockEntity be) {
		super(SncMenus.ELECTRIC_FURNACE, id, be, dataOf(be), 2);
		addSlot(new Slot(be, 0, MachinePanel.x("electric_furnace", 0), MachinePanel.y("electric_furnace", 0)));
		addSlot(new OutputSlot(be, 1, MachinePanel.x("electric_furnace", 1), MachinePanel.y("electric_furnace", 1)));
		addPlayerSlots(inv);
	}

	public ElectricFurnaceMenu(int id, Inventory inv) {
		super(SncMenus.ELECTRIC_FURNACE, id, new SimpleContainer(2), dummyData(), 2);
		addSlot(new Slot(machine, 0, MachinePanel.x("electric_furnace", 0), MachinePanel.y("electric_furnace", 0)));
		addSlot(new OutputSlot(machine, 1, MachinePanel.x("electric_furnace", 1), MachinePanel.y("electric_furnace", 1)));
		addPlayerSlots(inv);
	}

	private static ContainerData dataOf(ElectricFurnaceBlockEntity be) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case 0 -> (int) (be.getEnergy() & 0xFFFFL);
					case 1 -> (int) ((be.getEnergy() >>> 16) & 0xFFFFL);
					case 2 -> (int) (be.getCapacity() & 0xFFFFL);
					case 3 -> (int) ((be.getCapacity() >>> 16) & 0xFFFFL);
					case 4 -> be.getProgress();
					case 5 -> 0;
					case 6 -> be.getMaxProgress();
					default -> 0;
				};
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return 7;
			}
		};
	}

	/** Players cannot insert into the output slot. */
	public static class OutputSlot extends Slot {
		public OutputSlot(net.minecraft.world.Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}
	}
}
