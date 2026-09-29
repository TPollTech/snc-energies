package com.snc.energies.menu;

import com.snc.energies.blockentity.SiloBlockEntity;
import com.snc.energies.registry.SncMenus;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Bulk bin interface: intake buffer, sixteen bulk rows (16,384 each, one item
 * kind per row) and the bottom extraction buffer. Rows accept only their own
 * item kind, so different goods never mix inside the silo.
 */
public final class SiloMenu extends SncMachineMenu {
    public static final int TOTAL_CAPACITY=SiloBlockEntity.ROWS*SiloBlockEntity.PER_STACK;

    public SiloMenu(int id,Inventory inventory){
        this(id,inventory,new SimpleContainer(SiloBlockEntity.ROWS+2),new SimpleContainerData(7),null);
    }

    public SiloMenu(int id,Inventory inventory,SiloBlockEntity silo){
        this(id,inventory,silo,new ContainerData(){
            @Override public int get(int index){return index<2?(int)(index==0?silo.total()&65535:silo.total()>>>16&65535):0;}
            @Override public void set(int index,int value){}
            @Override public int getCount(){return 7;}
        },silo);
    }

    private SiloMenu(int id,Inventory inventory,Container container,ContainerData data,SiloBlockEntity silo){
        super(SncMenus.SILO,id,container,data,SiloBlockEntity.ROWS+2);
        var layout=MachinePanel.of("silo");
        for(int i=0;i<SiloBlockEntity.ROWS+2;i++){
            final int slot=i;
            var position=layout.position(i);
            addSlot(new Slot(container,i,position.x(),position.y()){
                @Override public boolean isActive(){return position.x()>=0;}
                @Override public boolean mayPlace(ItemStack stack){
                    if(!isActive()||slot==SiloBlockEntity.OUT) return false;
                    if(slot==SiloBlockEntity.IN) return true;
                    ItemStack row=container.getItem(slot);
                    return row.isEmpty()||ItemStack.isSameItemSameComponents(row,stack);
                }
                @Override public int getMaxStackSize(){
                    return slot>=SiloBlockEntity.ROW_BASE&&slot<SiloBlockEntity.OUT?SiloBlockEntity.PER_STACK:64;
                }
            });
        }
        addPlayerSlots(inventory);
    }

    public long total(){return (data.get(0)&65535L)|((data.get(1)&65535L)<<16);}
    public int totalCapacity(){return TOTAL_CAPACITY;}

    @Override public boolean isProductionMenu(){return false;}
}
