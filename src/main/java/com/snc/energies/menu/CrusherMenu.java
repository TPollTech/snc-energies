package com.snc.energies.menu;

import com.snc.energies.blockentity.CrusherBlockEntity;
import com.snc.energies.registry.SncMenus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;

/**
 * Crusher menu: slot 0 = input, slot 1 = output.
 */
public class CrusherMenu extends SncMachineMenu {
	public CrusherMenu(int id, Inventory inv, CrusherBlockEntity be) {
		super(SncMenus.CRUSHER, id, be, dataOf(be), 2);
		addSlot(new Slot(be, 0, MachinePanel.x("crusher", 0), MachinePanel.y("crusher", 0)));
		addSlot(new ElectricFurnaceMenu.OutputSlot(be, 1, MachinePanel.x("crusher", 1), MachinePanel.y("crusher", 1)));
		addPlayerSlots(inv);
	}

	public CrusherMenu(int id, Inventory inv) {
		super(SncMenus.CRUSHER, id, new SimpleContainer(2), dummyData(), 2);
		addSlot(new Slot(machine, 0, MachinePanel.x("crusher", 0), MachinePanel.y("crusher", 0)));
		addSlot(new ElectricFurnaceMenu.OutputSlot(machine, 1, MachinePanel.x("crusher", 1), MachinePanel.y("crusher", 1)));
		addPlayerSlots(inv);
	}

	private static ContainerData dataOf(CrusherBlockEntity be) {
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
}
