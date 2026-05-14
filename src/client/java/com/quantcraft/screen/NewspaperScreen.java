package com.quantcraft.screen;

import com.quantcraft.item.NewspaperItem;
import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;

public class NewspaperScreen extends Screen {
    record Content(String masthead, String sub, String headline, List<String> body, String edition) {}

    private static final Map<NewspaperItem.NewspaperType, Content> DATA;
    static {
        DATA = new EnumMap<>(NewspaperItem.NewspaperType.class);
        DATA.put(NewspaperItem.NewspaperType.DIAMOND_DISCOVERY, new Content(
                "THE OVERWORLD TIMES", "Mining Special",
                "TOWN DISCOVERS 50ct\nDIAMOND IN LOCAL MINES",
                List.of("Workers in Stonehaven unearthed what", "experts call the largest gem ever",
                        "recorded in the region.", "", "\"We just kept digging,\" said one miner."),
                "Vol. 1, No. 4"));
        DATA.put(NewspaperItem.NewspaperType.DRAGON_SLAIN, new Content(
                "THE OVERWORLD TIMES", "Breaking",
                "ENDER DRAGON SLAIN —\nARCANE MARKETS CRASH",
                List.of("Brave adventurers defeated the Ender", "Dragon, flooding the market with",
                        "arcane materials overnight.", "", "Ender Pearl futures fell 40%."),
                "Vol. 3, No. 1"));
        DATA.put(NewspaperItem.NewspaperType.TRADE_WAR, new Content(
                "THE VILLAGE GAZETTE", "Economics",
                "TRADE WAR ERUPTS\nBETWEEN VILLAGES",
                List.of("Two villages have imposed retaliatory", "tariffs on manufactured goods.", "",
                        "Glass, Brick and Paper fell sharply.", "Agrarian exports surge."),
                "Vol. 2, No. 7"));
        DATA.put(NewspaperItem.NewspaperType.MINING_BOOM, new Content(
                "THE OVERWORLD TIMES", "Industry",
                "NEW VEIN FOUND —\nIRON SUPPLY FLOODED",
                List.of("A massive iron deposit was located", "beneath the eastern hills.", "",
                        "Iron Ingot prices fell 25%.", "Coal also softened on excess supply."),
                "Vol. 1, No. 12"));
        DATA.put(NewspaperItem.NewspaperType.LUMBER_SHORTAGE, new Content(
                "THE FOREST HERALD", "Environment",
                "ANCIENT BLIGHT STRIKES\nOVERWORLD FORESTS",
                List.of("A fungal blight is killing old-growth", "trees across northern biomes.", "",
                        "Oak, Birch and Spruce logs surged", "over 30% in a single session."),
                "Vol. 5, No. 2"));
        DATA.put(NewspaperItem.NewspaperType.GOLD_RUSH, new Content(
                "THE OVERWORLD TIMES", "Markets",
                "GOLD RUSH!\nPROSPECTORS FLOOD HILLS",
                List.of("Thousands flooded the Eastern", "Badlands following gold strike",
                        "rumours overnight.", "", "Gold Ingot futures tumble on supply glut."),
                "Vol. 2, No. 3"));
        DATA.put(NewspaperItem.NewspaperType.HARVEST_FESTIVAL, new Content(
                "THE VILLAGE GAZETTE", "Agriculture",
                "RECORD HARVEST —\nBEST YIELDS IN DECADES",
                List.of("This season's crop yields exceeded", "all expectations. Wheat, Carrot",
                        "and Potato at 30-year highs.", "", "Prices collapsed across Agrarian sector."),
                "Vol. 4, No. 9"));
        DATA.put(NewspaperItem.NewspaperType.ARCANE_ANOMALY, new Content(
                "THE ARCANE OBSERVER", "Markets",
                "PORTAL ACTIVITY SPIKES\nARCANE DEMAND",
                List.of("Unexplained portal fluctuations", "sent demand for magical",
                        "components soaring overnight.", "", "Ender Pearls and Blaze Rods surged 25%."),
                "Vol. 1, No. 1"));
        DATA.put(NewspaperItem.NewspaperType.LIVESTOCK_PLAGUE, new Content(
                "THE OVERWORLD TIMES", "Agriculture",
                "LIVESTOCK PLAGUE\nSWEEPS THE REGION",
                List.of("A virulent plague decimated livestock", "across three biomes in two days.", "",
                        "Leather, Wool, Feather collapsed", "as supply evaporated."),
                "Vol. 3, No. 6"));
        DATA.put(NewspaperItem.NewspaperType.EMERALD_CARTEL, new Content(
                "THE VILLAGE GAZETTE", "Markets",
                "VILLAGER SYNDICATE\nCORNERS EMERALD SUPPLY",
                List.of("A secretive cartel has reportedly", "cornered 80% of emerald supply,",
                        "sending prices rocketing.", "", "EMER surged 40%, sustained boom expected."),
                "Vol. 6, No. 3"));
    }

    private final NewspaperItem.NewspaperType type;
    private final Content                     content;

    private static final int W          = 280;
    private static final int H          = 250;
    private static final int MASTHEAD_H = 24;
    private static final int BTN_W      = 104;
    private static final int BTN_H      = 16;

    private static final int BG          = 0xFFFFF8E7;
    private static final int MASTHEAD_BG = 0xFF2A2A2A;
    private static final int WHITE       = 0xFFFFFFFF;
    private static final int INK         = 0xFF0A0800;
    private static final int RULE        = 0xFF4A4430;
    private static final int GRAY        = 0xFF555040;
    private static final int BTN_BG      = 0xFF2A2015;
    private static final int BTN_HILITE  = 0xFF4A3A25;
    private static final int BTN_SHADOW  = 0xFF100C06;
    private static final int BTN_TEXT    = 0xFFFFEECC;

    public NewspaperScreen(NewspaperItem.NewspaperType type) {
        super(Text.literal("Newspaper"));
        this.type    = type;
        this.content = DATA.getOrDefault(type,
                new Content("THE OVERWORLD TIMES", "News", "MARKET UPDATE", List.of("No details."), ""));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // suppress full-screen dark overlay — newspaper panel is fully opaque
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        int ox = (width  - W) / 2;
        int oy = (height - H) / 2;

        // Parchment background
        ctx.fill(ox, oy, ox + W, oy + H, BG);

        // Outer border (2 px)
        ctx.fill(ox,         oy,         ox + W,     oy + 2,     RULE);
        ctx.fill(ox,         oy + H - 2, ox + W,     oy + H,     RULE);
        ctx.fill(ox,         oy,         ox + 2,     oy + H,     RULE);
        ctx.fill(ox + W - 2, oy,         ox + W,     oy + H,     RULE);

        // ── Masthead bar ──────────────────────────────────────────────────────
        ctx.fill(ox + 2, oy + 2, ox + W - 2, oy + 2 + MASTHEAD_H, MASTHEAD_BG);

        // Masthead text: bold white 1.3×
        ctx.getMatrices().push();
        float mhScale = 1.3f;
        int   mhCX    = ox + W / 2;
        int   mhTY    = (int)(oy + 2 + MASTHEAD_H / 2f - textRenderer.fontHeight * mhScale / 2f);
        ctx.getMatrices().translate(mhCX, mhTY, 0);
        ctx.getMatrices().scale(mhScale, mhScale, 1f);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(content.masthead()).formatted(Formatting.BOLD), 0, 0, WHITE);
        ctx.getMatrices().pop();

        int cy = oy + 2 + MASTHEAD_H + 4;

        // ── Rule 1 ────────────────────────────────────────────────────────────
        ctx.fill(ox + 8, cy, ox + W - 8, cy + 1, RULE);
        cy += 4;

        // Edition + section (italic gray, centered)
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(content.edition() + "  ·  " + content.sub()).formatted(Formatting.ITALIC),
                ox + W / 2, cy, GRAY);
        cy += textRenderer.fontHeight + 4;

        // ── Rule 2 ────────────────────────────────────────────────────────────
        ctx.fill(ox + 8, cy, ox + W - 8, cy + 1, RULE);
        cy += 6;

        // ── Headline: bold ink 1.2×, centered, up to 2 lines ─────────────────
        String[] hlLines    = content.headline().split("\n");
        float    hlScale    = 1.2f;
        int      hlLineStep = textRenderer.fontHeight + 2; // pre-scale spacing

        ctx.getMatrices().push();
        ctx.getMatrices().translate(ox + W / 2f, cy, 0);
        ctx.getMatrices().scale(hlScale, hlScale, 1f);
        int hlOY = 0;
        for (String line : hlLines) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(line).formatted(Formatting.BOLD), 0, hlOY, INK);
            hlOY += hlLineStep;
        }
        ctx.getMatrices().pop();
        cy += (int)(hlOY * hlScale) + 2;

        // ── Double rule ───────────────────────────────────────────────────────
        ctx.fill(ox + 8, cy,     ox + W - 8, cy + 1,     RULE);
        ctx.fill(ox + 8, cy + 3, ox + W - 8, cy + 4,     RULE);
        cy += 9;

        // ── Two-column body ───────────────────────────────────────────────────
        int bodyTop  = cy;
        int bodyBot  = oy + H - 32;
        int colMid   = ox + W / 2;
        int col1X    = ox + 10;
        int col2X    = colMid + 6;
        int lineStep = textRenderer.fontHeight + 1;

        // Vertical divider
        ctx.fill(colMid - 1, bodyTop, colMid, bodyBot, RULE);

        List<String> body = content.body();
        int half = (body.size() + 1) / 2;
        int c1y = bodyTop;
        int c2y = bodyTop;
        for (int i = 0; i < body.size(); i++) {
            String line = body.get(i);
            if (i < half) {
                if (c1y + lineStep <= bodyBot) {
                    ctx.drawText(textRenderer, line, col1X, c1y, INK, false);
                    c1y += lineStep;
                }
            } else {
                if (c2y + lineStep <= bodyBot) {
                    ctx.drawText(textRenderer, line, col2X, c2y, INK, false);
                    c2y += lineStep;
                }
            }
        }

        // ── Dark "Read & Discard" button ──────────────────────────────────────
        int btnX = ox + W / 2 - BTN_W / 2;
        int btnY = oy + H - BTN_H - 8;

        boolean hovered = mx >= btnX && mx < btnX + BTN_W && my >= btnY && my < btnY + BTN_H;
        int bgColor = hovered ? 0xFF3A3020 : BTN_BG;

        ctx.fill(btnX,              btnY,              btnX + BTN_W, btnY + BTN_H,     bgColor);
        ctx.fill(btnX,              btnY,              btnX + BTN_W, btnY + 1,          BTN_HILITE);
        ctx.fill(btnX,              btnY,              btnX + 1,     btnY + BTN_H,      BTN_HILITE);
        ctx.fill(btnX,              btnY + BTN_H - 1,  btnX + BTN_W, btnY + BTN_H,     BTN_SHADOW);
        ctx.fill(btnX + BTN_W - 1,  btnY,              btnX + BTN_W, btnY + BTN_H,     BTN_SHADOW);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Read & Discard"),
                ox + W / 2, btnY + (BTN_H - textRenderer.fontHeight) / 2, BTN_TEXT);

        super.render(ctx, mx, my, d);
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        int ox   = (width  - W) / 2;
        int oy   = (height - H) / 2;
        int btnX = ox + W / 2 - BTN_W / 2;
        int btnY = oy + H - BTN_H - 8;
        if (button == 0 && mx >= btnX && mx < btnX + BTN_W && my >= btnY && my < btnY + BTN_H) {
            ModPacketsClient.sendNewspaperRead(type);
            close();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean shouldPause() { return false; }
}
