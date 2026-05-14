package com.quantcraft.screen;

import com.quantcraft.screen.CommodityExchangeScreenHandler.CommodityRow;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import java.util.List;

public class CommodityExchangeScreen extends HandledScreen<CommodityExchangeScreenHandler> {
    private static final int BG    = 0xFF1a1a2e;
    private static final int PANEL = 0xFF16213e;
    private static final int GOLD  = 0xFFe2b96f;
    private static final int GREEN = 0xFF00ff88;
    private static final int RED   = 0xFFff4466;
    private static final int GRAY  = 0xFF8888aa;
    private static final int SEL   = 0xFF0f3460;
    private static final int ROWS  = 12;
    private static final int ROW_H = 13;

    private int scroll = 0;

    public CommodityExchangeScreen(CommodityExchangeScreenHandler h, PlayerInventory inv, Text title) {
        super(h, inv, title);
        backgroundWidth  = 340;
        backgroundHeight = 230;
    }

    @Override protected void init() {
        super.init();
        int rx = (width - backgroundWidth) / 2 + 210, y = (height - backgroundHeight) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Buy 1"),   btn -> click(100)).dimensions(rx + 5,  y + 60,  55, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sell 1"),  btn -> click(101)).dimensions(rx + 65, y + 60,  55, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Buy 16"),  btn -> click(102)).dimensions(rx + 5,  y + 80,  55, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sell 16"), btn -> click(103)).dimensions(rx + 65, y + 80,  55, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Buy 64"),  btn -> click(104)).dimensions(rx + 5,  y + 100, 55, 16).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sell 64"), btn -> click(105)).dimensions(rx + 65, y + 100, 55, 16).build());
    }

    private void click(int id) { if (client != null) client.interactionManager.clickButton(handler.syncId, id); }

    @Override protected void drawBackground(DrawContext ctx, float d, int mx, int my) {
        int x = (width - backgroundWidth) / 2, y = (height - backgroundHeight) / 2;
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, BG);
        ctx.fill(x, y, x + 205, y + backgroundHeight, PANEL);
        ctx.fill(x + 205, y, x + 206, y + backgroundHeight, 0xFF334455);
    }

    @Override protected void drawForeground(DrawContext ctx, int mx, int my) {
        List<CommodityRow> rows = handler.rows;
        int sel = handler.getSelectedIndex();
        ctx.drawText(textRenderer, "COMMODITY EXCHANGE", 6, 5, GOLD, false);
        ctx.drawText(textRenderer, String.format("%.1f¢", handler.playerBalance), 6, 15, 0xFFddddee, false);
        ctx.fill(0, 24, 204, 25, 0xFF334455);
        ctx.drawText(textRenderer, "ITEM", 6,   27, GRAY,  false);
        ctx.drawText(textRenderer, "BUY",  88,  27, GREEN, false);
        ctx.drawText(textRenderer, "SELL", 132, 27, RED,   false);
        ctx.drawText(textRenderer, "HELD", 172, 27, GRAY,  false);
        ctx.fill(0, 35, 204, 36, 0xFF334455);
        int sy = 38;
        for (int i = scroll; i < Math.min(rows.size(), scroll + ROWS); i++) {
            CommodityRow row = rows.get(i);
            int          ry  = sy + (i - scroll) * ROW_H;
            if (i == sel) ctx.fill(0, ry - 1, 204, ry + ROW_H - 1, SEL);
            ctx.drawText(textRenderer, row.def().ticker(), 6, ry, 0xFFddddee, false);
            ctx.drawText(textRenderer, String.format("%.1f", row.buyPrice()),  88,  ry, GREEN, false);
            ctx.drawText(textRenderer, String.format("%.1f", row.sellPrice()), 132, ry, RED,   false);
            if (row.playerHeld() > 0)
                ctx.drawText(textRenderer, String.valueOf(row.playerHeld()), 174, ry, GOLD, false);
        }
        // Right panel
        int rx = 212;
        if (sel < rows.size()) {
            CommodityRow r = rows.get(sel);
            ctx.drawText(textRenderer, r.def().displayName(), rx, 6, GOLD, false);
            ctx.fill(rx, 16, backgroundWidth - 4, 17, 0xFF334455);
            ctx.drawText(textRenderer, "Buy:",  rx,      22, GRAY, false);
            ctx.drawText(textRenderer, String.format("%.2f¢", r.buyPrice()),  rx + 30, 22, GREEN, false);
            ctx.drawText(textRenderer, "Sell:", rx,      32, GRAY, false);
            ctx.drawText(textRenderer, String.format("%.2f¢", r.sellPrice()), rx + 30, 32, RED,   false);
            ctx.drawText(textRenderer, "Held:", rx,      42, GRAY, false);
            ctx.drawText(textRenderer, r.playerHeld() + " items", rx + 32, 42, 0xFFffffff, false);
            ctx.fill(rx, 52, backgroundWidth - 4, 53, 0xFF334455);
            ctx.drawText(textRenderer, "Prices track stock market.", rx, backgroundHeight - 20, GRAY, false);
            ctx.drawText(textRenderer, "Floor prevents crashes.",    rx, backgroundHeight - 10, GRAY, false);
        }
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        int x = (width - backgroundWidth) / 2, y = (height - backgroundHeight) / 2;
        List<CommodityRow> rows = handler.rows;
        for (int i = scroll; i < Math.min(rows.size(), scroll + ROWS); i++) {
            int rowY = y + 38 + (i - scroll) * ROW_H;
            if (my >= rowY && my < rowY + ROW_H && mx >= x && mx < x + 205) {
                if (client != null) client.interactionManager.clickButton(handler.syncId, i);
                return true;
            }
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        int max = Math.max(0, handler.rows.size() - ROWS);
        scroll = (int)Math.max(0, Math.min(max, scroll - v));
        return true;
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        super.render(ctx, mx, my, d);
    }
}
