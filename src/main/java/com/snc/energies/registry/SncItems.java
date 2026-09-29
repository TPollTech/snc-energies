package com.snc.energies.registry;

import com.snc.energies.SncEnergies;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Item registry + creative tab (AGENTS.md rule: one place only).
 */
public final class SncItems {
    public static final java.util.Map<IndustryKind, Item> INDUSTRY = registerIndustry();
    private static java.util.Map<IndustryKind, Item> registerIndustry() {
        var items = new java.util.EnumMap<IndustryKind, Item>(IndustryKind.class);
        SncBlocks.INDUSTRY.forEach((kind, block) -> items.put(kind, register(kind.id, SncBlocks.itemOf(block))));
        return java.util.Collections.unmodifiableMap(items);
    }
    public static final Item STEAM_PIPE = register("steam_pipe", SncBlocks.itemOf(SncBlocks.STEAM_PIPE));
    public static final Item ITEM_PIPE = register("item_pipe", SncBlocks.itemOf(SncBlocks.ITEM_PIPE));
    public static final Item TIN_ORE = register("tin_ore", SncBlocks.itemOf(SncBlocks.TIN_ORE));
    public static final Item DEEPSLATE_TIN_ORE = register("deepslate_tin_ore", SncBlocks.itemOf(SncBlocks.DEEPSLATE_TIN_ORE));
    public static final Item RAW_TIN = registerMaterial("raw_tin");
    public static final Item TIN_INGOT = registerMaterial("tin_ingot");
    public static final Item BRONZE_INGOT = registerMaterial("bronze_ingot");
    public static final Item STEEL_INGOT = registerMaterial("steel_ingot");
    public static final Item STEEL_PLATE = registerMaterial("steel_plate");
    public static final Item COPPER_WIRE = registerMaterial("copper_wire");
    public static final Item STEEL_GEAR = registerMaterial("steel_gear");
    public static final Item BASIC_CIRCUIT = registerMaterial("basic_circuit");
    public static final Item INSULATED_PLATE = registerMaterial("insulated_plate");
    public static final Item REFINED_VOLTAITE = registerMaterial("refined_voltaite");
    public static final Item ADVANCED_CIRCUIT = registerMaterial("advanced_circuit");
    public static final Item MINERAL_MATRIX = registerMaterial("mineral_matrix");
    public static final Item SAWDUST = registerMaterial("sawdust");
    public static final Item IRON_DUST = registerMaterial("iron_dust");
    public static final Item COPPER_DUST = registerMaterial("copper_dust");
    public static final Item GOLD_DUST = registerMaterial("gold_dust");
    public static final Item TIN_DUST = registerMaterial("tin_dust");
    public static final Item GRAIN_MILL = register("grain_mill", SncBlocks.itemOf(SncBlocks.GRAIN_MILL));
    public static final Item SEED_PRESS = register("seed_press", SncBlocks.itemOf(SncBlocks.SEED_PRESS));
    public static final Item RICE_SEEDS = registerSeed("rice_seeds", SncBlocks.RICE_CROP);
    public static final Item SOY_SEEDS = registerSeed("soy_seeds", SncBlocks.SOY_CROP);
    public static final Item MATE_SEEDS = registerSeed("mate_seeds", SncBlocks.MATE_CROP);
    public static final Item MATE_LEAF = registerMaterial("mate_leaf");
    public static final Item DRIED_MATE = registerMaterial("dried_mate");
    public static final Item GROUND_MATE = registerMaterial("ground_mate");
    public static final Item MATE_INFUSION = register("mate_infusion", new Item(new Item.Properties()
        .setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("mate_infusion"))).stacksTo(1)
        .food(new net.minecraft.world.food.FoodProperties.Builder().nutrition(2).saturationModifier(.2f).alwaysEdible().build(),
            net.minecraft.world.item.component.Consumables.DEFAULT_DRINK).usingConvertsTo(net.minecraft.world.item.Items.BOWL)));
    public static final Item RICE_PADDY = registerMaterial("rice_paddy");
    public static final Item RICE = registerMaterial("rice");
    public static final Item SOYBEAN = registerMaterial("soybean");
    public static final Item FLOUR = registerMaterial("flour");
    public static final Item RICE_HUSK = registerMaterial("rice_husk");
    public static final Item VEGETABLE_OIL = registerMaterial("vegetable_oil");
    public static final Item SOY_MEAL = registerMaterial("soy_meal");
    public static final Item BIOMASS_BRIQUETTE = registerMaterial("biomass_briquette");
    public static final Item FIELD_GUIDE = register("field_guide", new com.snc.energies.item.FieldGuideItem(
        new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("field_guide")))));
    /** Configures industrial side modes and inverted redstone; cycles on use. */
    public static final Item SCREWDRIVER = register("screwdriver", new com.snc.energies.item.ScrewdriverItem(
        new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("screwdriver")))));
    /** Places the approved SNC 75 tractor; use on a tractor opens its supply panel. */
    public static final Item TRACTOR = register("tractor", new com.snc.energies.item.TractorItem(
        new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("tractor")))));
    /** The Mercadão counter shelf: four shelf variants in one block. */
    public static final Item MERCADAO_SHELF = register("mercadao_shelf", SncBlocks.itemOf(SncBlocks.MERCADAO_SHELF));
    /** Spawns the Mercadão attendant (26.3 eggs carry TypedEntityData<EntityType>). */
    public static final Item MERCAJEIRO_EGG = register("mercajeiro_spawn_egg", new net.minecraft.world.item.SpawnEggItem(
        new Item.Properties().stacksTo(64)
            .setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("mercajeiro_spawn_egg")))
            .component(net.minecraft.core.component.DataComponents.ENTITY_DATA,
                net.minecraft.world.item.component.TypedEntityData.of(SncEntities.MERCAJEIRO, new net.minecraft.nbt.CompoundTag()))));
    /** Places the SNC 75-P detachable planter implement. */
    public static final Item PLANTER = register("planter", new com.snc.energies.item.PlanterItem(
        new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("planter")))));
    /** Places the approved SNC 90 harvester; use on a harvester opens its supply panel. */
    public static final Item HARVESTER = register("harvester", new com.snc.energies.item.HarvesterItem(
        new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("harvester")))));
    /** Places the SNC 90-C grain cart; use on a cart opens its tank panel. */
    public static final Item GRAIN_CART = register("grain_cart", new com.snc.energies.item.GrainCartItem(
        new Item.Properties().stacksTo(1).setId(ResourceKey.create(Registries.ITEM, SncEnergies.id("grain_cart")))));

    private static Item registerSeed(String name, net.minecraft.world.level.block.Block crop) {
        return register(name, new net.minecraft.world.item.BlockItem(crop,
            new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SncEnergies.id(name)))));
    }
    public static final Item WOOD_STOVE = register("wood_stove", SncBlocks.itemOf(SncBlocks.WOOD_STOVE));
	public static final Item RAW_VOLTAITE = registerMaterial("raw_voltaite");
	public static final Item VOLTAITE_DUST = registerMaterial("voltaite_dust");
	public static final Item VOLTAITE_INGOT = registerMaterial("voltaite_ingot");

	public static final Item COAL_GENERATOR = register("coal_generator", SncBlocks.itemOf(SncBlocks.COAL_GENERATOR));
	public static final Item ELECTRIC_FURNACE = register("electric_furnace", SncBlocks.itemOf(SncBlocks.ELECTRIC_FURNACE));
	public static final Item CRUSHER = register("crusher", SncBlocks.itemOf(SncBlocks.CRUSHER));
	public static final Item ENERGY_CUBE = register("energy_cube", SncBlocks.itemOf(SncBlocks.ENERGY_CUBE));
	public static final Item ENERGY_CABLE = register("energy_cable", SncBlocks.itemOf(SncBlocks.ENERGY_CABLE));
	public static final Item VOLTAITE_ORE = register("voltaite_ore", SncBlocks.itemOf(SncBlocks.VOLTAITE_ORE));
	public static final Item DEEPSLATE_VOLTAITE_ORE = register("deepslate_voltaite_ore", SncBlocks.itemOf(SncBlocks.DEEPSLATE_VOLTAITE_ORE));

	private static Item registerMaterial(String name) {
		return register(name, new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, SncEnergies.id(name)))));
	}

	private SncItems() {
	}

	private static Item register(String name, Item item) {
		return Registry.register(BuiltInRegistries.ITEM, SncEnergies.id(name), item);
	}

	public static void bootstrap() {
		CreativeModeTab tab = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
				.title(Component.translatable("itemGroup.snc_energies.main"))
				.icon(() -> new ItemStack(COAL_GENERATOR))
				.displayItems((params, output) -> {
                    output.accept(FIELD_GUIDE); output.accept(SCREWDRIVER); output.accept(TRACTOR);
                    output.accept(MERCADAO_SHELF); output.accept(MERCAJEIRO_EGG);
                    output.accept(PLANTER); output.accept(HARVESTER); output.accept(GRAIN_CART);
                    INDUSTRY.values().forEach(output::accept); output.accept(STEAM_PIPE); output.accept(ITEM_PIPE);
                    for(Item material : new Item[]{TIN_ORE,DEEPSLATE_TIN_ORE,RAW_TIN,TIN_INGOT,BRONZE_INGOT,STEEL_INGOT,
                        STEEL_PLATE,COPPER_WIRE,STEEL_GEAR,BASIC_CIRCUIT,INSULATED_PLATE,REFINED_VOLTAITE,ADVANCED_CIRCUIT,MINERAL_MATRIX,
                        SAWDUST,IRON_DUST,COPPER_DUST,GOLD_DUST,TIN_DUST}) output.accept(material);
                    output.accept(GRAIN_MILL); output.accept(SEED_PRESS);
                    output.accept(RICE_SEEDS); output.accept(SOY_SEEDS);
                    output.accept(MATE_SEEDS); output.accept(MATE_LEAF); output.accept(DRIED_MATE); output.accept(GROUND_MATE); output.accept(MATE_INFUSION);
                    output.accept(RICE_PADDY); output.accept(RICE); output.accept(SOYBEAN);
                    output.accept(FLOUR); output.accept(RICE_HUSK); output.accept(VEGETABLE_OIL);
                    output.accept(SOY_MEAL); output.accept(BIOMASS_BRIQUETTE);
					output.accept(COAL_GENERATOR);
                    output.accept(WOOD_STOVE);
					output.accept(ELECTRIC_FURNACE);
					output.accept(CRUSHER);
					output.accept(ENERGY_CUBE);
					output.accept(ENERGY_CABLE);
					output.accept(RAW_VOLTAITE);
					output.accept(VOLTAITE_DUST);
					output.accept(VOLTAITE_INGOT);
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB,
				ResourceKey.create(Registries.CREATIVE_MODE_TAB, SncEnergies.id("main")), tab);
		SncEnergies.LOGGER.info("SNC Energies items registered");
	}
}
