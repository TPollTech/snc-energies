package com.snc.energies.client.screen;

import com.snc.energies.menu.WorkshopMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class WorkshopScreen extends IdentityMachineScreen<WorkshopMenu> {
    public WorkshopScreen(WorkshopMenu menu,Inventory inventory,Component title){super(menu,inventory,title,menu.isPress()?"seed_press":"grain_mill");}
}
