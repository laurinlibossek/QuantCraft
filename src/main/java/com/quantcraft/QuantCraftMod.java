package com.quantcraft;

import com.quantcraft.command.AdminCommands;
import com.quantcraft.command.PlayerCommands;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.item.CocaineCrashTracker;
import com.quantcraft.events.MarketEventListener;
import com.quantcraft.market.MarketEngine;
import com.quantcraft.market.StockRegistry;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.network.ModPackets;
import com.quantcraft.registry.*;
import com.quantcraft.registry.ModEffects;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.stream.Collectors;

public class QuantCraftMod implements ModInitializer {
    public static final String MOD_ID = "quantcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static int             tickCounter      = 0;
    private static MinecraftServer serverInstance;
    private static boolean         marketWasOpen    = true;
    private static boolean         hasAnnouncedOpen = false;

    @Override
    public void onInitialize() {
        QuantCraftConfig.load();
        StockRegistry.initialize();
        ModEffects.register();
        com.quantcraft.recipe.CocaineRecipeCondition.register();
        ModItems.register();
        ModBlocks.register();
        ModItemGroups.register();
        ModBlockEntityTypes.register();
        ModScreenHandlerTypes.register();
        ModStructures.register();
        ModPackets.registerServerReceivers();
        AdminCommands.register();
        PlayerCommands.register();

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world == server.getOverworld()) {
                serverInstance = server;
                MarketPersistentState state = MarketPersistentState.getOrCreate(world);
                MarketEngine.getInstance().loadState(state);
                LOGGER.info("[QuantCraft] Market loaded. {} stocks.", StockRegistry.getAll().size());
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Update marketOpen every tick so trades are gated at the same threshold as the broadcast
            long    tod   = server.getOverworld().getTimeOfDay() % 24000L;
            boolean isNow = tod < 13000L;
            MarketEngine.getInstance().setMarketOpen(isNow);

            if (isNow && !marketWasOpen && !hasAnnouncedOpen) {
                broadcastMarketOpen(server);
                hasAnnouncedOpen = true;
            } else if (!isNow && marketWasOpen) {
                var ps = MarketPersistentState.getOrCreate(server.getOverworld());
                MarketEngine.getInstance().snapshotClosingPrices(ps);
                broadcastMarketClose(server);
                hasAnnouncedOpen = false;
            }
            marketWasOpen = isNow;

            tickCounter++;
            if (tickCounter >= QuantCraftConfig.getMarketTickInterval()) {
                tickCounter = 0;
                var overworld = server.getOverworld();
                var state     = MarketPersistentState.getOrCreate(overworld);
                MarketEngine.getInstance().tick(server, state);
                state.markDirty();
            }

            for (var player : server.getPlayerManager().getPlayerList()) {
                CocaineCrashTracker.tick(player);
            }
        });

        MarketEventListener.register();
        LOGGER.info("[QuantCraft] Initialized.");
    }

    public static MinecraftServer getServer()      { return serverInstance; }
    public static int             getTickCounter() { return tickCounter; }

    private static void broadcastMarketOpen(net.minecraft.server.MinecraftServer server) {
        var engine  = MarketEngine.getInstance();
        var closing = engine.getClosingPrices();
        var snap    = engine.getSnapshot();

        server.getPlayerManager().broadcast(Text.literal("§6§l[ MARKET OPEN ]"), false);

        if (closing.isEmpty()) {
            server.getPlayerManager().broadcast(Text.literal("§7Type /qc prices for full list."), false);
            return;
        }

        List<String[]> movers = new ArrayList<>();
        for (var e : snap.entrySet()) {
            double prev = closing.getOrDefault(e.getKey(), 0.0);
            if (prev <= 0) continue;
            double pct = (e.getValue().getCurrentPrice() - prev) / prev * 100.0;
            movers.add(new String[]{e.getKey(), String.format("%+.1f%%", pct), pct >= 0 ? "g" : "r",
                    String.valueOf(pct)});
        }
        movers.sort((a, b) -> Double.compare(Double.parseDouble(b[3]), Double.parseDouble(a[3])));

        server.getPlayerManager().broadcast(
                Text.literal("§eOpening prices — top movers from yesterday's close:"), false);

        List<String[]> gainers = movers.stream().filter(m -> Double.parseDouble(m[3]) > 0)
                .limit(3).collect(Collectors.toList());
        List<String[]> losers  = new ArrayList<>(movers.stream()
                .filter(m -> Double.parseDouble(m[3]) < 0)
                .collect(Collectors.toList()));
        Collections.reverse(losers);
        losers = losers.stream().limit(3).collect(Collectors.toList());

        if (!gainers.isEmpty()) {
            StringBuilder sb = new StringBuilder("§a▲ ");
            gainers.forEach(g -> sb.append(g[0]).append(" ").append(g[1]).append("   "));
            server.getPlayerManager().broadcast(Text.literal(sb.toString().stripTrailing()), false);
        }
        if (!losers.isEmpty()) {
            StringBuilder sb = new StringBuilder("§c▼ ");
            losers.forEach(g -> sb.append(g[0]).append(" ").append(g[1]).append("   "));
            server.getPlayerManager().broadcast(Text.literal(sb.toString().stripTrailing()), false);
        }
        server.getPlayerManager().broadcast(Text.literal("§7Type /qc prices for full list."), false);
    }

    private static void broadcastMarketClose(MinecraftServer server) {
        var snap = MarketEngine.getInstance().getSnapshot();
        server.getPlayerManager().broadcast(Text.literal("§c§l[ MARKET CLOSED ]"), false);
        server.getPlayerManager().broadcast(Text.literal("§eToday's closing prices:"), false);

        var tickers = new ArrayList<>(snap.entrySet());
        for (int i = 0; i < tickers.size(); i += 4) {
            StringBuilder sb = new StringBuilder("§7");
            for (int j = i; j < Math.min(i + 4, tickers.size()); j++) {
                var e = tickers.get(j);
                sb.append(String.format("%-5s §f%.1f¢§7  ", e.getKey(), e.getValue().getCurrentPrice()));
            }
            server.getPlayerManager().broadcast(Text.literal(sb.toString().stripTrailing()), false);
        }
        server.getPlayerManager().broadcast(Text.literal("§7Market reopens at dawn."), false);
    }
}
