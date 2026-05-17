package com.quantcraft.market;

import java.util.Random;
import java.util.UUID;

public class LiquidityBot {
    private final String ticker;
    private double cashReserve;
    private int    shareReserve;
    private double targetPriceMid;
    private double spreadPct    = 0.04;
    private int    orderSize    = 10;
    private int    ordersPerSide = 3;
    private boolean enabled     = true;

    private static final UUID BOT_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final Random random = new Random();

    public LiquidityBot(String ticker, double initialPrice, int initialShares) {
        this.ticker        = ticker;
        this.targetPriceMid = initialPrice;
        this.shareReserve  = initialShares;
        this.cashReserve   = initialPrice * initialShares * 2.0;
    }

    public void tick(StockState state, StockDefinition def) {
        if (!enabled) return;
        if (!MarketEngine.getInstance().isMarketOpen()) return;
        OrderBook book  = state.getOrderBook();
        double    price = state.getCurrentPrice();
        for (LimitOrder o : book.getAll()) {
            if (!o.getPlayerUuid().equals(BOT_UUID)) continue;
            if (o.getSide() == LimitOrder.Side.BUY)  cashReserve  += o.getLimitPrice() * o.getRemainingQty();
            else                                      shareReserve += o.getRemainingQty();
        }
        book.cancelAllForPlayer(BOT_UUID);

        double base = def.basePrice();
        targetPriceMid = targetPriceMid + (base - targetPriceMid) * def.meanReversionStrength() * 0.015
                + (random.nextGaussian() * def.volatility() * 0.012 * price)
                + (random.nextGaussian() * base * 0.005);
        targetPriceMid = Math.max(base * 0.3, Math.min(base * 3.0, targetPriceMid));

        double spread = Math.max(0.5, price * spreadPct);
        for (int i = 1; i <= ordersPerSide; i++) {
            double bid = Math.max(1.0, targetPriceMid - spread * i);
            double ask = targetPriceMid + spread * i;
            if (cashReserve >= bid * orderSize) {
                book.addOrder(new LimitOrder(BOT_UUID, ticker, LimitOrder.Side.BUY,  orderSize, bid, 0));
                cashReserve -= bid * orderSize;
            }
            if (shareReserve >= orderSize) {
                book.addOrder(new LimitOrder(BOT_UUID, ticker, LimitOrder.Side.SELL, orderSize, ask, 0));
                shareReserve -= orderSize;
            }
        }
    }

    public void onOrderFilled(LimitOrder order, double fillPrice) {
        // Cash/shares were already reserved in tick() when the order was placed.
        // On fill we only record the conversion (reserved asset → acquired asset).
        if (order.getSide() == LimitOrder.Side.BUY) {
            shareReserve += order.getFilledQty();
        } else {
            cashReserve += fillPrice * order.getFilledQty();
        }
    }

    public static UUID getBotUuid()           { return BOT_UUID; }
    public boolean     isEnabled()            { return enabled; }
    public void        setEnabled(boolean v)  { enabled = v; }
    public double      getCashReserve()       { return cashReserve; }
    public int         getShareReserve()      { return shareReserve; }
    public void        setCashReserve(double v){ cashReserve = v; }
    public void        setShareReserve(int v) { shareReserve = v; }
    public double      getTargetPriceMid()    { return targetPriceMid; }
    public void        setTargetPriceMid(double v){ targetPriceMid = v; }
    public double      getSpreadPct()         { return spreadPct; }
    public void        setSpreadPct(double v) { spreadPct = v; }
    public int         getOrderSize()         { return orderSize; }
    public void        setOrderSize(int v)    { orderSize = v; }
    public String      getTicker()            { return ticker; }
}
