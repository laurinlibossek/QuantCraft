package com.quantcraft.market;

public class CandleData {
    private final double open;
    private double high, low, close;
    private int volume;
    private final long tickIndex;

    public CandleData(double open, long tickIndex) {
        this.open = open; this.high = open; this.low = open; this.close = open;
        this.volume = 0; this.tickIndex = tickIndex;
    }

    public void update(double price, int shares) {
        if (price > high) high = price;
        if (price < low)  low  = price;
        close = price; volume += shares;
    }

    public void seal(double finalPrice) { this.close = finalPrice; }

    public double getOpen()      { return open; }
    public double getHigh()      { return high; }
    public double getLow()       { return low; }
    public double getClose()     { return close; }
    public int    getVolume()    { return volume; }
    public long   getTickIndex() { return tickIndex; }
    public boolean isBullish()   { return close >= open; }
}
