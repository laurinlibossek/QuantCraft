package com.quantcraft.screen;

import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class PaymentRequestScreen extends Screen {

    private static final int W = 240, H = 130;

    private static final int BG       = 0xFFD4C88A;
    private static final int BORDER   = 0xFF7A6A40;
    private static final int C_TITLE  = 0xFF1E1408;
    private static final int C_TEXT   = 0xFF3A2810;
    private static final int C_AMOUNT = 0xFF8A6A1A;

    private final String requestId;
    private final String requesterName;
    private final double amount;

    private int px, py;

    public PaymentRequestScreen(String requestId, String requesterName, double amount) {
        super(Text.literal("Payment Request"));
        this.requestId      = requestId;
        this.requesterName  = requesterName;
        this.amount         = amount;
    }

    @Override
    protected void init() {
        px = (this.width  - W) / 2;
        py = (this.height - H) / 2;

        int btnY = py + H - 26;
        int btnW = 76;

        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("§a✓ Pay"), btn -> onAccept())
                .dimensions(px + W / 2 - btnW - 4, btnY, btnW, 16)
                .build()
        );
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("§c✗ Decline"), btn -> onReject())
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

        String title = "— PAYMENT REQUEST —";
        int tx = px + (W - textRenderer.getWidth(title)) / 2;
        ctx.drawText(textRenderer, title, tx, py + 8, C_TITLE, false);
        ctx.fill(px + 8, py + 17, px + W - 8, py + 18, BORDER);

        int ly = py + 28;

        String line1 = requesterName + " is requesting:";
        ctx.drawText(textRenderer, line1, px + 12, ly, C_TEXT, false);
        ly += 16;

        String amountLine = String.format("%.1f¢", amount);
        int amtWidth = textRenderer.getWidth(amountLine);
        ctx.drawText(textRenderer, amountLine, px + (W - amtWidth) / 2, ly, C_AMOUNT, false);
        ly += 18;

        String line2 = "Do you want to pay this amount?";
        int l2w = textRenderer.getWidth(line2);
        ctx.drawText(textRenderer, line2, px + (W - l2w) / 2, ly, C_TEXT, false);

        super.render(ctx, mx, my, delta);
    }

    private void onAccept() {
        ModPacketsClient.sendPaymentResponse(requestId, true);
        this.close();
    }

    private void onReject() {
        ModPacketsClient.sendPaymentResponse(requestId, false);
        this.close();
    }
}
