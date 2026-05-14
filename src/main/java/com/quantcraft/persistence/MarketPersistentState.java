package com.quantcraft.persistence;

import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.market.*;
import net.minecraft.nbt.*;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import java.util.*;

public class MarketPersistentState extends PersistentState {
    private static final String KEY = "quantcraft_market";

    private final Map<String,StockState>    stockStates = new LinkedHashMap<>();
    private final Map<UUID,PlayerPortfolio> portfolios  = new HashMap<>();
    final Map<String,LiquidityBot>          savedBots   = new HashMap<>();

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
                for (String tk : hld.getKeys()) {
                    int q = hld.getInt(tk);
                    if (q > 0) p.addShares(tk, q);
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
            bn.putDouble("coins",   bot.getCoinReserve());
            bn.putInt("shares",     bot.getShareReserve());
            bn.putDouble("target",  bot.getTargetPriceMid());
            bn.putDouble("spread",  bot.getSpreadPct());
            bn.putBoolean("enabled", bot.isEnabled());
            botsNbt.put(tk, bn);
        });
        nbt.put("bots", botsNbt);

        return nbt;
    }

    public void saveStates(Map<String,StockState> snapshot) {
        stockStates.clear(); stockStates.putAll(snapshot); markDirty();
    }

    public StockState getStockState(String tk) { return stockStates.get(tk); }

    public PlayerPortfolio getPortfolio(UUID uuid) {
        return portfolios.computeIfAbsent(uuid, id -> new PlayerPortfolio(QuantCraftConfig.getStartingBalance()));
    }

    public Map<UUID,PlayerPortfolio> getAllPortfolios() { return Collections.unmodifiableMap(portfolios); }

    public void resetAllPortfolios() { portfolios.clear(); markDirty(); }

    public LiquidityBot getLiquidityBot(String ticker) { return savedBots.get(ticker); }
}
