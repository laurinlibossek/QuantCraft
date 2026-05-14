package com.quantcraft.market;

import java.util.*;

public class PlayerPortfolio {
    private double coinBalance;
    private final Map<String,Integer> holdings = new LinkedHashMap<>();

    public PlayerPortfolio(double startingBalance) { this.coinBalance = startingBalance; }

    public boolean buy(String ticker, int qty, double priceEach) {
        double cost = priceEach * qty;
        if (coinBalance < cost) return false;
        coinBalance -= cost;
        holdings.merge(ticker, qty, Integer::sum);
        return true;
    }

    public boolean sell(String ticker, int qty, double priceEach) {
        int owned = holdings.getOrDefault(ticker, 0);
        if (owned < qty) return false;
        coinBalance += priceEach * qty;
        int remaining = owned - qty;
        if (remaining == 0) holdings.remove(ticker);
        else holdings.put(ticker, remaining);
        return true;
    }

    public void addCoins(double amount)    { coinBalance += amount; }
    public void deductCoins(double amount) { coinBalance = Math.max(0, coinBalance - amount); }
    public void setCoins(double amount)    { coinBalance = Math.max(0, amount); }

    public void addShares(String ticker, int qty) { holdings.merge(ticker, qty, Integer::sum); }

    public void removeShares(String ticker, int qty) {
        int cur = holdings.getOrDefault(ticker, 0);
        int rem = cur - qty;
        if (rem <= 0) holdings.remove(ticker);
        else holdings.put(ticker, rem);
    }

    public double getCoinBalance()                  { return coinBalance; }
    public Map<String,Integer> getHoldings()        { return Collections.unmodifiableMap(holdings); }
    public int getHolding(String ticker)            { return holdings.getOrDefault(ticker, 0); }

    public double getTotalValue(Map<String,StockState> snapshot) {
        double total = coinBalance;
        for (var entry : holdings.entrySet()) {
            StockState s = snapshot.get(entry.getKey());
            if (s != null) total += s.getCurrentPrice() * entry.getValue();
        }
        return total;
    }
}
