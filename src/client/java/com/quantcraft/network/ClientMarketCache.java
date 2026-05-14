package com.quantcraft.network;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientMarketCache {
    private static final Map<String, double[]> PRICES = new ConcurrentHashMap<>();

    public static void update(String ticker, double price, double change) {
        PRICES.put(ticker, new double[]{price, change});
    }

    public static double getPrice(String ticker) {
        double[] d = PRICES.get(ticker); return d != null ? d[0] : 0;
    }

    public static double getChange(String ticker) {
        double[] d = PRICES.get(ticker); return d != null ? d[1] : 0;
    }

    public static Map<String, double[]> getAll() {
        return Collections.unmodifiableMap(PRICES);
    }
}
