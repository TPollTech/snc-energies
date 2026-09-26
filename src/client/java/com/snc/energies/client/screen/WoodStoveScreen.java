package com.snc.energies.client.screen;

import com.snc.energies.menu.WoodStoveMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WoodStoveScreen extends IdentityMachineScreen<WoodStoveMenu> {
    public WoodStoveScreen(WoodStoveMenu menu,Inventory inventory,Component title){super(menu,inventory,title,"wood_stove");}
}
