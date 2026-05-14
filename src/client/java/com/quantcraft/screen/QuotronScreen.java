package com.quantcraft.screen;

import com.quantcraft.blockentity.QuotronBlockEntity;
import com.quantcraft.market.StockDefinition;
import com.quantcraft.market.StockRegistry;
import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class QuotronScreen extends Screen {

    // ── Layout constants ───────────────────────────────────────────────────────
    private static final int W           = 280;
    private static final int H           = 200;
    private static final int LEFT_W      = 145;   // left panel width
    private static final int DIVIDER_X   = LEFT_W; // relative to panel origin
    private static final int ROW_H       = 16;
    private static final int HEADER_H    = 28;
    private static final int BODY_TOP    = HEADER_H + 4;

    // ── Colours ────────────────────────────────────────────────────────────────
    private static final int BG          = 0xFF0d1117;
    private static final int HDR_BG      = 0xFF161b22;
    private static final int DIVIDER     = 0xFF30363d;
    private static final int ROW_HOVER   = 0xFF21262d;
    private static final int GOLD        = 0xFFd4a843;
    private static final int GREEN       = 0xFF3fb950;
    private static final int RED         = 0xFFf85149;
    private static final int WHITE       = 0xFFcdd9e5;
    private static final int MUTED       = 0xFF8b949e;
    private static final int DIM         = 0xFF484f58;

    // ── State ──────────────────────────────────────────────────────────────────
    private final List<String>       tracked;
    private final Map<String,Double> prices;
    private final Map<String,Double> changes;
    private final BlockPos           blockPos;

    /** All stocks in registry order, for the right panel. */
    private final List<StockDefinition> allStocks;

    /** Scroll offset in rows for the right panel. */
    private int rightScroll = 0;

    /** Coin balance injected when the screen was opened (best-effort; may be 0). */
    private final double coinBalance;

    public QuotronScreen(List<String> tracked, Map<String,Double> prices,
                         Map<String,Double> changes, BlockPos pos, double coinBalance) {
        super(Text.literal("Quotron Terminal"));
        this.tracked     = new ArrayList<>(tracked);
        this.prices      = prices;
        this.changes     = changes;
        this.blockPos    = pos;
        this.coinBalance = coinBalance;
        this.allStocks   = StockRegistry.getAll();
    }

    // Legacy constructor for callers that don't yet pass coinBalance
    public QuotronScreen(List<String> tracked, Map<String,Double> prices,
                         Map<String,Double> changes, BlockPos pos) {
        this(tracked, prices, changes, pos, 0.0);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────
    private int ox() { return (width  - W) / 2; }
    private int oy() { return (height - H) / 2; }

    /** Rows that fit in the right panel body. */
    private int rightVisibleRows() { return (H - BODY_TOP - 4) / ROW_H; }

    // ── Rendering ─────────────────────────────────────────────────────────────

    /** Suppress the full-screen dark overlay that Screen.render() would add via renderBackground(). */
    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // intentionally empty — we draw only the UI panel background below
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int ox = ox(), oy = oy();

        // Panel background only — no full-screen overlay
        ctx.fill(ox, oy, ox + W, oy + H, BG);

        // ── Left panel ────────────────────────────────────────────────────────
        drawLeftPanel(ctx, ox, oy, mx, my);

        // ── Vertical divider ──────────────────────────────────────────────────
        ctx.fill(ox + DIVIDER_X, oy, ox + DIVIDER_X + 1, oy + H, DIVIDER);

        // ── Right panel ───────────────────────────────────────────────────────
        drawRightPanel(ctx, ox, oy, mx, my);

        // Render child widgets (buttons etc.) but skip renderBackground
        super.render(ctx, mx, my, delta);
    }

    private void drawLeftPanel(DrawContext ctx, int ox, int oy, int mx, int my) {
        // Header bar
        ctx.fill(ox, oy, ox + DIVIDER_X, oy + HEADER_H, HDR_BG);
        ctx.drawText(textRenderer, "§6MY WATCHLIST", ox + 6, oy + 5, GOLD, false);
        if (coinBalance > 0) {
            ctx.drawText(textRenderer,
                    String.format("§7%.1f¢", coinBalance),
                    ox + 6, oy + 15, MUTED, false);
        }

        int bodyY = oy + BODY_TOP;
        int maxY  = oy + H - 4;

        if (tracked.isEmpty()) {
            ctx.drawText(textRenderer, "§7No stocks tracked.", ox + 6, bodyY, MUTED, false);
            ctx.drawText(textRenderer, "§8Use the list →",     ox + 6, bodyY + 10, DIM, false);
            return;
        }

        for (int i = 0; i < tracked.size(); i++) {
            String tk = tracked.get(i);
            int ry = bodyY + i * ROW_H;
            if (ry + ROW_H > maxY) break;

            boolean hover = mx >= ox + 2 && mx < ox + DIVIDER_X - 2
                    && my >= ry && my < ry + ROW_H;
            if (hover) ctx.fill(ox + 2, ry, ox + DIVIDER_X - 2, ry + ROW_H - 1, ROW_HOVER);

            double p = prices.getOrDefault(tk, 0.0);
            double c = changes.getOrDefault(tk, 0.0);
            String changeStr = (c >= 0 ? "§a▲+" : "§c▼") + String.format("%.1f%%", c);

            ctx.drawText(textRenderer, tk,                        ox + 5,          ry + 4, WHITE,             false);
            ctx.drawText(textRenderer, String.format("%.1f", p),  ox + 36,         ry + 4, GOLD,              false);
            ctx.drawText(textRenderer, changeStr,                  ox + 78,         ry + 4, c >= 0 ? GREEN : RED, false);

            // [-] remove button
            int btnX = ox + DIVIDER_X - 18;
            boolean btnHover = mx >= btnX && mx < btnX + 14 && my >= ry + 2 && my < ry + ROW_H - 2;
            ctx.drawText(textRenderer, "§c[-]", btnX, ry + 4, btnHover ? RED : MUTED, false);
        }
    }

    private void drawRightPanel(DrawContext ctx, int ox, int oy, int mx, int my) {
        int rx = ox + DIVIDER_X + 1; // right panel origin x
        int rw = W - DIVIDER_X - 1; // right panel width

        // Header bar
        ctx.fill(rx, oy, ox + W, oy + HEADER_H, HDR_BG);
        ctx.drawText(textRenderer, "§7ALL STOCKS", rx + 5, oy + 9, MUTED, false);

        int bodyY = oy + BODY_TOP;
        int maxY  = oy + H - 4;
        int vis   = rightVisibleRows();

        // Clamp scroll
        int maxScroll = Math.max(0, allStocks.size() - vis);
        rightScroll = Math.max(0, Math.min(rightScroll, maxScroll));

        for (int vi = 0; vi < vis; vi++) {
            int si = vi + rightScroll;
            if (si >= allStocks.size()) break;

            StockDefinition def = allStocks.get(si);
            String tk = def.ticker();
            int ry = bodyY + vi * ROW_H;
            if (ry + ROW_H > maxY) break;

            boolean inWatch = tracked.contains(tk);
            boolean rowHover = mx >= rx + 2 && mx < ox + W - 2
                    && my >= ry && my < ry + ROW_H;
            if (rowHover) ctx.fill(rx + 2, ry, ox + W - 2, ry + ROW_H - 1, ROW_HOVER);

            double p = prices.getOrDefault(tk, 0.0);

            // Sector abbreviation (first 4 chars)
            String sector = def.sector().name().substring(0, Math.min(4, def.sector().name().length()));

            ctx.drawText(textRenderer, tk,                       rx + 5,  ry + 4, WHITE, false);
            ctx.drawText(textRenderer, "§8" + sector,            rx + 32, ry + 4, DIM,   false);
            ctx.drawText(textRenderer, String.format("%.1f", p), rx + 65, ry + 4, GOLD,  false);

            // [+] / [✓] button
            int btnX = ox + W - 22;
            if (inWatch) {
                ctx.drawText(textRenderer, "§8[✓]", btnX, ry + 4, DIM, false);
            } else {
                boolean btnHover = mx >= btnX && mx < btnX + 16 && my >= ry + 2 && my < ry + ROW_H - 2;
                ctx.drawText(textRenderer, "§a[+]", btnX, ry + 4, btnHover ? GREEN : MUTED, false);
            }
        }

        // Scroll hint if list is taller than the panel
        if (allStocks.size() > vis) {
            String hint = String.format("§8%d/%d", rightScroll + 1, allStocks.size());
            ctx.drawText(textRenderer, hint, ox + W - textRenderer.getWidth(hint.replaceAll("§.", "")) - 3,
                    oy + H - 10, DIM, false);
        }
    }

    // ── Mouse interaction ──────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int ox = ox(), oy = oy();

        // Left panel: [-] remove buttons
        int bodyY = oy + BODY_TOP;
        for (int i = 0; i < tracked.size(); i++) {
            int ry   = bodyY + i * ROW_H;
            int btnX = ox + DIVIDER_X - 18;
            if (mx >= btnX && mx < btnX + 14 && my >= ry + 2 && my < ry + ROW_H - 2) {
                tracked.remove(i);
                return true;
            }
        }

        // Right panel: [+] add buttons
        int rx  = ox + DIVIDER_X + 1;
        int vis = rightVisibleRows();
        for (int vi = 0; vi < vis; vi++) {
            int si = vi + rightScroll;
            if (si >= allStocks.size()) break;
            String tk  = allStocks.get(si).ticker();
            int    ry  = bodyY + vi * ROW_H;
            int    btnX = ox + W - 22;
            if (mx >= btnX && mx < btnX + 16 && my >= ry + 2 && my < ry + ROW_H - 2) {
                if (!tracked.contains(tk) && tracked.size() < QuotronBlockEntity.MAX_TRACKED) {
                    tracked.add(tk);
                }
                return true;
            }
        }

        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        int ox = ox(), oy = oy();
        if (mx >= ox + DIVIDER_X + 1 && mx < ox + W && my >= oy && my < oy + H) {
            rightScroll -= (int) Math.signum(v);
            int maxScroll = Math.max(0, allStocks.size() - rightVisibleRows());
            rightScroll = Math.max(0, Math.min(rightScroll, maxScroll));
            return true;
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public void close() {
        ModPacketsClient.sendUpdateQuotron(tracked, blockPos);
        super.close();
    }

    @Override public boolean shouldPause() { return false; }
}
