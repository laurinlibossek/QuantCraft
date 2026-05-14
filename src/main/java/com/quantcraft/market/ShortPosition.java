package com.quantcraft.market;

import java.util.UUID;

public class ShortPosition {
    private final UUID   playerUuid;
    private final String ticker;
    private final int    shares;
    private final double openPrice;
    private final double marginReserve;
    private final long   openedAtTick;
    private double       accruedFee;

    public ShortPosition(UUID playerUuid, String ticker, int shares,
                         double openPrice, double marginReserve, long openedAtTick) {
        this.playerUuid    = playerUuid;
        this.ticker        = ticker;
        this.shares        = shares;
        this.openPrice     = openPrice;
        this.marginReserve = marginReserve;
        this.openedAtTick  = openedAtTick;
        this.accruedFee    = 0.0;
    }

    /** Profit when price falls, loss when price rises, minus fees. */
    public double getCurrentPnL(double currentPrice) {
        return (openPrice - currentPrice) * shares - accruedFee;
    }

    /** Margin call when unrealised loss + fees exceeds 80% of the locked margin. */
    public boolean isMarginCalled(double currentPrice) {
        return (currentPrice - openPrice) * shares + accruedFee > marginReserve * 0.8;
    }

    public UUID   getPlayerUuid()    { return playerUuid; }
    public String getTicker()        { return ticker; }
    public int    getShares()        { return shares; }
    public double getOpenPrice()     { return openPrice; }
    public double getMarginReserve() { return marginReserve; }
    public long   getOpenedAtTick()  { return openedAtTick; }
    public double getAccruedFee()    { return accruedFee; }
    public void   addFee(double fee) { accruedFee += fee; }
}
