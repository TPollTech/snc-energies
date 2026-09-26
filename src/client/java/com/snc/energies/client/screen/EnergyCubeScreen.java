package com.snc.energies.client.screen;

import com.snc.energies.menu.EnergyCubeMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class EnergyCubeScreen extends IdentityMachineScreen<EnergyCubeMenu> {
    public EnergyCubeScreen(EnergyCubeMenu menu,Inventory inventory,Component title){super(menu,inventory,title,"energy_cube");}
}
