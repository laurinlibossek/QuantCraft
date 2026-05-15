package com.quantcraft.market;

import net.minecraft.util.Identifier;
import java.util.*;

public class StockRegistry {
    private static final List<StockDefinition>      ALL             = new ArrayList<>();
    private static final Map<String,StockDefinition> BY_TICKER      = new LinkedHashMap<>();
    private static final Map<String,Double>          DIVIDEND_RATES = new LinkedHashMap<>();

    public static void initialize() {
        // ── AGRARIAN ────────────────────────────────────────────────────────
        // Wheat/Carrot/Potato: trivially farmable, pennies per item
        // Apple: less farmable (tree RNG or trading), slight premium
        // Melon: farmable but slower than crops, mid-tier
        add("WHEAT","Wheat",        "minecraft:wheat",         AGRARIAN,   4,0.15,0.35, 80000, 1.0, 0.90);
        add("CRRT", "Carrot",       "minecraft:carrot",        AGRARIAN,   5,0.18,0.32, 60000, 1.5, 0.90);
        add("POTAT","Potato",       "minecraft:potato",        AGRARIAN,   5,0.16,0.34, 60000, 1.5, 0.90);
        add("APPLE","Apple",        "minecraft:apple",         AGRARIAN,   8,0.20,0.30, 50000, 2.5, 0.92);
        add("MELON","Melon Slice",  "minecraft:melon_slice",   AGRARIAN,   3,0.22,0.28, 55000, 0.8, 0.90);
        // ── MINING ──────────────────────────────────────────────────────────
        // Coal: common, drops from wither skeletons and mining
        // Iron: smelting required, moderate effort; farmable via iron golems
        // Gold: farmable (piglin farms) but slower than iron
        // Diamond: genuinely rare, no farm, only mining or loot
        // Emerald: obtainable from villager trades easily, NOT rare
        // Lapis: found with diamond but in larger veins
        // Redstone: found deeper, large veins, moderate
        // Quartz: Nether-only but abundant there
        add("COAL", "Coal",         "minecraft:coal",          MINING,    10,0.28,0.30, 40000,  3.0, 0.92);
        add("IRON", "Iron Ingot",   "minecraft:iron_ingot",    MINING,    25,0.32,0.28, 30000,  8.0, 0.92);
        add("GOLD", "Gold Ingot",   "minecraft:gold_ingot",    MINING,    45,0.38,0.25, 20000, 15.0, 0.93);
        add("DIAM", "Diamond",      "minecraft:diamond",       MINING,   280,0.55,0.20,  5000,120.0, 0.93);
        add("EMER", "Emerald",      "minecraft:emerald",       MINING,    55,0.40,0.30, 15000, 18.0, 0.92);
        add("LAPIS","Lapis Lazuli", "minecraft:lapis_lazuli",  MINING,    12,0.30,0.30, 25000,  4.0, 0.91);
        add("RDST", "Redstone",     "minecraft:redstone",      MINING,    14,0.35,0.28, 25000,  5.0, 0.91);
        add("QRTZ", "Quartz",       "minecraft:quartz",        MINING,    15,0.28,0.32, 30000,  5.0, 0.91);
        // ── LUMBER ──────────────────────────────────────────────────────────
        // Logs: trivially farmable with tree farms, very cheap
        add("OAKW", "Oak Log",      "minecraft:oak_log",       LUMBER,     3,0.14,0.40,100000, 0.8, 0.90);
        add("BIRC", "Birch Log",    "minecraft:birch_log",     LUMBER,     3,0.15,0.38,100000, 0.8, 0.90);
        add("SPRCE","Spruce Log",   "minecraft:spruce_log",    LUMBER,     3,0.14,0.40,100000, 0.8, 0.90);
        // ── ARCANE ──────────────────────────────────────────────────────────
        // Ender Pearl: enderman farms make these farmable but risky early-game
        // Blaze Rod: Nether fortress required, moderately dangerous
        // Glowstone: Nether, breaks into dust, witches also drop
        // Ghast Tear: rare drop from dangerous mob, hard to farm
        // Bone: skeleton farms trivial, should be cheap
        add("EPRL", "Ender Pearl",  "minecraft:ender_pearl",   ARCANE,    40,0.80,0.15,  3000, 15.0, 0.93);
        add("BLAZ", "Blaze Rod",    "minecraft:blaze_rod",     ARCANE,    55,0.85,0.14,  3000, 20.0, 0.93);
        add("GLOW", "Glowstone",    "minecraft:glowstone_dust",ARCANE,    18,0.72,0.18,  5000,  6.0, 0.92);
        add("GHST", "Ghast Tear",   "minecraft:ghast_tear",    ARCANE,   150,0.90,0.12,  2000, 65.0, 0.94);
        add("SKEL", "Bone",         "minecraft:bone",          ARCANE,     6,0.50,0.22,  8000,  2.0, 0.91);
        // ── LIVESTOCK ───────────────────────────────────────────────────────
        // Leather: cow farms easy, but slower than crops
        // Wool: trivial with shears on sheep
        // Feather: chicken farms trivial
        add("LEAT", "Leather",      "minecraft:leather",       LIVESTOCK, 12,0.22,0.32, 35000,  4.0, 0.91);
        add("WOOL", "Wool",         "minecraft:white_wool",    LIVESTOCK,  8,0.20,0.34, 40000,  2.5, 0.91);
        add("FTHR", "Feather",      "minecraft:feather",       LIVESTOCK,  5,0.25,0.30, 50000,  1.5, 0.90);
        // ── MANUFACTURED ────────────────────────────────────────────────────
        // Glass: sand + fuel, easy but requires smelting
        // Brick: clay is limited, smelting required — actually somewhat scarce
        // Paper: sugar cane farm trivial
        add("GLASS","Glass",        "minecraft:glass",       MANUFACTURED, 8,0.14,0.38, 30000, 2.5, 0.91);
        add("BRICK","Brick",        "minecraft:brick",       MANUFACTURED,18,0.16,0.36, 35000, 6.0, 0.91);
        add("PAPER","Paper",        "minecraft:paper",       MANUFACTURED, 4,0.18,0.34, 40000, 1.0, 0.90);

        // ── Dividend rates (paid per payout cycle) ───────────────────────────
        // Cheap & farmable sectors get higher dividend rates (income stocks)
        // Expensive & scarce sectors get lower rates (growth stocks)
        // AGRARIAN — easy to farm, reward holding
        for (String tk : new String[]{"WHEAT","CRRT","POTAT","APPLE","MELON"}) DIVIDEND_RATES.put(tk, 0.020);
        // LUMBER — trivial farms
        for (String tk : new String[]{"OAKW","BIRC","SPRCE"})                   DIVIDEND_RATES.put(tk, 0.022);
        // LIVESTOCK — moderate farms
        for (String tk : new String[]{"LEAT","WOOL","FTHR"})                    DIVIDEND_RATES.put(tk, 0.018);
        // MANUFACTURED — mixed effort
        for (String tk : new String[]{"GLASS","BRICK","PAPER"})                 DIVIDEND_RATES.put(tk, 0.016);
        // MINING — common ores + emerald (villager-farmable)
        for (String tk : new String[]{"COAL","IRON","GOLD","EMER","LAPIS","RDST","QRTZ"}) DIVIDEND_RATES.put(tk, 0.010);
        // MINING — diamond (genuinely rare, growth stock)
        DIVIDEND_RATES.put("DIAM", 0.005);
        // ARCANE — dangerous to obtain, low dividend (growth stocks)
        for (String tk : new String[]{"EPRL","BLAZ","GLOW","GHST","SKEL"})      DIVIDEND_RATES.put(tk, 0.004);
    }

    private static final MarketSector AGRARIAN     = MarketSector.AGRARIAN;
    private static final MarketSector MINING       = MarketSector.MINING;
    private static final MarketSector LUMBER       = MarketSector.LUMBER;
    private static final MarketSector ARCANE       = MarketSector.ARCANE;
    private static final MarketSector LIVESTOCK    = MarketSector.LIVESTOCK;
    private static final MarketSector MANUFACTURED = MarketSector.MANUFACTURED;

    private static void add(String tk, String nm, String id, MarketSector sec,
                            double base, double vol, double mr,
                            int shares, double floor, double factor) {
        var def = new StockDefinition(tk, nm, new Identifier(id), sec, base, vol, mr, shares, floor, factor);
        ALL.add(def); BY_TICKER.put(tk, def);
    }

    public static List<StockDefinition>      getAll()                    { return Collections.unmodifiableList(ALL); }
    public static StockDefinition            get(String ticker)          { return BY_TICKER.get(ticker); }
    public static double                     getDividendRate(String t)   { return DIVIDEND_RATES.getOrDefault(t, 0.0); }
}
