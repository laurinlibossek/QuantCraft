package com.quantcraft.screen;

import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class OtcTradeScreen extends Screen {

    private static final int W = 240, H = 150;

    private static final int BG       = 0xFFD4C88A;
    private static final int BORDER   = 0xFF7A6A40;
    private static final int C_TITLE  = 0xFF1E1408;
    private static final int C_TEXT   = 0xFF3A2810;
    private static final int C_GREEN  = 0xFF1A7A1A;
    private static final int C_RED    = 0xFF8A1A1A;

    private final String offerId;
    private final String proposerName;
    private final String ticker;
    private final int    shares;
    private final double pricePerShare;
    private final double marketPrice;

    private int px, py;

    public OtcTradeScreen(String offerId, String proposerName, String ticker,
                          int shares, double pricePerShare, double marketPrice) {
        super(Text.literal("Trade Offer"));
        this.offerId       = offerId;
        this.proposerName  = proposerName;
        this.ticker        = ticker;
        this.shares        = shares;
        this.pricePerShare = pricePerShare;
        this.marketPrice   = marketPrice;
    }

    @Override
    protected void init() {
        px = (this.width  - W) / 2;
        py = (this.height - H) / 2;

        int btnY = py + H - 26;
        int btnW = 76;

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("§a✓ Accept"), btn -> onAccept())
                .dimensions(px + W / 2 - btnW - 4, btnY, btnW, 16)
                .build()
        );
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("§c✗ Reject"), btn -> onReject())
                .dimensions(px + W / 2 + 4, btnY, btnW, 16)
                .build()
        );
    }

    @Override
    public boolean shouldPause() { return false; }

    @Override
    public boolean shouldCloseOnEsc() { return false; }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {}

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(px, py, px + W, py + H, BG);
        ctx.fill(px, py, px + W, py + 1, BORDER);
        ctx.fill(px, py + H - 1, px + W, py + H, BORDER);
        ctx.fill(px, py, px + 1, py + H, BORDER);
        ctx.fill(px + W - 1, py, px + W, py + H, BORDER);

        String title = "— TRADE OFFER —";
        int tx = px + (W - textRenderer.getWidth(title)) / 2;
        ctx.drawText(textRenderer, title, tx, py + 8, C_TITLE, false);
        ctx.fill(px + 8, py + 17, px + W - 8, py + 18, BORDER);

        int ly = py + 22;

        ctx.drawText(textRenderer,
                proposerName + " wants to sell you:",
                px + 12, ly, C_TEXT, false);
        ly += 12;

        String offerLine = String.format("%d x %s  @ %.2f / share",
                shares, ticker, pricePerShare);
        ctx.drawText(textRenderer, offerLine, px + 12, ly, C_TEXT, false);
        ly += 12;

        String totalLine = String.format("Total cost:  %,.2f¢", (double) shares * pricePerShare);
        ctx.drawText(textRenderer, totalLine, px + 12, ly, C_TEXT, false);
        ly += 16;

        double diff    = pricePerShare - marketPrice;
        double diffPct = marketPrice > 0 ? diff / marketPrice * 100.0 : 0;
        String mktLine = String.format("Market price: %.2f  (%+.1f%%)", marketPrice, diffPct);
        int mktColor = diff <= 0 ? C_GREEN : C_RED;
        ctx.drawText(textRenderer, mktLine, px + 12, ly, mktColor, false);

        super.render(ctx, mx, my, delta);
    }

    private void onAccept() {
        ModPacketsClient.sendOtcResponse(offerId, true);
        this.close();
    }

    private void onReject() {
        ModPacketsClient.sendOtcResponse(offerId, false);
        this.close();
    }
}
