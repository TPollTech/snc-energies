package com.snc.energies.client.screen;

import com.snc.energies.entity.TractorEntity;
import com.snc.energies.menu.TractorMenu;
import com.snc.energies.menu.MachinePanel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Supply panel: oil gauge, three seed rows, ignition and planter-lift controls. */
public final class TractorScreen extends AbstractContainerScreen<TractorMenu> {
    private final MachinePanel panel;
    private Button ignitionButton;
    private Button liftButton;
    private Button hitchButton;

    public TractorScreen(TractorMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachinePanel.WIDTH, MachinePanel.HEIGHT);
        this.panel = MachinePanel.of("tractor");
    }

    @Override
    protected void init() {
        super.init();
        int[] b = panel.button();
        ignitionButton = addRenderableWidget(Button.builder(ignitionLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TractorMenu.BUTTON_IGNITION);
                }
            }).bounds(leftPos + b[0], topPos + b[1], b[2], b[3]).build());
        liftButton = addRenderableWidget(Button.builder(liftLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TractorMenu.BUTTON_LIFT);
                }
            }).bounds(leftPos + b[0] + 116, topPos + b[1], b[2], b[3]).build());
        hitchButton = addRenderableWidget(Button.builder(hitchLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, TractorMenu.BUTTON_HITCH);
                }
            }).bounds(leftPos + b[0], topPos + b[1] + 22, b[2] + 116, b[3]).build());
    }

    private Component hitchLabel() {
        return Component.translatable(menu.attached() ? "gui.snc_energies.tractor.detach" : "gui.snc_energies.tractor.attach");
    }

    private Component ignitionLabel() {
        return Component.translatable(menu.working() ? "gui.snc_energies.tractor.stop" : "gui.snc_energies.tractor.start");
    }

    private Component liftLabel() {
        return Component.translatable(menu.raised() ? "gui.snc_energies.tractor.lower" : "gui.snc_energies.tractor.raise");
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
        // Oil gauge under the slots.
        int gx = leftPos + 13, gy = topPos + 111, gw = 108, gh = 13;
        g.fill(gx, gy, gx + gw, gy + gh, color("#2c3a42"));
        int fill = (int) Math.round(Math.clamp((double) menu.oil() / TractorEntity.oilCapacity(), 0, 1) * gw);
        if (fill > 0) g.fill(gx, gy, gx + fill, gy + gh, color("#4d7c5f"));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 13, 10, 0xffedf2dc);
        g.text(font, Component.translatable("gui.snc_energies.panel.subtitle.tractor"),
            13, 23, color(panel.accent()));
        String[] roles = {"oil", "row1", "row2", "row3"};
        for (var position : panel.slots()) {
            g.text(font, Component.translatable("gui.snc_energies.tractor.slot." + roles[position.index()]),
                position.x() - 2, position.y() - 12, color(panel.accent()));
        }
        String state = menu.working()
            ? (menu.raised() ? "gui.snc_energies.tractor.state.raised" : "gui.snc_energies.tractor.state.working")
            : "gui.snc_energies.tractor.state.off";
        g.text(font, Component.translatable(state), 139, 115, 0xffe8d9c0);
        g.text(font, Component.translatable("gui.snc_energies.tractor.oil_gauge",
            menu.oil(), TractorEntity.oilCapacity()), 17, 115, 0xffd9e6d4);
        for (int row = 0; row < 3; row++) {
            if (!menu.rowSeeded(row)) {
                int x = panel.position(row + 1).x();
                g.text(font, Component.translatable("gui.snc_energies.tractor.row_empty"), x - 2, 99, 0xffc98f6a);
            }
        }
        g.text(font, playerInventoryTitle, 48, 93, 0xffabbdb6);
        if (ignitionButton != null) ignitionButton.setMessage(ignitionLabel());
        if (liftButton != null) liftButton.setMessage(liftLabel());
        if (hitchButton != null) hitchButton.setMessage(hitchLabel());
    }
}
