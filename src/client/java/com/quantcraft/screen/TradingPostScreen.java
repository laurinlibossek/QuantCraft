package com.quantcraft.screen;

import com.quantcraft.market.*;
import com.quantcraft.network.ModPacketsClient;
import com.quantcraft.screen.TradingPostScreenHandler.StockDisplayData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import java.util.*;

public class TradingPostScreen extends HandledScreen<TradingPostScreenHandler> {
    private static final int BG    = 0xFF1a1a2e;
    private static final int PANEL = 0xFF16213e;
    private static final int GOLD  = 0xFFe2b96f;
    private static final int GREEN = 0xFF00ff88;
    private static final int RED   = 0xFFff4466;
    private static final int GRAY  = 0xFF8888aa;
    private static final int SEL   = 0xFF0f3460;
    private static final int ROWS  = 10;
    private static final int ROW_H = 14;

    private int scroll = 0, activeTab = 0;

    public TradingPostScreen(TradingPostScreenHandler h, PlayerInventory inv, Text title) {
        super(h, inv, title);
        backgroundWidth  = 330;
        backgroundHeight = 230;
    }

    @Override protected void init() {
        super.init();
        int x = (width - backgroundWidth) / 2, y = (height - backgroundHeight) / 2;
        // Trade buttons
        addDrawableChild(ButtonWidget.builder(Text.literal("Buy 1"),   btn -> click(100)).dimensions(x + 5,   y + backgroundHeight - 48, 52, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sell 1"),  btn -> click(101)).dimensions(x + 60,  y + backgroundHeight - 48, 55, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Buy 64"),  btn -> click(102)).dimensions(x + 120, y + backgroundHeight - 48, 55, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Sell 64"), btn -> click(103)).dimensions(x + 180, y + backgroundHeight - 48, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Withdraw $"), btn -> { click(200); this.close(); }).dimensions(x + backgroundWidth - 82, y + backgroundHeight - 48, 76, 18).build());
        // Tabs
        addDrawableChild(ButtonWidget.builder(Text.literal("Market"),    btn -> activeTab = 0).dimensions(x + 5,   y + backgroundHeight - 25, 60, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Portfolio"), btn -> activeTab = 1).dimensions(x + 70,  y + backgroundHeight - 25, 65, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("News"),      btn -> activeTab = 2).dimensions(x + 140, y + backgroundHeight - 25, 50, 18).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Orders"),    btn -> activeTab = 3).dimensions(x + 195, y + backgroundHeight - 25, 55, 18).build());
    }

    private void click(int id) { ModPacketsClient.sendButtonClick(handler.syncId, id); }

    @Override protected void drawBackground(DrawContext ctx, float d, int mx, int my) {
        int x = (width - backgroundWidth) / 2, y = (height - backgroundHeight) / 2;
        ctx.fill(x, y, x + backgroundWidth, y + backgroundHeight, BG);
        ctx.fill(x, y, x + backgroundWidth, y + 18, PANEL);
        ctx.fill(x, y + 18, x + backgroundWidth, y + 19, 0xFF334455);
        ctx.fill(x, y + backgroundHeight - 56, x + backgroundWidth, y + backgroundHeight - 55, 0xFF334455);
        ctx.fill(x, y + backgroundHeight - 30, x + backgroundWidth, y + backgroundHeight - 29, 0xFF334455);
    }

    @Override protected void drawForeground(DrawContext ctx, int mx, int my) {
        ctx.drawText(textRenderer, "QUANTCRAFT EXCHANGE", 6, 5, GOLD, false);
        String bal = String.format("%.1f¢", handler.playerBalance);
        ctx.drawText(textRenderer, bal, backgroundWidth - textRenderer.getWidth(bal) - 4, 5, GOLD, false);
        switch (activeTab) {
            case 0 -> drawMarket(ctx, mx - (width - backgroundWidth) / 2, my - (height - backgroundHeight) / 2);
            case 1 -> drawPortfolio(ctx);
            case 2 -> drawNews(ctx);
            case 3 -> drawOrders(ctx);
        }
    }

    private void drawMarket(DrawContext ctx, int lx, int ly) {
        List<StockDisplayData> stocks = handler.stocks;
        int hy = 22;
        ctx.drawText(textRenderer, "TICKER", 6,   hy, GRAY, false);
        ctx.drawText(textRenderer, "PRICE",  80,  hy, GRAY, false);
        ctx.drawText(textRenderer, "CHG%",   128, hy, GRAY, false);
        ctx.drawText(textRenderer, "CHART",  172, hy, GRAY, false);
        ctx.drawText(textRenderer, "HELD",   255, hy, GRAY, false);
        ctx.drawText(textRenderer, "AVAIL",  285, hy, GRAY, false);
        int sy  = 33;
        int sel = handler.getSelectedIndex();
        for (int i = scroll; i < Math.min(stocks.size(), scroll + ROWS); i++) {
            StockDisplayData s  = stocks.get(i);
            int              ry = sy + (i - scroll) * ROW_H;
            if (i == sel) ctx.fill(0, ry - 1, backgroundWidth, ry + ROW_H - 1, SEL);
            ctx.drawText(textRenderer, s.definition().ticker(), 6, ry, 0xFFddddee, false);
            ctx.drawText(textRenderer, String.format("%.1f", s.price()), 80, ry, 0xFFffffff, false);
            double chg = s.changePercent();
            String cs  = (chg >= 0 ? "+" : "") + String.format("%.1f%%", chg);
            ctx.drawText(textRenderer, cs, 128, ry, chg >= 0 ? GREEN : RED, false);
            drawCandleChart(ctx, s.candles(), s.history(), 172, ry, 78, ROW_H - 2);
            int held = handler.playerHoldings.getOrDefault(s.definition().ticker(), 0);
            if (held > 0) ctx.drawText(textRenderer, String.valueOf(held), 257, ry, GOLD, false);
            int avail      = Math.max(0, s.totalShares() - s.sharesHeld());
            int availColor = avail < s.totalShares() * 0.1 ? RED : GRAY;
            String av = avail > 9999 ? (avail / 1000) + "k" : String.valueOf(avail);
            ctx.drawText(textRenderer, av, 287, ry, availColor, false);
        }
    }

    private void drawCandleChart(DrawContext ctx, List<double[]> candles, List<Double> history,
                                 int x, int y, int w, int h) {
        if (candles != null && candles.size() > 1) {
            int          num = w / 3;
            List<double[]> vis = candles.subList(Math.max(0, candles.size() - num), candles.size());
            double mn  = vis.stream().mapToDouble(c -> c[2]).min().orElse(0);
            double mx  = vis.stream().mapToDouble(c -> c[1]).max().orElse(1);
            double rng = Math.max(0.01, mx - mn);
            for (int i = 0; i < vis.size(); i++) {
                double[] c   = vis.get(i);
                int      cx  = x + i * 3;
                int      col = c[4] > 0 ? GREEN : RED;
                int      wt  = y + h - (int)((c[1] - mn) / rng * h);
                int      wb  = y + h - (int)((c[2] - mn) / rng * h);
                ctx.fill(cx + 1, wt, cx + 2, wb, col);
                int bt = y + h - (int)((Math.max(c[0], c[3]) - mn) / rng * h);
                int bb = y + h - (int)((Math.min(c[0], c[3]) - mn) / rng * h);
                bb = Math.max(bb, bt + 1);
                ctx.fill(cx, bt, cx + 3, bb, col);
            }
        } else if (history != null && history.size() > 1) {
            double mn  = history.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double mx  = history.stream().mapToDouble(Double::doubleValue).max().orElse(1);
            double rng = Math.max(0.01, mx - mn);
            int    bw  = Math.max(1, w / history.size());
            int    col = history.get(history.size() - 1) >= history.get(0) ? GREEN : RED;
            for (int i = 0; i < history.size(); i++) {
                int bh = (int)((history.get(i) - mn) / rng * h);
                bh = Math.max(1, bh);
                ctx.fill(x + i * bw, y + h - bh, x + i * bw + bw - 1, y + h, col);
            }
        }
    }

    private void drawPortfolio(DrawContext ctx) {
        int ry = 25;
        ctx.drawText(textRenderer, "YOUR PORTFOLIO", 6, ry, GOLD, false); ry += 14;
        ctx.drawText(textRenderer, String.format("Cash: %.1f¢", handler.playerBalance), 6, ry, 0xFFddddee, false); ry += 14;
        double total = 0;
        if (handler.playerHoldings.isEmpty()) {
            ctx.drawText(textRenderer, "No holdings.", 6, ry, GRAY, false); ry += 12;
        } else {
            for (var e : handler.playerHoldings.entrySet()) {
                var sd = handler.stocks.stream().filter(s -> s.definition().ticker().equals(e.getKey())).findFirst().orElse(null);
                if (sd == null) continue;
                double val = sd.price() * e.getValue(); total += val;
                ctx.drawText(textRenderer, String.format("%s ×%d = %.1f¢", e.getKey(), e.getValue(), val), 6, ry, 0xFFddddee, false); ry += 12;
                if (ry > backgroundHeight - 80) { ctx.drawText(textRenderer, "...", 6, ry, GRAY, false); ry += 12; break; }
            }
        }
        if (!handler.openShorts.isEmpty()) {
            ry += 4;
            ctx.fill(0, ry, backgroundWidth, ry + 1, 0xFF334455); ry += 4;
            ctx.drawText(textRenderer, "SHORT POSITIONS", 6, ry, RED, false); ry += 12;
            for (var sp : handler.openShorts) {
                if (ry > backgroundHeight - 68) break;
                int col = sp.pnl() >= 0 ? GREEN : RED;
                ctx.drawText(textRenderer, String.format("%-5s ×%d open:%.1f cur:%.1f  PnL:%s%.1f¢  fee:%.2f¢",
                        sp.ticker(), sp.shares(), sp.openPrice(), sp.currentPrice(),
                        sp.pnl() >= 0 ? "+" : "", sp.pnl(), sp.accruedFee()),
                        6, ry, col, false); ry += 11;
            }
        }
        ry += 4;
        ctx.drawText(textRenderer, String.format("Total: %.1f¢", handler.playerBalance + total), 6, ry, GOLD, false);
    }

    private void drawNews(DrawContext ctx) {
        int ry = 25;
        ctx.drawText(textRenderer, "MARKET NEWS", 6, ry, GOLD, false); ry += 14;
        if (handler.recentNews.isEmpty()) { ctx.drawText(textRenderer, "No news yet.", 6, ry, GRAY, false); return; }
        for (String n : handler.recentNews) { ctx.drawText(textRenderer, "• " + n, 6, ry, 0xFFccccdd, false); ry += 12; }
    }

    private void drawOrders(DrawContext ctx) {
        int ry = 25;
        ctx.drawText(textRenderer, "PENDING LIMIT ORDERS", 6, ry, GOLD, false); ry += 12;
        ctx.drawText(textRenderer, "Use /qc cancelorder <ticker> <id>", 6, ry, GRAY, false); ry += 12;
        ctx.fill(0, ry, backgroundWidth, ry + 1, 0xFF334455); ry += 4;
        boolean any = false;
        for (StockDisplayData s : handler.stocks) {
            StockState ss = MarketEngine.getInstance().getState(s.definition().ticker());
            if (ss == null) continue;
            UUID myId = net.minecraft.client.MinecraftClient.getInstance().player != null
                    ? net.minecraft.client.MinecraftClient.getInstance().player.getUuid() : null;
            if (myId == null) continue;
            for (LimitOrder o : ss.getOrderBook().getAll()) {
                if (!o.getPlayerUuid().equals(myId)) continue;
                any = true;
                int col = o.getSide() == LimitOrder.Side.BUY ? GREEN : RED;
                ctx.drawText(textRenderer, String.format("%-7s %s ×%d @ %.1f¢ %s",
                        o.getTicker(), o.getSide(), o.getRemainingQty(), o.getLimitPrice(),
                        o.getOrderId().toString().substring(0, 8)), 6, ry, col, false);
                ry += 11;
                if (ry > backgroundHeight - 70) break;
            }
        }
        if (!any) ctx.drawText(textRenderer, "No pending orders.", 6, ry, GRAY, false);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        int x  = (width - backgroundWidth) / 2, y = (height - backgroundHeight) / 2;
        int ly = (int)my - y;
        if (activeTab == 0 && ly >= 33 && ly < 33 + ROWS * ROW_H) {
            int row = scroll + (ly - 33) / ROW_H;
            if (row >= 0 && row < handler.stocks.size() && client != null)
                client.interactionManager.clickButton(handler.syncId, row);
        }
        return super.mouseClicked(mx, my, btn);
    }

    @Override public boolean mouseScrolled(double mx, double my, double h, double v) {
        int max = Math.max(0, handler.stocks.size() - ROWS);
        scroll = (int)Math.max(0, Math.min(max, scroll - v));
        return true;
    }

    public void onPortfolioUpdate(double balance, Map<String, Integer> holdings) {
        handler.playerBalance = balance;
        handler.playerHoldings.clear();
        handler.playerHoldings.putAll(holdings);
    }

    public void onMarketUpdate(java.util.Map<String, double[]> updates) {
        for (int i = 0; i < handler.stocks.size(); i++) {
            StockDisplayData s = handler.stocks.get(i);
            double[] v = updates.get(s.definition().ticker());
            if (v == null) continue;
            handler.stocks.set(i, new StockDisplayData(
                    s.definition(), v[0], v[1], s.history(), s.candles(), s.totalShares(), s.sharesHeld()));
        }
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        renderBackground(ctx, mx, my, d);
        super.render(ctx, mx, my, d);
    }
}
