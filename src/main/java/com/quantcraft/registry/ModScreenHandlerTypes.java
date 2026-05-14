package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.screen.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlerTypes {
    public static ScreenHandlerType<TradingPostScreenHandler>       TRADING_POST;
    public static ScreenHandlerType<CommodityExchangeScreenHandler> COMMODITY_EXCHANGE;

    public static void register() {
        TRADING_POST = Registry.register(Registries.SCREEN_HANDLER, id("trading_post"),
                new ExtendedScreenHandlerType<>(TradingPostScreenHandler::new));

        COMMODITY_EXCHANGE = Registry.register(Registries.SCREEN_HANDLER, id("commodity_exchange"),
                new ExtendedScreenHandlerType<>(CommodityExchangeScreenHandler::new));
    }

    private static Identifier id(String p) { return new Identifier(QuantCraftMod.MOD_ID, p); }
}
