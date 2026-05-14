package com.quantcraft.market;

import java.util.*;
import java.util.stream.Collectors;

public class OrderBook {
    private final String ticker;
    private final List<LimitOrder> orders = new ArrayList<>();

    public OrderBook(String ticker) { this.ticker = ticker; }

    public void addOrder(LimitOrder order)         { orders.add(order); }
    public boolean cancelOrder(UUID orderId)       { return orders.removeIf(o -> o.getOrderId().equals(orderId)); }
    public void cancelAllForPlayer(UUID playerUuid){ orders.removeIf(o -> o.getPlayerUuid().equals(playerUuid)); }

    public List<LimitOrder> processOrders(double currentPrice) {
        List<LimitOrder> triggered = orders.stream()
                .filter(o -> o.triggers(currentPrice))
                .collect(Collectors.toList());
        triggered.forEach(o -> o.fill(o.getRemainingQty()));
        orders.removeAll(triggered);
        return triggered;
    }

    public List<LimitOrder> getAll() { return Collections.unmodifiableList(orders); }

    public List<LimitOrder> getBuys() {
        return orders.stream()
                .filter(o -> o.getSide() == LimitOrder.Side.BUY)
                .sorted(Comparator.comparingDouble(LimitOrder::getLimitPrice).reversed())
                .collect(Collectors.toList());
    }

    public List<LimitOrder> getSells() {
        return orders.stream()
                .filter(o -> o.getSide() == LimitOrder.Side.SELL)
                .sorted(Comparator.comparingDouble(LimitOrder::getLimitPrice))
                .collect(Collectors.toList());
    }

    public int totalBuyVolume() {
        return orders.stream().filter(o -> o.getSide() == LimitOrder.Side.BUY)
                .mapToInt(LimitOrder::getRemainingQty).sum();
    }

    public int totalSellVolume() {
        return orders.stream().filter(o -> o.getSide() == LimitOrder.Side.SELL)
                .mapToInt(LimitOrder::getRemainingQty).sum();
    }

    public String getTicker() { return ticker; }
}
