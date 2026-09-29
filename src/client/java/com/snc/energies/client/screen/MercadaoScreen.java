package com.snc.energies.client.screen;

import com.snc.energies.menu.MercadaoMenu;
import com.snc.energies.menu.MachinePanel;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/**
 * The Mercadão counter screen: real shelf items rendered in the panel, one
 * buy action per position (click the item, vanilla-trading style), live
 * wallet balance and the 07:00 restock note. Purchase goes through the menu
 * button channel (server-authoritative), never client-side.
 */
public final class MercadaoScreen extends AbstractContainerScreen<MercadaoMenu> {
    private static final MachinePanel PANEL = MachinePanel.of("mercadao");
    private static final int GRID_X = 16;
    private static final int GRID_Y = 44;
    private static final int CELL = 20;
    private static final int ROW_STEP = 34;
    private static final int COLS = 5;

    public MercadaoScreen(MercadaoMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, MachinePanel.WIDTH, MachinePanel.HEIGHT);
    }

    private static int color(String value) {
        return 0xff000000 | Integer.parseInt(value.substring(1), 16);
    }

    private int cellX(int col) {
        return leftPos + GRID_X + 2 + col * CELL;
    }

    private int cellY(int row) {
        return topPos + GRID_Y + row * ROW_STEP;
    }

    /** Grid position under the mouse, or -1. */
    private int hoveredPosition(double mouseX, double mouseY) {
        int col = (int) ((mouseX - leftPos - GRID_X - 2) / CELL);
        int row = (int) ((mouseY - topPos - GRID_Y) / ROW_STEP);
        if (col < 0 || col >= COLS || row < 0 || row >= 2) return -1;
        int x = cellX(col);
        int y = cellY(row);
        if (mouseX < x || mouseX >= x + 16 || mouseY < y || mouseY >= y + 13) return -1;
        return row * COLS + col;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubled) {
        int position = hoveredPosition(event.x(), event.y());
        if (position >= 0 && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, position);
            return true;
        }
        return super.mouseClicked(event, doubled);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        for (var r : PANEL.rects()) {
            int x = ((Number) r.get(0)).intValue() + leftPos;
            int y = ((Number) r.get(1)).intValue() + topPos;
            g.fill(x, y, x + ((Number) r.get(2)).intValue(), y + ((Number) r.get(3)).intValue(),
                color((String) r.get(4)));
        }
        int row = 0;
        int col = 0;
        for (MercadaoMenu.Entry entry : menu.entries()) {
            int x = cellX(col);
            int y = cellY(row);
            if (entry != null) {
                if (entry.stock() > 0) {
                    g.item(entry.offer().stack(), x, y);
                    g.itemDecorations(font, entry.offer().stack(), x, y);
                } else {
                    g.fill(x, y, x + 16, y + 16, color("#171d20"));
                }
                g.text(font, "R$ " + entry.price(), x, y + 17,
                    entry.stock() > 0 ? 0xffd9e6d4 : 0xff8a6a4a);
            }
            col++;
            if (col == COLS) { col = 0; row++; }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        g.text(font, title, 13, 10, 0xffedf2dc);
        g.text(font, Component.translatable("gui.snc_energies.panel.subtitle.mercadao"), 13, 23,
            color(PANEL.accent()));
        g.text(font, menu.shelfTitle(), 13, 33, 0xffc9d8a8);
        g.text(font, Component.translatable("gui.snc_energies.mercadao.money", menu.money()),
            150, 23, 0xfff2dc9a);
        g.text(font, Component.translatable("gui.snc_energies.mercadao.restock"), 150, 33, 0xff9aa89a);
        g.text(font, playerInventoryTitle, 48, 145, 0xffabbdb6);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        super.extractTooltip(g, mouseX, mouseY);
        int position = hoveredPosition(mouseX, mouseY);
        if (position >= 0) {
            var entry = menu.entries().get(position);
            if (entry != null) {
                g.setTooltipForNextFrame(Component.translatable(
                    entry.stock() > 0 ? "gui.snc_energies.mercadao.stock"
                        : "gui.snc_energies.mercadao.sold_out_short",
                    entry.offer().stack().getHoverName(), entry.stock()), mouseX, mouseY);
            }
        }
    }
}
