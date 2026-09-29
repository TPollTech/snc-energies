package com.snc.energies.client.screen;

import com.snc.energies.menu.PlanterMenu;
import com.snc.energies.menu.MachinePanel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Planter tank panel: one visible slot per seed row and the operator inventory. */
public final class PlanterScreen extends AbstractContainerScreen<PlanterMenu> {
    private final MachinePanel panel;

    public PlanterScreen(PlanterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachinePanel.WIDTH, MachinePanel.HEIGHT);
        this.panel = MachinePanel.of("planter");
    }

    private static int color(String value) {
        return 0xff000000 | Integer.parseInt(value.substring(1), 16);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        for (var r : panel.rects()) {
            int x = ((Number) r.get(0)).intValue() + leftPos;
            int y = ((Number) r.get(1)).intValue() + topPos;
            g.fill(x, y, x + ((Number) r.get(2)).intValue(), y + ((Number) r.get(3)).intValue(),
                color((String) r.get(4)));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 13, 10, 0xffedf2dc);
        g.text(font, Component.translatable("gui.snc_energies.panel.subtitle.planter"),
            13, 23, color(panel.accent()));
        for (var position : panel.slots()) {
            g.text(font, Component.translatable("gui.snc_energies.planter.slot." + position.role()),
                position.x() - 2, position.y() - 12, color(panel.accent()));
        }
        g.text(font, playerInventoryTitle, 48, 93, 0xffabbdb6);
    }
}
