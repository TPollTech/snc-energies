package com.snc.energies.registry;

import com.snc.energies.SncEnergies;
import com.snc.energies.menu.CoalGeneratorMenu;
import com.snc.energies.menu.CrusherMenu;
import com.snc.energies.menu.ElectricFurnaceMenu;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

/**
 * Menu type registry.
 */
public final class SncMenus {
    public static final java.util.Map<IndustryKind,MenuType<com.snc.energies.menu.IndustrialMenu>> INDUSTRY = industryTypes();
    private static java.util.Map<IndustryKind,MenuType<com.snc.energies.menu.IndustrialMenu>> industryTypes(){
        var result=new java.util.EnumMap<IndustryKind,MenuType<com.snc.energies.menu.IndustrialMenu>>(IndustryKind.class);
        for(var kind:IndustryKind.values())result.put(kind,Registry.register(BuiltInRegistries.MENU,SncEnergies.id(kind.id),
            new MenuType<>((id,inventory)->new com.snc.energies.menu.IndustrialMenu(id,inventory,kind),FeatureFlags.VANILLA_SET)));
        return java.util.Map.copyOf(result);
    }
    public static final MenuType<com.snc.energies.menu.WorkshopMenu> SEED_PRESS=Registry.register(BuiltInRegistries.MENU,
        SncEnergies.id("seed_press"),new MenuType<>((id,inventory)->new com.snc.energies.menu.WorkshopMenu(id,inventory,true),FeatureFlags.VANILLA_SET));
    public static final MenuType<com.snc.energies.menu.EnergyCubeMenu> ENERGY_CUBE=Registry.register(BuiltInRegistries.MENU,
        SncEnergies.id("energy_cube"),new MenuType<>(com.snc.energies.menu.EnergyCubeMenu::new,FeatureFlags.VANILLA_SET));
    public static final MenuType<com.snc.energies.menu.IndustrialMenu> INDUSTRIAL = Registry.register(
        BuiltInRegistries.MENU, SncEnergies.id("industrial"), new MenuType<>(com.snc.energies.menu.IndustrialMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<com.snc.energies.menu.WorkshopMenu> WORKSHOP = Registry.register(
        BuiltInRegistries.MENU, SncEnergies.id("workshop"),
        new MenuType<>(com.snc.energies.menu.WorkshopMenu::new, FeatureFlags.VANILLA_SET));
    public static final MenuType<com.snc.energies.menu.WoodStoveMenu> WOOD_STOVE = Registry.register(
            BuiltInRegistries.MENU, SncEnergies.id("wood_stove"),
            new MenuType<>(com.snc.energies.menu.WoodStoveMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<CoalGeneratorMenu> COAL_GENERATOR = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("coal_generator"),
			new MenuType<>(CoalGeneratorMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<ElectricFurnaceMenu> ELECTRIC_FURNACE = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("electric_furnace"),
			new MenuType<>(ElectricFurnaceMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<CrusherMenu> CRUSHER = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("crusher"),
			new MenuType<>(CrusherMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<com.snc.energies.menu.ItemPipeFilterMenu> ITEM_PIPE_FILTER = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("item_pipe_filter"),
			new MenuType<>(com.snc.energies.menu.ItemPipeFilterMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<com.snc.energies.menu.TractorMenu> TRACTOR = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("tractor"),
			new MenuType<>(com.snc.energies.menu.TractorMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<com.snc.energies.menu.MercadaoMenu> MERCADAO = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("mercadao"),
			new MenuType<>(com.snc.energies.menu.MercadaoMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<com.snc.energies.menu.PlanterMenu> PLANTER = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("planter"),
			new MenuType<>(com.snc.energies.menu.PlanterMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<com.snc.energies.menu.HarvesterMenu> HARVESTER = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("harvester"),
			new MenuType<>(com.snc.energies.menu.HarvesterMenu::new, FeatureFlags.VANILLA_SET));
	public static final MenuType<com.snc.energies.menu.GrainCartMenu> GRAIN_CART = Registry.register(
			BuiltInRegistries.MENU, SncEnergies.id("grain_cart"),
			new MenuType<>(com.snc.energies.menu.GrainCartMenu::new, FeatureFlags.VANILLA_SET));

	private SncMenus() {
	}

	public static void bootstrap() {
		SncEnergies.LOGGER.info("SNC Energies menus registered");
	}
}
