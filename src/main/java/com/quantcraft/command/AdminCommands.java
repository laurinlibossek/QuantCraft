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
                    .executes(ctx -> pressure(ctx.getSource(), StringArgumentType.getString(ctx, "target"), -50.0, "CRASH"))))
                .then(literal("boom").then(argument("target", StringArgumentType.word())
                    .executes(ctx -> pressure(ctx.getSource(), StringArgumentType.getString(ctx, "target"), +50.0, "BOOM"))))
                .then(literal("setprice").then(argument("ticker", StringArgumentType.word())
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
                .then(literal("tick")    .executes(ctx -> forceTick(ctx.getSource())))
                .then(literal("multiplier").then(argument("value", FloatArgumentType.floatArg(0.1f, 10f))
                    .executes(ctx -> {
                        float v = FloatArgumentType.getFloat(ctx, "value");
                        QuantCraftConfig.setGlobalVolatilityMultiplier(v);
                        ctx.getSource().sendFeedback(() -> Text.literal("[QCAdmin] Volatility multiplier set to " + v), true);
                        return 1;
                    })))
                .then(literal("event").then(argument("event", StringArgumentType.word())
                    .executes(ctx -> fireEvent(ctx.getSource(), StringArgumentType.getString(ctx, "event")))))
                .then(literal("info").then(argument("ticker", StringArgumentType.word())
                    .executes(ctx -> stockInfo(ctx.getSource(), StringArgumentType.getString(ctx, "ticker")))))
                .then(literal("listplayers").executes(ctx -> listPlayers(ctx.getSource())))
                .then(literal("cancelorders").then(argument("player", EntityArgumentType.player())
                    .executes(ctx -> cancelOrders(ctx.getSource(), EntityArgumentType.getPlayer(ctx, "player")))))
                .then(literal("floor").then(argument("ticker", StringArgumentType.word())
                    .then(argument("price", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> {
                            // priceFloor is final in record — this command documents the intent but can't mutate it
                            ctx.getSource().sendFeedback(() -> Text.literal("[QCAdmin] Note: price floor is set at definition time only."), false);
                            return 1;
                        }))))
                .then(literal("bot").then(argument("ticker", StringArgumentType.word())
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

    private static int forceTick(ServerCommandSource src) {
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        MarketEngine.getInstance().tick(src.getServer(), ps);
        ps.markDirty();
        src.sendFeedback(() -> Text.literal("[QCAdmin] Tick forced."), false);
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
}
