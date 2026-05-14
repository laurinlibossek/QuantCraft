package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.*;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModItemGroups {
    public static final ItemGroup QUANTCRAFT_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(ModItems.TRADING_POST))
            .displayName(Text.translatable("itemGroup.quantcraft"))
            .entries((context, entries) -> {
                entries.add(ModItems.DOLLAR_BILL);
                entries.add(ModItems.TRADING_POST);
                entries.add(ModItems.COMMODITY_EXCHANGE);
                entries.add(ModItems.QUOTRON);
                entries.add(ModItems.NP_DIAMOND_DISCOVERY);
                entries.add(ModItems.NP_DRAGON_SLAIN);
                entries.add(ModItems.NP_TRADE_WAR);
                entries.add(ModItems.NP_MINING_BOOM);
                entries.add(ModItems.NP_LUMBER_SHORTAGE);
                entries.add(ModItems.NP_GOLD_RUSH);
                entries.add(ModItems.NP_HARVEST_FESTIVAL);
                entries.add(ModItems.NP_ARCANE_ANOMALY);
                entries.add(ModItems.NP_LIVESTOCK_PLAGUE);
                entries.add(ModItems.NP_EMERALD_CARTEL);
            })
            .build();

    public static void register() {
        Registry.register(Registries.ITEM_GROUP,
                new Identifier(QuantCraftMod.MOD_ID, "quantcraft"),
                QUANTCRAFT_GROUP);
    }
}
