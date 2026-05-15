package com.quantcraft.screen;

import com.quantcraft.network.ClientMarketCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.*;

public class PortfolioScreen extends Screen {

    private static final int W  = 310;
    private static final int H  = 220;
    private static final int ROW_H   = 12;
    private static final int COL_HEADER_Y = 30;
    private static final int ROWS_START_Y = COL_HEADER_Y + ROW_H + 2;
    private static final int MAX_VISIBLE_ROWS = 8;

    private static final int COL_TICKER = 8;
    private static final int COL_SHARES = 68;
    private static final int COL_AVG    = 112;
    private static final int COL_PRICE  = 168;
    private static final int COL_PNL    = 226;

    private static final int C_TITLE   = 0xFF2B1E0C;
    private static final int C_HEADER  = 0xFF6B5A3C;
    private static final int C_TEXT    = 0xFF3A2A12;
    private static final int C_MUTED   = 0xFF8A7A58;
    private static final int C_UP      = 0xFF1A8A1A;
    private static final int C_DOWN    = 0xFF9A1A1A;
    private static final int C_ZERO    = 0xFF6A6A5A;
    private static final int C_DIVIDER = 0xFFB0A080;
    private static final int BG_PANEL  = 0xFFD6C89A;
    private static final int BG_ROW_A  = 0x18000000;

    private final double balance;
    private final Map<String, Integer> holdings;
    private final Map<String, Double> avgCosts;

    private final List<HoldingRow> rows = new ArrayList<>();
    private double totalPnl;
    private double portfolioValue;

    private int panelX, panelY;
    private int scrollOffset = 0;

    public PortfolioScreen(double balance, Map<String, Integer> holdings, Map<String, Double> avgCosts) {
        super(Text.literal("Portfolio"));
        this.balance  = balance;
        this.holdings = holdings;
        this.avgCosts = avgCosts;
    }

    @Override
    protected void init() {
        panelX = (this.width  - W) / 2;
        panelY = (this.height - H) / 2;
        buildRows();
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Close"), btn -> this.close())
                .dimensions(panelX + W - 60, panelY + H - 22, 52, 14)
                .build()
        );
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(panelX, panelY, panelX + W, panelY + H, BG_PANEL);
        ctx.fill(panelX, panelY, panelX + W, panelY + 1, 0xFF8A7A58);
        ctx.fill(panelX, panelY + H - 1, panelX + W, panelY + H, 0xFF8A7A58);
        ctx.fill(panelX, panelY, panelX + 1, panelY + H, 0xFF8A7A58);
        ctx.fill(panelX + W - 1, panelY, panelX + W, panelY + H, 0xFF8A7A58);

        String title = "MY PORTFOLIO";
        int titleX = panelX + (W - this.textRenderer.getWidth(title)) / 2;
        ctx.drawText(this.textRenderer, title, titleX, panelY + 8, C_TITLE, false);
        ctx.fill(panelX + 8, panelY + 18, panelX + W - 8, panelY + 19, C_DIVIDER);

        drawColHeaders(ctx);
        drawRows(ctx);
        drawFooter(ctx);

        super.render(ctx, mx, my, delta);
    }

    private void drawColHeaders(DrawContext ctx) {
        int y = panelY + COL_HEADER_Y;
        ctx.fill(panelX + 4, y, panelX + W - 4, y + ROW_H + 1, 0x33000000);
        drawCol(ctx, "TICKER", panelX + COL_TICKER, y + 2, C_HEADER, false);
        drawCol(ctx, "SHARES", panelX + COL_SHARES, y + 2, C_HEADER, true);
        drawCol(ctx, "AVG",    panelX + COL_AVG,    y + 2, C_HEADER, true);
        drawCol(ctx, "PRICE",  panelX + COL_PRICE,  y + 2, C_HEADER, true);
        drawCol(ctx, "P&L",    panelX + COL_PNL,    y + 2, C_HEADER, true);
        ctx.fill(panelX + 4, y + ROW_H + 2, panelX + W - 4, y + ROW_H + 3, C_DIVIDER);
    }

    private void drawRows(DrawContext ctx) {
        int visibleCount = Math.min(MAX_VISIBLE_ROWS, rows.size() - scrollOffset);
        for (int i = 0; i < visibleCount; i++) {
            int dataIdx = i + scrollOffset;
            HoldingRow row = rows.get(dataIdx);
            int y = panelY + ROWS_START_Y + i * ROW_H;

            if (i % 2 == 0) ctx.fill(panelX + 4, y, panelX + W - 4, y + ROW_H, BG_ROW_A);

            int pnlColor = row.pnl > 0 ? C_UP : row.pnl < 0 ? C_DOWN : C_ZERO;

            drawCol(ctx, row.ticker,            panelX + COL_TICKER, y + 2, C_TEXT,  false);
            drawCol(ctx, String.format("%,d", row.shares), panelX + COL_SHARES, y + 2, C_TEXT, true);
            drawCol(ctx, fmtPrice(row.avgCost), panelX + COL_AVG,    y + 2, C_MUTED, true);
            drawCol(ctx, fmtPrice(row.price),   panelX + COL_PRICE,  y + 2, C_TEXT,  true);
            drawCol(ctx, fmtPnl(row.pnl),       panelX + COL_PNL,    y + 2, pnlColor, true);
        }

        if (rows.size() > MAX_VISIBLE_ROWS) {
            int shownEnd = Math.min(scrollOffset + MAX_VISIBLE_ROWS, rows.size());
            String hint = (scrollOffset + 1) + "-" + shownEnd + " of " + rows.size();
            ctx.drawText(this.textRenderer, hint,
                    panelX + W - 4 - this.textRenderer.getWidth(hint),
                    panelY + ROWS_START_Y + MAX_VISIBLE_ROWS * ROW_H + 1,
                    C_MUTED, false);
        }
    }

    private void drawFooter(DrawContext ctx) {
        int footerY = panelY + H - 34;
        ctx.fill(panelX + 4, footerY, panelX + W - 4, footerY + 1, C_DIVIDER);

        int pnlColor = totalPnl > 0 ? C_UP : totalPnl < 0 ? C_DOWN : C_ZERO;

        ctx.drawText(this.textRenderer, "Cash: " + fmtPrice(balance),
                panelX + 8, footerY + 4, C_TEXT, false);
        ctx.drawText(this.textRenderer, "Value: " + fmtPrice(portfolioValue),
                panelX + 8, footerY + 14, C_MUTED, false);
        String pnlLine = "Total P&L: " + fmtPnl(totalPnl);
        ctx.drawText(this.textRenderer, pnlLine,
                panelX + W - 64 - this.textRenderer.getWidth(pnlLine),
                footerY + 4, pnlColor, false);
    }

    private void drawCol(DrawContext ctx, String text, int x, int y, int color, boolean rightAlign) {
        if (rightAlign) {
            x = x + 56 - this.textRenderer.getWidth(text);
        }
        ctx.drawText(this.textRenderer, text, x, y, color, false);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int maxScroll = Math.max(0, rows.size() - MAX_VISIBLE_ROWS);
        scrollOffset = (int) Math.max(0, Math.min(maxScroll, scrollOffset - verticalAmount));
        return true;
    }

    private void buildRows() {
        rows.clear();
        totalPnl       = 0;
        portfolioValue = balance;

        holdings.forEach((ticker, shares) -> {
            if (shares <= 0) return;
            double price   = ClientMarketCache.getPrice(ticker);
            double avgCost = avgCosts.getOrDefault(ticker, price);
            double pnl     = (price - avgCost) * shares;

            rows.add(new HoldingRow(ticker, shares, avgCost, price, pnl));
            totalPnl       += pnl;
            portfolioValue += price * shares;
        });

        rows.sort(Comparator.comparingDouble((HoldingRow r) -> r.pnl).reversed());
    }

    private static String fmtPrice(double v) { return String.format("%.1f", v); }
    private static String fmtPnl(double v) {
        return v >= 0 ? String.format("+%,.0f", v) : String.format("%,.0f", v);
    }

    private record HoldingRow(String ticker, int shares, double avgCost, double price, double pnl) {}
}
