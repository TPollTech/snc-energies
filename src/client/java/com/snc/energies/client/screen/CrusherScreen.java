package com.snc.energies.client.screen;

import com.snc.energies.menu.CrusherMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class CrusherScreen extends IdentityMachineScreen<CrusherMenu> {
    public CrusherScreen(CrusherMenu menu,Inventory inventory,Component title){super(menu,inventory,title,"crusher");}
}
