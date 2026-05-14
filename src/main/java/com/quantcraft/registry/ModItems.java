package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.item.*;
import net.minecraft.item.*;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

public class ModItems {
    public static final Item DOLLAR_BILL       = new DollarBillItem(new Item.Settings().maxCount(64));
    public static final Item TRADING_POST      = new BlockItem(ModBlocks.TRADING_POST,       new Item.Settings());
    public static final Item COMMODITY_EXCHANGE= new BlockItem(ModBlocks.COMMODITY_EXCHANGE, new Item.Settings());
    public static final Item QUOTRON           = new BlockItem(ModBlocks.QUOTRON,            new Item.Settings());

    public static final Item NP_DIAMOND_DISCOVERY = new NewspaperItem(NewspaperItem.NewspaperType.DIAMOND_DISCOVERY, new Item.Settings());
    public static final Item NP_DRAGON_SLAIN      = new NewspaperItem(NewspaperItem.NewspaperType.DRAGON_SLAIN,      new Item.Settings());
    public static final Item NP_TRADE_WAR         = new NewspaperItem(NewspaperItem.NewspaperType.TRADE_WAR,         new Item.Settings());
    public static final Item NP_MINING_BOOM       = new NewspaperItem(NewspaperItem.NewspaperType.MINING_BOOM,       new Item.Settings());
    public static final Item NP_LUMBER_SHORTAGE   = new NewspaperItem(NewspaperItem.NewspaperType.LUMBER_SHORTAGE,   new Item.Settings());
    public static final Item NP_GOLD_RUSH         = new NewspaperItem(NewspaperItem.NewspaperType.GOLD_RUSH,         new Item.Settings());
    public static final Item NP_HARVEST_FESTIVAL  = new NewspaperItem(NewspaperItem.NewspaperType.HARVEST_FESTIVAL,  new Item.Settings());
    public static final Item NP_ARCANE_ANOMALY    = new NewspaperItem(NewspaperItem.NewspaperType.ARCANE_ANOMALY,    new Item.Settings());
    public static final Item NP_LIVESTOCK_PLAGUE  = new NewspaperItem(NewspaperItem.NewspaperType.LIVESTOCK_PLAGUE,  new Item.Settings());
    public static final Item NP_EMERALD_CARTEL    = new NewspaperItem(NewspaperItem.NewspaperType.EMERALD_CARTEL,    new Item.Settings());

    public static void register() {
        reg("dollar_bill",                  DOLLAR_BILL);
        reg("trading_post",                 TRADING_POST);
        reg("commodity_exchange",           COMMODITY_EXCHANGE);
        reg("quotron",                      QUOTRON);
        reg("newspaper_diamond_discovery",  NP_DIAMOND_DISCOVERY);
        reg("newspaper_dragon_slain",       NP_DRAGON_SLAIN);
        reg("newspaper_trade_war",          NP_TRADE_WAR);
        reg("newspaper_mining_boom",        NP_MINING_BOOM);
        reg("newspaper_lumber_shortage",    NP_LUMBER_SHORTAGE);
        reg("newspaper_gold_rush",          NP_GOLD_RUSH);
        reg("newspaper_harvest_festival",   NP_HARVEST_FESTIVAL);
        reg("newspaper_arcane_anomaly",     NP_ARCANE_ANOMALY);
        reg("newspaper_livestock_plague",   NP_LIVESTOCK_PLAGUE);
        reg("newspaper_emerald_cartel",     NP_EMERALD_CARTEL);
    }

    private static void reg(String p, Item i) {
        Registry.register(Registries.ITEM, new Identifier(QuantCraftMod.MOD_ID, p), i);
    }
}
