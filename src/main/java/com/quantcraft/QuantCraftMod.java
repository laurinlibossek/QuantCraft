package com.quantcraft;

import com.quantcraft.command.AdminCommands;
import com.quantcraft.command.PlayerCommands;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.events.MarketEventListener;
import com.quantcraft.market.MarketEngine;
import com.quantcraft.market.StockRegistry;
import com.quantcraft.network.ModPackets;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.registry.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuantCraftMod implements ModInitializer {
    public static final String MOD_ID = "quantcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static int             tickCounter    = 0;
    private static MinecraftServer serverInstance;

    @Override
    public void onInitialize() {
        QuantCraftConfig.load();
        StockRegistry.initialize();
        ModItems.register();
        ModBlocks.register();
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
            tickCounter++;
            if (tickCounter >= QuantCraftConfig.getMarketTickInterval()) {
                tickCounter = 0;
                var overworld = server.getOverworld();
                var state     = MarketPersistentState.getOrCreate(overworld);
                MarketEngine.getInstance().tick(server, state);
                state.markDirty();
            }
        });

        MarketEventListener.register();
        LOGGER.info("[QuantCraft] Initialized.");
    }

    public static MinecraftServer getServer()      { return serverInstance; }
    public static int             getTickCounter() { return tickCounter; }
}
