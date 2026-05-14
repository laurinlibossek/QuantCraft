package com.quantcraft.market;

import java.util.UUID;

public class LimitOrder {
    public enum Side { BUY, SELL }

    private final UUID   orderId;
    private final UUID   playerUuid;
    private final String ticker;
    private final Side   side;
    private final int    quantity;
    private final double limitPrice;
    private final long   placedTick;
    private int filledQty;

    public LimitOrder(UUID playerUuid, String ticker, Side side,
                      int quantity, double limitPrice, long placedTick) {
        this.orderId    = UUID.randomUUID();
        this.playerUuid = playerUuid;
        this.ticker     = ticker;
        this.side       = side;
        this.quantity   = quantity;
        this.limitPrice = limitPrice;
        this.placedTick = placedTick;
        this.filledQty  = 0;
    }

    public int fill(int amount) {
        int can = Math.min(amount, getRemainingQty());
        filledQty += can;
        return can;
    }

    public boolean isFilled()        { return filledQty >= quantity; }
    public int getRemainingQty()     { return quantity - filledQty; }

    public boolean triggers(double marketPrice) {
        return side == Side.BUY ? marketPrice <= limitPrice : marketPrice >= limitPrice;
    }

    public UUID   getOrderId()    { return orderId; }
    public UUID   getPlayerUuid() { return playerUuid; }
    public String getTicker()     { return ticker; }
    public Side   getSide()       { return side; }
    public int    getQuantity()   { return quantity; }
    public double getLimitPrice() { return limitPrice; }
    public long   getPlacedTick() { return placedTick; }
    public int    getFilledQty()  { return filledQty; }
}
