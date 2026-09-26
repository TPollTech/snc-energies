package com.snc.energies.menu;

import com.snc.energies.blockentity.EnergyCubeBlockEntity;
import com.snc.energies.registry.SncMenus;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;

/** Read-only battery telemetry; the cube never exposes item storage. */
public final class EnergyCubeMenu extends SncMachineMenu {
    public EnergyCubeMenu(int id,Inventory inventory){super(SncMenus.ENERGY_CUBE,id,new SimpleContainer(0),dummyData(),0);addPlayerSlots(inventory);}
    public EnergyCubeMenu(int id,Inventory inventory,EnergyCubeBlockEntity cube){
        super(SncMenus.ENERGY_CUBE,id,cube,new ContainerData(){
            @Override public int get(int index){return switch(index){
                case 0 -> (int)(cube.getEnergy()&65535);case 1 -> (int)(cube.getEnergy()>>>16);
                case 2 -> (int)(cube.getCapacity()&65535);case 3 -> (int)(cube.getCapacity()>>>16);default -> 0;
            };}
            @Override public void set(int index,int value){}
            @Override public int getCount(){return 7;}
        },0);addPlayerSlots(inventory);
    }
}
