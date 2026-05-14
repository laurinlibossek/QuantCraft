package com.quantcraft.screen;

import com.quantcraft.item.NewspaperItem;
import com.quantcraft.network.ModPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
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

    private static final int W    = 220, H = 230;
    private static final int BG   = 0xFFF5E6C8;
    private static final int INK  = 0xFF1a1005;
    private static final int RED  = 0xFF8B0000;
    private static final int LINE = 0xFFAA9070;

    public NewspaperScreen(NewspaperItem.NewspaperType type) {
        super(Text.literal("Newspaper"));
        this.type    = type;
        this.content = DATA.getOrDefault(type,
                new Content("THE OVERWORLD TIMES", "News", "MARKET UPDATE", List.of("No details."), ""));
    }

    @Override protected void init() {
        int x = (width - W) / 2, y = (height - H) / 2;
        addDrawableChild(ButtonWidget.builder(Text.literal("Read & Discard"), btn -> {
            ModPacketsClient.sendNewspaperRead(type); close();
        }).dimensions(x + W / 2 - 55, y + H - 28, 110, 18).build());
    }

    @Override public void render(DrawContext ctx, int mx, int my, float d) {
        int x = (width - W) / 2, y = (height - H) / 2;
        ctx.fill(x, y, x + W, y + H, BG);
        // Border
        ctx.fill(x + 2, y + 2, x + W - 2, y + 3, LINE);
        ctx.fill(x + 2, y + H - 3, x + W - 2, y + H - 2, LINE);
        ctx.fill(x + 4, y + 4, x + W - 4, y + 5, LINE);
        ctx.fill(x + 2, y + 2, x + 3, y + H - 2, LINE);
        ctx.fill(x + W - 3, y + 2, x + W - 2, y + H - 2, LINE);
        ctx.drawCenteredTextWithShadow(textRenderer, content.masthead(), x + W / 2, y + 8, RED);
        ctx.fill(x + 8, y + 18, x + W - 8, y + 19, INK);
        ctx.drawCenteredTextWithShadow(textRenderer, content.edition(), x + W / 2, y + 21, INK);
        ctx.fill(x + 8, y + 30, x + W - 8, y + 31, INK);
        ctx.drawCenteredTextWithShadow(textRenderer, content.sub(), x + W / 2, y + 34, RED);
        int hy = y + 44;
        for (String l : content.headline().split("\n")) {
            ctx.drawCenteredTextWithShadow(textRenderer, l, x + W / 2, hy, INK);
            hy += 11;
        }
        ctx.fill(x + 8, hy + 2, x + W - 8, hy + 3, LINE); hy += 8;
        for (String l : content.body()) {
            ctx.drawText(textRenderer, l, x + 12, hy, INK, false);
            hy += 10;
            if (hy > y + H - 36) break;
        }
        super.render(ctx, mx, my, d);
    }

    @Override public boolean shouldPause() { return false; }
}
