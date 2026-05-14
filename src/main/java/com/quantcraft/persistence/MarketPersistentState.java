package com.quantcraft.persistence;

import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.market.*;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.*;

public class MarketPersistentState extends PersistentState {
    private static final String KEY = "quantcraft_market";

    private final Map<String,StockState>          stockStates          = new LinkedHashMap<>();
    private final Map<UUID,PlayerPortfolio>        portfolios           = new HashMap<>();
    final Map<String,LiquidityBot>                 savedBots            = new HashMap<>();
    private final List<ActiveMarketEvent>          activeEvents         = new ArrayList<>();
    private final Map<UUID,Double>                 dailyEarnings        = new HashMap<>();
    private long                                   lastResetDayEpoch    = -1;
    private final Map<String,Double>               closingPrices        = new LinkedHashMap<>();
    private long                                   lastDividendPayoutMillis = 0L;
    private long                                   marketTickCount      = 0L;
    private final Map<UUID,List<ShortPosition>>    shortPositions       = new HashMap<>();

    public static MarketPersistentState getOrCreate(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(MarketPersistentState::new, MarketPersistentState::fromNbt, null), KEY);
    }

    private static MarketPersistentState fromNbt(NbtCompound nbt) {
        MarketPersistentState state = new MarketPersistentState();

        if (nbt.contains("stocks")) {
            NbtCompound stocks = nbt.getCompound("stocks");
            NbtCompound held   = nbt.contains("sharesHeld") ? nbt.getCompound("sharesHeld") : new NbtCompound();
            for (String tk : stocks.getKeys()) {
                NbtCompound s     = stocks.getCompound(tk);
                double cur        = s.getDouble("current");
                double prev       = s.getDouble("previous");
                int    sharesHeld = held.contains(tk) ? held.getInt(tk) : 0;
                StockState ss     = new StockState(tk, cur);
                NbtList hist      = s.getList("history", NbtElement.DOUBLE_TYPE);
                List<Double> h    = new ArrayList<>();
                for (NbtElement el : hist) h.add(((NbtDouble) el).doubleValue());
                ss.restoreHistory(h, prev, sharesHeld);
                state.stockStates.put(tk, ss);
            }
        }

        if (nbt.contains("portfolios")) {
            NbtCompound pNbts = nbt.getCompound("portfolios");
            for (String uid : pNbts.getKeys()) {
                UUID            uuid = UUID.fromString(uid);
                NbtCompound     pn   = pNbts.getCompound(uid);
                PlayerPortfolio p    = new PlayerPortfolio(pn.getDouble("balance"));
                NbtCompound     hld  = pn.getCompound("holdings");
                NbtCompound     acq  = pn.contains("acquiredAt") ? pn.getCompound("acquiredAt") : new NbtCompound();
                for (String tk : hld.getKeys()) {
                    int q = hld.getInt(tk);
                    if (q > 0) {
                        long tick = acq.contains(tk) ? acq.getLong(tk) : 0L;
                        p.addSharesAt(tk, q, tick);
                    }
                }
                state.portfolios.put(uuid, p);
            }
        }

        if (nbt.contains("bots")) {
            NbtCompound botsNbt = nbt.getCompound("bots");
            for (String tk : botsNbt.getKeys()) {
                NbtCompound     bn  = botsNbt.getCompound(tk);
                StockDefinition def = StockRegistry.get(tk);
                if (def == null) continue;
                LiquidityBot bot = new LiquidityBot(tk, bn.getDouble("target"),
                        (int)(def.totalShares() * 0.20));
                bot.setCoinReserve(bn.getDouble("coins"));
                bot.setShareReserve(bn.getInt("shares"));
                bot.setTargetPriceMid(bn.getDouble("target"));
                bot.setSpreadPct(bn.getDouble("spread"));
                bot.setEnabled(bn.getBoolean("enabled"));
                state.savedBots.put(tk, bot);
            }
        }

        if (nbt.contains("activeEvents")) {
            NbtList list = nbt.getList("activeEvents", NbtElement.COMPOUND_TYPE);
            for (NbtElement el : list) {
                NbtCompound en = (NbtCompound) el;
                String       headline        = en.getString("headline");
                String       ticker          = en.contains("ticker") ? en.getString("ticker") : null;
                MarketSector sector          = en.contains("sector") ? MarketSector.valueOf(en.getString("sector")) : null;
                double       pressurePerTick = en.getDouble("pressure");
                int          ticks           = en.getInt("ticks");
                long         started         = en.getLong("started");
                state.activeEvents.add(new ActiveMarketEvent(headline, ticker, sector, pressurePerTick, ticks, started));
            }
        }

        if (nbt.contains("dailyEarnings")) {
            NbtCompound en = nbt.getCompound("dailyEarnings");
            for (String uid : en.getKeys()) {
                try { state.dailyEarnings.put(UUID.fromString(uid), en.getDouble(uid)); }
                catch (IllegalArgumentException ignored) {}
            }
        }
        state.lastResetDayEpoch = nbt.getLong("lastResetDay");

        if (nbt.contains("closingPrices")) {
            NbtCompound cp = nbt.getCompound("closingPrices");
            for (String tk : cp.getKeys()) state.closingPrices.put(tk, cp.getDouble(tk));
        }

        state.lastDividendPayoutMillis = nbt.contains("lastDividendMillis")
                ? nbt.getLong("lastDividendMillis")
                : (nbt.contains("lastDividendTick") ? System.currentTimeMillis() : 0L);
        state.marketTickCount = nbt.getLong("marketTickCount");

        if (nbt.contains("shortPositions")) {
            NbtCompound spMap = nbt.getCompound("shortPositions");
            for (String uid : spMap.getKeys()) {
                UUID    uuid = UUID.fromString(uid);
                NbtList list = spMap.getList(uid, NbtElement.COMPOUND_TYPE);
                List<ShortPosition> positions = new ArrayList<>();
                for (NbtElement el : list) {
                    NbtCompound sn  = (NbtCompound) el;
                    ShortPosition sp = new ShortPosition(
                            uuid,
                            sn.getString("ticker"),
                            sn.getInt("shares"),
                            sn.getDouble("openPrice"),
                            sn.getDouble("margin"),
                            sn.getLong("openedAt"));
                    sp.addFee(sn.getDouble("accruedFee"));
                    positions.add(sp);
                }
                if (!positions.isEmpty()) state.shortPositions.put(uuid, positions);
            }
        }

        if (nbt.contains("limitOrders")) {
            NbtCompound ordersNbt = nbt.getCompound("limitOrders");
            for (String tk : ordersNbt.getKeys()) {
                StockState ss = state.stockStates.get(tk);
                if (ss == null) continue;
                NbtList list = ordersNbt.getList(tk, NbtElement.COMPOUND_TYPE);
                for (NbtElement el : list) {
                    NbtCompound on = (NbtCompound) el;
                    LimitOrder o = new LimitOrder(
                            UUID.fromString(on.getString("player")), tk,
                            LimitOrder.Side.valueOf(on.getString("side")),
                            on.getInt("qty"), on.getDouble("price"), on.getLong("tick"));
                    ss.getOrderBook().addOrder(o);
                }
            }
        }

        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound stocks     = new NbtCompound();
        NbtCompound sharesHeld = new NbtCompound();
        for (var e : stockStates.entrySet()) {
            StockState  s  = e.getValue();
            NbtCompound sn = new NbtCompound();
            sn.putDouble("current",  s.getCurrentPrice());
            sn.putDouble("previous", s.getPreviousPrice());
            NbtList hist = new NbtList();
            for (double p : s.getPriceHistory()) hist.add(NbtDouble.of(p));
            sn.put("history", hist);
            stocks.put(e.getKey(), sn);
            sharesHeld.putInt(e.getKey(), s.getSharesHeld());
        }
        nbt.put("stocks",     stocks);
        nbt.put("sharesHeld", sharesHeld);

        NbtCompound pNbts = new NbtCompound();
        for (var e : portfolios.entrySet()) {
            PlayerPortfolio p  = e.getValue();
            NbtCompound     pn = new NbtCompound();
            pn.putDouble("balance", p.getCoinBalance());
            NbtCompound hld = new NbtCompound();
            p.getHoldings().forEach((tk, q) -> hld.putInt(tk, q));
            pn.put("holdings", hld);
            NbtCompound acq = new NbtCompound();
            p.getAllAcquiredTicks().forEach(acq::putLong);
            pn.put("acquiredAt", acq);
            pNbts.put(e.getKey().toString(), pn);
        }
        nbt.put("portfolios", pNbts);

        NbtCompound ordersNbt = new NbtCompound();
        for (var e : stockStates.entrySet()) {
            NbtList list = new NbtList();
            for (LimitOrder o : e.getValue().getOrderBook().getAll()) {
                NbtCompound on = new NbtCompound();
                on.putString("player", o.getPlayerUuid().toString());
                on.putString("side",   o.getSide().name());
                on.putInt("qty",       o.getRemainingQty());
                on.putDouble("price",  o.getLimitPrice());
                on.putLong("tick",     o.getPlacedTick());
                list.add(on);
            }
            if (!list.isEmpty()) ordersNbt.put(e.getKey(), list);
        }
        nbt.put("limitOrders", ordersNbt);

        NbtCompound botsNbt = new NbtCompound();
        MarketEngine.getInstance().getAllBots().forEach((tk, bot) -> {
            NbtCompound bn = new NbtCompound();
            bn.putDouble("coins",    bot.getCoinReserve());
            bn.putInt("shares",      bot.getShareReserve());
            bn.putDouble("target",   bot.getTargetPriceMid());
            bn.putDouble("spread",   bot.getSpreadPct());
            bn.putBoolean("enabled", bot.isEnabled());
            botsNbt.put(tk, bn);
        });
        nbt.put("bots", botsNbt);

        NbtList eventsNbt = new NbtList();
        MarketEngine.getInstance().getActiveEvents().forEach(e -> {
            NbtCompound en = new NbtCompound();
            en.putString("headline", e.getHeadline());
            if (e.getAffectedTicker() != null) en.putString("ticker",  e.getAffectedTicker());
            if (e.getAffectedSector() != null) en.putString("sector",  e.getAffectedSector().name());
            en.putDouble("pressure", e.getPressurePerTick());
            en.putInt("ticks",       e.getTicksRemaining());
            en.putLong("started",    e.getStartedAtTick());
            eventsNbt.add(en);
        });
        nbt.put("activeEvents", eventsNbt);

        NbtCompound earningsNbt = new NbtCompound();
        dailyEarnings.forEach((uuid, amount) -> earningsNbt.putDouble(uuid.toString(), amount));
        nbt.put("dailyEarnings", earningsNbt);
        nbt.putLong("lastResetDay", lastResetDayEpoch);

        NbtCompound cpNbt = new NbtCompound();
        closingPrices.forEach(cpNbt::putDouble);
        nbt.put("closingPrices", cpNbt);

        nbt.putLong("lastDividendMillis", lastDividendPayoutMillis);
        nbt.putLong("marketTickCount", MarketEngine.getInstance().getMarketTickCount());

        NbtCompound spMap = new NbtCompound();
        shortPositions.forEach((uuid, list) -> {
            NbtList spList = new NbtList();
            for (ShortPosition sp : list) {
                NbtCompound sn = new NbtCompound();
                sn.putString("ticker",     sp.getTicker());
                sn.putInt("shares",        sp.getShares());
                sn.putDouble("openPrice",  sp.getOpenPrice());
                sn.putDouble("margin",     sp.getMarginReserve());
                sn.putLong("openedAt",     sp.getOpenedAtTick());
                sn.putDouble("accruedFee", sp.getAccruedFee());
                spList.add(sn);
            }
            if (!spList.isEmpty()) spMap.put(uuid.toString(), spList);
        });
        nbt.put("shortPositions", spMap);

        return nbt;
    }

    // ── Stock states ──────────────────────────────────────────────────────────
    public void saveStates(Map<String,StockState> snapshot) {
        stockStates.clear(); stockStates.putAll(snapshot); markDirty();
    }
    public StockState getStockState(String tk) { return stockStates.get(tk); }

    // ── Portfolios ────────────────────────────────────────────────────────────
    public PlayerPortfolio getPortfolio(UUID uuid) {
        return portfolios.computeIfAbsent(uuid, id -> new PlayerPortfolio(QuantCraftConfig.getStartingBalance()));
    }
    public Map<UUID,PlayerPortfolio> getAllPortfolios() { return Collections.unmodifiableMap(portfolios); }
    public void resetAllPortfolios() { portfolios.clear(); markDirty(); }

    // ── Bots ──────────────────────────────────────────────────────────────────
    public LiquidityBot getLiquidityBot(String ticker) { return savedBots.get(ticker); }

    // ── Active events ─────────────────────────────────────────────────────────
    public List<ActiveMarketEvent> getActiveEvents() { return Collections.unmodifiableList(activeEvents); }

    // ── Daily commodity earnings ──────────────────────────────────────────────
    public double getDailyExchangeEarnings(UUID uuid) {
        return dailyEarnings.getOrDefault(uuid, 0.0);
    }
    public void addDailyExchangeEarnings(UUID uuid, double amount) {
        dailyEarnings.merge(uuid, amount, Double::sum);
        markDirty();
    }
    public void checkDailyReset() {
        long today = LocalDate.now(ZoneOffset.UTC).toEpochDay();
        if (today != lastResetDayEpoch) {
            dailyEarnings.clear();
            lastResetDayEpoch = today;
            markDirty();
        }
    }

    // ── Closing prices ────────────────────────────────────────────────────────
    public Map<String,Double> getClosingPrices() { return Collections.unmodifiableMap(closingPrices); }
    public void setClosingPrices(Map<String,Double> prices) {
        closingPrices.clear(); closingPrices.putAll(prices); markDirty();
    }

    // ── Dividend payout timing ───────────────────────────────────────────────
    public long getLastDividendPayoutMillis()            { return lastDividendPayoutMillis; }
    public void setLastDividendPayoutMillis(long millis) { lastDividendPayoutMillis = millis; markDirty(); }

    // ── Market tick count (for holding period tracking) ──────────────────────
    public long getMarketTickCount()          { return marketTickCount; }
    public void setMarketTickCount(long v)    { marketTickCount = v; markDirty(); }

    // ── Short positions ───────────────────────────────────────────────────────
    public Map<UUID,List<ShortPosition>> getAllShortPositions() {
        return Collections.unmodifiableMap(shortPositions);
    }
    public List<ShortPosition> getShorts(UUID uuid) {
        return shortPositions.getOrDefault(uuid, Collections.emptyList());
    }
    public void addShort(UUID uuid, ShortPosition sp) {
        shortPositions.computeIfAbsent(uuid, k -> new ArrayList<>()).add(sp);
        markDirty();
    }
    public void removeShort(UUID uuid, ShortPosition sp) {
        List<ShortPosition> list = shortPositions.get(uuid);
        if (list != null) { list.remove(sp); if (list.isEmpty()) shortPositions.remove(uuid); }
        markDirty();
    }
}
