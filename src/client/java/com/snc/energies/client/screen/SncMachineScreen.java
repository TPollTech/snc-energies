package com.snc.energies.client.screen;

import com.snc.energies.SncEnergies;
import com.snc.energies.menu.SncMachineMenu;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * Base screen: custom HD-styled background, energy bar and progress arrow
 * drawn crisply by code (MC 26.2 extraction render API).
 */
public abstract class SncMachineScreen<T extends SncMachineMenu> extends AbstractContainerScreen<T> {
	protected final Identifier background;

	protected SncMachineScreen(T menu, Inventory inv, Component title, String backgroundName) {
		super(menu, inv, title, 176, 166);
		this.background = SncEnergies.id("textures/gui/" + backgroundName + ".png");
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		g.blit(RenderPipelines.GUI_TEXTURED, background, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight, imageWidth, imageHeight);
		renderEnergyBar(g);
		renderProgress(g);
	}

	protected void renderEnergyBar(GuiGraphicsExtractor g) {
		long energy = menu.getSyncedEnergy();
		long capacity = menu.getSyncedCapacity();
		if (capacity <= 0) {
			return;
		}
		int fillHeight = (int) Math.round(64.0 * energy / capacity);
		int x = energyBarX();
		int y = energyBarY();
		if (fillHeight > 0) {
			g.fill(x, y + (64 - fillHeight), x + 14, y + 64, 0xFF40DCFF);
			g.fill(x, y + (64 - fillHeight), x + 14, y + (64 - fillHeight) + 2, 0xFFA8F6FF);
		}
	}

	protected abstract int energyBarX();

	protected abstract int energyBarY();

	protected void renderProgress(GuiGraphicsExtractor g) {
		// default: no arrow
	}

	protected void renderProgressArrow(GuiGraphicsExtractor g, int x, int y) {
		int progress = menu.getSyncedProgress();
		int total = Math.max(1, menu.getSyncedBurnTotal());
		int width = 24 * progress / total;
		if (width > 0) {
			g.fill(x, y, x + width, y + 4, 0xFFFFCA5C);
			g.fill(x, y + 4, x + 24, y + 8, 0x40FFFFFF);
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
		g.text(font, title, titleLabelX, titleLabelY, 0xFFE8E4DA);
		g.text(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xFFA0A4AC);
	}
}
