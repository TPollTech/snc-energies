package com.snc.energies.menu;

import com.snc.energies.blockentity.WoodStoveBlockEntity;
import com.snc.energies.registry.SncMenus;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Wood stove menu: machine slot 0 = fuel.
 */
public class WoodStoveMenu extends SncMachineMenu {
	public WoodStoveMenu(int id, Inventory inv, WoodStoveBlockEntity be) {
		super(SncMenus.WOOD_STOVE, id, be, dataOf(be), 1);
		addSlot(new WoodStoveBlockEntity.FuelSlot(be, 0, MachinePanel.x("wood_stove", 0), MachinePanel.y("wood_stove", 0)));
		addPlayerSlots(inv);
	}

	/** Client-side factory constructor. */
	public WoodStoveMenu(int id, Inventory inv) {
		super(SncMenus.WOOD_STOVE, id, new SimpleContainer(1), dummyData(), 1);
		addSlot(new WoodStoveBlockEntity.FuelSlot(machine, 0, MachinePanel.x("wood_stove", 0), MachinePanel.y("wood_stove", 0)));
		addPlayerSlots(inv);
	}

	private static ContainerData dataOf(WoodStoveBlockEntity be) {
		return new ContainerData() {
			@Override
			public int get(int index) {
				return switch (index) {
					case 0 -> (int) (be.getEnergy() & 0xFFFFL);
					case 1 -> (int) ((be.getEnergy() >>> 16) & 0xFFFFL);
					case 2 -> (int) (be.getCapacity() & 0xFFFFL);
					case 3 -> (int) ((be.getCapacity() >>> 16) & 0xFFFFL);
					case 4 -> 0;
					case 5 -> be.getBurnTime();
					case 6 -> be.getBurnTimeTotal();
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

