package com.quantcraft.market;

import java.util.Random;
import java.util.UUID;

public class LiquidityBot {
    private final String ticker;
    private double coinReserve;
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
        this.coinReserve   = initialPrice * initialShares * 2.0;
    }

    public void tick(StockState state, StockDefinition def) {
        if (!enabled) return;
        OrderBook book  = state.getOrderBook();
        double    price = state.getCurrentPrice();
        for (LimitOrder o : book.getAll()) {
            if (!o.getPlayerUuid().equals(BOT_UUID)) continue;
            if (o.getSide() == LimitOrder.Side.BUY)  coinReserve  += o.getLimitPrice() * o.getRemainingQty();
            else                                      shareReserve += o.getRemainingQty();
        }
        book.cancelAllForPlayer(BOT_UUID);

        double base = def.basePrice();
        targetPriceMid = targetPriceMid + (base - targetPriceMid) * def.meanReversionStrength() * 0.5
                + (random.nextGaussian() * def.volatility() * 0.02 * price);
        targetPriceMid = Math.max(1.0, targetPriceMid);

        double spread = Math.max(0.5, price * spreadPct);
        for (int i = 1; i <= ordersPerSide; i++) {
            double bid = Math.max(1.0, targetPriceMid - spread * i);
            double ask = targetPriceMid + spread * i;
            if (coinReserve >= bid * orderSize)
                book.addOrder(new LimitOrder(BOT_UUID, ticker, LimitOrder.Side.BUY,  orderSize, bid, 0));
            if (shareReserve >= orderSize)
                book.addOrder(new LimitOrder(BOT_UUID, ticker, LimitOrder.Side.SELL, orderSize, ask, 0));
        }
    }

    public void onOrderFilled(LimitOrder order, double fillPrice) {
        if (order.getSide() == LimitOrder.Side.BUY) {
            coinReserve  -= fillPrice * order.getFilledQty();
            shareReserve += order.getFilledQty();
        } else {
            coinReserve  += fillPrice * order.getFilledQty();
            shareReserve -= order.getFilledQty();
        }
        coinReserve  = Math.max(0, coinReserve);
        shareReserve = Math.max(0, shareReserve);
    }

    public static UUID getBotUuid()           { return BOT_UUID; }
    public boolean     isEnabled()            { return enabled; }
    public void        setEnabled(boolean v)  { enabled = v; }
    public double      getCoinReserve()       { return coinReserve; }
    public int         getShareReserve()      { return shareReserve; }
    public void        setCoinReserve(double v){ coinReserve = v; }
    public void        setShareReserve(int v) { shareReserve = v; }
    public double      getTargetPriceMid()    { return targetPriceMid; }
    public void        setTargetPriceMid(double v){ targetPriceMid = v; }
    public double      getSpreadPct()         { return spreadPct; }
    public void        setSpreadPct(double v) { spreadPct = v; }
    public int         getOrderSize()         { return orderSize; }
    public void        setOrderSize(int v)    { orderSize = v; }
    public String      getTicker()            { return ticker; }
}
