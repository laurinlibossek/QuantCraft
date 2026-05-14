package com.quantcraft.command;

import com.mojang.brigadier.arguments.*;
import com.quantcraft.market.*;
import com.quantcraft.persistence.MarketPersistentState;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import java.util.*;

import static net.minecraft.server.command.CommandManager.*;

public class PlayerCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) ->
            dispatcher.register(literal("qc")
                .then(literal("balance")  .executes(ctx -> balance(ctx.getSource())))
                .then(literal("portfolio").executes(ctx -> portfolio(ctx.getSource())))
                .then(literal("price").then(argument("ticker", StringArgumentType.word())
                    .executes(ctx -> price(ctx.getSource(), StringArgumentType.getString(ctx, "ticker")))))
                .then(literal("prices")
                    .executes(ctx -> prices(ctx.getSource(), null))
                    .then(argument("sector", StringArgumentType.word())
                        .executes(ctx -> prices(ctx.getSource(), StringArgumentType.getString(ctx, "sector")))))
                .then(literal("buy").then(argument("ticker", StringArgumentType.word())
                    .then(argument("qty", IntegerArgumentType.integer(1))
                        .executes(ctx -> trade(ctx.getSource(),
                            StringArgumentType.getString(ctx, "ticker"),
                            IntegerArgumentType.getInteger(ctx, "qty"), true)))))
                .then(literal("sell").then(argument("ticker", StringArgumentType.word())
                    .then(argument("qty", IntegerArgumentType.integer(1))
                        .executes(ctx -> trade(ctx.getSource(),
                            StringArgumentType.getString(ctx, "ticker"),
                            IntegerArgumentType.getInteger(ctx, "qty"), false)))))
                .then(literal("limitbuy").then(argument("ticker", StringArgumentType.word())
                    .then(argument("qty", IntegerArgumentType.integer(1))
                        .then(argument("price", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> limitOrder(ctx.getSource(),
                                StringArgumentType.getString(ctx, "ticker"),
                                IntegerArgumentType.getInteger(ctx, "qty"),
                                DoubleArgumentType.getDouble(ctx, "price"),
                                LimitOrder.Side.BUY))))))
                .then(literal("limitsell").then(argument("ticker", StringArgumentType.word())
                    .then(argument("qty", IntegerArgumentType.integer(1))
                        .then(argument("price", DoubleArgumentType.doubleArg(0))
                            .executes(ctx -> limitOrder(ctx.getSource(),
                                StringArgumentType.getString(ctx, "ticker"),
                                IntegerArgumentType.getInteger(ctx, "qty"),
                                DoubleArgumentType.getDouble(ctx, "price"),
                                LimitOrder.Side.SELL))))))
                .then(literal("orders")      .executes(ctx -> listOrders(ctx.getSource())))
                .then(literal("cancelorder").then(argument("ticker", StringArgumentType.word())
                    .then(argument("orderId", StringArgumentType.word())
                        .executes(ctx -> cancelOrder(ctx.getSource(),
                            StringArgumentType.getString(ctx, "ticker"),
                            StringArgumentType.getString(ctx, "orderId"))))))
                .then(literal("float").then(argument("ticker", StringArgumentType.word())
                    .executes(ctx -> showFloat(ctx.getSource(), StringArgumentType.getString(ctx, "ticker")))))
                .then(literal("pay").then(argument("player", EntityArgumentType.player())
                    .then(argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> pay(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(literal("request").then(argument("player", EntityArgumentType.player())
                    .then(argument("amount", DoubleArgumentType.doubleArg(0))
                        .executes(ctx -> request(ctx.getSource(),
                            EntityArgumentType.getPlayer(ctx, "player"),
                            DoubleArgumentType.getDouble(ctx, "amount"))))))
                .then(literal("news").executes(ctx -> news(ctx.getSource())))
                .then(literal("tick").executes(ctx -> tickInfo(ctx.getSource())))
            )
        );
    }

    private static int balance(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        var    ps  = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        double bal = ps.getPortfolio(p.getUuid()).getCoinBalance();
        src.sendFeedback(() -> Text.literal(String.format("§eCoin balance: §f%.1f¢", bal)), false);
        return 1;
    }

    private static int portfolio(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var port = ps.getPortfolio(p.getUuid());
        var snap = MarketEngine.getInstance().getSnapshot();
        src.sendFeedback(() -> Text.literal("§6=== Your Portfolio ==="), false);
        src.sendFeedback(() -> Text.literal(String.format("§eCash: §f%.1f¢", port.getCoinBalance())), false);
        if (port.getHoldings().isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7No holdings."), false);
        } else {
            port.getHoldings().forEach((tk, qty) -> {
                StockState ss  = snap.get(tk);
                double     val = ss != null ? ss.getCurrentPrice() * qty : 0;
                src.sendFeedback(() -> Text.literal(String.format("  §f%-6s §7x%d = §e%.1f¢", tk, qty, val)), false);
            });
        }
        src.sendFeedback(() -> Text.literal(String.format("§6Total: §f%.1f¢", port.getTotalValue(snap))), false);
        return 1;
    }

    private static int price(ServerCommandSource src, String ticker) {
        String     t   = ticker.toUpperCase();
        StockState ss  = MarketEngine.getInstance().getState(t);
        StockDefinition def = StockRegistry.get(t);
        if (ss == null || def == null) { src.sendError(Text.literal("Unknown: " + ticker)); return 0; }
        double pct   = ss.getDailyChangePercent();
        String spark = buildSparkline(ss.getPriceHistory());
        src.sendFeedback(() -> Text.literal(String.format("§e%s §7(%s) §f%.1f¢ %s§f%+.2f%% §7%s",
                def.ticker(), def.displayName(), ss.getCurrentPrice(), pct >= 0 ? "§a" : "§c", pct, spark)), false);
        src.sendFeedback(() -> Text.literal(String.format("§7Exchange: Buy §f%.1f¢ §7Sell §f%.1f¢ §7| Float avail: §f%,d",
                def.getItemBuyPrice(ss.getCurrentPrice()), def.getItemSellPrice(ss.getCurrentPrice()),
                ss.getAvailableShares(def.totalShares()))), false);
        return 1;
    }

    private static int prices(ServerCommandSource src, String sectorFilter) {
        MarketSector filter = null;
        if (sectorFilter != null) {
            try { filter = MarketSector.valueOf(sectorFilter.toUpperCase()); }
            catch (IllegalArgumentException e) { src.sendError(Text.literal("Unknown sector: " + sectorFilter)); return 0; }
        }
        final MarketSector ff = filter;
        src.sendFeedback(() -> Text.literal(ff == null ? "§6=== All Stocks ===" : "§6=== " + ff + " ==="), false);
        for (StockDefinition d : StockRegistry.getAll()) {
            if (ff != null && d.sector() != ff) continue;
            StockState ss = MarketEngine.getInstance().getState(d.ticker());
            if (ss == null) continue;
            double pct = ss.getDailyChangePercent();
            String col = pct >= 0 ? "§a" : "§c";
            src.sendFeedback(() -> Text.literal(String.format("  §f%-6s §7%-14s §f%8.1f¢ %s%+.2f%%",
                    d.ticker(), d.displayName(), ss.getCurrentPrice(), col, pct)), false);
        }
        return 1;
    }

    private static int trade(ServerCommandSource src, String ticker, int qty, boolean isBuy) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        String t  = ticker.toUpperCase();
        var    ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        boolean ok = isBuy
                ? MarketEngine.getInstance().executeMarketBuy(t, qty, p.getUuid(), ps)
                : MarketEngine.getInstance().executeMarketSell(t, qty, p.getUuid(), ps);
        StockState ss    = MarketEngine.getInstance().getState(t);
        double     price = ss != null ? ss.getCurrentPrice() : 0;
        if (ok) src.sendFeedback(() -> Text.literal(String.format("§a%s %d %s @ §e%.1f¢", isBuy ? "Bought" : "Sold", qty, t, price)), false);
        else    src.sendError(Text.literal(isBuy ? "Insufficient funds or no shares available." : "You don't own enough shares."));
        return ok ? 1 : 0;
    }

    private static int limitOrder(ServerCommandSource src, String ticker, int qty, double limitPrice, LimitOrder.Side side) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        String t = ticker.toUpperCase();
        if (StockRegistry.get(t) == null) { src.sendError(Text.literal("Unknown ticker: " + t)); return 0; }
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var port = ps.getPortfolio(p.getUuid());
        if (side == LimitOrder.Side.BUY) {
            double cost = limitPrice * qty;
            if (port.getCoinBalance() < cost) {
                src.sendError(Text.literal(String.format("Need §e%.1f¢§c, have §e%.1f¢", cost, port.getCoinBalance())));
                return 0;
            }
            port.deductCoins(cost); ps.markDirty();
        } else {
            if (port.getHolding(t) < qty) { src.sendError(Text.literal("You don't own " + qty + " shares of " + t)); return 0; }
        }
        LimitOrder order = new LimitOrder(p.getUuid(), t, side, qty, limitPrice, src.getServer().getTicks());
        MarketEngine.getInstance().placeLimitOrder(order);
        StockState ss  = MarketEngine.getInstance().getState(t);
        double     cur = ss != null ? ss.getCurrentPrice() : 0;
        src.sendFeedback(() -> Text.literal(String.format("§aLimit %s placed: %d §f%s§a @ §e%.1f¢§a (now:§e%.1f¢§a) ID:§7%s",
                side, qty, t, limitPrice, cur, order.getOrderId().toString().substring(0, 8))), false);
        return 1;
    }

    private static int listOrders(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        src.sendFeedback(() -> Text.literal("§6=== Pending Limit Orders ==="), false);
        boolean any = false;
        for (StockDefinition def : StockRegistry.getAll()) {
            StockState ss = MarketEngine.getInstance().getState(def.ticker());
            if (ss == null) continue;
            for (LimitOrder o : ss.getOrderBook().getAll()) {
                if (!o.getPlayerUuid().equals(p.getUuid())) continue;
                any = true;
                String col = o.getSide() == LimitOrder.Side.BUY ? "§a" : "§c";
                src.sendFeedback(() -> Text.literal(String.format("  %s%s §f%s ×%d @ §e%.1f¢ §7ID:%s",
                        col, o.getSide(), o.getTicker(), o.getRemainingQty(), o.getLimitPrice(),
                        o.getOrderId().toString().substring(0, 8))), false);
            }
        }
        if (!any) src.sendFeedback(() -> Text.literal("§7None."), false);
        return 1;
    }

    private static int cancelOrder(ServerCommandSource src, String ticker, String idStr) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        String     t  = ticker.toUpperCase();
        StockState ss = MarketEngine.getInstance().getState(t);
        if (ss == null) { src.sendError(Text.literal("Unknown ticker.")); return 0; }
        Optional<LimitOrder> found = ss.getOrderBook().getAll().stream()
                .filter(o -> o.getPlayerUuid().equals(p.getUuid()) && o.getOrderId().toString().startsWith(idStr))
                .findFirst();
        if (found.isEmpty()) { src.sendError(Text.literal("Order not found.")); return 0; }
        LimitOrder o = found.get();
        if (o.getSide() == LimitOrder.Side.BUY) {
            var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
            ps.getPortfolio(p.getUuid()).addCoins(o.getLimitPrice() * o.getRemainingQty());
            ps.markDirty();
        }
        ss.getOrderBook().cancelOrder(o.getOrderId());
        src.sendFeedback(() -> Text.literal("§aOrder cancelled. Coins refunded."), false);
        return 1;
    }

    private static int showFloat(ServerCommandSource src, String ticker) {
        String          t   = ticker.toUpperCase();
        StockDefinition def = StockRegistry.get(t);
        StockState      ss  = MarketEngine.getInstance().getState(t);
        if (def == null || ss == null) { src.sendError(Text.literal("Unknown: " + t)); return 0; }
        LiquidityBot bot       = MarketEngine.getInstance().getBot(t);
        int          botShares = bot != null ? bot.getShareReserve() : 0;
        src.sendFeedback(() -> Text.literal(String.format(
                "§e%s §7Float: Total:§f%,d §7Player:§f%,d §7Bot:§f%,d §7Avail:§f%,d",
                t, def.totalShares(), ss.getSharesHeld() - botShares, botShares,
                ss.getAvailableShares(def.totalShares()))), false);
        return 1;
    }

    private static int pay(ServerCommandSource src, ServerPlayerEntity target, double amount) {
        if (!(src.getEntity() instanceof ServerPlayerEntity payer)) return 0;
        if (payer.getUuid().equals(target.getUuid())) { src.sendError(Text.literal("Can't pay yourself.")); return 0; }
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var from = ps.getPortfolio(payer.getUuid());
        var to   = ps.getPortfolio(target.getUuid());
        if (from.getCoinBalance() < amount) { src.sendError(Text.literal("Insufficient balance.")); return 0; }
        from.deductCoins(amount); to.addCoins(amount); ps.markDirty();
        payer.sendMessage(Text.literal(String.format("§aPaid §e%.1f¢§a to §f%s", amount, target.getName().getString())));
        target.sendMessage(Text.literal(String.format("§aReceived §e%.1f¢§a from §f%s", amount, payer.getName().getString())));
        return 1;
    }

    private static int request(ServerCommandSource src, ServerPlayerEntity target, double amount) {
        if (!(src.getEntity() instanceof ServerPlayerEntity req)) return 0;
        MutableText btn = Text.literal(" [Pay] ").formatted(Formatting.GREEN, Formatting.BOLD)
                .styled(s -> s.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        String.format("/qc pay %s %.2f", req.getName().getString(), amount)))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        Text.literal(String.format("Send %.1f¢ to %s", amount, req.getName().getString())))));
        target.sendMessage(Text.literal(String.format("§e%s §7requests §e%.1f¢§7.", req.getName().getString(), amount)).append(btn));
        req.sendMessage(Text.literal("§7Request sent to §f" + target.getName().getString()));
        return 1;
    }

    private static int news(ServerCommandSource src) {
        var h = MarketEngine.getInstance().getRecentNews();
        if (h.isEmpty()) { src.sendFeedback(() -> Text.literal("§7No news yet."), false); return 1; }
        src.sendFeedback(() -> Text.literal("§6=== Market News ==="), false);
        h.forEach(n -> src.sendFeedback(() -> Text.literal("§7• §f" + n), false));
        return 1;
    }

    private static int tickInfo(ServerCommandSource src) {
        int interval = com.quantcraft.config.QuantCraftConfig.getMarketTickInterval();
        int cur      = com.quantcraft.QuantCraftMod.getTickCounter();
        src.sendFeedback(() -> Text.literal(String.format(
                "§eNext market tick in §f%d §eticks (§f%d§e elapsed / §f%d§e interval)",
                interval - cur, cur, interval)), false);
        return 1;
    }

    private static String buildSparkline(List<Double> h) {
        if (h.size() < 2) return "";
        List<Double> r   = h.subList(Math.max(0, h.size() - 10), h.size());
        double       mn  = r.stream().mapToDouble(Double::doubleValue).min().orElse(0);
        double       mx  = r.stream().mapToDouble(Double::doubleValue).max().orElse(1);
        double       rng = Math.max(0.01, mx - mn);
        String       b   = "▁▂▃▄▅▆▇█";
        StringBuilder sb = new StringBuilder();
        for (double v : r) {
            int i = (int)((v - mn) / rng * (b.length() - 1));
            sb.append(b.charAt(Math.max(0, Math.min(i, b.length() - 1))));
        }
        return sb.toString();
    }
}
