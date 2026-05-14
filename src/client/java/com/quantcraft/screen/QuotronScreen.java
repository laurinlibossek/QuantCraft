package com.quantcraft.screen;

import com.quantcraft.market.StockDefinition;
import com.quantcraft.market.StockRegistry;
import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class QuotronScreen extends Screen {
    private final List<String>         tracked;
    private final Map<String,Double>   prices, changes;
    private final BlockPos             blockPos;
    private TextFieldWidget            input;
    private String                     error = "";

    private static final int W        = 240, H = 175;
    private static final int BG_DARK  = 0xFF0d1117;
    private static final int BG_PANEL = 0xFF161b22;
    private static final int GOLD     = 0xFFd4a843;
    private static final int GREEN    = 0xFF3fb950;
    private static final int RED      = 0xFFf85149;
    private static final int MUTED    = 0xFF8b949e;

    public QuotronScreen(List<String> tracked, Map<String,Double> prices,
                         Map<String,Double> changes, BlockPos pos) {
        super(Text.literal("Quotron Terminal"));
        this.tracked  = new ArrayList<>(tracked);
        this.prices   = prices;
        this.changes  = changes;
        this.blockPos = pos;
    }

    @Override protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        input = new TextFieldWidget(textRenderer, x + 10, y + H - 34, 100, 16, Text.literal("Ticker"));
        input.setMaxLength(5);
        input.setPlaceholder(Text.literal("e.g. DIAM").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        addDrawableChild(input);
        addDrawableChild(ButtonWidget.builder(Text.literal("+ Add"), btn -> addTicker()).dimensions(x + 115, y + H - 35, 50, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Done"),  btn -> close()).dimensions(x + W - 55, y + H - 35, 50, 18).build());
    }

    private void addTicker() {
        String t = input.getText().trim().toUpperCase();
        if (t.isEmpty()) return;
        if (tracked.size() >= 5)       { error = "Max 5 stocks."; return; }
        if (tracked.contains(t))       { error = "Already tracking " + t; return; }
        if (StockRegistry.get(t) == null) { error = "Unknown: " + t; return; }
        tracked.add(t); input.setText(""); error = "";
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        int x = (width - W) / 2, y = (height - H) / 2;
        ctx.fill(x, y, x + W, y + H, BG_DARK);
        ctx.fill(x, y, x + W, y + 16, BG_PANEL);
        ctx.fill(x, y + H - 42, x + W, y + H - 41, 0xFF30363d);
        ctx.drawText(textRenderer, "■■ QUOTRON TERMINAL", x + 6, y + 4, GOLD, false);
        int ry = y + 22;
        if (tracked.isEmpty()) ctx.drawText(textRenderer, "No stocks tracked.", x + 6, ry, MUTED, false);
        for (String tk : tracked) {
            StockDefinition def = StockRegistry.get(tk);
            if (def == null) continue;
            double p = prices.getOrDefault(tk, 0.0);
            double c = changes.getOrDefault(tk, 0.0);
            String cs = (c >= 0 ? "§a▲ +" : "§c▼ ") + String.format("%.1f%%", c);
            boolean hover = mx >= x + 5 && mx <= x + W - 30 && my >= ry - 1 && my <= ry + 11;
            if (hover) ctx.fill(x + 5, ry - 1, x + W - 30, ry + 11, 0xFF21262d);
            ctx.drawText(textRenderer, tk,                           x + 8,   ry, 0xFFcdd9e5,  false);
            ctx.drawText(textRenderer, def.displayName(),            x + 50,  ry, MUTED,       false);
            ctx.drawText(textRenderer, String.format("%.1f¢", p),   x + 148, ry, 0xFFFFddaa,  false);
            ctx.drawText(textRenderer, cs,                           x + 190, ry, c >= 0 ? GREEN : RED, false);
            boolean rh = mx >= x + W - 22 && mx <= x + W - 8 && my >= ry && my <= ry + 10;
            ctx.drawText(textRenderer, "×", x + W - 18, ry, rh ? RED : MUTED, false);
            ry += 14;
        }
        if (!error.isEmpty()) ctx.drawText(textRenderer, error, x + 10, y + H - 48, RED, false);
        super.render(ctx, mx, my, d);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        int x = (width - W) / 2, y = (height - H) / 2;
        int ry = y + 22;
        for (String tk : new ArrayList<>(tracked)) {
            if (mx >= x + W - 22 && mx <= x + W - 8 && my >= ry && my <= ry + 10) {
                tracked.remove(tk); return true;
            }
            ry += 14;
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public void close()          { ModPacketsClient.sendUpdateQuotron(tracked, blockPos); super.close(); }
    @Override public boolean shouldPause() { return false; }
}
