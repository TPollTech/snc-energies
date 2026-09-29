package com.snc.energies.client.screen;

import com.snc.energies.entity.HarvesterEntity;
import com.snc.energies.menu.HarvesterMenu;
import com.snc.energies.menu.MachinePanel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Supply panel: oil gauge, grain-tank row, ignition and header-lift controls. */
public final class HarvesterScreen extends AbstractContainerScreen<HarvesterMenu> {
    private final MachinePanel panel;
    private Button ignitionButton;
    private Button headerButton;
    private Button cartButton;
    private Button unloadButton;

    public HarvesterScreen(HarvesterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachinePanel.WIDTH, MachinePanel.HEIGHT);
        this.panel = MachinePanel.of("harvester");
    }

    @Override
    protected void init() {
        super.init();
        int[] b = panel.button();
        ignitionButton = addRenderableWidget(Button.builder(ignitionLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, HarvesterMenu.BUTTON_IGNITION);
                }
            }).bounds(leftPos + b[0], topPos + b[1], b[2], b[3]).build());
        headerButton = addRenderableWidget(Button.builder(headerLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, HarvesterMenu.BUTTON_HEADER);
                }
            }).bounds(leftPos + b[0] + 116, topPos + b[1], b[2], b[3]).build());
        cartButton = addRenderableWidget(Button.builder(cartLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, HarvesterMenu.BUTTON_CART);
                }
            }).bounds(leftPos + b[0], topPos + b[1] + 22, b[2], b[3]).build());
        unloadButton = addRenderableWidget(Button.builder(unloadLabel(), button -> {
                if (minecraft.gameMode != null) {
                    minecraft.gameMode.handleInventoryButtonClick(menu.containerId, HarvesterMenu.BUTTON_UNLOAD_CART);
                }
            }).bounds(leftPos + b[0] + 116, topPos + b[1] + 22, b[2], b[3]).build());
    }

    private Component cartLabel() {
        return Component.translatable(menu.cartAttached()
            ? "gui.snc_energies.harvester.detach_cart" : "gui.snc_energies.harvester.attach_cart");
    }

    private Component unloadLabel() {
        return Component.translatable("gui.snc_energies.harvester.unload_cart");
    }

    private Component ignitionLabel() {
        return Component.translatable(menu.working() ? "gui.snc_energies.harvester.stop" : "gui.snc_energies.harvester.start");
    }

    private Component headerLabel() {
        return Component.translatable(menu.raised() ? "gui.snc_energies.harvester.lower" : "gui.snc_energies.harvester.raise");
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
        int fill = (int) Math.round(Math.clamp((double) menu.oil() / HarvesterEntity.oilCapacity(), 0, 1) * gw);
        if (fill > 0) g.fill(gx, gy, gx + fill, gy + gh, color("#4d7c5f"));
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 13, 10, 0xffedf2dc);
        g.text(font, Component.translatable("gui.snc_energies.panel.subtitle.harvester"),
            13, 23, color(panel.accent()));
        String[] roles = {"oil", "t1", "t2", "t3", "t4", "t5", "t6", "t7", "t8", "t9"};
        for (var position : panel.slots()) {
            g.text(font, Component.translatable("gui.snc_energies.harvester.slot." + roles[position.index()]),
                position.x() - 2, position.y() - 12, color(panel.accent()));
        }
        String state = menu.working()
            ? (menu.raised() ? "gui.snc_energies.harvester.state.raised" : "gui.snc_energies.harvester.state.working")
            : "gui.snc_energies.harvester.state.off";
        g.text(font, Component.translatable(state), 139, 115, 0xffe8d9c0);
        g.text(font, Component.translatable("gui.snc_energies.harvester.oil_gauge",
            menu.oil(), HarvesterEntity.oilCapacity()), 17, 115, 0xffd9e6d4);
        g.text(font, playerInventoryTitle, 48, 93, 0xffabbdb6);
        if (ignitionButton != null) ignitionButton.setMessage(ignitionLabel());
        if (headerButton != null) headerButton.setMessage(headerLabel());
        if (cartButton != null) cartButton.setMessage(cartLabel());
    }
}
