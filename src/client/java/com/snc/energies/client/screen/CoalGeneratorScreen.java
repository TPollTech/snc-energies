package com.snc.energies.client.screen;

import com.snc.energies.menu.CoalGeneratorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CoalGeneratorScreen extends IdentityMachineScreen<CoalGeneratorMenu> {
    public CoalGeneratorScreen(CoalGeneratorMenu menu,Inventory inventory,Component title){super(menu,inventory,title,"coal_generator");}
}
