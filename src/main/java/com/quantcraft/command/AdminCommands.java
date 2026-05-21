package com.quantcraft.command;

import com.mojang.brigadier.arguments.*;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.market.*;
import com.quantcraft.persistence.MarketPersistentState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.Arrays;

import static net.minecraft.server.command.CommandManager.*;

public class AdminCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(literal("qcadmin").requires(src -> src.hasPermissionLevel(2))
                .then(literal("crash").then(argument("target", StringArgumentType.word())
                    .suggests(CommandSuggestions.TICKER_OR_SECTOR)
                    .executes(ctx -> pressure(ctx.getSource(), StringArgumentType.getString(ctx, "target"), -200.0, "CRASH"))))
                .then(literal("boom").then(argument("target", StringArgumentType.word())
                    .suggests(CommandSuggestions.TICKER_OR_SECTOR)
                    .executes(ctx -> pressure(ctx.getSource(), StringArgumentType.getString(ctx, "target"), +200.0, "BOOM"))))
                .then(literal("setprice").then(argument("ticker", StringArgumentType.word())
                    .suggests(CommandSuggestions.TICKER)
                    .then(argument("price", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> setPrice(ctx.getSource(),
                            StringArgumentType.getString(ctx, "ticker"),
                            DoubleArgumentType.getDouble(ctx, "price"))))))
                .then(literal("setbalance").then(argument("player", EntityArgumentType.player())
                    .then(argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> adjustBal(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            DoubleArgumentType.getDouble(ctx, "amount"), "SET")))))
                .then(literal("give").then(argument("player", EntityArgumentType.player())
                    .then(argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> adjustBal(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            DoubleArgumentType.getDouble(ctx, "amount"), "ADD")))))
                .then(literal("take").then(argument("player", EntityArgumentType.player())
                    .then(argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> adjustBal(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            DoubleArgumentType.getDouble(ctx, "amount"), "TAKE")))))
                .then(literal("reset")
                    .then(literal("market")    .executes(ctx -> reset(ctx.getSource(), true,  false)))
                    .then(literal("portfolios").executes(ctx -> reset(ctx.getSource(), false, true)))
                    .then(literal("all")       .executes(ctx -> reset(ctx.getSource(), true,  true))))
                .then(literal("freeze")  .executes(ctx -> setFrozen(ctx.getSource(), true)))
                .then(literal("unfreeze").executes(ctx -> setFrozen(ctx.getSource(), false)))
                .then(literal("multiplier").then(argument("value", FloatArgumentType.floatArg(0.1f, 10f))
                    .executes(ctx -> {
                        float v = FloatArgumentType.getFloat(ctx, "value");
                        QuantCraftConfig.setGlobalVolatilityMultiplier(v);
                        ctx.getSource().sendFeedback(() -> Text.literal("[QCAdmin] Volatility multiplier set to " + v), true);
                        return 1;
                    })))
                .then(literal("event").then(argument("event", StringArgumentType.word())
                    .suggests(CommandSuggestions.EVENT_TYPE)
                    .executes(ctx -> fireEvent(ctx.getSource(), StringArgumentType.getString(ctx, "event")))))
                .then(literal("info").then(argument("ticker", StringArgumentType.word())
                    .suggests(CommandSuggestions.TICKER)
                    .executes(ctx -> stockInfo(ctx.getSource(), StringArgumentType.getString(ctx, "ticker")))))
                .then(literal("listplayers").executes(ctx -> listPlayers(ctx.getSource())))
                .then(literal("season")
                    .executes(ctx -> seasonInfo(ctx.getSource()))
                    .then(literal("set").then(argument("phase", StringArgumentType.word())
                        .suggests((ctx, b) -> net.minecraft.command.CommandSource.suggestMatching(
                            java.util.Arrays.stream(com.quantcraft.market.MarketSeason.values())
                                .map(s -> s.name().toLowerCase()), b))
                        .executes(ctx -> seasonSet(ctx.getSource(), StringArgumentType.getString(ctx, "phase"))))))
                .then(literal("cancelorders").then(argument("player", EntityArgumentType.player())
                    .executes(ctx -> cancelOrders(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
                .then(literal("help").executes(ctx -> help(ctx.getSource())))
                .then(literal("bot").then(argument("ticker", StringArgumentType.word())
                    .suggests(CommandSuggestions.TICKER)
                    .then(literal("enable") .executes(ctx -> botEnabled(ctx.getSource(), StringArgumentType.getString(ctx, "ticker"), true)))
                    .then(literal("disable").executes(ctx -> botEnabled(ctx.getSource(), StringArgumentType.getString(ctx, "ticker"), false)))
                    .then(literal("info")   .executes(ctx -> botInfo(ctx.getSource(), StringArgumentType.getString(ctx, "ticker"))))
                    .then(literal("cash").then(argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> {
                            LiquidityBot b = MarketEngine.getInstance().getBot(StringArgumentType.getString(ctx, "ticker").toUpperCase());
                            if (b == null) { ctx.getSource().sendError(Text.literal("No bot.")); return 0; }
                            b.setCashReserve(DoubleArgumentType.getDouble(ctx, "amount"));
                            ctx.getSource().sendFeedback(() -> Text.literal("[QCAdmin] Bot cash set."), false); return 1;
                        })))
                    .then(literal("shares").then(argument("amount", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            LiquidityBot b = MarketEngine.getInstance().getBot(StringArgumentType.getString(ctx, "ticker").toUpperCase());
                            if (b == null) { ctx.getSource().sendError(Text.literal("No bot.")); return 0; }
                            b.setShareReserve(IntegerArgumentType.getInteger(ctx, "amount"));
                            ctx.getSource().sendFeedback(() -> Text.literal("[QCAdmin] Bot shares set."), false); return 1;
                        })))
                    .then(literal("spread").then(argument("value", DoubleArgumentType.doubleArg(0.001, 0.5))
                        .executes(ctx -> {
                            LiquidityBot b = MarketEngine.getInstance().getBot(StringArgumentType.getString(ctx, "ticker").toUpperCase());
                            if (b == null) { ctx.getSource().sendError(Text.literal("No bot.")); return 0; }
                            b.setSpreadPct(DoubleArgumentType.getDouble(ctx, "value"));
                            ctx.getSource().sendFeedback(() -> Text.literal("[QCAdmin] Bot spread set."), false); return 1;
                        })))
                ))
            )
        );
    }

    private static int pressure(ServerCommandSource src, String target, double delta, String label) {
        MarketEngine eng = MarketEngine.getInstance();
        try {
            MarketSector sec = MarketSector.valueOf(target.toUpperCase());
            eng.applySectorPressure(sec, delta);
            src.sendFeedback(() -> Text.literal("[QCAdmin] " + label + " sector " + target.toUpperCase()), true);
            return 1;
        } catch (IllegalArgumentException ignored) {}
        StockState s = eng.getState(target.toUpperCase());
        if (s == null) { src.sendError(Text.literal("Unknown sector/ticker: " + target)); return 0; }
        s.applyEventPressure(delta);
        src.sendFeedback(() -> Text.literal("[QCAdmin] " + label + " ticker " + target.toUpperCase()), true);
        return 1;
    }

    private static int setPrice(ServerCommandSource src, String ticker, double price) {
        StockState s = MarketEngine.getInstance().getState(ticker.toUpperCase());
        if (s == null) { src.sendError(Text.literal("Unknown: " + ticker)); return 0; }
        s.updatePrice(price);
        src.sendFeedback(() -> Text.literal("[QCAdmin] " + ticker.toUpperCase() + " forced to " + price + "¢"), true);
        return 1;
    }

    private static int adjustBal(ServerCommandSource src, ServerPlayerEntity player, double amount, String mode) {
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var p  = ps.getPortfolio(player.getUuid());
        switch (mode) {
            case "SET"  -> p.setBalance(amount);
            case "ADD"  -> p.addBalance(amount);
            case "TAKE" -> p.deductBalance(amount);
        }
        ps.markDirty();
        src.sendFeedback(() -> Text.literal(String.format("[QCAdmin] %s balance: %.1f¢",
                player.getName().getString(), p.getBalance())), true);
        return 1;
    }

    private static int reset(ServerCommandSource src, boolean prices, boolean portfolios) {
        if (prices)     MarketEngine.getInstance().resetToBasePrice();
        if (portfolios) {
            var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
            ps.resetAllPortfolios();
        }
        src.sendFeedback(() -> Text.literal("[QCAdmin] Reset: " +
                (prices && portfolios ? "all" : prices ? "prices" : "portfolios")), true);
        return 1;
    }

    private static int setFrozen(ServerCommandSource src, boolean frozen) {
        MarketEngine.getInstance().setFrozen(frozen);
        src.sendFeedback(() -> Text.literal("[QCAdmin] Market " + (frozen ? "FROZEN" : "UNFROZEN")), true);
        return 1;
    }


    private static int fireEvent(ServerCommandSource src, String name) {
        try {
            WorldMarketEvent ev = WorldMarketEvent.valueOf(name.toUpperCase());
            MarketEngine.getInstance().fireEvent(ev, null);
            src.sendFeedback(() -> Text.literal("[QCAdmin] Fired: " + name.toUpperCase()), true);
            return 1;
        } catch (IllegalArgumentException e) {
            src.sendError(Text.literal("Unknown event. Options: " + Arrays.toString(WorldMarketEvent.values())));
            return 0;
        }
    }

    private static int stockInfo(ServerCommandSource src, String ticker) {
        StockState      s = MarketEngine.getInstance().getState(ticker.toUpperCase());
        StockDefinition d = StockRegistry.get(ticker.toUpperCase());
        if (s == null || d == null) { src.sendError(Text.literal("Unknown: " + ticker)); return 0; }
        src.sendFeedback(() -> Text.literal(String.format(
                "[QCAdmin] %s | Price:%.2f | Prev:%.2f | Chg:%+.2f%% | Sector:%s | Float:%d | Held:%d | Base:%.2f",
                d.ticker(), s.getCurrentPrice(), s.getPreviousPrice(), s.getDailyChangePercent(),
                d.sector(), d.totalShares(), s.getSharesHeld(), d.basePrice())), false);
        return 1;
    }

    private static int listPlayers(ServerCommandSource src) {
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var snap = MarketEngine.getInstance().getSnapshot();
        ps.getAllPortfolios().forEach((uuid, p) ->
            src.sendFeedback(() -> Text.literal(String.format("  %s | Cash:%.1f | Total:%.1f",
                    uuid, p.getBalance(), p.getTotalValue(snap))), false));
        return 1;
    }

    private static int cancelOrders(ServerCommandSource src, ServerPlayerEntity target) {
        int count = 0;
        for (StockDefinition def : StockRegistry.getAll()) {
            StockState ss = MarketEngine.getInstance().getState(def.ticker());
            if (ss == null) continue;
            for (LimitOrder o : ss.getOrderBook().getAll()) {
                if (!o.getPlayerUuid().equals(target.getUuid())) continue;
                if (o.getSide() == LimitOrder.Side.BUY) {
                    var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
                    ps.getPortfolio(target.getUuid()).addBalance(o.getLimitPrice() * o.getRemainingQty());
                    ps.markDirty();
                }
                count++;
            }
            ss.getOrderBook().cancelAllForPlayer(target.getUuid());
        }
        final int c = count;
        src.sendFeedback(() -> Text.literal("[QCAdmin] Cancelled " + c + " orders for " + target.getName().getString()), true);
        return 1;
    }

    private static int botEnabled(ServerCommandSource src, String ticker, boolean enabled) {
        LiquidityBot b = MarketEngine.getInstance().getBot(ticker.toUpperCase());
        if (b == null) { src.sendError(Text.literal("No bot for: " + ticker)); return 0; }
        b.setEnabled(enabled);
        src.sendFeedback(() -> Text.literal("[QCAdmin] Bot " + ticker.toUpperCase() + (enabled ? " ENABLED" : " DISABLED")), true);
        return 1;
    }

    private static int botInfo(ServerCommandSource src, String ticker) {
        LiquidityBot b = MarketEngine.getInstance().getBot(ticker.toUpperCase());
        if (b == null) { src.sendError(Text.literal("No bot for: " + ticker)); return 0; }
        src.sendFeedback(() -> Text.literal(String.format(
                "[QCAdmin] Bot %s | En:%b | Shares:%,d | Cash:%.1f | Spread:%.1f%% | Target:%.2f",
                ticker.toUpperCase(), b.isEnabled(), b.getShareReserve(),
                b.getCashReserve(), b.getSpreadPct() * 100, b.getTargetPriceMid())), false);
        return 1;
    }

    private static int seasonInfo(ServerCommandSource src) {
        MarketEngine eng    = MarketEngine.getInstance();
        com.quantcraft.market.MarketSeason season = eng.getCurrentSeason();
        long elapsed   = eng.getSeasonElapsedTicks(src.getServer());
        long remaining = Math.max(0, MarketEngine.PHASE_LENGTH_TICKS - elapsed);
        src.sendFeedback(() -> Text.literal(String.format(
                "[QCAdmin] Season: %s%s §7| Elapsed: %d ticks | Remaining: %d ticks | Next: %s",
                season.color, season.displayName, elapsed, remaining, season.next().displayName)), false);
        return 1;
    }

    private static int help(ServerCommandSource src) {
        String[] lines = {
            "§4§l=== QuantCraft Admin Commands ===",
            "§c/qcadmin crash <ticker|sector> §7— Heavy downward pressure on a stock or sector",
            "§c/qcadmin boom <ticker|sector> §7— Heavy upward pressure on a stock or sector",
            "§c/qcadmin setprice <ticker> <price> §7— Force a stock to an exact price",
            "§c/qcadmin give <player> <amount> §7— Add funds to a player's account",
            "§c/qcadmin take <player> <amount> §7— Deduct funds from a player's account",
            "§c/qcadmin setbalance <player> <amount> §7— Set a player's balance to an exact value",
            "§c/qcadmin reset market §7— Reset all prices to base values",
            "§c/qcadmin reset portfolios §7— Wipe all player portfolios",
            "§c/qcadmin reset all §7— Reset both prices and portfolios",
            "§c/qcadmin freeze §7— Pause the entire market simulation",
            "§c/qcadmin unfreeze §7— Resume the market simulation",
            "§c/qcadmin multiplier <0.1-10> §7— Scale global volatility",
            "§c/qcadmin event <event> §7— Manually fire a market event",
            "§c/qcadmin info <ticker> §7— Debug dump for a stock",
            "§c/qcadmin listplayers §7— List all players with cash and total value",
            "§c/qcadmin season §7— Show current season and remaining ticks",
            "§c/qcadmin season set <phase> §7— Force the market into a specific season",
            "§c/qcadmin cancelorders <player> §7— Cancel all limit orders for a player",
            "§c/qcadmin bot <ticker> enable|disable §7— Enable/disable liquidity bot",
            "§c/qcadmin bot <ticker> info §7— Show bot state",
            "§c/qcadmin bot <ticker> cash <amount> §7— Set bot cash reserve",
            "§c/qcadmin bot <ticker> shares <amount> §7— Set bot share reserve",
            "§c/qcadmin bot <ticker> spread <value> §7— Set bot bid/ask spread",
        };
        for (String line : lines) src.sendFeedback(() -> Text.literal(line), false);
        return 1;
    }

    private static int seasonSet(ServerCommandSource src, String phase) {
        com.quantcraft.market.MarketSeason target;
        try {
            target = com.quantcraft.market.MarketSeason.valueOf(phase.toUpperCase());
        } catch (IllegalArgumentException e) {
            src.sendError(Text.literal("Unknown phase. Options: recovery, expansion, peak, contraction"));
            return 0;
        }
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        MarketEngine.getInstance().forceSetSeason(target, src.getServer().getOverworld().getTime(), ps);
        src.sendFeedback(() -> Text.literal("[QCAdmin] Season forced to: " + target.color + target.displayName), true);
        return 1;
    }
}
