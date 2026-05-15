package com.quantcraft.screen;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;

import java.util.List;

public class PriceHistoryWidget implements Drawable, Element, Selectable {

    private int x, y, width, height;
    private List<Double> history;
    private boolean focused;

    private static final int C_BG        = 0xFF1A1A14;
    private static final int C_BORDER    = 0xFF4A4230;
    private static final int C_UP        = 0xFF22CC44;
    private static final int C_DOWN      = 0xFFCC2222;
    private static final int C_FILL_UP   = 0x4422CC44;
    private static final int C_FILL_DOWN = 0x44CC2222;
    private static final int C_DOT       = 0xFFFFFFFF;
    private static final int C_LABEL     = 0xFF888870;
    private static final int C_WHITE     = 0xFFFFFFFF;

    public PriceHistoryWidget(int x, int y, int width, int height, List<Double> history) {
        this.x       = x;
        this.y       = y;
        this.width   = width;
        this.height  = height;
        this.history = history;
    }

    public void setHistory(List<Double> history) { this.history = history; }
    public void setPosition(int x, int y)        { this.x = x; this.y = y; }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(x, y, x + width, y + height, C_BG);
        ctx.fill(x, y, x + width, y + 1, C_BORDER);
        ctx.fill(x, y + height - 1, x + width, y + height, C_BORDER);
        ctx.fill(x, y, x + 1, y + height, C_BORDER);
        ctx.fill(x + width - 1, y, x + width, y + height, C_BORDER);

        if (history == null || history.size() < 2) {
            var tr = MinecraftClient.getInstance().textRenderer;
            String msg = "No history";
            ctx.drawText(tr, msg, x + (width - tr.getWidth(msg)) / 2, y + height / 2 - 4, C_LABEL, false);
            return;
        }

        double minPrice = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double maxPrice = history.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double range    = Math.max(0.01, maxPrice - minPrice);

        double openPrice    = history.get(0);
        double currentPrice = history.get(history.size() - 1);
        boolean isUp        = currentPrice >= openPrice;

        int lineColor = isUp ? C_UP : C_DOWN;
        int fillColor = isUp ? C_FILL_UP : C_FILL_DOWN;

        int cx = x + 1, cy = y + 1;
        int cw = width - 2, ch = height - 2;
        int n = history.size();

        for (int i = 0; i < n - 1; i++) {
            int x0 = cx + (int) ((double) i / (n - 1) * cw);
            int x1 = cx + (int) ((double) (i + 1) / (n - 1) * cw);
            int y0 = cy + ch - (int) ((history.get(i) - minPrice) / range * ch);
            int y1 = cy + ch - (int) ((history.get(i + 1) - minPrice) / range * ch);
            int top = Math.min(y0, y1);
            int bottom = cy + ch;
            ctx.fill(x0, top, x1 + 1, bottom, fillColor);
        }

        for (int i = 0; i < n - 1; i++) {
            int x0 = cx + (int) ((double) i / (n - 1) * cw);
            int x1 = cx + (int) ((double) (i + 1) / (n - 1) * cw);
            int y0 = cy + ch - (int) ((history.get(i) - minPrice) / range * ch);
            int y1 = cy + ch - (int) ((history.get(i + 1) - minPrice) / range * ch);
            drawLine(ctx, x0, y0, x1, y1, lineColor);
        }

        int lastX = cx + cw - 1;
        int lastY = cy + ch - (int) ((currentPrice - minPrice) / range * ch);
        ctx.fill(lastX - 1, lastY - 1, lastX + 2, lastY + 2, C_DOT);

        var tr = MinecraftClient.getInstance().textRenderer;
        double changePct = (currentPrice - openPrice) / openPrice * 100.0;
        int labelColor = isUp ? C_UP : C_DOWN;

        String priceStr = currentPrice >= 1000 ? String.format("%,.0f", currentPrice) : String.format("%.2f", currentPrice);
        String changeStr = changePct >= 0 ? String.format("+%.1f%%", changePct) : String.format("%.1f%%", changePct);

        ctx.drawText(tr, priceStr, x, y - 9, C_WHITE, false);
        ctx.drawText(tr, changeStr, x + width - tr.getWidth(changeStr), y - 9, labelColor, false);
    }

    private static void drawLine(DrawContext ctx, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0), sx = x0 < x1 ? 1 : -1;
        int dy = -Math.abs(y1 - y0), sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;
        while (true) {
            ctx.fill(x0, y0, x0 + 1, y0 + 1, color);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) { err += dy; x0 += sx; }
            if (e2 <= dx) { err += dx; y0 += sy; }
        }
    }

    @Override public SelectionType getType() { return SelectionType.NONE; }
    @Override public void appendNarrations(NarrationMessageBuilder b) {}
    @Override public boolean isMouseOver(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height;
    }
    @Override public void setFocused(boolean focused) { this.focused = focused; }
    @Override public boolean isFocused() { return focused; }
}
