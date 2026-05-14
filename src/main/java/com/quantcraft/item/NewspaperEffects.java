package com.quantcraft.item;

import com.quantcraft.market.*;
import net.minecraft.server.MinecraftServer;

public class NewspaperEffects {
    public static void apply(NewspaperItem.NewspaperType type, MinecraftServer server) {
        MarketEngine e = MarketEngine.getInstance();
        switch (type) {
            case DIAMOND_DISCOVERY -> { e.applyTickerPressure("DIAM", -35); e.applySectorPressure(MarketSector.MINING, -10); e.pushNewsPublic("BREAKING: Diamond find floods MINING sector"); }
            case DRAGON_SLAIN      -> { e.applySectorPressure(MarketSector.ARCANE, -40); e.pushNewsPublic("BREAKING: Dragon slain — Arcane sector in freefall!"); }
            case TRADE_WAR         -> { e.applySectorPressure(MarketSector.MANUFACTURED, -15); e.applySectorPressure(MarketSector.AGRARIAN, +10); e.pushNewsPublic("Trade war erupts — Manufactured down, Agrarian up"); }
            case MINING_BOOM       -> { e.applyTickerPressure("IRON", -25); e.applyTickerPressure("COAL", -10); e.pushNewsPublic("New vein — Iron & Coal oversupplied"); }
            case LUMBER_SHORTAGE   -> { e.applySectorPressure(MarketSector.LUMBER, +30); e.pushNewsPublic("Forest blight — Lumber sector soars"); }
            case GOLD_RUSH         -> { e.applyTickerPressure("GOLD", -20); e.pushNewsPublic("Gold rush! GOLD tumbles on supply fears"); }
            case HARVEST_FESTIVAL  -> { e.applySectorPressure(MarketSector.AGRARIAN, -20); e.pushNewsPublic("Record harvest — Agrarian oversupplied"); }
            case ARCANE_ANOMALY    -> { e.applySectorPressure(MarketSector.ARCANE, +25); e.pushNewsPublic("Arcane anomaly spikes magical goods demand"); }
            case LIVESTOCK_PLAGUE  -> { e.applySectorPressure(MarketSector.LIVESTOCK, -30); e.pushNewsPublic("Livestock plague — LIVESTOCK sector collapses"); }
            case EMERALD_CARTEL    -> { e.applyTickerPressure("EMER", +40); e.applyTickerSustainedBoom("EMER", 10); e.pushNewsPublic("Villager cartel corners emerald supply — EMER rockets"); }
        }
    }
}
