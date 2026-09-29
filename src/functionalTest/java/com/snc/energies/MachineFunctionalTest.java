package com.snc.energies;

import com.snc.energies.blockentity.*;
import com.snc.energies.block.*;
import com.snc.energies.energy.*;
import com.snc.energies.menu.*;
import com.snc.energies.registry.*;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import com.mojang.authlib.GameProfile;
import java.util.UUID;

public final class MachineFunctionalTest implements DedicatedServerModInitializer {
    private int ticks;
    private boolean ready;
    private CoalGeneratorBlockEntity generator;
    private ElectricFurnaceBlockEntity furnace, blocked;
    private CrusherBlockEntity crusher;
    private EnergyCubeBlockEntity battery;
    private ServerLevel world;
    private WoodStoveBlockEntity woodStove;
    private WoodStoveBlockEntity largeStove;
    private ElectricFurnaceBlockEntity largeFurnace;
    private ElectricFurnaceBlockEntity woodFurnace;
    private ServerPlayer player;
    private BlockPos p(int x) { return new BlockPos(x, 100, 0); }
    private MachineBlockEntity place(int x, Block block) {
        world.setBlock(p(x), block.defaultBlockState(), 3);
        return (MachineBlockEntity)world.getBlockEntity(p(x));
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        SncEnergies.LOGGER.info("FUNCTIONAL PASS: {}", message);
    }
    @Override public void onInitializeServer() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                world = server.overworld();
                world.setChunkForced(0, 0, true);
                ready = true;
            } catch (Throwable error) { fail(server, error); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!ready) return;
            try {
                ticks++;
                if (cartSuiteTicks > 0 && --cartSuiteTicks == 0) {
                    GrainCartFunctionalTest.run(world, player);
                }
                if (ticks == 40) setup(server);
                if (ticks == 340) {
                    check(largeFurnace.getItem(1).is(Items.IRON_INGOT), "large stove supplies energy through a cable on its far structural cell");
                    check(world.getBlockState(WoodStoveBlock.cell(largeStove.getBlockPos(),Direction.NORTH,7)).getValue(CoalGeneratorBlock.LIT), "large chimney shares the controller lit state");
                    check(woodFurnace.getItem(1).is(Items.IRON_INGOT), "wood stove powers an electric furnace from one log");
                    check(woodStove.getItem(0).isEmpty() && woodStove.getBurnTime() > 0, "wood stove consumes logs and retains remaining burn");
                    check(furnace.getItem(1).is(Items.IRON_INGOT), "generator and multi-cable network smelt iron using native ticks");
                    check(crusher.getItem(1).is(SncItems.VOLTAITE_DUST) && crusher.getItem(1).getCount() == 2, "crusher produces two Voltaite dust");
                    check(generator.getItem(0).isEmpty() && generator.getBurnTime() > 0, "one coal starts a persistent burn");
                    check(battery.getEnergy() > 0, "surplus power charges battery");
                    check(blocked.getItem(0).getCount() == 1 && blocked.getItem(1).is(Items.GOLD_INGOT)
                        && blocked.getEnergy() == 10000, "incompatible output prevents consumption or item conversion");
                                        var saved = generator.saveWithFullMetadata(world.registryAccess());
                    var restored = (CoalGeneratorBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                        p(0), generator.getBlockState(), saved, world.registryAccess());
                    check(restored != null && restored.getEnergy() == generator.getEnergy()
                        && restored.getBurnTime() == generator.getBurnTime(), "generator energy and burn survive serialization");
                    var furnaceSaved = furnace.saveWithFullMetadata(world.registryAccess());
                    var restoredFurnace = (ElectricFurnaceBlockEntity)net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                        p(3), furnace.getBlockState(), furnaceSaved, world.registryAccess());
                    check(restoredFurnace != null && restoredFurnace.getItem(1).is(Items.IRON_INGOT)
                        && restoredFurnace.getEnergy() == furnace.getEnergy(), "furnace inventory and energy survive serialization");
                    world.setBlock(p(0), Blocks.AIR.defaultBlockState(), 3);
                    furnace.getEnergyStorage(Direction.UP).extract(Long.MAX_VALUE, false);
                    furnace.setItem(0, new ItemStack(SncItems.VOLTAITE_DUST));
                    furnace.setItem(1, ItemStack.EMPTY);
                }
                if (ticks == 540) {
                    check(furnace.getItem(1).is(SncItems.VOLTAITE_INGOT), "battery powers furnace without generator and dust smelts to ingot");
                    check(com.snc.energies.world.EndServerTicks.TICKERS.isEmpty(), "no global machine tick registrations remain");
                    SncEnergies.LOGGER.info("FUNCTIONAL TESTS PASSED");
                    ready = false;
                    server.halt(false);
                }
            } catch (Throwable error) { fail(server, error); }
        });
    }
    private void setup(MinecraftServer server) {
        testAdventuresBiomass();
                for (int x=0;x<=7;x++) for (int z=0;z<=1;z++) world.setBlock(new BlockPos(x,100,z),Blocks.AIR.defaultBlockState(),3);
        generator = (CoalGeneratorBlockEntity)place(0, SncBlocks.COAL_GENERATOR);
        place(1, SncBlocks.ENERGY_CABLE);
        place(2, SncBlocks.ENERGY_CABLE);
        furnace = (ElectricFurnaceBlockEntity)place(3, SncBlocks.ELECTRIC_FURNACE);
        world.setBlock(new BlockPos(2,100,1), SncBlocks.CRUSHER.defaultBlockState(),3);
        crusher = (CrusherBlockEntity)world.getBlockEntity(new BlockPos(2,100,1));
        world.setBlock(new BlockPos(1,100,1), SncBlocks.ENERGY_CUBE.defaultBlockState(),3);
        battery = (EnergyCubeBlockEntity)world.getBlockEntity(new BlockPos(1,100,1));
        blocked = (ElectricFurnaceBlockEntity)place(7, SncBlocks.ELECTRIC_FURNACE);
        blocked.setItem(0,new ItemStack(Items.RAW_IRON));
        blocked.setItem(1,new ItemStack(Items.GOLD_INGOT));
        blocked.getEnergyStorage(Direction.UP).insert(10000,false);
        SimpleEnergyStorage single = new SimpleEnergyStorage(10);
        single.insert(1,false);
        check(EnergyTransfer.distribute(world,p(0),single,160) == 1 && single.getEnergy() == 0
            && furnace.getEnergy()+crusher.getEnergy()+battery.getEnergy() == 1, "transfer conserves a one-unit source with a 160-unit budget");
        player = new ServerPlayer(server,world,new GameProfile(UUID.randomUUID(),"MachineTest"),ClientInformation.createDefault());
        check(generator.createMenu(1,player.getInventory(),player) instanceof CoalGeneratorMenu, "generator exposes its menu");
        check(furnace.createMenu(2,player.getInventory(),player) instanceof ElectricFurnaceMenu, "furnace exposes its menu");
        check(crusher.createMenu(3,player.getInventory(),player) instanceof CrusherMenu, "crusher exposes its menu");
        CoalGeneratorMenu clientMenu = new CoalGeneratorMenu(4,player.getInventory());
        clientMenu.setData(0,64000); clientMenu.setData(2,64000); clientMenu.setData(5,1200);
        check(clientMenu.getSyncedEnergy()==64000 && clientMenu.getSyncedCapacity()==64000 && clientMenu.getSyncedBurn()==1200, "menu retains synchronized energy and burn values");
                world.setBlock(p(10), Blocks.AIR.defaultBlockState(), 3);
        world.setBlock(p(11), Blocks.AIR.defaultBlockState(), 3);
        woodStove = (WoodStoveBlockEntity)place(10, SncBlocks.WOOD_STOVE);
        woodFurnace = (ElectricFurnaceBlockEntity)place(11, SncBlocks.ELECTRIC_FURNACE);
        check(woodStove.createMenu(5,player.getInventory(),player) instanceof WoodStoveMenu, "wood stove exposes its own fuel menu");
        check(WoodStoveBlockEntity.woodBurnTicks(new ItemStack(Items.OAK_LOG)) == 600
            && WoodStoveBlockEntity.woodBurnTicks(new ItemStack(Items.OAK_PLANKS)) == 300
            && WoodStoveBlockEntity.woodBurnTicks(new ItemStack(Items.STICK)) == 100, "wood stove accepts logs, planks and sticks");
        check(WoodStoveBlockEntity.woodBurnTicks(new ItemStack(Items.CRIMSON_PLANKS)) == 0
            && WoodStoveBlockEntity.woodBurnTicks(new ItemStack(Items.COAL)) == 0, "wood stove rejects non-wood and nonflammable fuel");
        woodStove.setItem(0, new ItemStack(Items.OAK_LOG));
        woodFurnace.setItem(0, new ItemStack(Items.RAW_IRON));
        testLargeStove(player);
        generator.setItem(0,new ItemStack(Items.COAL));
        furnace.setItem(0,new ItemStack(Items.RAW_IRON));
        crusher.setItem(0,new ItemStack(SncItems.RAW_VOLTAITE));
        ColonialFunctionalTest.run(world, player);
        IndustryFunctionalTest.run(world, player);
        AutomationFunctionalTest.run(world);
        MercadaoFunctionalTest.run(world, player);
        TractorFunctionalTest.run(world, player);
        HarvesterFunctionalTest.run(world, player);
        AgroFunctionalTest.run(world, player);
        // Worldgen probe: the vanilla village is the positive control. If the
        // Mercadão entries are absent while vanilla's are present, the mod's
        // worldgen datapack never loaded (wrong folder layout).
        var structures = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE);
        var structureSets = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.STRUCTURE_SET);
        var templatePools = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TEMPLATE_POOL);
        SncEnergies.LOGGER.info("WORLDGEN structure: mercadao={} village_plains={}",
            structures.get(SncEnergies.id("mercadao")).isPresent(),
            structures.get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "village_plains")).isPresent());
        SncEnergies.LOGGER.info("WORLDGEN structure_set: mercadao={} villages={}",
            structureSets.get(SncEnergies.id("mercadao")).isPresent(),
            structureSets.get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "villages")).isPresent());
        SncEnergies.LOGGER.info("WORLDGEN template_pool: mercadao/start={}",
            templatePools.get(SncEnergies.id("mercadao/start")).isPresent());
        // Ore feature probe: 26.3 keeps configured features in worldgen/feature/
        // (there is no configured_feature/ folder); a regression back to the old
        // layout would silently drop both ores. Vanilla coal is the control, and
        // a registered placed feature transitively proves its configured feature.
        var placedFeatures = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.PLACED_FEATURE);
        SncEnergies.LOGGER.info("WORLDGEN placed_feature: tin_ore={} voltaite_ore={} ore_coal_upper={}",
            placedFeatures.get(SncEnergies.id("tin_ore")).isPresent(),
            placedFeatures.get(SncEnergies.id("voltaite_ore")).isPresent(),
            placedFeatures.get(net.minecraft.resources.Identifier.fromNamespaceAndPath("minecraft", "ore_coal_upper")).isPresent());
        check(placedFeatures.get(SncEnergies.id("tin_ore")).isPresent()
            && placedFeatures.get(SncEnergies.id("voltaite_ore")).isPresent(),
            "ore configured and placed features registered in worldgen");
        check(server.getRecipeManager().byKey(net.minecraft.resources.ResourceKey.create(
            net.minecraft.core.registries.Registries.RECIPE, SncEnergies.id("tractor"))).isPresent(),
            "tractor recipe loaded by RecipeManager");
        // End-to-end placement probe (server console carries the Located/
        // not-found feedback; the village is the control). No radius argument
        // in this version: /locate structure <id> only.
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "locate structure snc_energies:mercadao");
        server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack(),
            "locate structure minecraft:village_plains");
        // Deferred one tick: the vehicle suites above tear their entities down
        // inside this same tick; starting the cart suite on the next tick keeps
        // its entity lookups (hitch search, purge) deterministic in this world.
        cartSuiteTicks = 2;
    }
    private int cartSuiteTicks;

    private int stoveDrops() {
        return world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(0,98,3,16,105,16)).stream()
            .filter(e -> e.getItem().is(SncItems.WOOD_STOVE)).mapToInt(e -> e.getItem().getCount()).sum();
    }
    private void testAdventuresBiomass() {
        var id = net.minecraft.resources.Identifier.fromNamespaceAndPath("intoxicantes", "bagaco_de_cana");
        var registry = net.minecraft.core.registries.BuiltInRegistries.ITEM;
        boolean loaded = com.snc.energies.compat.AdventuresCompatibility.isLoaded();
        check(registry.containsKey(id) == loaded, "companion biomass registry matches the selected test profile");
        check(WoodStoveBlockEntity.woodBurnTicks(ItemStack.EMPTY) == 0, "empty biomass cannot produce energy");
        if (!loaded) return;
        ItemStack bagasse = new ItemStack(registry.getValue(id), 2);
        check(WoodStoveBlockEntity.woodBurnTicks(bagasse) == 100, "Adventures bagasse resolves through the optional biomass tag");
        BlockPos location = new BlockPos(0, 105, 0);
        world.setBlock(location, SncBlocks.WOOD_STOVE.defaultBlockState(), 3);
        var stove = (WoodStoveBlockEntity) world.getBlockEntity(location);
        check(new WoodStoveBlockEntity.FuelSlot(stove, 0, 0, 0).mayPlace(bagasse), "stove menu accepts Adventures bagasse");
        stove.setItem(0, bagasse);
        for (int i = 0; i < 100; i++) stove.serverTick();
        check(stove.getEnergy() == 8000 && stove.getItem(0).getCount() == 1 && stove.getBurnTime() == 0,
                "one bagasse produces exactly 8000 energy and consumes exactly one item");
        var saved = stove.saveWithFullMetadata(world.registryAccess());
        var restored = (WoodStoveBlockEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(
                location, stove.getBlockState(), saved, world.registryAccess());
        check(restored != null && restored.getEnergy() == 8000 && restored.getItem(0).getCount() == 1,
                "biomass inventory and energy survive save/load");
        world.removeBlock(location, false);
    }
    private void testLargeStove(ServerPlayer player) {
        for(int x=1;x<15;x++) for(int z=4;z<14;z++) for(int y=100;y<103;y++) world.setBlock(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState(),3);
        BlockPos anchor=new BlockPos(4,100,8);
        for(Direction facing : new Direction[]{Direction.NORTH,Direction.SOUTH,Direction.EAST,Direction.WEST}) {
            BlockPos obstructed=WoodStoveBlock.cell(anchor,facing,7);
            world.setBlock(obstructed,Blocks.STONE.defaultBlockState(),3);
            check(!WoodStoveBlock.canAssemble(world,anchor,facing) && world.getBlockState(obstructed).is(Blocks.STONE), "obstruction blocks placement without replacing terrain: "+facing);
            world.removeBlock(obstructed,false);
            var state=SncBlocks.WOOD_STOVE.defaultBlockState().setValue(MachineBlock.FACING,facing).setValue(WoodStoveBlock.ASSEMBLED,true);
            check(WoodStoveBlock.canAssemble(world,anchor,facing), "clear footprint accepted: "+facing);
            world.setBlock(anchor,state,3);
            SncBlocks.WOOD_STOVE.setPlacedBy(world,anchor,state,player,new ItemStack(SncItems.WOOD_STOVE));
            var owner=world.getBlockEntity(anchor);
            for(int i=1;i<8;i++) {
                BlockPos part=WoodStoveBlock.cell(anchor,facing,i);
                check(WoodStovePartBlock.controller(world,part,world.getBlockState(part))==owner, "structural cell shares controller "+facing+" / "+i);
            }
            check(world.getBlockState(anchor).getPistonPushReaction()==net.minecraft.world.level.material.PushReaction.IMMOVEABLE,"structure cannot be separated by pistons: "+facing);
            int before=stoveDrops();
            world.destroyBlock(WoodStoveBlock.cell(anchor,facing,7),true);
            for(int i=0;i<8;i++) check(world.getBlockState(WoodStoveBlock.cell(anchor,facing,i)).isAir(),"breaking chimney removes cell "+facing+" / "+i);
            check(stoveDrops()==before+1,"breaking structure drops exactly one stove: "+facing);
        }
        BlockPos pos=new BlockPos(10,100,8);
        var state=SncBlocks.WOOD_STOVE.defaultBlockState().setValue(MachineBlock.FACING,Direction.NORTH).setValue(WoodStoveBlock.ASSEMBLED,true);
        world.setBlock(pos,state,3);
        SncBlocks.WOOD_STOVE.setPlacedBy(world,pos,state,player,new ItemStack(SncItems.WOOD_STOVE));
        largeStove=(WoodStoveBlockEntity)world.getBlockEntity(pos);
        world.setBlock(new BlockPos(13,100,9),SncBlocks.ENERGY_CABLE.defaultBlockState(),3);
        world.setBlock(new BlockPos(14,100,9),SncBlocks.ELECTRIC_FURNACE.defaultBlockState(),3);
        largeFurnace=(ElectricFurnaceBlockEntity)world.getBlockEntity(new BlockPos(14,100,9));
        largeStove.setItem(0,new ItemStack(Items.OAK_LOG));
        largeFurnace.setItem(0,new ItemStack(Items.RAW_IRON));
        check(!woodStove.getBlockState().getValue(WoodStoveBlock.ASSEMBLED),"legacy compact stove keeps its original footprint");
    }
    private void fail(MinecraftServer server, Throwable error) {
        ready=false;
        SncEnergies.LOGGER.error("FUNCTIONAL TESTS FAILED",error);
        server.halt(false);
    }
}
