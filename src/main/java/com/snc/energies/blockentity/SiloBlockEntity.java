package com.snc.energies.blockentity;

import com.snc.energies.menu.SiloMenu;
import com.snc.energies.registry.SiloSize;
import com.snc.energies.registry.SncBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Agroindustrial bulk bin: one inventory behind the whole 2x2x3 structure.
 * Slot 0 is the hopper intake buffer, slots 1..16 are the sixteen bulk rows
 * (up to 16,384 items each, one item kind per row) and slot 17 is the bottom
 * extraction buffer hoppers pull from. No energy, no recipe: pure storage.
 */
public final class SiloBlockEntity extends MachineBlockEntity implements WorldlyContainer {
    public static final int ROWS=16, PER_STACK=16384;
    public static final int IN=0, ROW_BASE=1, OUT=ROWS+1;
    private static final int[] INSERT_SLOTS={IN}, EXTRACT_SLOTS={OUT}, BOTH_SLOTS={IN,OUT};

    public SiloBlockEntity(BlockPos pos,BlockState state){super(SncBlockEntities.SILO,pos,state,ROWS+2);}

    public ItemStack row(int index){return getItem(ROW_BASE+index);}
    public int rowCount(int index){return row(index).getCount();}

    public long total(){
        long sum=0;
        for(int i=0;i<ROWS;i++) sum+=rowCount(i);
        return sum;
    }

    /** Bulk deposit from a carried stack into one row; returns the accepted amount. */
    public int deposit(int index,ItemStack carried){
        ItemStack target=row(index);
        if(carried.isEmpty()) return 0;
        if(target.isEmpty()){
            int amount=Math.min(carried.getCount(),PER_STACK);
            setItem(ROW_BASE+index,carried.split(amount));
            return amount;
        }
        if(!ItemStack.isSameItemSameComponents(target,carried)) return 0;
        int amount=Math.min(carried.getCount(),PER_STACK-target.getCount());
        if(amount>0){target.grow(amount);carried.shrink(amount);setChanged();}
        return amount;
    }

    /** Bulk withdrawal from one row; the caller merges the result into its carried stack. */
    public ItemStack withdraw(int index,int amount){
        ItemStack source=row(index);
        if(source.isEmpty()) return ItemStack.EMPTY;
        int take=Math.min(Math.max(amount,1),source.getCount());
        setChanged();
        return source.split(take);
    }

    /** Drains the intake buffer into rows and refills the extraction buffer from rows. */
    @Override public void serverTick(){
        if(!canTick()) return;
        boolean changed=false;
        ItemStack inbox=getItem(IN);
        if(!inbox.isEmpty()){
            for(int i=0;i<ROWS&&inbox.getCount()>0;i++) changed|=deposit(i,inbox)>0;
            if(inbox.isEmpty()) setItem(IN,ItemStack.EMPTY);
        }
        ItemStack outbox=getItem(OUT);
        for(int i=0;i<ROWS;i++){
            ItemStack source=row(i);
            if(source.isEmpty()) continue;
            if(outbox.isEmpty()){
                outbox=source.split(Math.min(source.getCount(),source.getMaxStackSize()));
                setItem(OUT,outbox);
                changed=true;
            } else if(ItemStack.isSameItemSameComponents(outbox,source)){
                int amount=Math.min(source.getCount(),outbox.getMaxStackSize()-outbox.getCount());
                if(amount>0){outbox.grow(amount);source.shrink(amount);changed=true;}
            }
            if(outbox.getCount()>=outbox.getMaxStackSize()) break;
        }
        if(changed) setChanged();
    }

    /** Rows hold far more than the vanilla 64; base setItem clamps to this. */
    @Override public int getMaxStackSize(){return PER_STACK;}

    /** Pure storage: never part of an energy network. */
    @Override public com.snc.energies.energy.EnergyStorage getEnergyStorage(Direction side){return null;}

    @Override public int[] getSlotsForFace(Direction side){
        if(side==Direction.DOWN) return EXTRACT_SLOTS;
        if(side==Direction.UP) return INSERT_SLOTS;
        return BOTH_SLOTS;
    }

    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){
        return slot==IN&&side!=Direction.DOWN;
    }

    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){
        return slot==OUT&&side==Direction.DOWN;
    }

    @Override public boolean canPlaceItem(int slot,ItemStack stack){return slot==IN;}

    @Override protected String defaultLangKey(){return "block.snc_energies.silo";}

    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){
        return new SiloMenu(id,inventory,this);
    }

    /** Rows above 64 violate ItemStack.CODEC validation on load (the 0.5.0 filter lesson), so each bulk row saves as a one-item sample plus an explicit count. */
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output){
        super.saveAdditional(output);
        var rows=output.childrenList("BulkRows");
        for(int i=0;i<ROWS;i++){
            ItemStack stack=row(i);
            if(stack.isEmpty()) continue;
            var child=rows.addChild();
            child.store("Sample",ItemStack.CODEC,stack.copyWithCount(1));
            child.putInt("Count",stack.getCount());
        }
    }

    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input){
        super.loadAdditional(input);
        int index=0;
        for(var child:input.childrenListOrEmpty("BulkRows")){
            if(index>=ROWS) break;
            var sample=child.read("Sample",ItemStack.CODEC);
            if(sample.isEmpty()||sample.get().isEmpty()) continue;
            int count=Math.clamp(child.getIntOr("Count",1),1,PER_STACK);
            setItem(ROW_BASE+index,sample.get().copyWithCount(count));
            index++;
        }
    }

    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level!=null&&!level.isClientSide()){Containers.dropContents(level,worldPosition,this);clearContent();}
        super.preRemoveSideEffects(pos,state);
    }
}
