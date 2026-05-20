package com.quantcraft.market;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.events.MarketEventListener;
import com.quantcraft.network.ModPackets;
import com.quantcraft.persistence.MarketPersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;
import java.util.EnumMap;

public class MarketEngine {
    private static final MarketEngine INSTANCE = new MarketEngine();
    public static MarketEngine getInstance() { return INSTANCE; }
    private MarketEngine() {}

    private final Map<String,StockState>   states        = new LinkedHashMap<>();
    private final Map<String,LiquidityBot> bots          = new LinkedHashMap<>();
    private final List<ActiveMarketEvent>  activeEvents  = new ArrayList<>();
    private final List<String>             recentNews    = new ArrayList<>();
    private final Map<String,Double>       closingPrices = new LinkedHashMap<>();
    private static final int MAX_NEWS = 5;
    private boolean      frozen         = false;
    private boolean      marketOpen     = true;
    private long         marketTickCount = 0;
    private MarketSeason currentSeason  = MarketSeason.RECOVERY;
    private long         seasonStartTick = -1L;

    // Each phase lasts 3 Minecraft days = 72 000 game ticks. Blend starts 12 000 ticks before end.
    public  static final long PHASE_LENGTH_TICKS = 72_000L;
    private static final long BLEND_TICKS        = 12_000L;

    // ── Initialisation ───────────────────────────────────────────────────────
    public void loadState(MarketPersistentState ps) {
        states.clear(); bots.clear(); activeEvents.clear(); closingPrices.clear();
        activeEvents.addAll(ps.getActiveEvents());
        closingPrices.putAll(ps.getClosingPrices());
        marketTickCount = ps.getMarketTickCount();
        Random rand = new Random();
        for (StockDefinition def : StockRegistry.getAll()) {
            StockState saved = ps.getStockState(def.ticker());
            if (saved != null) {
                states.put(def.ticker(), saved);
            } else {
                double seed = 0.75 + rand.nextDouble() * 0.5;
                states.put(def.ticker(), new StockState(def.ticker(), def.basePrice() * seed));
            }
            LiquidityBot bot = ps.getLiquidityBot(def.ticker());
            if (bot == null) {
                int reserve = (int)(def.totalShares() * 0.25);
                // Larger order sizes for high-float (farmable) stocks so the bot provides real resistance
                int orderSz = def.totalShares() >= 50_000 ? 64 : def.totalShares() >= 20_000 ? 32 : 16;
                bot = new LiquidityBot(def.ticker(), def.basePrice(), reserve);
                bot.setOrderSize(orderSz);
            }
            bots.put(def.ticker(), bot);
            StockState ss = states.get(def.ticker());
            if (ss != null) bot.tick(ss, def);
        }
        reconcileSharesHeld(ps);
        MarketSeason[] phases = MarketSeason.values();
        currentSeason  = phases[Math.floorMod(ps.getCyclePhaseIndex(), phases.length)];
        seasonStartTick = ps.getCyclePhaseStartTick();
        recentNews.clear();
        pushSeasonStatus();
        QuantCraftMod.LOGGER.info("[QuantCraft] Loaded {} stocks. Season: {}", states.size(), currentSeason.displayName);
    }

    private void reconcileSharesHeld(MarketPersistentState ps) {
        Map<String, Integer> totals = new HashMap<>();
        for (var e : ps.getAllPortfolios().entrySet())
            e.getValue().getHoldings().forEach((tk, qty) -> totals.merge(tk, qty, Integer::sum));
        for (var e : bots.entrySet()) {
            String tk = e.getKey();
            totals.merge(tk, e.getValue().getShareReserve(), Integer::sum);
        }
        for (var e : states.entrySet()) {
            StockState ss = e.getValue();
            int reconciled = totals.getOrDefault(e.getKey(), 0);
            int delta = reconciled - ss.getSharesHeld();
            if (delta != 0) {
                ss.adjustSharesHeld(delta);
                QuantCraftMod.LOGGER.info("[QuantCraft] Reconciled {} sharesHeld: {} -> {}", e.getKey(), ss.getSharesHeld() - delta, reconciled);
            }
        }
    }

    // ── Tick ─────────────────────────────────────────────────────────────────
    public void tick(MinecraftServer server, MarketPersistentState ps) {
        if (frozen) return;
        marketTickCount++;
        MarketEventListener.resetEventCounts();
        sectorEventCounts.clear();
        ps.checkDailyReset();
        tickSimulated(server);
        processShortPositions(server, ps);
        payDividendsIfDue(server, ps);
        if (marketTickCount % 200 == 0) {
            OtcTradeManager.getInstance().purgeExpired(server.getOverworld().getTime());
            PaymentRequestManager.getInstance().purgeExpired(server.getOverworld().getTime());
        }
        tickSeason(server, ps);
        ps.saveStates(states);
        ModPackets.broadcastMarketUpdate(server, getSnapshot());
    }

    /** Called by QuantCraftMod at dusk to snapshot prices before the market closes. */
    public void snapshotClosingPrices(MarketPersistentState ps) {
        closingPrices.clear();
        for (var e : states.entrySet()) closingPrices.put(e.getKey(), e.getValue().getCurrentPrice());
        ps.setClosingPrices(closingPrices);
        ps.markDirty();
    }

    public Map<String,Double> getClosingPrices() { return Collections.unmodifiableMap(closingPrices); }

    // Season blend values recomputed each tick, shared across per-stock loop
    private double blendedSeasonPressure   = 0.0;
    private double blendedSeasonVolatility = 1.0;
    private double seasonBlendFactor       = 0.0; // 0 = fully current phase, 1 = fully next

    private synchronized void tickSimulated(MinecraftServer server) {
        long    wt        = server.getOverworld().getTimeOfDay();
        long    tod       = wt % 24000L;
        boolean isNight   = tod > 13000L;
        boolean isFullMoon= (server.getOverworld().getMoonPhase() == 0);
        boolean isThunder = server.getOverworld().isThundering();
        // Market open: dawn (tod 0) through dusk (tod 12999)
        boolean wasOpen = marketOpen;
        marketOpen = tod < 13000L;
        if (marketOpen && !wasOpen) {
            for (StockState ss : states.values()) ss.snapshotOpenPrice();
        }

        // Compute blended season values (lerp during last BLEND_TICKS of phase)
        if (seasonStartTick >= 0) {
            long elapsed = server.getOverworld().getTime() - seasonStartTick;
            long blendStart = PHASE_LENGTH_TICKS - BLEND_TICKS;
            if (elapsed >= blendStart) {
                seasonBlendFactor = Math.min(1.0, (double)(elapsed - blendStart) / BLEND_TICKS);
                MarketSeason next = currentSeason.next();
                blendedSeasonPressure   = lerp(currentSeason.pressureCoeff,  next.pressureCoeff,  seasonBlendFactor);
                blendedSeasonVolatility = lerp(currentSeason.volatilityMult, next.volatilityMult, seasonBlendFactor);
            } else {
                seasonBlendFactor       = 0.0;
                blendedSeasonPressure   = currentSeason.pressureCoeff;
                blendedSeasonVolatility = currentSeason.volatilityMult;
            }
        } else {
            seasonBlendFactor       = 0.0;
            blendedSeasonPressure   = currentSeason.pressureCoeff;
            blendedSeasonVolatility = currentSeason.volatilityMult;
        }

        // Apply active newspaper events only during market hours; pause countdown at night
        if (marketOpen) {
            for (ActiveMarketEvent event : activeEvents) {
                if (event.getAffectedTicker() != null) {
                    pressureTicker(event.getAffectedTicker(), event.getPressurePerTick());
                } else if (event.getAffectedSector() != null) {
                    pressureSector(event.getAffectedSector(), event.getPressurePerTick());
                }
                event.tick();
            }
            activeEvents.removeIf(ActiveMarketEvent::isExpired);
        }

        for (StockDefinition def : StockRegistry.getAll()) {
            StockState state = states.get(def.ticker());
            if (state == null) continue;

            state.getCandleHistory().openNewCandle(state.getCurrentPrice());
            if (marketOpen) processLimitOrders(def, state, server);

            LiquidityBot bot = bots.get(def.ticker());
            if (bot != null) bot.tick(state, def);

            double eventPressure = state.consumeEventPressure();
            double supplyEffect = state.tickSupplyPressure();
            double sectorMod = computeSectorModifier(def.sector(), isNight, isFullMoon, isThunder, def.basePrice());

            float mult = QuantCraftConfig.getGlobalVolatilityMultiplier() * (float) blendedSeasonVolatility;
            double seasonNudge = blendedSeasonPressure * def.basePrice();
            double priceChange = (eventPressure * 0.1 + supplyEffect + sectorMod) * mult + seasonNudge;
            state.updatePrice(state.getCurrentPrice() + priceChange);

            double pct = state.getDailyChangePercent();
            if (Math.abs(pct) > 5.0 && QuantCraftConfig.isMarketNewsEnabled())
                pushNews(MarketNewsGenerator.generate(def.displayName(), pct));
        }
    }

    // ── Limit order processing ────────────────────────────────────────────────
    private void processLimitOrders(StockDefinition def, StockState state, MinecraftServer server) {
        var ps    = MarketPersistentState.getOrCreate(server.getOverworld());
        double price = state.getCurrentPrice();
        List<LimitOrder> filled = state.getOrderBook().processOrders(price);
        for (LimitOrder order : filled) {
            if (order.getPlayerUuid().equals(LiquidityBot.getBotUuid())) {
                LiquidityBot bot = bots.get(def.ticker());
                if (bot != null) bot.onOrderFilled(order, price);
            } else {
                PlayerPortfolio portfolio = ps.getPortfolio(order.getPlayerUuid());
                int qty = order.getFilledQty();
                double taxRate = QuantCraftConfig.getTaxRate();
                if (order.getSide() == LimitOrder.Side.BUY) {
                    int avail = state.getAvailableShares(def.totalShares());
                    int fill  = Math.min(qty, avail);
                    int unfilled = qty - fill;
                    if (unfilled > 0) {
                        double refundCost = order.getLimitPrice() * unfilled;
                        double refundTax  = refundCost * taxRate;
                        portfolio.addBalance(refundCost + refundTax);
                        notify(server, order.getPlayerUuid(),
                                String.format("§eLimit BUY partial: %d/%d %s filled, §e%.1f¢§e refunded for unfilled portion",
                                        fill, qty, def.ticker(), refundCost + refundTax));
                    }
                    if (fill > 0) {
                        portfolio.addShares(def.ticker(), fill, price);
                        state.adjustSharesHeld(+fill);
                        state.getCandleHistory().recordTrade(price, fill);
                        fireEvent(WorldMarketEvent.PLAYER_BOUGHT_STOCK, def.ticker());
                        notify(server, order.getPlayerUuid(),
                                String.format("§aLimit BUY filled: %d %s @ §e%.1f¢ §7(%.0f%% tax escrowed)", fill, def.ticker(), price, taxRate * 100));
                    }
                } else {
                    int fill = qty;
                    double tax = price * fill * taxRate;
                    portfolio.addBalance(price * fill - tax);
                    state.adjustSharesHeld(-fill);
                    state.getCandleHistory().recordTrade(price, fill);
                    fireEvent(WorldMarketEvent.PLAYER_SOLD_STOCK, def.ticker());
                    notify(server, order.getPlayerUuid(),
                            String.format("§aLimit SELL filled: %d %s @ §e%.1f¢ §7(%.0f%% tax)", fill, def.ticker(), price, taxRate * 100));
                }
            }
        }
        ps.markDirty();
    }

    // ── Market order execution ────────────────────────────────────────────────
    public synchronized boolean executeMarketBuy(String ticker, int qty, UUID playerUuid, MarketPersistentState ps) {
        if (!marketOpen) return false;
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = getState(ticker);
        if (def == null || ss == null) return false;
        int avail = ss.getAvailableShares(def.totalShares());
        int fill  = Math.min(qty, avail);
        if (fill <= 0) return false;
        double price = ss.getCurrentPrice();
        double tax   = price * fill * QuantCraftConfig.getTaxRate();
        PlayerPortfolio p = ps.getPortfolio(playerUuid);
        if (p.getBalance() < price * fill + tax) return false;
        if (!p.buy(ticker, fill, price)) return false;
        p.deductBalance(tax);
        ss.adjustSharesHeld(+fill);
        ss.getCandleHistory().recordTrade(price, fill);
        ss.applyEventPressure(+fill * price / def.totalShares() * 5.0);
        ps.markDirty();
        return true;
    }

    public synchronized boolean executeMarketSell(String ticker, int qty, UUID playerUuid, MarketPersistentState ps) {
        if (!marketOpen) return false;
        StockState ss = getState(ticker);
        if (ss == null) return false;
        double price = ss.getCurrentPrice();
        double tax   = price * qty * QuantCraftConfig.getTaxRate();
        PlayerPortfolio p = ps.getPortfolio(playerUuid);
        if (!p.sell(ticker, qty, price)) return false;
        p.deductBalance(tax);
        StockDefinition def = StockRegistry.get(ticker);
        ss.adjustSharesHeld(-qty);
        ss.getCandleHistory().recordTrade(price, qty);
        ss.applyEventPressure(-qty * price / def.totalShares() * 5.0);
        ps.markDirty();
        return true;
    }

    // ── Short position processing ─────────────────────────────────────────────
    private void processShortPositions(MinecraftServer server, MarketPersistentState ps) {
        for (var entry : new HashMap<>(ps.getAllShortPositions()).entrySet()) {
            UUID uuid = entry.getKey();
            for (ShortPosition pos : new ArrayList<>(entry.getValue())) {
                try {
                    StockState ss = states.get(pos.getTicker());
                    if (ss == null) continue;
                    double price = ss.getCurrentPrice();
                    pos.addFee(price * pos.getShares() * 0.002);
                    if (marketOpen && pos.isMarginCalled(price)) {
                        forceCloseShort(uuid, pos, price, server, ps);
                    }
                } catch (Exception ex) {
                    QuantCraftMod.LOGGER.error("[QuantCraft] Short processing error for {}: {}", uuid, ex.getMessage());
                }
            }
        }
    }

    private void forceCloseShort(UUID uuid, ShortPosition pos, double price,
                                  MinecraftServer server, MarketPersistentState ps) {
        double buyback = price * pos.getShares();
        PlayerPortfolio port = ps.getPortfolio(uuid);
        double deducted = Math.min(port.getBalance(), buyback);
        port.deductBalance(deducted);
        double loss     = (price - pos.getOpenPrice()) * pos.getShares() + pos.getAccruedFee();
        double returned = Math.max(0, pos.getMarginReserve() - loss);
        port.addBalance(returned);
        StockState ss = states.get(pos.getTicker());
        if (ss != null) {
            LiquidityBot bot = bots.get(pos.getTicker());
            if (bot != null) bot.setShareReserve(bot.getShareReserve() + pos.getShares());
            ss.adjustSharesHeld(-pos.getShares());
        }
        ps.removeShort(uuid, pos);
        ps.markDirty();
        String msg = String.format("§c§lMARGIN CALL: §fYour SHORT §e%s§f was force-closed. Loss: §c%.1f¢",
                pos.getTicker(), loss);
        notify(server, uuid, msg);
    }

    // ── Dividend payouts ──────────────────────────────────────────────────────
    // 3 in-game days = 3 × 24000 = 72000 game ticks
    private static final long DIVIDEND_INTERVAL_TICKS = 72_000L;
    // Minimum holding period in market ticks before dividends are paid
    private static final long DIVIDEND_HOLDING_PERIOD = 60L;

    private void payDividendsIfDue(MinecraftServer server, MarketPersistentState ps) {
        long now = server.getOverworld().getTime();
        if (now - ps.getLastDividendPayoutTick() < DIVIDEND_INTERVAL_TICKS) return;

        boolean anyPaid = false;
        for (var entry : ps.getAllPortfolios().entrySet()) {
            UUID uuid = entry.getKey();
            PlayerPortfolio port = entry.getValue();
            if (port.getHoldings().isEmpty()) continue;
            try {
                StringBuilder sb = new StringBuilder();
                double total = 0;
                double taxRate = QuantCraftConfig.getTaxRate();
                for (var holding : port.getHoldings().entrySet()) {
                    String tk    = holding.getKey();
                    int    qty   = holding.getValue();
                    long   acq   = port.getAcquiredAtTick(tk);
                    if (marketTickCount - acq < DIVIDEND_HOLDING_PERIOD) continue;
                    double rate  = StockRegistry.getDividendRate(tk);
                    if (rate == 0) continue;
                    StockState ss = states.get(tk);
                    if (ss == null) continue;
                    double gross  = qty * ss.getCurrentPrice() * rate * getBlendedDividendMult();
                    if (gross < 0.01) continue;
                    double tax    = gross * taxRate;
                    double payout = gross - tax;
                    port.addBalance(payout);
                    total += payout;
                    sb.append(String.format("§a§lDIVIDEND PAID: §f%.2f¢ §7(%s ×%d, %.0f%% withheld)\n",
                            payout, tk, qty, taxRate * 100));
                }
                if (total > 0) {
                    String lines = sb.toString().trim();
                    for (String line : lines.split("\n"))
                        notify(server, uuid, line);
                    anyPaid = true;
                }
            } catch (Exception ex) {
                QuantCraftMod.LOGGER.error("[QuantCraft] Dividend error for {}: {}", uuid, ex.getMessage());
            }
        }

        ps.setLastDividendPayoutTick(now);
        ps.markDirty();
        if (anyPaid) {
            server.getPlayerManager().broadcast(
                    net.minecraft.text.Text.literal("§6§lDividend payout complete. Check your balance."), false);
        }
    }

    // ── Market cycle (season) ─────────────────────────────────────────────────
    private void tickSeason(MinecraftServer server, MarketPersistentState ps) {
        long now = server.getOverworld().getTime();
        if (seasonStartTick < 0) {
            // First-run initialisation
            seasonStartTick = now;
            ps.setCyclePhaseStartTick(now);
            ps.setCyclePhaseIndex(currentSeason.ordinal());
            return;
        }
        long elapsed = now - seasonStartTick;
        if (elapsed >= PHASE_LENGTH_TICKS) {
            MarketSeason next = currentSeason.next();
            currentSeason   = next;
            seasonStartTick = now;
            ps.setCyclePhaseIndex(next.ordinal());
            ps.setCyclePhaseStartTick(now);
            server.getPlayerManager().broadcast(
                    net.minecraft.text.Text.literal(next.transitionMessage), false);
            pushSeasonStatus();
        }
    }

    private static double lerp(double a, double b, double t) { return a + (b - a) * t; }

    public MarketSeason getCurrentSeason()   { return currentSeason; }
    public long         getSeasonStartTick() { return seasonStartTick; }

    public void forceSetSeason(MarketSeason season, long nowTick, MarketPersistentState ps) {
        currentSeason   = season;
        seasonStartTick = nowTick;
        ps.setCyclePhaseIndex(season.ordinal());
        ps.setCyclePhaseStartTick(nowTick);
    }

    /** Returns ticks elapsed in the current phase (based on overworld time). */
    public long getSeasonElapsedTicks(MinecraftServer server) {
        return seasonStartTick < 0 ? 0 : server.getOverworld().getTime() - seasonStartTick;
    }

    public double getBlendedDividendMult() {
        if (seasonBlendFactor == 0.0) return currentSeason.dividendMult;
        return lerp(currentSeason.dividendMult, currentSeason.next().dividendMult, seasonBlendFactor);
    }

    // ── Event system ─────────────────────────────────────────────────────────
    private final Map<MarketSector, Integer> sectorEventCounts = new EnumMap<>(MarketSector.class);
    private static final int MAX_SECTOR_EVENTS_PER_TICK = 20;

    public void fireEvent(WorldMarketEvent event, String optTicker) {
        if (!QuantCraftConfig.isEventPressureEnabled()) return;
        switch (event) {
            // Supply-side: player adds supply → price drops (educational: supply up = price down)
            case PLAYER_MINED_ORE      -> pressureSectorCapped(MarketSector.MINING,   -1.5);
            case PLAYER_MINED_WOOD     -> pressureSectorCapped(MarketSector.LUMBER,   -1.0);
            case PLAYER_HARVESTED_CROP -> pressureSectorCapped(MarketSector.AGRARIAN, -0.8);
            // Mob kills: supply of mob drops increases → Livestock down, Arcane reagent demand up
            case PLAYER_KILLED_MOB     -> { pressureSectorCapped(MarketSector.ARCANE, +0.5); pressureSectorCapped(MarketSector.LIVESTOCK, -0.5); }
            // Rare events: meaningful but not market-breaking
            case PLAYER_KILLED_BOSS    -> { pressureSector(MarketSector.ARCANE, +8.0); pressureSector(MarketSector.MINING, +3.0); pushNews("BREAKING: Boss slain — Arcane demand surges!"); }
            case PLAYER_ENTERED_NETHER -> { pressureSector(MarketSector.ARCANE, +2.0); pressureSector(MarketSector.MINING, +1.0); }
            // Weather & world events: moderate, spread across sectors
            case RAID_OCCURRED         -> { pressureSector(MarketSector.AGRARIAN, -5.0); pressureSector(MarketSector.MANUFACTURED, +4.0); pressureSector(MarketSector.LIVESTOCK, -2.0); pushNews("Raid! Agrarian drops, Manufacturing surges."); }
            case THUNDER_STORM         -> { pressureSector(MarketSector.MINING, +2.0); pressureSector(MarketSector.LUMBER, +1.5); pressureSector(MarketSector.ARCANE, +1.5); pressureSector(MarketSector.AGRARIAN, -1.0); }
            case CLEAR_WEATHER         -> { pressureSector(MarketSector.AGRARIAN, +2.0); pressureSector(MarketSector.LIVESTOCK, +1.5); pressureSector(MarketSector.LUMBER, +1.0); }
            case FULL_MOON             -> { pressureSector(MarketSector.ARCANE, +4.0); pressureSector(MarketSector.LIVESTOCK, -1.0); pushNews("Full moon rises — Arcane markets stir."); }
            // Player trading signals demand — scaled by stock's base price so cheap stocks don't spike 5% per trade
            case PLAYER_BOUGHT_STOCK   -> { if (optTicker != null) { StockDefinition d = StockRegistry.get(optTicker); pressureTicker(optTicker, d != null ? +0.3 * d.volatility() * (d.basePrice() / 10.0) : +0.5); } }
            case PLAYER_SOLD_STOCK     -> { if (optTicker != null) { StockDefinition d = StockRegistry.get(optTicker); pressureTicker(optTicker, d != null ? -0.3 * d.volatility() * (d.basePrice() / 10.0) : -0.5); } }
            // Admin overrides
            case ADMIN_SECTOR_CRASH    -> { if (optTicker != null) try { pressureSector(MarketSector.valueOf(optTicker), -50.0); } catch (Exception ignored) {} }
            case ADMIN_SECTOR_BOOM     -> { if (optTicker != null) try { pressureSector(MarketSector.valueOf(optTicker), +50.0); } catch (Exception ignored) {} }
        }
    }

    public void resetEventCounts() { sectorEventCounts.clear(); }

    private void pressureSectorCapped(MarketSector sector, double delta) {
        int count = sectorEventCounts.getOrDefault(sector, 0);
        if (count >= MAX_SECTOR_EVENTS_PER_TICK) return;
        sectorEventCounts.put(sector, count + 1);
        double scale = 1.0 - (count / (double) MAX_SECTOR_EVENTS_PER_TICK) * 0.7;
        pressureSector(sector, delta * scale);
    }

    private void pressureSector(MarketSector sector, double delta) {
        for (StockDefinition d : StockRegistry.getAll())
            if (d.sector() == sector) { StockState s = states.get(d.ticker()); if (s != null) s.applyEventPressure(delta * d.volatility()); }
    }
    private void pressureTicker(String ticker, double delta) { StockState s = states.get(ticker); if (s != null) s.applyEventPressure(delta); }
    private void pressureAll(double delta)                   { states.values().forEach(s -> s.applyEventPressure(delta)); }

    private final Random sectorRng = new Random();

    private double computeSectorModifier(MarketSector sec, boolean night, boolean full, boolean thunder, double base) {
        // Ambient drift: every sector gets mild random momentum each tick
        double ambient = sectorRng.nextGaussian() * base * 0.003;

        // Sector-specific modifiers only apply during market hours (daytime).
        if (!marketOpen) return ambient;

        double specific = switch (sec) {
            case ARCANE       -> (full ? base * 0.008 : 0);
            case MINING       -> thunder ? base * 0.006 : 0;
            case AGRARIAN     -> base * 0.002;
            case LUMBER       -> thunder ? -base * 0.002 : 0;
            case LIVESTOCK    -> base * 0.001;
            case MANUFACTURED -> 0;
        };
        return ambient + specific;
    }

    private void pushNews(String headline) {
        recentNews.add(0, headline);
        if (recentNews.size() > MAX_NEWS) recentNews.remove(recentNews.size() - 1);
    }

    private void pushSeasonStatus() {
        recentNews.removeIf(s -> s.contains("Season:"));
        recentNews.add(0, currentSeason.color + "Season: " + currentSeason.displayName);
        if (recentNews.size() > MAX_NEWS) recentNews.remove(recentNews.size() - 1);
    }

    private void notify(MinecraftServer server, UUID uuid, String msg) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p != null) p.sendMessage(Text.literal(msg), true);
    }

    // ── Admin helpers ─────────────────────────────────────────────────────────
    public void setFrozen(boolean v)                          { frozen = v; }
    public boolean isFrozen()                                 { return frozen; }
    public boolean isMarketOpen()                             { return marketOpen; }
    public void setMarketOpen(boolean v)                      { marketOpen = v; }
    public void applySectorPressure(MarketSector s, double d) { pressureSector(s, d); }
    public void applyTickerPressure(String t, double d)       { pressureTicker(t, d); }

    /**
     * Adds an active newspaper event. If a conflicting event (same ticker or same sector)
     * already exists, the new event is rejected and the player is notified.
     * If the reading player holds more than 10% of the affected ticker's float,
     * pressurePerTick is halved for this event only.
     */
    public void addActiveEvent(ActiveMarketEvent event, net.minecraft.server.network.ServerPlayerEntity player) {
        boolean conflict = activeEvents.stream().anyMatch(e -> {
            if (event.getAffectedTicker() != null && event.getAffectedTicker().equals(e.getAffectedTicker()))
                return true;
            if (event.getAffectedSector() != null && event.getAffectedSector() == e.getAffectedSector())
                return true;
            // Cross-type: new ticker event conflicts with active sector event covering that ticker's sector
            if (event.getAffectedTicker() != null && e.getAffectedSector() != null) {
                StockDefinition d = StockRegistry.get(event.getAffectedTicker());
                if (d != null && d.sector() == e.getAffectedSector()) return true;
            }
            // Cross-type: new sector event conflicts with active ticker event belonging to that sector
            if (event.getAffectedSector() != null && e.getAffectedTicker() != null) {
                StockDefinition d = StockRegistry.get(e.getAffectedTicker());
                if (d != null && d.sector() == event.getAffectedSector()) return true;
            }
            return false;
        });
        if (conflict) {
            if (player != null)
                player.sendMessage(Text.literal("§7A similar event is already active."), true);
            return;
        }
        if (player != null && event.getAffectedTicker() != null) {
            StockDefinition def = StockRegistry.get(event.getAffectedTicker());
            var ps = com.quantcraft.persistence.MarketPersistentState.getOrCreate(
                    player.getServer().getOverworld());
            if (def != null) {
                int held = ps.getPortfolio(player.getUuid()).getHolding(event.getAffectedTicker());
                if (held > def.totalShares() * 0.10) {
                    event.halfPressure();
                    player.sendMessage(Text.literal(
                            "§7Market regulators noted unusual positioning before the announcement."), true);
                }
            }
        }
        activeEvents.add(event);
        pushNews(event.getHeadline());
    }

    public List<ActiveMarketEvent> getActiveEvents() { return Collections.unmodifiableList(activeEvents); }

    public void resetToBasePrice() {
        for (StockDefinition d : StockRegistry.getAll()) {
            StockState s = states.get(d.ticker());
            if (s != null) s.updatePrice(d.basePrice());
        }
    }

    public void placeLimitOrder(LimitOrder o) {
        StockState s = states.get(o.getTicker());
        if (s != null) s.getOrderBook().addOrder(o);
    }

    public boolean cancelLimitOrder(String tk, UUID id) {
        StockState s = states.get(tk);
        return s != null && s.getOrderBook().cancelOrder(id);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────
    public long                     getMarketTickCount() { return marketTickCount; }
    public void                     setMarketTickCount(long v) { marketTickCount = v; }
    public Map<String,StockState>   getSnapshot()    { return Collections.unmodifiableMap(states); }
    public StockState               getState(String t) { return states.get(t); }
    public LiquidityBot             getBot(String t)   { return bots.get(t); }
    public Map<String,LiquidityBot> getAllBots()       { return Collections.unmodifiableMap(bots); }
    public List<String>             getHistoricalNews(){ return Collections.unmodifiableList(recentNews); }

    /** Returns active event status lines followed by the historical headline log. */
    public List<String> getRecentNews() {
        List<String> combined = new ArrayList<>();
        for (ActiveMarketEvent e : activeEvents) combined.add(e.getStatusLine());
        combined.addAll(recentNews);
        return Collections.unmodifiableList(combined);
    }
}
