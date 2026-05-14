package com.quantcraft.market;

import com.quantcraft.persistence.MarketPersistentState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class CommodityMarket {
    private static final CommodityMarket INSTANCE = new CommodityMarket();
    public static CommodityMarket getInstance() { return INSTANCE; }
    private CommodityMarket() {}

    public boolean buyItem(ServerPlayerEntity player, String ticker, int qty, MarketPersistentState ps) {
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = MarketEngine.getInstance().getState(ticker);
        if (def == null || ss == null) return false;
        double cost = def.getItemBuyPrice(ss.getCurrentPrice()) * qty;
        PlayerPortfolio portfolio = ps.getPortfolio(player.getUuid());
        if (portfolio.getCoinBalance() < cost) return false;
        ItemStack items = new ItemStack(def.getItem(), qty);
        if (!player.getInventory().insertStack(items)) return false;
        portfolio.deductCoins(cost);
        ss.applyEventPressure(+qty * 0.1);
        ps.markDirty();
        return true;
    }

    public boolean sellItem(ServerPlayerEntity player, String ticker, int qty, MarketPersistentState ps) {
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = MarketEngine.getInstance().getState(ticker);
        if (def == null || ss == null) return false;
        int held = countItems(player, def);
        if (held < qty) return false;
        double gain = def.getItemSellPrice(ss.getCurrentPrice()) * qty;
        removeItems(player, def, qty);
        ps.getPortfolio(player.getUuid()).addCoins(gain);
        ss.applyEventPressure(-qty * 0.08);
        ps.markDirty();
        return true;
    }

    public double[] getPrices(String ticker) {
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = MarketEngine.getInstance().getState(ticker);
        if (def == null || ss == null) return new double[]{0, 0};
        return new double[]{
                def.getItemBuyPrice(ss.getCurrentPrice()),
                def.getItemSellPrice(ss.getCurrentPrice())
        };
    }

    private int countItems(ServerPlayerEntity p, StockDefinition def) {
        return p.getInventory().main.stream()
                .filter(s -> !s.isEmpty() && s.getItem() == def.getItem())
                .mapToInt(ItemStack::getCount).sum();
    }

    private void removeItems(ServerPlayerEntity p, StockDefinition def, int qty) {
        int rem = qty;
        for (ItemStack s : p.getInventory().main) {
            if (s.isEmpty() || s.getItem() != def.getItem()) continue;
            int take = Math.min(rem, s.getCount());
            s.decrement(take);
            rem -= take;
            if (rem <= 0) break;
        }
    }
}
