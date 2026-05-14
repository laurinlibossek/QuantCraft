package com.quantcraft.screen;

import com.quantcraft.item.NewspaperItem;
import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.*;

public class NewspaperScreen extends Screen {
    record Article(String masthead, String section, String headline, List<String> body, String edition) {}

    private static final Map<NewspaperItem.NewspaperType, Article> DATA;
    static {
        DATA = new EnumMap<>(NewspaperItem.NewspaperType.class);
        DATA.put(NewspaperItem.NewspaperType.DIAMOND_DISCOVERY, new Article(
                "THE OVERWORLD TIMES", "Mining Special",
                "50-CARAT DIAMOND FOUND\nIN LOCAL MINES",
                List.of(
                    "Workers in Stonehaven struck it rich",
                    "yesterday, unearthing what geologists are",
                    "calling the region's largest diamond ever.",
                    "",
                    "\"We just kept digging,\" said one miner,",
                    "exhausted but grinning. The find has",
                    "already impacted markets, with Diamond",
                    "prices falling 18% on oversupply fears.",
                    "",
                    "Officials expect mining operations to",
                    "intensify across the region."
                ),
                "Vol. 1, No. 4"));

        DATA.put(NewspaperItem.NewspaperType.TRADE_WAR, new Article(
                "THE VILLAGE GAZETTE", "Economics",
                "TRADE TENSIONS ESCALATE\nBETWEEN REGIONS",
                List.of(
                    "Two neighboring settlements have begun",
                    "imposing retaliatory tariffs on each",
                    "other's goods, sparking fears of wider",
                    "trade disruptions.",
                    "",
                    "Manufactured goods like Glass, Brick, and",
                    "Paper have been hit hardest, with prices",
                    "down 22-31% across the board.",
                    "",
                    "Agrarian exports, meanwhile, have surged",
                    "as demand shifts to food production. No",
                    "resolution in sight."
                ),
                "Vol. 2, No. 7"));

        DATA.put(NewspaperItem.NewspaperType.MINING_BOOM, new Article(
                "THE OVERWORLD TIMES", "Industry",
                "MASSIVE IRON VEIN LOCATED\nBENEATH EASTERN HILLS",
                List.of(
                    "Surveyors have confirmed discovery of",
                    "one of the largest iron deposits on",
                    "record, buried beneath the Eastern Hills.",
                    "",
                    "The find has already flooded markets,",
                    "with Iron Ingot prices down 25% in",
                    "emergency trading. Coal prices have also",
                    "softened on excess supply.",
                    "",
                    "Economists warn of potential oversupply",
                    "if extraction rates accelerate further."
                ),
                "Vol. 1, No. 12"));

        DATA.put(NewspaperItem.NewspaperType.LUMBER_SHORTAGE, new Article(
                "THE FOREST HERALD", "Environment",
                "ANCIENT BLIGHT DESTROYS\nOVERWORLD FORESTS",
                List.of(
                    "A fungal blight of unknown origin is",
                    "rapidly killing centuries-old trees",
                    "across northern biomes.",
                    "",
                    "Foresters report that Oak, Birch, and",
                    "Spruce logs have surged over 30% in",
                    "emergency trading as supply collapses.",
                    "",
                    "Lumber mills are shuttering operations,",
                    "and construction projects are being delayed",
                    "indefinitely. Remedy remains elusive."
                ),
                "Vol. 5, No. 2"));

        DATA.put(NewspaperItem.NewspaperType.GOLD_RUSH, new Article(
                "THE OVERWORLD TIMES", "Markets",
                "GOLD STRIKE TRIGGERS\nMASSIVE RUSH TO BADLANDS",
                List.of(
                    "Rumors of a massive gold strike in the",
                    "Eastern Badlands have sent thousands of",
                    "prospectors racing to stake claims.",
                    "",
                    "The sudden influx has created a frenzy,",
                    "with tent cities springing up overnight.",
                    "",
                    "Gold Ingot futures, however, have tumbled",
                    "29% on supply glut fears. Established",
                    "traders warn this bubble may burst soon."
                ),
                "Vol. 2, No. 3"));

        DATA.put(NewspaperItem.NewspaperType.HARVEST_FESTIVAL, new Article(
                "THE VILLAGE GAZETTE", "Agriculture",
                "RECORD HARVEST EXCEEDS\nALL EXPECTATIONS",
                List.of(
                    "This season's crop yields have shattered",
                    "all records, with Wheat, Carrot, and",
                    "Potato production at 30-year highs.",
                    "",
                    "Farmers report ideal growing conditions,",
                    "and early harvests have already begun.",
                    "",
                    "Prices across the Agrarian sector have",
                    "collapsed 35-42%, threatening farm",
                    "incomes. Relief programs being considered."
                ),
                "Vol. 4, No. 9"));

        DATA.put(NewspaperItem.NewspaperType.ARCANE_ANOMALY, new Article(
                "THE ARCANE OBSERVER", "Markets",
                "UNEXPLAINED PORTAL SPIKES\nDRIVE ARCANE DEMAND",
                List.of(
                    "Dimensional fluctuations across multiple",
                    "regions have sent demand for arcane",
                    "components through the ceiling.",
                    "",
                    "Researchers remain baffled by the surge",
                    "in portal activity, with no clear cause",
                    "or timeline identified.",
                    "",
                    "Ender Pearl and Blaze Rod futures have",
                    "surged 25% as traders hedge against",
                    "further dimensional instability."
                ),
                "Vol. 1, No. 1"));

        DATA.put(NewspaperItem.NewspaperType.LIVESTOCK_PLAGUE, new Article(
                "THE OVERWORLD TIMES", "Agriculture",
                "LIVESTOCK PLAGUE DECIMATES\nREGIONAL HERDS",
                List.of(
                    "A virulent plague has swept through",
                    "livestock populations across three major",
                    "biomes in just two days.",
                    "",
                    "Herds have been virtually eliminated,",
                    "with supplies of Leather, Wool, and",
                    "Feathers now critically scarce.",
                    "",
                    "Prices for these commodities have",
                    "collapsed as panic selling takes hold.",
                    "Recovery could take months or years."
                ),
                "Vol. 3, No. 6"));

        DATA.put(NewspaperItem.NewspaperType.EMERALD_CARTEL, new Article(
                "THE VILLAGE GAZETTE", "Markets",
                "VILLAGER SYNDICATE CORNERS\nEMERALD SUPPLY",
                List.of(
                    "A secretive cartel of merchant villagers",
                    "has reportedly cornered approximately 80%",
                    "of the regional emerald supply.",
                    "",
                    "The monopoly has sent prices rocketing,",
                    "with Emeralds up 40% in emergency",
                    "trading. Experts expect sustained boom.",
                    "",
                    "Authorities are investigating potential",
                    "antitrust violations, but enforcement",
                    "remains unclear."
                ),
                "Vol. 6, No. 3"));
    }

    private final NewspaperItem.NewspaperType type;
    private final Article article;

    private static final int PANEL_W = 300;
    private static final int PANEL_H = 320;
    private static final int BG_COLOR = 0xFFFFFCF0;
    private static final int INK_DARK = 0xFF1a1408;
    private static final int INK_BODY = 0xFF2a2008;
    private static final int SECTION_COLOR = 0xFF6a5a3a;
    private static final int MASTHEAD_BG = 0xFF2a2a2a;
    private static final int MASTHEAD_TEXT = 0xFFe8d8b8;
    private static final int BUTTON_BG = 0xFF3a2a18;
    private static final int BUTTON_TEXT = 0xFFe8d8b8;

    public NewspaperScreen(NewspaperItem.NewspaperType type) {
        super(Text.literal("Newspaper"));
        this.type = type;
        this.article = DATA.getOrDefault(type,
                new Article("THE OVERWORLD TIMES", "News", "MARKET UPDATE",
                        List.of("No details available."), ""));
    }

    @Override
    public void renderBackground(DrawContext ctx, int mx, int my, float delta) {
        // Suppress full-screen dark overlay — newspaper panel is fully opaque
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        int ox = (width - PANEL_W) / 2;
        int oy = (height - PANEL_H) / 2;

        // Parchment background
        ctx.fill(ox, oy, ox + PANEL_W, oy + PANEL_H, BG_COLOR);

        // Border (2px all sides)
        int border = 0xFF8a7a5a;
        ctx.fill(ox, oy, ox + PANEL_W, oy + 2, border);
        ctx.fill(ox, oy + PANEL_H - 2, ox + PANEL_W, oy + PANEL_H, border);
        ctx.fill(ox, oy, ox + 2, oy + PANEL_H, border);
        ctx.fill(ox + PANEL_W - 2, oy, ox + PANEL_W, oy + PANEL_H, border);

        // Masthead bar (dark header)
        int masthead_h = 32;
        ctx.fill(ox + 2, oy + 2, ox + PANEL_W - 2, oy + 2 + masthead_h, MASTHEAD_BG);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(article.masthead).formatted(Formatting.BOLD),
                ox + PANEL_W / 2, oy + 10, MASTHEAD_TEXT);

        int cy = oy + 2 + masthead_h + 6;

        // Edition + section line (centered, smaller text)
        String metaLine = article.edition + "  ·  " + article.section;
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal(metaLine).formatted(Formatting.ITALIC),
                ox + PANEL_W / 2, cy, SECTION_COLOR);
        cy += 12;

        // Horizontal rule
        ctx.fill(ox + 12, cy, ox + PANEL_W - 12, cy + 1, SECTION_COLOR);
        cy += 8;

        // Headline (bold, large, 1.3x scale, up to 2 lines)
        String[] hlLines = article.headline.split("\n");
        ctx.getMatrices().push();
        float hlScale = 1.25f;
        int hlCx = ox + PANEL_W / 2;
        int hlTy = (int)(cy + textRenderer.fontHeight * hlScale / 2f);
        ctx.getMatrices().translate(hlCx, hlTy, 0);
        ctx.getMatrices().scale(hlScale, hlScale, 1f);
        int hlOy = 0;
        for (String line : hlLines) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal(line).formatted(Formatting.BOLD), 0, hlOy, INK_DARK);
            hlOy += (int)(textRenderer.fontHeight + 2);
        }
        ctx.getMatrices().pop();
        cy += (int)(hlOy * hlScale) + 4;

        // Double rule under headline
        ctx.fill(ox + 12, cy, ox + PANEL_W - 12, cy + 1, SECTION_COLOR);
        ctx.fill(ox + 12, cy + 3, ox + PANEL_W - 12, cy + 4, SECTION_COLOR);
        cy += 10;

        // Body text in two-column layout
        int bodyWidth = PANEL_W - 28;
        int colWidth = (bodyWidth - 4) / 2;
        int bodyBottom = oy + PANEL_H - 50;
        int lineHeight = textRenderer.fontHeight + 2;

        List<String> bodyLines = article.body;
        int mid = (bodyLines.size() + 1) / 2;

        // Left column
        int c1y = cy;
        for (int i = 0; i < mid; i++) {
            if (c1y + lineHeight > bodyBottom) break;
            ctx.drawText(textRenderer, bodyLines.get(i), ox + 14, c1y, INK_BODY, false);
            c1y += lineHeight;
        }

        // Right column
        int c2y = cy;
        for (int i = mid; i < bodyLines.size(); i++) {
            if (c2y + lineHeight > bodyBottom) break;
            ctx.drawText(textRenderer, bodyLines.get(i), ox + 14 + colWidth + 4, c2y, INK_BODY, false);
            c2y += lineHeight;
        }

        // "Read & Discard" button
        int btnW = 120;
        int btnH = 20;
        int btnX = ox + PANEL_W / 2 - btnW / 2;
        int btnY = oy + PANEL_H - 30;
        boolean hovered = mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH;
        int btnColor = hovered ? 0xFF4a3a28 : BUTTON_BG;
        ctx.fill(btnX, btnY, btnX + btnW, btnY + btnH, btnColor);
        ctx.fill(btnX, btnY, btnX + btnW, btnY + 1, 0xFF6a5a48);
        ctx.fill(btnX, btnY, btnX + 1, btnY + btnH, 0xFF6a5a48);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Read & Discard"),
                ox + PANEL_W / 2, btnY + (btnH - textRenderer.fontHeight) / 2, BUTTON_TEXT);

        super.render(ctx, mx, my, delta);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int ox = (width - PANEL_W) / 2;
        int oy = (height - PANEL_H) / 2;
        int btnW = 120;
        int btnH = 20;
        int btnX = ox + PANEL_W / 2 - btnW / 2;
        int btnY = oy + PANEL_H - 30;

        if (button == 0 && mx >= btnX && mx < btnX + btnW && my >= btnY && my < btnY + btnH) {
            ModPacketsClient.sendNewspaperRead(type);
            close();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean shouldPause() { return false; }
}
