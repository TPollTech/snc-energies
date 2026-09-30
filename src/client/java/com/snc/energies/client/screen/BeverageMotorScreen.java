package com.snc.energies.client.screen;

import com.snc.energies.menu.BeverageMotorMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public final class BeverageMotorScreen extends IdentityMachineScreen<BeverageMotorMenu> {
    public BeverageMotorScreen(BeverageMotorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, "beverage_motor");
    }
}
