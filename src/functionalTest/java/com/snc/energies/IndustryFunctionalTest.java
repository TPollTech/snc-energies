package com.snc.energies;

import com.snc.energies.block.*;
import com.snc.energies.blockentity.*;
import com.snc.energies.energy.SteamNetwork;
import com.snc.energies.registry.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

final class IndustryFunctionalTest {
    private final ServerLevel level;
    private final ServerPlayer player;
    private final BlockPos pos=new BlockPos(8,130,8);
    private IndustryFunctionalTest(ServerLevel level,ServerPlayer player){this.level=level;this.player=player;}
    static void run(ServerLevel level,ServerPlayer player){new IndustryFunctionalTest(level,player).run();}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}",message);}
    private IndustrialBlockEntity place(IndustryKind kind,BlockPos anchor,Direction facing){
        var block=SncBlocks.INDUSTRY.get(kind);var state=block.defaultBlockState().setValue(MachineBlock.FACING,facing);
        level.setBlock(anchor,state,3);block.setPlacedBy(level,anchor,state,player,new ItemStack(block.asItem()));
        return (IndustrialBlockEntity)level.getBlockEntity(anchor);
    }
    private int drops(Item item){return level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(1,118,1,15,136,15))
        .stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    private void run(){
        for(int x=1;x<15;x++)for(int z=1;z<15;z++)for(int y=120;y<135;y++)level.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
        for(var kind:IndustryKind.values())for(var facing:new Direction[]{Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST}){
            var block=SncBlocks.INDUSTRY.get(kind);BlockPos far=block.cell(pos,facing,kind.cells()-1);
            level.setBlock(far,Blocks.STONE.defaultBlockState(),3);
            check(!block.canPlace(level,pos,facing),"industrial footprint rejects obstruction: "+kind+" / "+facing);
            level.removeBlock(far,false);
            var machine=place(kind,pos,facing);
            boolean valid=true;
            for(int i=0;i<kind.cells();i++){
                var cell=block.cell(pos,facing,i);
                valid &= IndustrialBlock.controller(level,cell,level.getBlockState(cell))==machine;
                valid &= i==0||level.getBlockEntity(cell)==null;
            }
            check(valid,"industrial multiblock has exactly one controller and inventory: "+kind+" / "+facing);
            int before=drops(block.asItem()),contents=drops(SncItems.RAW_TIN);
            machine.setItem(0,new ItemStack(SncItems.RAW_TIN,17));
            level.destroyBlock(far,true);
            boolean cleared=true;for(int i=0;i<kind.cells();i++)cleared &=level.getBlockState(block.cell(pos,facing,i)).isAir();
            check(cleared&&drops(block.asItem())==before+1&&drops(SncItems.RAW_TIN)==contents+17,
                "industrial removal returns one machine and exact inventory: "+kind+" / "+facing);
        }
        var boiler=place(IndustryKind.BOILER,new BlockPos(2,120,2),Direction.NORTH);
        boiler.setItem(2,new ItemStack(SncItems.RICE_HUSK,3));
        boiler.serverTick();
        check(boiler.getItem(2).getCount()==3&&boiler.steam()==0&&boiler.status()==4,"dry boiler consumes neither fuel nor water");
        for(int batch=0;batch<2;batch++){
            boiler.setItem(3,new ItemStack(Items.WATER_BUCKET));
            for(int i=0;i<100;i++)boiler.serverTick();
        }
        check(boiler.water()==0&&boiler.steam()==16000&&boiler.getItem(2).getCount()==1&&boiler.getItem(6).getCount()==2,
            "boiler conserves two water buckets, fuel and 16000 mB steam");
        boiler.setItem(3,new ItemStack(Items.WATER_BUCKET));boiler.serverTick();
        check(boiler.water()==1000&&boiler.getItem(2).getCount()==1&&boiler.steam()==16000,"full boiler pauses fuel consumption safely");
        var saved=boiler.saveWithFullMetadata(level.registryAccess());
        var restored=(IndustrialBlockEntity)BlockEntity.loadStatic(boiler.getBlockPos(),boiler.getBlockState(),saved,level.registryAccess());
        check(restored!=null&&restored.water()==1000&&restored.steam()==16000&&restored.getItem(6).getCount()==3,"boiler fluids and bucket inventory survive persistence");
        level.setBlock(new BlockPos(4,120,2),SncBlocks.STEAM_PIPE.defaultBlockState(),3);
        level.setBlock(new BlockPos(5,120,2),SncBlocks.STEAM_PIPE.defaultBlockState(),3);
        var saw=place(IndustryKind.SAWMILL,new BlockPos(6,120,2),Direction.NORTH);
        check(SteamNetwork.distribute(level,boiler,123)==123&&boiler.steam()==15877&&saw.steam()==123,"steam network conserves a bounded transfer across two ducts");
        check(level.getBlockEntity(new BlockPos(4,120,2))==null,"steam ducts have no electric cable entities or per-pipe ticks");
        int accepted=saw.receiveSteam(999999);
        check(accepted==16000-123&&saw.receiveSteam(1)==0&&saw.receiveSteam(-2)==0,"steam insertion rejects overflow and negative amounts");
        check(SteamNetwork.distribute(level,boiler,123)==0&&boiler.steam()==15877,"full consumer cannot delete or duplicate source steam");
        level.destroyBlock(saw.getBlockPos(),true);level.destroyBlock(boiler.getBlockPos(),true);
        level.removeBlock(new BlockPos(4,120,2),false);level.removeBlock(new BlockPos(5,120,2),false);
        var turbine=place(IndustryKind.TURBINE,pos,Direction.NORTH);turbine.receiveSteam(8000);
        for(int i=0;i<100;i++)turbine.serverTick();
        check(turbine.steam()==0&&turbine.getEnergy()==8000&&turbine.getEnergyStorage(Direction.NORTH)==null,
            "turbine converts 8000 mB into exactly 8000 E and cannot accept back-fed power");
        level.destroyBlock(pos,true);
        for(var recipe:IndustryRecipes.all()){
            var machine=place(recipe.kind(),pos,Direction.NORTH);
            machine.setItem(0,new ItemStack(recipe.input(),recipe.count()));
            if(recipe.reagentCount()>0)machine.setItem(1,new ItemStack(recipe.reagent(),recipe.reagentCount()));
            int supply=recipe.kind().cost*recipe.ticks();
            if(recipe.kind().steamConsumer())machine.receiveSteam(supply);else machine.getEnergyStorage(Direction.UP).insert(supply,false);
            machine.setItem(4,new ItemStack(Items.BEDROCK,64));machine.serverTick();
            check(machine.getProgress()==0&&machine.getItem(0).getCount()==recipe.count()&&machine.status()==3,
                "blocked output prevents all processing: "+recipe.id());
            machine.setItem(4,ItemStack.EMPTY);
            for(int i=0;i<recipe.ticks()/2;i++)machine.serverTick();
            var partial=(IndustrialBlockEntity)BlockEntity.loadStatic(pos,machine.getBlockState(),machine.saveWithFullMetadata(level.registryAccess()),level.registryAccess());
            check(partial!=null&&partial.getProgress()==recipe.ticks()/2&&partial.steam()==machine.steam()&&partial.getEnergy()==machine.getEnergy(),
                "industrial partial batch survives save/load: "+recipe.id());
            for(int i=recipe.ticks()/2;i<recipe.ticks();i++)machine.serverTick();
            check(machine.getItem(0).isEmpty()&&machine.getItem(4).is(recipe.product())&&machine.getItem(4).getCount()==recipe.productCount()
                &&(recipe.residueCount()==0?machine.getItem(5).isEmpty():machine.getItem(5).is(recipe.residue())&&machine.getItem(5).getCount()==recipe.residueCount())
                &&machine.getEnergy()==0&&machine.steam()==0,"industrial recipe conserves inputs, outputs and operating cost: "+recipe.id());
            check(recipe.keepReagent()?machine.getItem(1).getCount()==recipe.reagentCount():machine.getItem(1).isEmpty(),"reagent consumption matches recipe contract: "+recipe.id());
            level.destroyBlock(pos,true);
        }
    }
}
