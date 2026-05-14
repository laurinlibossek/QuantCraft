package com.quantcraft.market;

import net.minecraft.util.Identifier;
import java.util.*;

public class StockRegistry {
    private static final List<StockDefinition>      ALL        = new ArrayList<>();
    private static final Map<String,StockDefinition> BY_TICKER  = new LinkedHashMap<>();
    private static final Map<String,String>          REAL_WORLD = new LinkedHashMap<>();

    public static void initialize() {
        // ── AGRARIAN ────────────────────────────────────────────────────────
        add("WHEAT","Wheat",        "minecraft:wheat",       AGRARIAN,   8,0.15,0.35,"ADM",  80000, 2.0, 0.90);
        add("CRRT", "Carrot",       "minecraft:carrot",      AGRARIAN,  10,0.18,0.32,"FDP",  60000, 2.5, 0.90);
        add("POTAT","Potato",       "minecraft:potato",      AGRARIAN,   9,0.16,0.34,"CALM", 60000, 2.0, 0.90);
        add("APPLE","Apple",        "minecraft:apple",       AGRARIAN,  14,0.20,0.30,"AAPL", 50000, 3.5, 0.92);
        add("MELON","Melon Slice",  "minecraft:melon_slice", AGRARIAN,  12,0.22,0.28,"DOLE", 55000, 3.0, 0.90);
        // ── MINING ──────────────────────────────────────────────────────────
        add("COAL", "Coal",         "minecraft:coal",        MINING,    18,0.28,0.30,"BTU",  40000,  6.0, 0.92);
        add("IRON", "Iron Ingot",   "minecraft:iron_ingot",  MINING,    35,0.32,0.28,"X",    30000, 12.0, 0.92);
        add("GOLD", "Gold Ingot",   "minecraft:gold_ingot",  MINING,    55,0.38,0.25,"NEM",  20000, 20.0, 0.93);
        add("DIAM", "Diamond",      "minecraft:diamond",     MINING,   250,0.55,0.20,"BHP",   5000,100.0, 0.93);
        add("EMER", "Emerald",      "minecraft:emerald",     MINING,   180,0.62,0.18,"RIO",   4000, 75.0, 0.93);
        add("LAPIS","Lapis Lazuli", "minecraft:lapis_lazuli",MINING,    22,0.30,0.30,"ALB",  25000,  8.0, 0.91);
        add("RDST", "Redstone",     "minecraft:redstone",    MINING,    28,0.35,0.28,"ITRI", 25000, 10.0, 0.91);
        add("QRTZ", "Quartz",       "minecraft:quartz",      MINING,    20,0.28,0.32,"MLM",  30000,  7.0, 0.91);
        // ── LUMBER ──────────────────────────────────────────────────────────
        add("OAKW", "Oak Log",      "minecraft:oak_log",     LUMBER,     6,0.14,0.40,"WY",  100000, 1.5, 0.90);
        add("BIRC", "Birch Log",    "minecraft:birch_log",   LUMBER,     7,0.15,0.38,"PCH", 100000, 1.5, 0.90);
        add("SPRCE","Spruce Log",   "minecraft:spruce_log",  LUMBER,     6,0.14,0.40,"RFP", 100000, 1.5, 0.90);
        // ── ARCANE ──────────────────────────────────────────────────────────
        add("EPRL", "Ender Pearl",  "minecraft:ender_pearl",   ARCANE,  60,0.80,0.15,"NVDA",  3000, 25.0, 0.93);
        add("BLAZ", "Blaze Rod",    "minecraft:blaze_rod",     ARCANE,  75,0.85,0.14,"ENPH",  3000, 30.0, 0.93);
        add("GLOW", "Glowstone",    "minecraft:glowstone_dust",ARCANE,  45,0.72,0.18,"SEDG",  5000, 18.0, 0.92);
        add("GHST", "Ghast Tear",   "minecraft:ghast_tear",    ARCANE, 120,0.90,0.12,"MSTR",  2000, 55.0, 0.94);
        add("SKEL", "Bone",         "minecraft:bone",           ARCANE,  15,0.50,0.22,"CRWD",  8000,  5.0, 0.91);
        // ── LIVESTOCK ───────────────────────────────────────────────────────
        add("LEAT", "Leather",      "minecraft:leather",      LIVESTOCK, 20,0.22,0.32,"TAP",  35000,  7.0, 0.91);
        add("WOOL", "Wool",         "minecraft:white_wool",   LIVESTOCK, 18,0.20,0.34,"BG",   40000,  6.0, 0.91);
        add("FTHR", "Feather",      "minecraft:feather",      LIVESTOCK, 12,0.25,0.30,"TITN", 50000,  4.0, 0.90);
        // ── MANUFACTURED ────────────────────────────────────────────────────
        add("GLASS","Glass",        "minecraft:glass",       MANUFACTURED,15,0.14,0.38,"OC",  30000, 5.0, 0.91);
        add("BRICK","Brick",        "minecraft:brick",       MANUFACTURED,10,0.16,0.36,"VMC", 35000, 3.5, 0.91);
        add("PAPER","Paper",        "minecraft:paper",       MANUFACTURED, 8,0.18,0.34,"IP",  40000, 2.5, 0.90);
    }

    private static final MarketSector AGRARIAN     = MarketSector.AGRARIAN;
    private static final MarketSector MINING       = MarketSector.MINING;
    private static final MarketSector LUMBER       = MarketSector.LUMBER;
    private static final MarketSector ARCANE       = MarketSector.ARCANE;
    private static final MarketSector LIVESTOCK    = MarketSector.LIVESTOCK;
    private static final MarketSector MANUFACTURED = MarketSector.MANUFACTURED;

    private static void add(String tk, String nm, String id, MarketSector sec,
                            double base, double vol, double mr, String real,
                            int shares, double floor, double factor) {
        var def = new StockDefinition(tk, nm, new Identifier(id), sec, base, vol, mr, shares, floor, factor);
        ALL.add(def); BY_TICKER.put(tk, def); REAL_WORLD.put(tk, real);
    }

    public static List<StockDefinition>      getAll()                    { return Collections.unmodifiableList(ALL); }
    public static StockDefinition            get(String ticker)          { return BY_TICKER.get(ticker); }
    public static String                     getRealWorldTicker(String t){ return REAL_WORLD.getOrDefault(t, ""); }
    public static Map<String,String>         getRealWorldTickers()       { return Collections.unmodifiableMap(REAL_WORLD); }
}
