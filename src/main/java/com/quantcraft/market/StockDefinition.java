package com.quantcraft.market;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public record StockDefinition(
        String ticker,
        String displayName,
        Identifier itemId,
        MarketSector sector,
        double basePrice,
        double volatility,
        double meanReversionStrength,
        int totalShares,
        double priceFloor,
        double commodityFactor
) {
    public Item getItem() { return Registries.ITEM.get(itemId); }

    public double getItemBuyPrice(double stockPrice) {
        return Math.max(priceFloor, stockPrice * commodityFactor);
    }

    public double getItemSellPrice(double stockPrice) {
        return Math.max(priceFloor * 0.75, stockPrice * (commodityFactor - 0.15));
    }
}
