package com.snc.energies.menu;

import com.snc.energies.blockentity.IndustrialBlockEntity;
import com.snc.energies.registry.*;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class IndustrialMenu extends SncMachineMenu {
    private final IndustryKind machineKind;
    public IndustrialMenu(int id,Inventory inventory){this(id,inventory,IndustryKind.BOILER);}
    public IndustrialMenu(int id,Inventory inventory,IndustryKind kind){this(id,inventory,new SimpleContainer(7),new SimpleContainerData(12),kind);}
    public IndustrialMenu(int id,Inventory inventory,IndustrialBlockEntity machine){
        this(id,inventory,machine,new ContainerData(){
            @Override public int get(int index){return switch(index){
                case 0 -> (int)(machine.getEnergy()&65535);case 1 -> (int)(machine.getEnergy()>>>16);
                case 2 -> (int)(machine.getCapacity()&65535);case 3 -> (int)(machine.getCapacity()>>>16);
                case 4 -> machine.getProgress();case 5 -> machine.getBurnTime();case 6 -> machine.getMaxProgress();
                case 7 -> machine.water();case 8 -> machine.steam();case 9 -> machine.status();case 10 -> machine.kind().ordinal();
                case 11 -> machine.runMode();default -> 0;
            };}
            @Override public void set(int index,int value){}
            @Override public int getCount(){return 12;}
        },machine.kind());
    }
    private IndustrialMenu(int id,Inventory inventory,Container container,ContainerData data,IndustryKind kind){
        super(SncMenus.INDUSTRY.get(kind),id,container,data,7);
        machineKind=kind;
        var layout=MachinePanel.of(kind.id);
        for(int i=0;i<7;i++){
            final int slot=i;
            var position=layout.position(i);
            addSlot(new Slot(container,i,position.x(),position.y()){
                @Override public boolean isActive(){return position.x()>=0;}
                @Override public boolean mayPlace(ItemStack stack){
                    if(slot>=4||!isActive())return false;
                    if(kind()==IndustryKind.BOILER)return slot==2?IndustrialBlockEntity.fuelTicks(stack)>0:slot==3&&stack.is(Items.WATER_BUCKET);
                    return slot<2&&IndustryRecipes.accepts(kind(),slot,stack);
                }
            });
        }
        addPlayerSlots(inventory);
    }
    public int water(){return data.get(7);}public int steam(){return data.get(8);}public int status(){return data.get(9);}
    public int runMode(){return data.get(11);}
    public IndustryKind kind(){return machineKind;}
    /** Authoritative server-side mode switch, mirroring the workshop crank. */
    @Override public boolean clickMenuButton(Player player,int button){
        return button==0&&stillValid(player)&&machine instanceof IndustrialBlockEntity industrial&&industrial.toggleRunMode();
    }
}
