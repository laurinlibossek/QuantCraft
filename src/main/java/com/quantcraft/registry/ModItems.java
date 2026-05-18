package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.item.CocaineItem;
import com.quantcraft.item.DollarBillItem;
import com.quantcraft.item.NewspaperItem;
import com.quantcraft.item.SealedNewspaperItem;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import java.util.List;

public class ModItems {
    public static final Item DOLLAR_BILL       = new DollarBillItem(new Item.Settings().maxCount(64));
    public static final Item STOCK_EXCHANGE    = new BlockItem(ModBlocks.STOCK_EXCHANGE, new Item.Settings()) {
        @Override public void appendTooltip(ItemStack s, World w, List<Text> t, TooltipContext c) {
            t.add(Text.literal("The QuantCraft stock exchange. Buy and sell shares,").formatted(Formatting.GRAY));
            t.add(Text.literal("check the news, and track your portfolio.").formatted(Formatting.GRAY));
            t.add(Text.literal("Hold dollar bills and right-click to deposit.").formatted(Formatting.DARK_GRAY));
        }
    };
    public static final Item COMMODITY_EXCHANGE= new BlockItem(ModBlocks.COMMODITY_EXCHANGE, new Item.Settings()) {
        @Override public void appendTooltip(ItemStack s, World w, List<Text> t, TooltipContext c) {
            t.add(Text.literal("Trade items for currency.").formatted(Formatting.GRAY));
            t.add(Text.literal("Prices follow the stock market.").formatted(Formatting.GRAY));
        }
    };
    public static final Item QUOTRON           = new BlockItem(ModBlocks.QUOTRON, new Item.Settings()) {
        @Override public void appendTooltip(ItemStack s, World w, List<Text> t, TooltipContext c) {
            t.add(Text.literal("Track up to 10 stocks and monitor live prices.").formatted(Formatting.GRAY));
        }
    };

    public static final Item SEALED_NEWSPAPER     = new SealedNewspaperItem(new Item.Settings());

    public static final Item NP_DIAMOND_DISCOVERY = new NewspaperItem(NewspaperItem.NewspaperType.DIAMOND_DISCOVERY, new Item.Settings());
    public static final Item NP_TRADE_WAR         = new NewspaperItem(NewspaperItem.NewspaperType.TRADE_WAR,         new Item.Settings());
    public static final Item NP_MINING_BOOM       = new NewspaperItem(NewspaperItem.NewspaperType.MINING_BOOM,       new Item.Settings());
    public static final Item NP_LUMBER_SHORTAGE   = new NewspaperItem(NewspaperItem.NewspaperType.LUMBER_SHORTAGE,   new Item.Settings());
    public static final Item NP_GOLD_RUSH         = new NewspaperItem(NewspaperItem.NewspaperType.GOLD_RUSH,         new Item.Settings());
    public static final Item NP_HARVEST_FESTIVAL  = new NewspaperItem(NewspaperItem.NewspaperType.HARVEST_FESTIVAL,  new Item.Settings());
    public static final Item NP_ARCANE_ANOMALY    = new NewspaperItem(NewspaperItem.NewspaperType.ARCANE_ANOMALY,    new Item.Settings());
    public static final Item NP_LIVESTOCK_PLAGUE  = new NewspaperItem(NewspaperItem.NewspaperType.LIVESTOCK_PLAGUE,  new Item.Settings());
    public static final Item NP_EMERALD_CARTEL    = new NewspaperItem(NewspaperItem.NewspaperType.EMERALD_CARTEL,    new Item.Settings());
    public static final Item COCAINE = new CocaineItem();

    public static void register() {
        reg("dollar_bill",                  DOLLAR_BILL);
        reg("stock_exchange",               STOCK_EXCHANGE);
        reg("commodity_exchange",           COMMODITY_EXCHANGE);
        reg("quotron",                      QUOTRON);
        reg("sealed_newspaper",             SEALED_NEWSPAPER);
        reg("newspaper_diamond_discovery",  NP_DIAMOND_DISCOVERY);
        reg("newspaper_trade_war",          NP_TRADE_WAR);
        reg("newspaper_mining_boom",        NP_MINING_BOOM);
        reg("newspaper_lumber_shortage",    NP_LUMBER_SHORTAGE);
        reg("newspaper_gold_rush",          NP_GOLD_RUSH);
        reg("newspaper_harvest_festival",   NP_HARVEST_FESTIVAL);
        reg("newspaper_arcane_anomaly",     NP_ARCANE_ANOMALY);
        reg("newspaper_livestock_plague",   NP_LIVESTOCK_PLAGUE);
        reg("newspaper_emerald_cartel",     NP_EMERALD_CARTEL);
        reg("cocaine", COCAINE);
    }

    private static void reg(String p, Item i) {
        Registry.register(Registries.ITEM, new Identifier(QuantCraftMod.MOD_ID, p), i);
    }
}
