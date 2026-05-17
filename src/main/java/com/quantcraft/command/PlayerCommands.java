package com.quantcraft.command;

import com.mojang.brigadier.arguments.*;
import com.quantcraft.market.*;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.registry.ModBlocks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
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
                            IntegerArgumentType.getInteger(ctx, "qty"),
                            true)))))
                .then(literal("sell").then(argument("ticker", StringArgumentType.word())
                    .then(argument("qty", IntegerArgumentType.integer(1))
                        .executes(ctx -> trade(ctx.getSource(),
                            StringArgumentType.getString(ctx, "ticker"),
                            IntegerArgumentType.getInteger(ctx, "qty"),
                            false)))))
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
                .then(literal("short").then(argument("ticker", StringArgumentType.word())
                    .then(argument("qty", IntegerArgumentType.integer(1))
                        .executes(ctx -> openShort(ctx.getSource(),
                            StringArgumentType.getString(ctx, "ticker"),
                            IntegerArgumentType.getInteger(ctx, "qty"))))))
                .then(literal("covershort").then(argument("ticker", StringArgumentType.word())
                    .executes(ctx -> coverShort(ctx.getSource(),
                        StringArgumentType.getString(ctx, "ticker")))))
                .then(literal("shorts").executes(ctx -> listShorts(ctx.getSource())))
                .then(literal("offer").then(argument("target", EntityArgumentType.player())
                    .then(argument("ticker", StringArgumentType.word())
                        .then(argument("shares", IntegerArgumentType.integer(1))
                            .then(argument("price", DoubleArgumentType.doubleArg(0.01))
                                .executes(ctx -> offer(ctx.getSource(),
                                    EntityArgumentType.getPlayer(ctx, "target"),
                                    StringArgumentType.getString(ctx, "ticker"),
                                    IntegerArgumentType.getInteger(ctx, "shares"),
                                    DoubleArgumentType.getDouble(ctx, "price"))))))))
                .then(literal("pnl").executes(ctx -> sendPortfolio(ctx.getSource())))
            )
        );
    }

    private static int trade(ServerCommandSource src, String ticker, int qty, boolean isBuy) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        if (!nearTradingPost(p, src)) return 0;
        if (!MarketEngine.getInstance().isMarketOpen()) {
            src.sendError(Text.literal("§cThe market is closed. Trading resumes at dawn.")); return 0;
        }
        String t = ticker.toUpperCase();
        if (StockRegistry.get(t) == null) { src.sendError(Text.literal("Unknown ticker: " + t)); return 0; }
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        boolean ok = isBuy
                ? MarketEngine.getInstance().executeMarketBuy(t, qty, p.getUuid(), ps)
                : MarketEngine.getInstance().executeMarketSell(t, qty, p.getUuid(), ps);
        StockState ss    = MarketEngine.getInstance().getState(t);
        double     price = ss != null ? ss.getCurrentPrice() : 0;
        src.sendFeedback(() -> Text.literal(ok
                ? String.format("§a%s %d %s @ §e%.1f¢", isBuy ? "Bought" : "Sold", qty, t, price)
                : (isBuy ? "§cInsufficient funds or no shares available." : "§cNot enough shares.")), false);
        if (ok) com.quantcraft.network.ModPackets.sendPortfolioToClient(p);
        return ok ? 1 : 0;
    }

    private static int balance(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        var    ps  = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        double bal = ps.getPortfolio(p.getUuid()).getBalance();
        src.sendFeedback(() -> Text.literal(String.format("§eBalance: §f%.1f¢", bal)), false);
        return 1;
    }

    private static int portfolio(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var port = ps.getPortfolio(p.getUuid());
        var snap = MarketEngine.getInstance().getSnapshot();
        src.sendFeedback(() -> Text.literal("§6=== Your Portfolio ==="), false);
        src.sendFeedback(() -> Text.literal(String.format("§eCash: §f%.1f¢", port.getBalance())), false);
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
        final MarketSector filter;
        if (sectorFilter != null) {
            try { filter = MarketSector.valueOf(sectorFilter.toUpperCase()); }
            catch (IllegalArgumentException e) { src.sendError(Text.literal("Unknown sector: " + sectorFilter)); return 0; }
        } else {
            filter = null;
        }
        String header = filter == null ? "§6=== All Stocks ===" : "§6=== " + filter + " ===";
        src.sendFeedback(() -> Text.literal(header), false);
        for (StockDefinition d : StockRegistry.getAll()) {
            if (filter != null && d.sector() != filter) continue;
            StockState ss = MarketEngine.getInstance().getState(d.ticker());
            if (ss == null) continue;
            double pct   = ss.getDailyChangePercent();
            String col   = pct >= 0 ? "§a" : "§c";
            String line  = String.format("  §f%-6s §7%-14s §f%8.1f¢ %s%+.2f%%",
                    d.ticker(), d.displayName(), ss.getCurrentPrice(), col, pct);
            src.sendFeedback(() -> Text.literal(line), false);
        }
        return 1;
    }

    private static final int TRADING_POST_RANGE = 8;

    /** Returns true if the player is within range of a Trading Post, sending an error message if not. */
    private static boolean nearTradingPost(ServerPlayerEntity p, ServerCommandSource src) {
        World world = p.getWorld();
        BlockPos center = p.getBlockPos();
        for (int dx = -TRADING_POST_RANGE; dx <= TRADING_POST_RANGE; dx++)
            for (int dy = -3; dy <= 3; dy++)
                for (int dz = -TRADING_POST_RANGE; dz <= TRADING_POST_RANGE; dz++)
                    if (world.getBlockState(center.add(dx, dy, dz)).getBlock() == ModBlocks.TRADING_POST)
                        return true;
        src.sendError(Text.literal("§cYou must be near a Trading Post to do that."));
        return false;
    }

    private static int limitOrder(ServerCommandSource src, String ticker, int qty, double limitPrice, LimitOrder.Side side) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        if (!nearTradingPost(p, src)) return 0;
        if (!MarketEngine.getInstance().isMarketOpen()) {
            src.sendError(Text.literal("§cThe market is closed. Trading resumes at dawn.")); return 0;
        }
        String t = ticker.toUpperCase();
        if (StockRegistry.get(t) == null) { src.sendError(Text.literal("Unknown ticker: " + t)); return 0; }
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var port = ps.getPortfolio(p.getUuid());
        if (side == LimitOrder.Side.BUY) {
            double cost = limitPrice * qty;
            double tax  = cost * com.quantcraft.config.QuantCraftConfig.getTaxRate();
            if (port.getBalance() < cost + tax) {
                src.sendError(Text.literal(String.format("Need §e%.1f¢§c (incl. tax), have §e%.1f¢", cost + tax, port.getBalance())));
                return 0;
            }
            port.deductBalance(cost + tax); ps.markDirty();
        } else {
            if (port.getHolding(t) < qty) { src.sendError(Text.literal("You don't own " + qty + " shares of " + t)); return 0; }
            port.removeShares(t, qty); ps.markDirty();
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
        if (!nearTradingPost(p, src)) return 0;
        String     t  = ticker.toUpperCase();
        StockState ss = MarketEngine.getInstance().getState(t);
        if (ss == null) { src.sendError(Text.literal("Unknown ticker.")); return 0; }
        Optional<LimitOrder> found = ss.getOrderBook().getAll().stream()
                .filter(o -> o.getPlayerUuid().equals(p.getUuid()) && o.getOrderId().toString().startsWith(idStr))
                .findFirst();
        if (found.isEmpty()) { src.sendError(Text.literal("Order not found.")); return 0; }
        LimitOrder o = found.get();
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        if (o.getSide() == LimitOrder.Side.BUY) {
            ps.getPortfolio(p.getUuid()).addBalance(o.getLimitPrice() * o.getRemainingQty());
        } else {
            ps.getPortfolio(p.getUuid()).addShares(o.getTicker(), o.getRemainingQty());
        }
        ps.markDirty();
        ss.getOrderBook().cancelOrder(o.getOrderId());
        src.sendFeedback(() -> Text.literal("§aOrder cancelled. Assets returned."), false);
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
        if (from.getBalance() < amount) { src.sendError(Text.literal("Insufficient balance.")); return 0; }
        from.deductBalance(amount); to.addBalance(amount); ps.markDirty();
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
        MarketEngine engine  = MarketEngine.getInstance();
        var          active  = engine.getActiveEvents();
        var          history = engine.getHistoricalNews();
        if (active.isEmpty() && history.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7No news yet."), false);
            return 1;
        }
        if (!active.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§6ACTIVE CONDITIONS:"), false);
            active.forEach(e -> src.sendFeedback(() -> Text.literal("§c⚡ §f" + e.getStatusLine()), false));
        }
        if (!history.isEmpty()) {
            src.sendFeedback(() -> Text.literal("§7RECENT HISTORY:"), false);
            history.forEach(n -> src.sendFeedback(() -> Text.literal("§7• §f" + n), false));
        }
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

    private static int openShort(ServerCommandSource src, String ticker, int qty) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        if (!nearTradingPost(p, src)) return 0;
        if (!MarketEngine.getInstance().isMarketOpen()) {
            src.sendError(Text.literal("§cThe market is closed. Trading resumes at dawn.")); return 0;
        }
        String t = ticker.toUpperCase();
        StockDefinition def = StockRegistry.get(t);
        StockState      ss  = MarketEngine.getInstance().getState(t);
        if (def == null || ss == null) { src.sendError(Text.literal("Unknown ticker: " + t)); return 0; }
        LiquidityBot bot = MarketEngine.getInstance().getBot(t);
        int maxBorrow = bot != null ? bot.getShareReserve() / 4 : 0;
        if (bot == null || qty > maxBorrow) {
            src.sendError(Text.literal(String.format("§cMax borrowable: %d shares (25%% of liquidity).", maxBorrow))); return 0;
        }
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        // Check player doesn't already have a short on this ticker
        boolean alreadyShort = ps.getShorts(p.getUuid()).stream()
                .anyMatch(sp -> sp.getTicker().equals(t));
        if (alreadyShort) { src.sendError(Text.literal("§cYou already have an open short on " + t + ".")); return 0; }
        double price  = ss.getCurrentPrice();
        double margin = price * qty;
        var port = ps.getPortfolio(p.getUuid());
        if (port.getBalance() < margin) {
            src.sendError(Text.literal(String.format("§cNeed §e%.1f¢§c margin (100%%), have §e%.1f¢", margin, port.getBalance())));
            return 0;
        }
        port.deductBalance(margin);
        bot.setShareReserve(bot.getShareReserve() - qty);
        ss.adjustSharesHeld(+qty);
        ShortPosition sp = new ShortPosition(p.getUuid(), t, qty, price, margin, src.getServer().getTicks());
        ps.addShort(p.getUuid(), sp);
        ps.markDirty();
        src.sendFeedback(() -> Text.literal(String.format(
                "§aShort opened: §e%d %s §aat §e%.1f¢§a. §7Margin locked: §e%.1f¢§7. Borrow fee: 0.2%%/tick.",
                qty, t, price, margin)), false);
        return 1;
    }

    private static int coverShort(ServerCommandSource src, String ticker) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        if (!nearTradingPost(p, src)) return 0;
        if (!MarketEngine.getInstance().isMarketOpen()) {
            src.sendError(Text.literal("§cThe market is closed. Trading resumes at dawn.")); return 0;
        }
        String t = ticker.toUpperCase();
        var ps = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        ShortPosition sp = ps.getShorts(p.getUuid()).stream()
                .filter(s -> s.getTicker().equals(t)).findFirst().orElse(null);
        if (sp == null) { src.sendError(Text.literal("No open short for " + t + ".")); return 0; }
        StockState ss = MarketEngine.getInstance().getState(t);
        if (ss == null) { src.sendError(Text.literal("Unknown ticker: " + t)); return 0; }
        double price   = ss.getCurrentPrice();
        LiquidityBot bot = MarketEngine.getInstance().getBot(t);
        double buyback = price * sp.getShares();
        double fee     = sp.getAccruedFee();
        // Margin covers buyback + fees. Remainder is returned to player.
        double returned = Math.max(0, sp.getMarginReserve() - buyback - fee);
        double pnl     = sp.getCurrentPnL(price);
        var port = ps.getPortfolio(p.getUuid());
        port.addBalance(returned);
        if (bot != null) bot.setShareReserve(bot.getShareReserve() + sp.getShares());
        ss.adjustSharesHeld(-sp.getShares());
        ps.removeShort(p.getUuid(), sp);
        ps.markDirty();
        final double displayPnl = pnl;
        final double displayFee = fee;
        final double displayReturned = returned;
        src.sendFeedback(() -> Text.literal(String.format(
                "§aShort closed: §e%s§a. PnL: %s%.1f¢§a. Returned: §e%.1f¢ §7Borrow fees: §c%.2f¢",
                t, displayPnl >= 0 ? "§a+" : "§c", displayPnl, displayReturned, displayFee)), false);
        return 1;
    }

    private static int listShorts(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        var ps   = MarketPersistentState.getOrCreate(src.getServer().getOverworld());
        var list = ps.getShorts(p.getUuid());
        src.sendFeedback(() -> Text.literal("§6=== Open Short Positions ==="), false);
        if (list.isEmpty()) { src.sendFeedback(() -> Text.literal("§7None."), false); return 1; }
        for (ShortPosition sp : list) {
            StockState ss    = MarketEngine.getInstance().getState(sp.getTicker());
            double     cur   = ss != null ? ss.getCurrentPrice() : 0;
            double     pnl   = sp.getCurrentPnL(cur);
            double     room  = sp.getMarginReserve() - Math.max(0, (cur - sp.getOpenPrice()) * sp.getShares());
            src.sendFeedback(() -> Text.literal(String.format(
                    "  §e%-5s §7×%d open:§f%.1f§7 cur:§f%.1f §7PnL:%s%.1f¢ §7fee:§c%.2f¢ §7margin left:§e%.1f¢",
                    sp.getTicker(), sp.getShares(), sp.getOpenPrice(), cur,
                    pnl >= 0 ? "§a+" : "§c", pnl, sp.getAccruedFee(), room)), false);
        }
        return 1;
    }

    private static int offer(ServerCommandSource src, ServerPlayerEntity target, String ticker, int shares, double price) {
        if (!(src.getEntity() instanceof ServerPlayerEntity proposer)) return 0;
        String t = ticker.toUpperCase();
        String err = OtcTradeManager.getInstance().propose(proposer, target, t, shares, price, src.getServer());
        if (err != null) { src.sendError(Text.literal(err)); return 0; }
        return 1;
    }

    private static int sendPortfolio(ServerCommandSource src) {
        if (!(src.getEntity() instanceof ServerPlayerEntity p)) return 0;
        com.quantcraft.network.ModPackets.sendPortfolioToClient(p);
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
