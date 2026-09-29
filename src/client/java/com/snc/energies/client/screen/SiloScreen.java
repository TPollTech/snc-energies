package com.snc.energies.client.screen;

import com.snc.energies.menu.SiloMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class SiloScreen extends IdentityMachineScreen<SiloMenu> {
    public SiloScreen(SiloMenu menu,Inventory inventory,Component title){super(menu,inventory,title,"silo");}
}
