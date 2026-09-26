package com.snc.energies;

import com.snc.energies.registry.*;
import com.snc.energies.block.*;
import com.snc.energies.blockentity.*;
import com.snc.energies.client.screen.*;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public final class WorkshopClientTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context){
        context.getInput().resizeWindow(1280,800);
        context.runOnClient(client->{client.options.guiScale().set(2);client.resizeGui();});
        try(var world=context.worldBuilder().create()){
            var server=world.getServer();
            server.runOnServer(mc->{
                var level=mc.overworld();var player=mc.getPlayerList().getPlayers().getFirst();
                for(int x=-6;x<8;x++)for(int z=-5;z<8;z++){
                    level.setBlockAndUpdate(new BlockPos(x,89,z),Blocks.STONE.defaultBlockState());
                    for(int y=90;y<96;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
                }
                var pos=new BlockPos(0,90,0);var state=SncBlocks.GRAIN_MILL.defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH);
                level.setBlock(pos,state,3);SncBlocks.GRAIN_MILL.setPlacedBy(level,pos,state,player,new ItemStack(SncItems.GRAIN_MILL));
                ((WorkshopBlockEntity)level.getBlockEntity(pos)).setItem(0,new ItemStack(SncItems.RICE_PADDY,2));
                var boiler=SncBlocks.INDUSTRY.get(IndustryKind.BOILER);pos=new BlockPos(3,90,0);state=boiler.defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH);
                level.setBlock(pos,state,3);boiler.setPlacedBy(level,pos,state,player,new ItemStack(boiler.asItem()));
                var machine=(IndustrialBlockEntity)level.getBlockEntity(pos);machine.setItem(2,new ItemStack(SncItems.BIOMASS_BRIQUETTE));machine.setItem(3,new ItemStack(Items.WATER_BUCKET));
                player.teleportTo(1,90,-2.5);player.getInventory().clearContent();
            });
            world.getConnection().waitForChunksRender();
            context.waitTicks(10);context.getInput().lookAt(0,0);context.waitTicks(5);
            context.takeScreenshot("colonial-and-boiler-models");
            context.waitFor(client->client.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit
                && client.level.getBlockState(hit.getBlockPos()).is(SncBlocks.GRAIN_MILL));
            context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_RIGHT);
            context.waitForScreen(WorkshopScreen.class);context.waitTicks(3);
            double[] mouse=context.computeOnClient(client->{var screen=client.gui.screen();return new double[]{
                ((screen.width-256)/2.0+124)*client.getWindow().getWidth()/screen.width,
                ((screen.height-238)/2.0+118)*client.getWindow().getHeight()/screen.height};});
            context.getInput().setCursorPos(mouse[0],mouse[1]);context.getInput().pressMouse(InputConstants.MOUSE_BUTTON_LEFT);
            context.waitTicks(5);context.takeScreenshot("workshop-menu-cranking");
            server.waitFor(mc->((WorkshopBlockEntity)mc.overworld().getBlockEntity(new BlockPos(0,90,0))).getItem(1).is(SncItems.RICE));
            context.waitFor(client->client.player.containerMenu.getSlot(1).getItem().is(SncItems.RICE));
            context.waitTicks(3);
            context.takeScreenshot("workshop-menu-output");
            server.runOnServer(mc->mc.getPlayerList().getPlayers().getFirst().openMenu(
                (IndustrialBlockEntity)mc.overworld().getBlockEntity(new BlockPos(3,90,0))));
            context.waitForScreen(IndustrialScreen.class);context.waitTicks(5);context.takeScreenshot("boiler-menu");
            for(var kind:IndustryKind.values()){
                server.runOnServer(mc->{
                    var level=mc.overworld();var pos=new BlockPos(0,90,0);var player=mc.getPlayerList().getPlayers().getFirst();
                    if(level.getBlockEntity(pos) instanceof MachineBlockEntity old)old.clearContent();
                    level.removeBlock(pos,false);
                    var block=SncBlocks.INDUSTRY.get(kind);var state=block.defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH);
                    level.setBlock(pos,state,3);block.setPlacedBy(level,pos,state,player,new ItemStack(block));
                    var machine=(IndustrialBlockEntity)level.getBlockEntity(pos);
                    if(kind==IndustryKind.BOILER){machine.setItem(2,new ItemStack(SncItems.BIOMASS_BRIQUETTE,8));machine.setItem(3,new ItemStack(Items.WATER_BUCKET));}
                    if(kind.steamConsumer())machine.receiveSteam(16000);
                    if(kind.electric())machine.getEnergyStorage(Direction.UP).insert(190000,false);
                    for(var recipe:IndustryRecipes.all())if(recipe.kind()==kind){
                        machine.setItem(0,new ItemStack(recipe.input(),recipe.count()*3));
                        if(recipe.reagentCount()>0)machine.setItem(1,new ItemStack(recipe.reagent(),recipe.reagentCount()*3));break;
                    }
                    player.openMenu(machine);
                });
                verifyPanel(context,kind.id);
                context.computeOnClient(client->{
                    var menu=(com.snc.energies.menu.IndustrialMenu)client.player.containerMenu;
                    var layout=com.snc.energies.menu.MachinePanel.of(kind.id);
                    if(menu.kind()!=kind)throw new AssertionError("Client received wrong machine kind");
                    for(int i=0;i<7;i++){
                        var slot=menu.getSlot(i);var position=layout.position(i);
                        if(slot.x!=position.x()||slot.y!=position.y()||slot.isActive()!=(position.x()>=0))throw new AssertionError("Mismatched slot layout "+kind+" / "+i);
                    }
                    return true;
                });
            }
            for(var block:new net.minecraft.world.level.block.Block[]{SncBlocks.SEED_PRESS,SncBlocks.WOOD_STOVE,SncBlocks.COAL_GENERATOR,SncBlocks.ELECTRIC_FURNACE,SncBlocks.CRUSHER,SncBlocks.ENERGY_CUBE}){
                String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block).getPath();
                server.runOnServer(mc->{
                    var level=mc.overworld();var pos=new BlockPos(0,90,0);var player=mc.getPlayerList().getPlayers().getFirst();
                    if(level.getBlockEntity(pos) instanceof MachineBlockEntity old)old.clearContent();level.removeBlock(pos,false);
                    level.setBlock(pos,block.defaultBlockState(),3);
                    var machine=(MachineBlockEntity)level.getBlockEntity(pos);player.openMenu(machine);
                });
                verifyPanel(context,id);
            }
            context.runOnClient(client->client.gui.setScreen(new FieldGuideScreen()));
            context.waitForScreen(FieldGuideScreen.class);context.waitTicks(5);context.takeScreenshot("progression-guide");
            SncEnergies.LOGGER.info("CLIENT FLOW PASSED: multiblock interaction, crank packet, server output, 16 distinct panels, synchronized slot layouts, boiler screen and guide");
        }
    }
    private static void verifyPanel(ClientGameTestContext context,String id){
        context.waitFor(client->client.gui.screen() instanceof IdentityMachineScreen<?> screen && screen.panelId().equals(id));
        context.waitTicks(5);context.takeScreenshot("panel-"+id);
    }
}
