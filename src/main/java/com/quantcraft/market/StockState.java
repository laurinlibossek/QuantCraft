package com.quantcraft.market;

import java.util.*;

public class StockState {
    private static final int HISTORY_SIZE = 30;

    private final String ticker;
    private double currentPrice, previousPrice, openPrice;
    private final ArrayDeque<Double> priceHistory = new ArrayDeque<>(HISTORY_SIZE);
    private double eventPressure = 0.0;
    private double supplyPressure = 0.0;
    private int sharesHeld = 0;
    private final OrderBook orderBook;
    private final CandleHistory candleHistory;

    public StockState(String ticker, double initialPrice) {
        this.ticker        = ticker;
        this.currentPrice  = initialPrice;
        this.previousPrice = initialPrice;
        this.openPrice     = initialPrice;
        this.orderBook     = new OrderBook(ticker);
        this.candleHistory = new CandleHistory(ticker);
        for (int i = 0; i < HISTORY_SIZE; i++) priceHistory.addLast(initialPrice);
    }

    public void updatePrice(double newPrice) {
        previousPrice = currentPrice;
        currentPrice  = Math.max(1.0, newPrice);
        candleHistory.record(currentPrice);
        if (priceHistory.size() >= HISTORY_SIZE) priceHistory.pollFirst();
        priceHistory.addLast(currentPrice);
    }

    public void applyEventPressure(double delta)  { eventPressure += delta; }
    public double consumeEventPressure()           { double p = eventPressure; eventPressure = 0; return p; }

    public void applySupplyPressure(double delta)  { supplyPressure += delta; }
    public double tickSupplyPressure() {
        double effect = supplyPressure * 0.05;
        supplyPressure *= 0.97;
        if (Math.abs(supplyPressure) < 0.01) supplyPressure = 0;
        return effect;
    }
    public double getSupplyPressure()              { return supplyPressure; }
    public void setSupplyPressure(double v)        { supplyPressure = v; }

    public void adjustSharesHeld(int delta)        { sharesHeld = Math.max(0, sharesHeld + delta); }
    public int getAvailableShares(int total)       { return Math.max(0, total - sharesHeld); }

    public void restoreHistory(List<Double> history, double previous, int held) {
        priceHistory.clear();
        for (double d : history) priceHistory.addLast(d);
        this.previousPrice = previous;
        this.openPrice     = previous;
        this.sharesHeld    = held;
    }

    public String         getTicker()             { return ticker; }
    public double         getCurrentPrice()       { return currentPrice; }
    public double         getPreviousPrice()      { return previousPrice; }
    public List<Double>   getPriceHistory()       { return new ArrayList<>(priceHistory); }
    public double         getDailyChange()        { return currentPrice - openPrice; }
    public double         getDailyChangePercent() {
        return openPrice == 0 ? 0.0 : ((currentPrice - openPrice) / openPrice) * 100.0;
    }
    public void           snapshotOpenPrice()    { openPrice = currentPrice; }
    public double         getOpenPrice()         { return openPrice; }
    public int            getSharesHeld()         { return sharesHeld; }
    public OrderBook      getOrderBook()          { return orderBook; }
    public CandleHistory  getCandleHistory()      { return candleHistory; }
}
