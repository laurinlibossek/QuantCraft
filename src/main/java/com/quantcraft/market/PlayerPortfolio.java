package com.quantcraft.market;

import java.util.*;

public class PlayerPortfolio {
    private double balance;
    private final Map<String,Integer> holdings = new LinkedHashMap<>();
    private final Map<String,Long> acquiredAtTick = new LinkedHashMap<>();
    private final Map<String,Double> avgCosts = new LinkedHashMap<>();

    public PlayerPortfolio(double startingBalance) { this.balance = startingBalance; }

    public boolean buy(String ticker, int qty, double priceEach) {
        double cost = priceEach * qty;
        if (balance < cost) return false;
        balance -= cost;
        addShares(ticker, qty, priceEach);
        return true;
    }

    public boolean sell(String ticker, int qty, double priceEach) {
        int owned = holdings.getOrDefault(ticker, 0);
        if (owned < qty) return false;
        balance += priceEach * qty;
        int remaining = owned - qty;
        if (remaining == 0) { holdings.remove(ticker); acquiredAtTick.remove(ticker); }
        else holdings.put(ticker, remaining);
        return true;
    }

    public void addBalance(double amount)    { balance += amount; }
    public void deductBalance(double amount) { balance = Math.max(0, balance - amount); }
    public void setBalance(double amount)    { balance = Math.max(0, amount); }

    public void addShares(String ticker, int qty) {
        holdings.merge(ticker, qty, Integer::sum);
        acquiredAtTick.putIfAbsent(ticker, MarketEngine.getInstance().getMarketTickCount());
    }

    public void addShares(String ticker, int qty, double priceEach) {
        int existing = holdings.getOrDefault(ticker, 0);
        double oldAvg = avgCosts.getOrDefault(ticker, 0.0);
        double newAvg = (existing == 0) ? priceEach
                : (oldAvg * existing + priceEach * qty) / (existing + qty);
        avgCosts.put(ticker, newAvg);
        addShares(ticker, qty);
    }

    public void addSharesAt(String ticker, int qty, long tick) {
        holdings.merge(ticker, qty, Integer::sum);
        acquiredAtTick.putIfAbsent(ticker, tick);
    }

    public void removeShares(String ticker, int qty) {
        int cur = holdings.getOrDefault(ticker, 0);
        int rem = cur - qty;
        if (rem <= 0) { holdings.remove(ticker); acquiredAtTick.remove(ticker); avgCosts.remove(ticker); }
        else holdings.put(ticker, rem);
    }

    public double getBalance()                  { return balance; }
    public Map<String,Integer> getHoldings()        { return Collections.unmodifiableMap(holdings); }
    public int getHolding(String ticker)            { return holdings.getOrDefault(ticker, 0); }
    public long getAcquiredAtTick(String ticker)    { return acquiredAtTick.getOrDefault(ticker, 0L); }
    public Map<String,Long> getAllAcquiredTicks()    { return Collections.unmodifiableMap(acquiredAtTick); }
    public Map<String,Double> getAvgCosts()         { return Collections.unmodifiableMap(avgCosts); }

    public void setAvgCost(String ticker, double cost) { avgCosts.put(ticker, cost); }

    public double getTotalValue(Map<String,StockState> snapshot) {
        double total = balance;
        for (var entry : holdings.entrySet()) {
            StockState s = snapshot.get(entry.getKey());
            if (s != null) total += s.getCurrentPrice() * entry.getValue();
        }
        return total;
    }
}
