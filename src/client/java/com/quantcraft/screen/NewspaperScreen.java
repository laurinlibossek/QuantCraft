package com.quantcraft.screen;

import com.quantcraft.item.NewspaperItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;

public class NewspaperScreen extends Screen {

    record Article(String masthead, String section, String headline,
                   List<String> body, String edition) {}

    // ── Article data ──────────────────────────────────────────────────────────
    private static final Map<NewspaperItem.NewspaperType, Article> DATA;
    static {
        DATA = new EnumMap<>(NewspaperItem.NewspaperType.class);
        DATA.put(NewspaperItem.NewspaperType.DIAMOND_DISCOVERY, new Article(
                "THE OVERWORLD TIMES", "Mining Special",
                "50-CARAT DIAMOND FOUND\nIN LOCAL MINES",
                List.of(
                    "Workers in Stonehaven struck it rich yesterday, unearthing what geologists",
                    "are calling the region's largest diamond ever.",
                    "",
                    "\"We just kept digging,\" said one miner, exhausted but grinning. The find",
                    "has already impacted markets, with Diamond prices falling 18% on oversupply.",
                    "",
                    "Officials expect mining operations to intensify across the region."
                ), "Vol. 1, No. 4"));

        DATA.put(NewspaperItem.NewspaperType.TRADE_WAR, new Article(
                "THE VILLAGE GAZETTE", "Economics",
                "TRADE TENSIONS ESCALATE\nBETWEEN REGIONS",
                List.of(
                    "Two neighboring settlements have begun imposing retaliatory tariffs on each",
                    "other's goods, sparking fears of wider trade disruptions.",
                    "",
                    "Manufactured goods like Glass, Brick, and Paper have been hit hardest,",
                    "with prices down 22-31% across the board.",
                    "",
                    "Agrarian exports have surged as demand shifts to food production.",
                    "No resolution in sight."
                ), "Vol. 2, No. 7"));

        DATA.put(NewspaperItem.NewspaperType.MINING_BOOM, new Article(
                "THE OVERWORLD TIMES", "Industry",
                "MASSIVE IRON VEIN LOCATED\nBENEATH EASTERN HILLS",
                List.of(
                    "Surveyors have confirmed discovery of one of the largest iron deposits on",
                    "record, buried beneath the Eastern Hills.",
                    "",
                    "The find has already flooded markets, with Iron Ingot prices down 25% in",
                    "emergency trading. Coal prices have also softened on excess supply.",
                    "",
                    "Economists warn of potential oversupply if extraction rates accelerate."
                ), "Vol. 1, No. 12"));

        DATA.put(NewspaperItem.NewspaperType.LUMBER_SHORTAGE, new Article(
                "THE FOREST HERALD", "Environment",
                "ANCIENT BLIGHT DESTROYS\nOVERWORLD FORESTS",
                List.of(
                    "A fungal blight of unknown origin is rapidly killing centuries-old trees",
                    "across northern biomes.",
                    "",
                    "Foresters report that Oak, Birch, and Spruce logs have surged over 30% in",
                    "emergency trading as supply collapses.",
                    "",
                    "Lumber mills are shuttering operations and construction projects are delayed.",
                    "Remedy remains elusive."
                ), "Vol. 5, No. 2"));

        DATA.put(NewspaperItem.NewspaperType.GOLD_RUSH, new Article(
                "THE OVERWORLD TIMES", "Markets",
                "GOLD STRIKE TRIGGERS\nMASSIVE RUSH TO BADLANDS",
                List.of(
                    "Rumors of a massive gold strike in the Eastern Badlands have sent thousands",
                    "of prospectors racing to stake claims.",
                    "",
                    "The sudden influx has created a frenzy, with tent cities springing up overnight.",
                    "",
                    "Gold Ingot futures tumbled 29% on supply glut fears. Established traders",
                    "warn this bubble may burst soon."
                ), "Vol. 2, No. 3"));

        DATA.put(NewspaperItem.NewspaperType.HARVEST_FESTIVAL, new Article(
                "THE VILLAGE GAZETTE", "Agriculture",
                "RECORD HARVEST EXCEEDS\nALL EXPECTATIONS",
                List.of(
                    "This season's crop yields have shattered all records, with Wheat, Carrot,",
                    "and Potato production at 30-year highs.",
                    "",
                    "Farmers report ideal growing conditions and early harvests have already begun.",
                    "",
                    "Prices across the Agrarian sector have collapsed 35-42%, threatening farm",
                    "incomes. Relief programs being considered."
                ), "Vol. 4, No. 9"));

        DATA.put(NewspaperItem.NewspaperType.ARCANE_ANOMALY, new Article(
                "THE ARCANE OBSERVER", "Markets",
                "UNEXPLAINED PORTAL SPIKES\nDRIVE ARCANE DEMAND",
                List.of(
                    "Dimensional fluctuations across multiple regions have sent demand for arcane",
                    "components through the ceiling.",
                    "",
                    "Researchers remain baffled by the surge in portal activity, with no clear",
                    "cause or timeline identified.",
                    "",
                    "Ender Pearl and Blaze Rod futures surged 25% as traders hedge against",
                    "further dimensional instability."
                ), "Vol. 1, No. 1"));

        DATA.put(NewspaperItem.NewspaperType.LIVESTOCK_PLAGUE, new Article(
                "THE OVERWORLD TIMES", "Agriculture",
                "LIVESTOCK PLAGUE DECIMATES\nREGIONAL HERDS",
                List.of(
                    "A virulent plague has swept through livestock populations across three major",
                    "biomes in just two days.",
                    "",
                    "Herds have been virtually eliminated, with supplies of Leather, Wool, and",
                    "Feathers now critically scarce.",
                    "",
                    "Prices for these commodities collapsed as panic selling takes hold.",
                    "Recovery could take months or years."
                ), "Vol. 3, No. 6"));

        DATA.put(NewspaperItem.NewspaperType.EMERALD_CARTEL, new Article(
                "THE VILLAGE GAZETTE", "Markets",
                "VILLAGER SYNDICATE CORNERS\nEMERALD SUPPLY",
                List.of(
                    "A secretive cartel of merchant villagers has reportedly cornered approximately",
                    "80% of the regional emerald supply.",
                    "",
                    "The monopoly has sent prices rocketing, with Emeralds up 40% in emergency",
                    "trading. Experts expect a sustained boom.",
                    "",
                    "Authorities are investigating potential antitrust violations, but enforcement",
                    "remains unclear."
                ), "Vol. 6, No. 3"));
    }

    // ── Layout constants ──────────────────────────────────────────────────────
    private static final int PANEL_W = 300;
    private static final int PANEL_H = 240;

    // ── Colours ───────────────────────────────────────────────────────────────
    private static final int BG_COLOR    = 0xFFFFFCF0;
    private static final int INK_DARK    = 0xFF1a1408;
    private static final int INK_BODY    = 0xFF2a2008;
    private static final int SECTION_C   = 0xFF6a5a3a;
    private static final int MASTHEAD_BG = 0xFF2a2a2a;
    private static final int MASTHEAD_TX = 0xFFe8d8b8;
    private static final int BORDER_C    = 0xFF8a7a5a;

    // ── Instance state ────────────────────────────────────────────────────────
    private final Article article;

    // Computed in init() when textRenderer is available
    private List<String> leftCol  = List.of();
    private List<String> rightCol = List.of();
    private int panelX, panelY;

    // ── Constructor ──────────────────────────────────────────────────────────
    public NewspaperScreen(NewspaperItem.NewspaperType type) {
        super(Text.literal("Newspaper"));
        this.article = DATA.getOrDefault(type,
                new Article("THE OVERWORLD TIMES", "News", "MARKET UPDATE",
                        List.of("No details available."), ""));
    }

    // ── Init ─────────────────────────────────────────────────────────────────
    @Override
    protected void init() {
        panelX = (width  - PANEL_W) / 2;
        panelY = (height - PANEL_H) / 2;

        // Column width: full content area minus 8 px gutter, halved
        int colWidth = (PANEL_W - 28 - 8) / 2;

        // Re-wrap body text to column width now that textRenderer is ready
        List<String> wrapped = rewrapBody(article.body, colWidth);
        // Split at the last paragraph boundary (empty line) at or before the midpoint
        int mid = (wrapped.size() + 1) / 2;
        int split = mid;
        for (int i = mid; i >= 1; i--) {
            if (wrapped.get(i - 1).isBlank()) { split = i; break; }
        }
        leftCol  = new ArrayList<>(wrapped.subList(0, split));
        rightCol = new ArrayList<>(wrapped.subList(split, wrapped.size()));

        int btnW = 80, btnH = 20;
        this.addDrawableChild(
            ButtonWidget.builder(Text.literal("Close"), btn -> close())
            .dimensions(panelX + PANEL_W / 2 - btnW / 2,
                        panelY + PANEL_H - 28,
                        btnW, btnH)
            .build()
        );
    }

    // ── Render ────────────────────────────────────────────────────────────────

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // Suppress full-screen dim so the world is visible behind the newspaper.
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {

        // ── Panel background + border ─────────────────────────────────────────
        ctx.fill(panelX, panelY, panelX + PANEL_W, panelY + PANEL_H, BG_COLOR);
        ctx.fill(panelX,               panelY,               panelX + PANEL_W, panelY + 2,       BORDER_C);
        ctx.fill(panelX,               panelY + PANEL_H - 2, panelX + PANEL_W, panelY + PANEL_H, BORDER_C);
        ctx.fill(panelX,               panelY,               panelX + 2,       panelY + PANEL_H, BORDER_C);
        ctx.fill(panelX + PANEL_W - 2, panelY,               panelX + PANEL_W, panelY + PANEL_H, BORDER_C);

        // ── Masthead ──────────────────────────────────────────────────────────
        final int MAST_H = 32;
        ctx.fill(panelX + 2, panelY + 2, panelX + PANEL_W - 2, panelY + 2 + MAST_H, MASTHEAD_BG);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(article.masthead).formatted(Formatting.BOLD),
                panelX + PANEL_W / 2, panelY + 10, MASTHEAD_TX);

        int cy = panelY + 2 + MAST_H + 6;

        // ── Edition + section line ────────────────────────────────────────────
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(article.edition + "  ·  " + article.section).formatted(Formatting.ITALIC),
                panelX + PANEL_W / 2, cy, SECTION_C);
        cy += 12;

        ctx.fill(panelX + 12, cy, panelX + PANEL_W - 12, cy + 1, SECTION_C);
        cy += 8;

        // ── Headline — scaled, shadow OFF ─────────────────────────────────────
        // FIX: was using drawCenteredTextWithShadow which doubles the dark ink
        // colour on parchment, making it look muddy. shadow=false is cleaner.
        String[] hlLines  = article.headline.split("\n");
        final float SCALE = 1.25f;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(panelX + PANEL_W / 2.0, cy, 0);
        ctx.getMatrices().scale(SCALE, SCALE, 1f);

        int hlY = 0;
        for (String line : hlLines) {
            Text t  = Text.literal(line).formatted(Formatting.BOLD);
            int  tw = textRenderer.getWidth(t);
            ctx.drawText(textRenderer, t, -tw / 2, hlY, INK_DARK, false); // shadow = false
            hlY += textRenderer.fontHeight + 2;
        }
        ctx.getMatrices().pop();

        cy += (int)(hlY * SCALE) + 4;

        // Double rule under headline
        ctx.fill(panelX + 12, cy,     panelX + PANEL_W - 12, cy + 1, SECTION_C);
        ctx.fill(panelX + 12, cy + 3, panelX + PANEL_W - 12, cy + 4, SECTION_C);
        cy += 10;

        // ── Two-column body ───────────────────────────────────────────────────
        // FIX: body lines are now re-wrapped to colWidth in init(), so they
        // never overflow into the adjacent column or past the panel edge.
        int colWidth   = (PANEL_W - 28 - 8) / 2;
        int col1X      = panelX + 14;
        int col2X      = col1X + colWidth + 8;
        int lineH      = textRenderer.fontHeight + 2;
        int bodyBottom = panelY + PANEL_H - 34;

        int c1y = cy;
        for (String line : leftCol) {
            if (c1y + lineH > bodyBottom) break;
            if (!line.isBlank()) ctx.drawText(textRenderer, line, col1X, c1y, INK_BODY, false);
            c1y += lineH;
        }

        int c2y = cy;
        for (String line : rightCol) {
            if (c2y + lineH > bodyBottom) break;
            if (!line.isBlank()) ctx.drawText(textRenderer, line, col2X, c2y, INK_BODY, false);
            c2y += lineH;
        }

        // Subtle column divider
        ctx.fill(col1X + colWidth + 3, cy, col1X + colWidth + 4, bodyBottom, 0x33000000);

        // ── Widgets (draws the Read & Discard ButtonWidget) ───────────────────
        super.render(ctx, mx, my, delta);
    }

    // ── Misc ──────────────────────────────────────────────────────────────────
    @Override
    public boolean shouldPause() { return false; }

    // ── Word-wrap helpers ─────────────────────────────────────────────────────

    /**
     * Joins raw body lines into paragraphs, then re-wraps each paragraph to
     * {@code maxWidth} pixels using the current textRenderer.
     * Empty source lines are treated as paragraph breaks.
     */
    private List<String> rewrapBody(List<String> raw, int maxWidth) {
        List<String> out  = new ArrayList<>();
        StringBuilder para = new StringBuilder();

        for (String line : raw) {
            if (line.isBlank()) {
                if (para.length() > 0) {
                    wrapParagraph(para.toString().trim(), maxWidth, out);
                    out.add("");       // blank line between paragraphs
                    para.setLength(0);
                }
            } else {
                if (para.length() > 0) para.append(' ');
                para.append(line.trim());
            }
        }
        if (para.length() > 0) wrapParagraph(para.toString().trim(), maxWidth, out);
        return out;
    }

    private void wrapParagraph(String text, int maxWidth, List<String> out) {
        String[]      words = text.split(" +");
        StringBuilder cur   = new StringBuilder();

        for (String word : words) {
            String candidate = cur.isEmpty() ? word : cur + " " + word;
            if (textRenderer.getWidth(candidate) > maxWidth && cur.length() > 0) {
                out.add(cur.toString());
                cur = new StringBuilder(word);
            } else {
                cur = new StringBuilder(candidate);
            }
        }
        if (cur.length() > 0) out.add(cur.toString());
    }
}
