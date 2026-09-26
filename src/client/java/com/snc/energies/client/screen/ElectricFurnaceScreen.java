package com.snc.energies.client.screen;

import com.snc.energies.menu.ElectricFurnaceMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class ElectricFurnaceScreen extends IdentityMachineScreen<ElectricFurnaceMenu> {
    public ElectricFurnaceScreen(ElectricFurnaceMenu menu,Inventory inventory,Component title){super(menu,inventory,title,"electric_furnace");}
}
