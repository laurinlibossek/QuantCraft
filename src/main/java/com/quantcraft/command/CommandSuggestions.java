package com.quantcraft.command;

import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.quantcraft.market.MarketSector;
import com.quantcraft.market.StockRegistry;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;
import java.util.stream.Stream;

public class CommandSuggestions {

    public static final SuggestionProvider<ServerCommandSource> TICKER = (ctx, builder) -> {
        return CommandSource.suggestMatching(
            StockRegistry.getAll().stream().map(def -> def.ticker()),
            builder
        );
    };

    public static final SuggestionProvider<ServerCommandSource> SECTOR = (ctx, builder) -> {
        return CommandSource.suggestMatching(
            java.util.Arrays.stream(MarketSector.values()).map(Enum::name),
            builder
        );
    };

    public static final SuggestionProvider<ServerCommandSource> TICKER_OR_SECTOR = (ctx, builder) -> {
        var tickers = StockRegistry.getAll().stream().map(def -> def.ticker());
        var sectors = java.util.Arrays.stream(MarketSector.values()).map(Enum::name);
        return CommandSource.suggestMatching(
            Stream.concat(tickers, sectors),
            builder
        );
    };

    public static final SuggestionProvider<ServerCommandSource> EVENT_TYPE = (ctx, builder) -> {
        return CommandSource.suggestMatching(
            java.util.List.of("harvest", "mining_boom", "mob_surge", "boss_kill",
                             "nether_spike", "thunder", "full_moon", "blood_moon"),
            builder
        );
    };
}
