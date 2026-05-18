package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.block.*;
import net.minecraft.block.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

public class ModBlocks {
    public static final Block STOCK_EXCHANGE     = new StockExchangeBlock(AbstractBlock.Settings.create().strength(2.5f).requiresTool());
    public static final Block COMMODITY_EXCHANGE = new CommodityExchangeBlock(AbstractBlock.Settings.create().strength(2.5f).requiresTool());
    // TODO #1 (issue 4): nonOpaque() present in PDF — kept; semicolon was missing in PDF, fixed here
    public static final Block QUOTRON            = new QuotronBlock(AbstractBlock.Settings.create().strength(1.5f).requiresTool().nonOpaque());

    public static void register() {
        Registry.register(Registries.BLOCK, id("stock_exchange"),     STOCK_EXCHANGE);
        Registry.register(Registries.BLOCK, id("commodity_exchange"), COMMODITY_EXCHANGE);
        Registry.register(Registries.BLOCK, id("quotron"),            QUOTRON);
    }

    private static Identifier id(String p) { return new Identifier(QuantCraftMod.MOD_ID, p); }
}
