package com.snc.energies.client.screen;

import com.snc.energies.menu.IndustrialMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class IndustrialScreen extends IdentityMachineScreen<IndustrialMenu> {
    public IndustrialScreen(IndustrialMenu menu,Inventory inventory,Component title){super(menu,inventory,title,menu.kind().id);}
}
