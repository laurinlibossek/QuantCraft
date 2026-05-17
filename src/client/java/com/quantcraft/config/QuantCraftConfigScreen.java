package com.quantcraft.config;

import me.shedaniel.clothconfig2.api.*;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class QuantCraftConfigScreen {
    public static Screen create(Screen parent) {
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("QuantCraft Settings"))
                .setSavingRunnable(QuantCraftConfig::save);

        ConfigEntryBuilder e = builder.entryBuilder();

        var general = builder.getOrCreateCategory(Text.literal("General"));

        general.addEntry(e.startIntField(Text.literal("Starting Balance"),
                QuantCraftConfig.getStartingBalance()).setDefaultValue(0).setMin(0).setMax(1000000)
                .setTooltip(Text.literal("Starting balance for new players"))
                .setSaveConsumer(QuantCraftConfig::setStartingBalance).build());

        general.addEntry(e.startIntField(Text.literal("Market Tick Interval (ticks)"),
                QuantCraftConfig.getMarketTickInterval()).setDefaultValue(1200).setMin(20).setMax(72000)
                .setTooltip(Text.literal("How often prices update. 1200 = 1 real minute"))
                .setSaveConsumer(QuantCraftConfig::setMarketTickInterval).build());

        var market = builder.getOrCreateCategory(Text.literal("Market Behaviour"));

        market.addEntry(e.startFloatField(Text.literal("Global Volatility Multiplier"),
                QuantCraftConfig.getGlobalVolatilityMultiplier()).setDefaultValue(1.0f).setMin(0.1f).setMax(5.0f)
                .setTooltip(Text.literal("2.0 = twice as wild. Applied to all stocks."))
                .setSaveConsumer(QuantCraftConfig::setGlobalVolatilityMultiplier).build());

        market.addEntry(e.startBooleanToggle(Text.literal("Enable Event Pressure"),
                QuantCraftConfig.isEventPressureEnabled()).setDefaultValue(true)
                .setTooltip(Text.literal("World events affect prices"))
                .setSaveConsumer(QuantCraftConfig::setEventPressureEnabled).build());

        market.addEntry(e.startBooleanToggle(Text.literal("Enable Market News"),
                QuantCraftConfig.isMarketNewsEnabled()).setDefaultValue(true)
                .setSaveConsumer(QuantCraftConfig::setMarketNewsEnabled).build());

        market.addEntry(e.startDoubleField(Text.literal("Transaction Tax Rate"),
                QuantCraftConfig.getTaxRate()).setDefaultValue(0.02).setMin(0.0).setMax(0.25)
                .setTooltip(Text.literal("Tax on stock trades and dividends. 0.02 = 2%. Removed from economy."))
                .setSaveConsumer(QuantCraftConfig::setTaxRate).build());

        var gameplay = builder.getOrCreateCategory(Text.literal("Gameplay"));

        gameplay.addEntry(e.startBooleanToggle(Text.literal("Enable Cocaine Crafting"),
                QuantCraftConfig.isCocaineCraftingEnabled()).setDefaultValue(true)
                .setTooltip(Text.literal("Allow cocaine to be crafted. Requires server restart."))
                .setSaveConsumer(QuantCraftConfig::setCocaineCraftingEnabled).build());

        return builder.build();
    }
}
