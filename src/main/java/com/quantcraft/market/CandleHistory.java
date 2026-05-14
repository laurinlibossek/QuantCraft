package com.quantcraft.market;

import java.util.*;

public class CandleHistory {
    private static final int MAX = 60;
    private final String ticker;
    private final ArrayDeque<CandleData> candles = new ArrayDeque<>(MAX);
    private CandleData current;
    private long tickIndex = 0;

    public CandleHistory(String ticker) { this.ticker = ticker; }

    public void record(double price) {
        if (current == null) current = new CandleData(price, tickIndex);
        else current.update(price, 0);
    }

    public void openNewCandle(double openPrice) {
        if (current != null) {
            current.seal(openPrice);
            if (candles.size() >= MAX) candles.pollFirst();
            candles.addLast(current);
        }
        current = new CandleData(openPrice, tickIndex++);
    }

    public void recordTrade(double price, int qty) {
        if (current != null) current.update(price, qty);
    }

    public List<CandleData> getCandles() { return new ArrayList<>(candles); }
    public CandleData getCurrent()       { return current; }
    public String getTicker()            { return ticker; }
}
