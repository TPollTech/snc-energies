package com.snc.energies.client;

import com.snc.energies.SncEnergies;
import com.snc.energies.client.screen.CoalGeneratorScreen;
import com.snc.energies.client.screen.CrusherScreen;
import com.snc.energies.client.screen.ElectricFurnaceScreen;
import com.snc.energies.registry.SncMenus;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/**
 * Client entrypoint: registers machine screens via the (widened) vanilla API.
 */
public class SncEnergiesClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
        SncMenus.INDUSTRY.values().forEach(type -> MenuScreens.register(type, com.snc.energies.client.screen.IndustrialScreen::new));
        MenuScreens.register(SncMenus.SEED_PRESS, com.snc.energies.client.screen.WorkshopScreen::new);
        MenuScreens.register(SncMenus.ENERGY_CUBE, com.snc.energies.client.screen.EnergyCubeScreen::new);
        MenuScreens.register(SncMenus.INDUSTRIAL, com.snc.energies.client.screen.IndustrialScreen::new);
        MenuScreens.register(SncMenus.WORKSHOP, com.snc.energies.client.screen.WorkshopScreen::new);
        com.snc.energies.item.FieldGuideItem.registerOpener(() -> net.minecraft.client.Minecraft.getInstance()
            .gui.setScreen(new com.snc.energies.client.screen.FieldGuideScreen()));
		MenuScreens.register(SncMenus.COAL_GENERATOR, CoalGeneratorScreen::new);
        MenuScreens.register(SncMenus.WOOD_STOVE, com.snc.energies.client.screen.WoodStoveScreen::new);
		MenuScreens.register(SncMenus.ELECTRIC_FURNACE, ElectricFurnaceScreen::new);
		MenuScreens.register(SncMenus.CRUSHER, CrusherScreen::new);
        MenuScreens.register(SncMenus.ITEM_PIPE_FILTER, com.snc.energies.client.screen.ItemPipeFilterScreen::new);
        MenuScreens.register(SncMenus.TRACTOR, com.snc.energies.client.screen.TractorScreen::new);
        MenuScreens.register(SncMenus.PLANTER, com.snc.energies.client.screen.PlanterScreen::new);
        MenuScreens.register(SncMenus.GRAIN_CART, com.snc.energies.client.screen.GrainCartScreen::new);
        MenuScreens.register(SncMenus.MERCADAO, com.snc.energies.client.screen.MercadaoScreen::new);
        com.snc.energies.client.mercajeiro.MercajeiroRenderer.bootstrap();
        net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry.register(
            com.snc.energies.registry.SncEntities.MERCAJEIRO, com.snc.energies.client.mercajeiro.MercajeiroRenderer::new);
        net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
            com.snc.energies.registry.SncBlockEntities.MERCADAO_SHELF, com.snc.energies.client.mercajeiro.MercadaoShelfRenderer::new);
        MenuScreens.register(SncMenus.HARVESTER, com.snc.energies.client.screen.HarvesterScreen::new);
        com.snc.energies.client.vehicle.TractorClient.bootstrap();
		SncEnergies.LOGGER.info("SNC Energies client ready");
	}
}
