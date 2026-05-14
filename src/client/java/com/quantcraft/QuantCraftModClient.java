package com.quantcraft;


import com.quantcraft.registry.ModScreenHandlerTypes;
import com.quantcraft.screen.CommodityExchangeScreen;
import com.quantcraft.screen.TradingPostScreen;
import com.quantcraft.network.ModPacketsClient;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screen.ingame.HandledScreens;

public class QuantCraftModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HandledScreens.register(ModScreenHandlerTypes.TRADING_POST,       TradingPostScreen::new);
        HandledScreens.register(ModScreenHandlerTypes.COMMODITY_EXCHANGE, CommodityExchangeScreen::new);
        ModPacketsClient.registerClientReceivers();
    }
}
