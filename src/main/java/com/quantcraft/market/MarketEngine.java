package com.quantcraft.market;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.network.ModPackets;
import com.quantcraft.persistence.MarketPersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

public class MarketEngine {
    private static final MarketEngine INSTANCE = new MarketEngine();
    public static MarketEngine getInstance() { return INSTANCE; }
    private MarketEngine() {}

    private final Map<String,StockState>   states         = new LinkedHashMap<>();
    private final Map<String,LiquidityBot> bots           = new LinkedHashMap<>();
    private final Map<String,int[]>        sustainedBoosts= new HashMap<>();
    private final List<String>             recentNews     = new ArrayList<>();
    private static final int MAX_NEWS = 5;
    private boolean frozen = false;

    // ── Initialisation ───────────────────────────────────────────────────────
    public void loadState(MarketPersistentState ps) {
        states.clear(); bots.clear();
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
                int reserve = (int)(def.totalShares() * 0.20);
                bot = new LiquidityBot(def.ticker(), def.basePrice(), reserve);
            }
            bots.put(def.ticker(), bot);
            StockState ss = states.get(def.ticker());
            if (ss != null) bot.tick(ss, def);
        }
        QuantCraftMod.LOGGER.info("[QuantCraft] Loaded {} stocks.", states.size());
    }

    // ── Tick ─────────────────────────────────────────────────────────────────
    public void tick(MinecraftServer server, MarketPersistentState ps) {
        if (frozen) return;
        tickSimulated(server);
        ps.saveStates(states);
        ModPackets.broadcastMarketUpdate(server, getSnapshot());
    }

    private void tickSimulated(MinecraftServer server) {
        long    wt        = server.getOverworld().getTimeOfDay();
        boolean isNight   = (wt % 24000L) > 13000L;
        boolean isFullMoon= (server.getOverworld().getMoonPhase() == 0);
        boolean isThunder = server.getOverworld().isThundering();

        for (StockDefinition def : StockRegistry.getAll()) {
            StockState state = states.get(def.ticker());
            if (state == null) continue;

            state.getCandleHistory().openNewCandle(state.getCurrentPrice());
            processLimitOrders(def, state, server);

            LiquidityBot bot = bots.get(def.ticker());
            if (bot != null) bot.tick(state, def);

            double eventPressure = state.consumeEventPressure();
            double sectorMod = computeSectorModifier(def.sector(), isNight, isFullMoon, isThunder, def.basePrice());

            int[] sustained = sustainedBoosts.get(def.ticker());
            if (sustained != null) {
                eventPressure += 3.0;
                sustained[0]--;
                if (sustained[0] <= 0) sustainedBoosts.remove(def.ticker());
            }

            float mult = QuantCraftConfig.getGlobalVolatilityMultiplier();
            if (eventPressure != 0 || sectorMod != 0)
                state.updatePrice(state.getCurrentPrice() + (eventPressure + sectorMod) * 0.3 * mult);

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
                if (order.getSide() == LimitOrder.Side.BUY) {
                    int avail = state.getAvailableShares(def.totalShares());
                    int fill  = Math.min(qty, avail);
                    if (fill > 0) {
                        portfolio.addShares(def.ticker(), fill);
                        state.adjustSharesHeld(+fill);
                        state.getCandleHistory().recordTrade(price, fill);
                        fireEvent(WorldMarketEvent.PLAYER_BOUGHT_STOCK, def.ticker());
                        notify(server, order.getPlayerUuid(),
                                String.format("§aLimit BUY filled: %d %s @ §e%.1f¢", fill, def.ticker(), price));
                    }
                } else {
                    int owned = portfolio.getHolding(def.ticker());
                    int fill  = Math.min(qty, owned);
                    if (fill > 0) {
                        portfolio.removeShares(def.ticker(), fill);
                        portfolio.addCoins(price * fill);
                        state.adjustSharesHeld(-fill);
                        state.getCandleHistory().recordTrade(price, fill);
                        fireEvent(WorldMarketEvent.PLAYER_SOLD_STOCK, def.ticker());
                        notify(server, order.getPlayerUuid(),
                                String.format("§aLimit SELL filled: %d %s @ §e%.1f¢", fill, def.ticker(), price));
                    }
                }
            }
        }
        ps.markDirty();
    }

    // ── Market order execution ────────────────────────────────────────────────
    public boolean executeMarketBuy(String ticker, int qty, UUID playerUuid, MarketPersistentState ps) {
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = getState(ticker);
        if (def == null || ss == null) return false;
        int avail = ss.getAvailableShares(def.totalShares());
        int fill  = Math.min(qty, avail);
        if (fill <= 0) return false;
        double price = ss.getCurrentPrice();
        PlayerPortfolio p = ps.getPortfolio(playerUuid);
        if (!p.buy(ticker, fill, price)) return false;
        ss.adjustSharesHeld(+fill);
        ss.getCandleHistory().recordTrade(price, fill);
        fireEvent(WorldMarketEvent.PLAYER_BOUGHT_STOCK, ticker);
        ps.markDirty();
        return true;
    }

    public boolean executeMarketSell(String ticker, int qty, UUID playerUuid, MarketPersistentState ps) {
        StockState ss = getState(ticker);
        if (ss == null) return false;
        double price = ss.getCurrentPrice();
        PlayerPortfolio p = ps.getPortfolio(playerUuid);
        if (!p.sell(ticker, qty, price)) return false;
        ss.adjustSharesHeld(-qty);
        ss.getCandleHistory().recordTrade(price, qty);
        fireEvent(WorldMarketEvent.PLAYER_SOLD_STOCK, ticker);
        ps.markDirty();
        return true;
    }

    // ── Event system ─────────────────────────────────────────────────────────
    public void fireEvent(WorldMarketEvent event, String optTicker) {
        if (!QuantCraftConfig.isEventPressureEnabled()) return;
        switch (event) {
            case PLAYER_MINED_ORE      -> pressureSector(MarketSector.MINING,   -2.5);
            case PLAYER_MINED_WOOD     -> pressureSector(MarketSector.LUMBER,   -1.5);
            case PLAYER_HARVESTED_CROP -> pressureSector(MarketSector.AGRARIAN, -1.0);
            case PLAYER_KILLED_MOB     -> { pressureSector(MarketSector.ARCANE, -1.5); pressureSector(MarketSector.LIVESTOCK, -1.0); }
            case PLAYER_KILLED_BOSS    -> { pressureSector(MarketSector.ARCANE, -20.0); pushNews("BREAKING: Boss slain — Arcane sector in freefall!"); }
            case PLAYER_ENTERED_NETHER -> pressureSector(MarketSector.ARCANE,   -3.0);
            case RAID_OCCURRED         -> { pressureSector(MarketSector.AGRARIAN, -12.0); pressureSector(MarketSector.MANUFACTURED, +8.0); pushNews("Raid! Agrarian crashes, Manufacturing surges."); }
            case THUNDER_STORM         -> { pressureSector(MarketSector.MINING, +2.5); pressureSector(MarketSector.LUMBER, +1.5); }
            case CLEAR_WEATHER         -> pressureAll(+0.5);
            case FULL_MOON             -> pressureSector(MarketSector.ARCANE,   -5.0);
            case PLAYER_BOUGHT_STOCK   -> { if (optTicker != null) pressureTicker(optTicker, +2.0); }
            case PLAYER_SOLD_STOCK     -> { if (optTicker != null) pressureTicker(optTicker, -2.0); }
            case ADMIN_SECTOR_CRASH    -> { if (optTicker != null) try { pressureSector(MarketSector.valueOf(optTicker), -50.0); } catch (Exception ignored) {} }
            case ADMIN_SECTOR_BOOM     -> { if (optTicker != null) try { pressureSector(MarketSector.valueOf(optTicker), +50.0); } catch (Exception ignored) {} }
        }
    }

    private void pressureSector(MarketSector sector, double delta) {
        for (StockDefinition d : StockRegistry.getAll())
            if (d.sector() == sector) { StockState s = states.get(d.ticker()); if (s != null) s.applyEventPressure(delta * d.volatility()); }
    }
    private void pressureTicker(String ticker, double delta) { StockState s = states.get(ticker); if (s != null) s.applyEventPressure(delta); }
    private void pressureAll(double delta)                   { states.values().forEach(s -> s.applyEventPressure(delta)); }

    private double computeSectorModifier(MarketSector sec, boolean night, boolean full, boolean thunder, double base) {
        return switch (sec) {
            case ARCANE   -> (night ? base * 0.008 : 0) + (full ? -base * 0.015 : 0);
            case MINING   -> thunder ? base * 0.006 : 0;
            case AGRARIAN -> thunder ? -base * 0.004 : 0;
            default       -> 0;
        };
    }

    private void pushNews(String headline) {
        recentNews.add(0, headline);
        if (recentNews.size() > MAX_NEWS) recentNews.remove(recentNews.size() - 1);
    }

    public void pushNewsPublic(String h) { pushNews(h); }

    private void notify(MinecraftServer server, UUID uuid, String msg) {
        ServerPlayerEntity p = server.getPlayerManager().getPlayer(uuid);
        if (p != null) p.sendMessage(Text.literal(msg), true);
    }

    // ── Admin helpers ─────────────────────────────────────────────────────────
    public void setFrozen(boolean v)                            { frozen = v; }
    public boolean isFrozen()                                   { return frozen; }
    public void applySectorPressure(MarketSector s, double d)   { pressureSector(s, d); }
    public void applyTickerPressure(String t, double d)         { pressureTicker(t, d); }
    public void applyTickerSustainedBoom(String t, int ticks)   { sustainedBoosts.put(t, new int[]{ticks}); }

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
    public Map<String,StockState>   getSnapshot()   { return Collections.unmodifiableMap(states); }
    public StockState               getState(String t){ return states.get(t); }
    public LiquidityBot             getBot(String t) { return bots.get(t); }
    public Map<String,LiquidityBot> getAllBots()     { return Collections.unmodifiableMap(bots); }
    public List<String>             getRecentNews()  { return Collections.unmodifiableList(recentNews); }
}
