package com.quantcraft.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.quantcraft.network.ClientMarketCache;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

public final class TickerHudOverlay {

    private static final int MARGIN_LEFT  = 4;
    private static final int MARGIN_TOP   = 40;
    private static final int ROW_HEIGHT   = 12;
    private static final int PADDING_X    = 4;
    private static final int PADDING_Y    = 2;
    private static final int PANEL_WIDTH  = 104;

    private static final int BG_COLOR    = 0x55000000;
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_UP    = 0xFF55FF55;
    private static final int COLOR_DOWN  = 0xFFFF5555;
    private static final int COLOR_FLAT  = 0xFFAAAAAA;

    private TickerHudOverlay() {}

    public static void register() {
        HudRenderCallback.EVENT.register(TickerHudOverlay::render);
    }

    private static void render(DrawContext context, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.currentScreen != null) return;

        List<String> pins = TickerPins.get();
        if (pins.isEmpty()) return;

        int panelH = PADDING_Y * 2 + pins.size() * ROW_HEIGHT;
        int x      = MARGIN_LEFT;
        int y      = MARGIN_TOP;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        context.fill(x, y, x + PANEL_WIDTH, y + panelH, BG_COLOR);

        for (int i = 0; i < pins.size(); i++) {
            String ticker = pins.get(i);
            int rowY = y + PADDING_Y + i * ROW_HEIGHT;

            double price  = ClientMarketCache.getPrice(ticker);
            double change = ClientMarketCache.getChange(ticker);

            String priceStr  = price > 0 ? formatPrice(price) : "---";
            String changeStr = price > 0 ? formatChange(change) : "";
            int changeColor  = change > 0 ? COLOR_UP : change < 0 ? COLOR_DOWN : COLOR_FLAT;

            context.drawText(mc.textRenderer, ticker, x + PADDING_X, rowY + 2, COLOR_WHITE, false);
            context.drawText(mc.textRenderer, priceStr, x + PADDING_X + 36, rowY + 2, COLOR_WHITE, false);

            int changeX = x + PANEL_WIDTH - PADDING_X - mc.textRenderer.getWidth(changeStr);
            context.drawText(mc.textRenderer, changeStr, changeX, rowY + 2, changeColor, false);
        }

        RenderSystem.disableBlend();
    }

    private static String formatPrice(double price) {
        return price >= 1000 ? String.format("%,.0f", price) : String.format("%.1f", price);
    }

    private static String formatChange(double pct) {
        return pct >= 0 ? String.format("+%.1f%%", pct) : String.format("%.1f%%", pct);
    }
}
