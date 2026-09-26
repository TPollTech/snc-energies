package com.snc.energies.blockentity;

import com.snc.energies.block.IndustrialBlock;
import com.snc.energies.energy.*;
import com.snc.energies.registry.*;
import com.snc.energies.menu.IndustrialMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One engine for multiblock production, with exact resource accounting. */
public final class IndustrialBlockEntity extends MachineBlockEntity implements WorldlyContainer {
    public static final int WATER_CAPACITY=8000, STEAM_CAPACITY=16000;
    public static final long ENERGY_CAPACITY=256000;
    private final SimpleEnergyStorage energy=new SimpleEnergyStorage(ENERGY_CAPACITY);
    private int water,steam,burn,progress,status,runMode=RUN_ALWAYS;
    private boolean redstoneActive=true;
    private String activeRecipe="";
    public static final int RUN_ALWAYS=0,RUN_WITH_REDSTONE=1;
    /** Server-authoritative mode switch; mirrors the workshop crank contract. */
    public boolean toggleRunMode(){
        runMode=runMode==RUN_ALWAYS?RUN_WITH_REDSTONE:RUN_ALWAYS;
        setChanged();
        return true;
    }
    public int runMode(){return runMode;}
    private boolean redstoneGate(){
        if(runMode==RUN_ALWAYS)return true;
        if(level==null)return false;
        boolean powered=level.hasNeighborSignal(worldPosition);
        if(powered)redstoneActive=true;else if(redstoneActive&&!powered&&progress==0)redstoneActive=false;
        return redstoneActive;
    }
    public IndustrialBlockEntity(BlockPos pos,BlockState state){super(SncBlockEntities.INDUSTRIAL,pos,state,7);}
    public IndustryKind kind(){return ((IndustrialBlock)getBlockState().getBlock()).kind;}
    public int water(){return water;} public int steam(){return steam;} public int status(){return status;}
    public int receiveSteam(int amount){
        if(!kind().steamConsumer()||amount<=0)return 0;
        int accepted=Math.min(amount,STEAM_CAPACITY-steam);steam+=accepted;if(accepted>0)setChanged();return accepted;
    }
    public void removeSteam(int amount){int removed=Math.min(Math.max(amount,0),steam);steam-=removed;if(removed>0)setChanged();}
    public static int fuelTicks(ItemStack stack){
        int biomass=WoodStoveBlockEntity.woodBurnTicks(stack);
        if(biomass>0)return biomass;
        return stack.is(Items.COAL)||stack.is(Items.CHARCOAL)?1600:stack.is(Items.COAL_BLOCK)?16000:0;
    }
    private boolean fits(int slot,ItemStack result){
        if(result.isEmpty())return true;
        ItemStack existing=getItem(slot);
        return existing.isEmpty()?result.getCount()<=result.getMaxStackSize():ItemStack.isSameItemSameComponents(existing,result)
            && existing.getCount()+result.getCount()<=existing.getMaxStackSize();
    }
    private void add(int slot,ItemStack stack){if(stack.isEmpty())return;if(getItem(slot).isEmpty())setItem(slot,stack);else getItem(slot).grow(stack.getCount());}
    private void boiler(){
        if(getItem(3).is(Items.WATER_BUCKET)&&water<=WATER_CAPACITY-1000&&fits(6,new ItemStack(Items.BUCKET))){
            getItem(3).shrink(1);water+=1000;add(6,new ItemStack(Items.BUCKET));setChanged();
        }
        status=water<10?4:steam>STEAM_CAPACITY-80?3:burn==0&&fuelTicks(getItem(2))==0?1:2;
        if(status==2){
            if(burn==0){burn=fuelTicks(getItem(2));getItem(2).shrink(1);}
            water-=10;steam+=80;burn--;setChanged();
        }
        SteamNetwork.distribute(level,this,160);
    }
    @Override public void serverTick(){
        if(!canTick())return;
        if(!redstoneGate()){status=7;return;}
        if(kind()==IndustryKind.BOILER){boiler();return;}
        if(kind()==IndustryKind.TURBINE){
            status=steam<80?5:energy.getCapacity()-energy.getEnergy()<80?3:2;
            if(status==2){removeSteam(80);energy.insert(80,false);setChanged();}
            if(EnergyTransfer.distribute(level,worldPosition,energy,160)>0)setChanged();
            return;
        }
        var recipe=IndustryRecipes.find(kind(),getItem(0),getItem(1));
        String key=recipe==null?"":recipe.id();
        if(!activeRecipe.equals(key)){activeRecipe=key;progress=0;setChanged();}
        if(recipe==null||!recipe.supplied(getItem(0),getItem(1))){status=0;return;}
        if(!fits(4,recipe.output())||!fits(5,recipe.extra())){status=3;return;}
        if(kind().steamConsumer()?steam<kind().cost:energy.getEnergy()<kind().cost){status=kind().steamConsumer()?5:6;return;}
        status=2;
        if(kind().steamConsumer())removeSteam(kind().cost);else energy.extract(kind().cost,false);
        progress++;
        if(progress>=recipe.ticks()){
            getItem(0).shrink(recipe.count());
            if(!recipe.keepReagent())getItem(1).shrink(recipe.reagentCount());
            add(4,recipe.output());add(5,recipe.extra());progress=0;
        }
        setChanged();
    }
    @Override public EnergyStorage getEnergyStorage(Direction side){return kind().electric()?energy:null;}
    @Override public long getEnergy(){return energy.getEnergy();}
    @Override public long getCapacity(){return ENERGY_CAPACITY;}
    @Override public int getProgress(){return progress;}
    @Override public int getMaxProgress(){var r=IndustryRecipes.find(kind(),getItem(0),getItem(1));return r==null?1:r.ticks();}
    @Override public int getBurnTime(){return burn;}
    @Override public boolean canPlaceItem(int slot,ItemStack stack){
        if(kind()==IndustryKind.BOILER)return slot==2?fuelTicks(stack)>0:slot==3&&stack.is(Items.WATER_BUCKET);
        return slot<2&&IndustryRecipes.accepts(kind(),slot,stack);
    }
    @Override public int[] getSlotsForFace(Direction side){return side==Direction.DOWN?new int[]{4,5,6}:new int[]{0,1,2,3};}
    @Override public boolean canPlaceItemThroughFace(int slot,ItemStack stack,Direction side){return side!=Direction.DOWN&&canPlaceItem(slot,stack);}
    @Override public boolean canTakeItemThroughFace(int slot,ItemStack stack,Direction side){return side==Direction.DOWN&&slot>=4;}
    @Override protected String defaultLangKey(){return "block.snc_energies."+kind().id;}
    @Override public AbstractContainerMenu createMenu(int id,Inventory inventory,Player player){return new IndustrialMenu(id,inventory,this);}
    @Override public void preRemoveSideEffects(BlockPos pos,BlockState state){
        if(level!=null&&!level.isClientSide()){Containers.dropContents(level,worldPosition,this);clearContent();}
        super.preRemoveSideEffects(pos,state);
    }
    @Override protected void saveAdditional(ValueOutput output){
        super.saveAdditional(output);energy.save(output);output.putInt("Water",water);output.putInt("Steam",steam);
        output.putInt("Burn",burn);output.putInt("Progress",progress);output.putString("ActiveRecipe",activeRecipe);
        output.putInt("RunMode",runMode);output.putBoolean("RedstoneActive",redstoneActive);
    }
    @Override protected void loadAdditional(ValueInput input){
        super.loadAdditional(input);energy.load(input);water=Math.clamp(input.getIntOr("Water",0),0,WATER_CAPACITY);
        steam=Math.clamp(input.getIntOr("Steam",0),0,STEAM_CAPACITY);burn=Math.clamp(input.getIntOr("Burn",0),0,16000);
        activeRecipe=input.getStringOr("ActiveRecipe","");progress=Math.clamp(input.getIntOr("Progress",0),0,getMaxProgress()-1);
        runMode=Math.clamp(input.getIntOr("RunMode",RUN_ALWAYS),RUN_ALWAYS,RUN_WITH_REDSTONE);
        redstoneActive=input.getBooleanOr("RedstoneActive",true);
    }
}
