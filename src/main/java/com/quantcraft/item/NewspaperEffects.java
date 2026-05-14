package com.quantcraft.item;

import com.quantcraft.market.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public class NewspaperEffects {
    public static void apply(NewspaperItem.NewspaperType type, MinecraftServer server, ServerPlayerEntity player) {
        MarketEngine e = MarketEngine.getInstance();
        int before = e.getActiveEvents().size();

        switch (type) {
            case DIAMOND_DISCOVERY -> e.addActiveEvent(new ActiveMarketEvent(
                    "Diamond Discovery — DIAM supply flooded",
                    "DIAM", null, -3.5, 15, server.getTicks()), player);
            case TRADE_WAR -> {
                e.addActiveEvent(new ActiveMarketEvent(
                        "Trade War — Manufacturing down, Agrarian surging",
                        null, MarketSector.MANUFACTURED, -2.0, 20, server.getTicks()), player);
                e.addActiveEvent(new ActiveMarketEvent(
                        "Trade War — Agrarian surging on import ban",
                        null, MarketSector.AGRARIAN, +1.5, 20, server.getTicks()), player);
            }
            case MINING_BOOM       -> {
                e.addActiveEvent(new ActiveMarketEvent(
                        "Mining Boom — IRON oversupplied",
                        "IRON", null, -3.0, 15, server.getTicks()), player);
                e.addActiveEvent(new ActiveMarketEvent(
                        "Mining Boom — COAL oversupplied",
                        "COAL", null, -1.5, 15, server.getTicks()), player);
            }
            case LUMBER_SHORTAGE   -> e.addActiveEvent(new ActiveMarketEvent(
                    "Lumber Shortage — LMBR sector surging",
                    null, MarketSector.LUMBER, +3.5, 20, server.getTicks()), player);
            case GOLD_RUSH         -> e.addActiveEvent(new ActiveMarketEvent(
                    "Gold Rush — GOLD supply flooded",
                    "GOLD", null, -3.0, 15, server.getTicks()), player);
            case HARVEST_FESTIVAL  -> e.addActiveEvent(new ActiveMarketEvent(
                    "Harvest Festival — Agrarian oversupplied",
                    null, MarketSector.AGRARIAN, -2.5, 15, server.getTicks()), player);
            case ARCANE_ANOMALY    -> e.addActiveEvent(new ActiveMarketEvent(
                    "Arcane Anomaly — magical demand spiking",
                    null, MarketSector.ARCANE, +3.0, 15, server.getTicks()), player);
            case LIVESTOCK_PLAGUE  -> e.addActiveEvent(new ActiveMarketEvent(
                    "Livestock Plague — LIVE sector collapsing",
                    null, MarketSector.LIVESTOCK, -4.0, 20, server.getTicks()), player);
            case EMERALD_CARTEL    -> e.addActiveEvent(new ActiveMarketEvent(
                    "Emerald Cartel — EMER cornered",
                    "EMER", null, +4.0, 20, server.getTicks()), player);
        }

        java.util.List<ActiveMarketEvent> allEvents = e.getActiveEvents();
        java.util.Set<String> broadcast = new java.util.LinkedHashSet<>();
        for (int i = before; i < allEvents.size(); i++) broadcast.add(allEvents.get(i).getHeadline());
        for (String headline : broadcast) {
            player.sendMessage(Text.literal("§6[Breaking News] §e" + headline), true);
            server.getPlayerManager().getPlayerList().forEach(p -> {
                if (!p.getUuid().equals(player.getUuid()))
                    p.sendMessage(Text.literal("§6[Breaking News] §e" + headline), false);
            });
        }
    }
}
