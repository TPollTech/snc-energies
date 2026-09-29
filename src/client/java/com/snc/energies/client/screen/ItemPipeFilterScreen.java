package com.snc.energies.client.screen;

import com.snc.energies.menu.ItemPipeFilterMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * Duct whitelist editor drawn with the same vector primitives as the
 * machine panels: read-only buffer slot, 3x3 whitelist, clear button.
 */
public final class ItemPipeFilterScreen extends AbstractContainerScreen<ItemPipeFilterMenu> {
    private static final int WIDTH = 176, HEIGHT = 166;
    private static final int INK = 0xffe8efe6, MUTED = 0xff9fb0a4, ACCENT = 0xffe5a03c;
    private static final int PANEL = 0xff2b3540, DARK = 0xff1a222b, SLOT = 0xff12161b;

    public ItemPipeFilterScreen(ItemPipeFilterMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH, HEIGHT);
    }

    @Override protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("gui.snc_energies.pipe_filter.clear"), button -> {
            if (minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
        }).bounds(leftPos + 128, topPos + 48, 40, 18).build());
    }

    private void frame(GuiGraphicsExtractor g, int x, int y, int w, int h, int fill, int border) {
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, border);
        g.fill(x, y, x + w, y + h, fill);
    }

    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        g.fill(leftPos, topPos, leftPos + WIDTH, topPos + HEIGHT, PANEL);
        g.fill(leftPos + 2, topPos + 2, leftPos + WIDTH - 2, topPos + HEIGHT - 2, DARK);
        g.fill(leftPos + 6, topPos + 6, leftPos + WIDTH - 6, topPos + 20, SLOT);
        g.fill(leftPos + 6, topPos + 24, leftPos + WIDTH - 6, topPos + 25, ACCENT);
        // Buffer cell with a frame highlight when the duct carries an item.
        boolean carrying = menu.bufferedItemId() != 0;
        frame(g, leftPos + 80, topPos + 20, 16, 16, SLOT, carrying ? ACCENT : 0xff3c4a56);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 3; col++) {
            frame(g, leftPos + 62 + col * 18, topPos + 48 + row * 18, 16, 16, SLOT, 0xff3c4a56);
        }
    }

    @Override protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 8, 10, INK);
        g.text(font, Component.translatable("gui.snc_energies.pipe_filter.buffer"), 8, 27, MUTED);
        g.text(font, Component.translatable("gui.snc_energies.pipe_filter.whitelist"), 8, 39, MUTED);
        g.text(font, Component.translatable("gui.snc_energies.pipe_filter.hint"), 8, 106, MUTED);
        g.text(font, playerInventoryTitle, 48, 145, 0xffabbdb6);
    }
}
