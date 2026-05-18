package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.screen.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.*;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;

public class ModScreenHandlerTypes {
    public static ScreenHandlerType<StockExchangeScreenHandler>     STOCK_EXCHANGE;
    public static ScreenHandlerType<CommodityExchangeScreenHandler> COMMODITY_EXCHANGE;

    public static void register() {
        STOCK_EXCHANGE = Registry.register(Registries.SCREEN_HANDLER, id("stock_exchange"),
                new ExtendedScreenHandlerType<>(StockExchangeScreenHandler::new));

        COMMODITY_EXCHANGE = Registry.register(Registries.SCREEN_HANDLER, id("commodity_exchange"),
                new ExtendedScreenHandlerType<>(CommodityExchangeScreenHandler::new));
    }

    private static Identifier id(String p) { return new Identifier(QuantCraftMod.MOD_ID, p); }
}
