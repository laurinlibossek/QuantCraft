package com.quantcraft;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.quantcraft.hud.TickerHudOverlay;
import com.quantcraft.hud.TickerPins;
import com.quantcraft.registry.ModScreenHandlerTypes;
import com.quantcraft.screen.CommodityExchangeScreen;
import com.quantcraft.screen.StockExchangeScreen;
import com.quantcraft.network.ModPacketsClient;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.text.Text;

public class QuantCraftModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlerTypes.STOCK_EXCHANGE,     StockExchangeScreen::new);
        HandledScreens.register(ModScreenHandlerTypes.COMMODITY_EXCHANGE, CommodityExchangeScreen::new);
        ModPacketsClient.registerClientReceivers();
        TickerHudOverlay.register();

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("qc")
                .then(ClientCommandManager.literal("pin")
                    .then(ClientCommandManager.argument("ticker", StringArgumentType.word())
                        .executes(ctx -> {
                            String ticker = StringArgumentType.getString(ctx, "ticker").toUpperCase();
                            boolean pinned = TickerPins.pin(ticker);
                            ctx.getSource().sendFeedback(Text.literal(pinned
                                    ? "§aPinned §f" + ticker + "§a to HUD."
                                    : "§eAlready pinned or cap reached (max " + TickerPins.MAX_PINS + ")."));
                            return pinned ? 1 : 0;
                        })
                    )
                )
                .then(ClientCommandManager.literal("unpin")
                    .then(ClientCommandManager.argument("ticker", StringArgumentType.word())
                        .executes(ctx -> {
                            String ticker = StringArgumentType.getString(ctx, "ticker").toUpperCase();
                            boolean removed = TickerPins.unpin(ticker);
                            ctx.getSource().sendFeedback(Text.literal(removed
                                    ? "§7Unpinned " + ticker + "."
                                    : "§e" + ticker + " was not pinned."));
                            return removed ? 1 : 0;
                        })
                    )
                )
            );
        });
    }
}
