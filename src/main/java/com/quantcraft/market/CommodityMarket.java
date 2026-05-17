package com.quantcraft.market;

import com.quantcraft.persistence.MarketPersistentState;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;


public class CommodityMarket {
    private static final CommodityMarket INSTANCE = new CommodityMarket();
    public static CommodityMarket getInstance() { return INSTANCE; }
    private CommodityMarket() {}

    private static final double TAX_RATE       = 0.05;
    private static final double DAILY_CAP      = 5000.0;

    public synchronized boolean buyItem(ServerPlayerEntity player, String ticker, int qty, MarketPersistentState ps) {
        if (!MarketEngine.getInstance().isMarketOpen()) {
            player.sendMessage(Text.literal("§cThe market is closed. Trading resumes at dawn."), true);
            return false;
        }
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = MarketEngine.getInstance().getState(ticker);
        if (def == null || ss == null) return false;
        double cost = def.getItemBuyPrice(ss.getCurrentPrice()) * qty;
        PlayerPortfolio portfolio = ps.getPortfolio(player.getUuid());
        if (portfolio.getBalance() < cost) return false;
        ItemStack items = new ItemStack(def.getItem(), qty);
        if (!player.getInventory().insertStack(items)) return false;
        portfolio.deductBalance(cost);
        ss.applyEventPressure(+qty * def.basePrice() / def.totalShares() * 2.0);
        ps.markDirty();
        return true;
    }

    public synchronized boolean sellItem(ServerPlayerEntity player, String ticker, int qty, MarketPersistentState ps) {
        if (!MarketEngine.getInstance().isMarketOpen()) {
            player.sendMessage(Text.literal("§cThe market is closed. Trading resumes at dawn."), true);
            return false;
        }
        StockDefinition def = StockRegistry.get(ticker);
        StockState      ss  = MarketEngine.getInstance().getState(ticker);
        if (def == null || ss == null) return false;
        int held = countItems(player, def);
        if (held < qty) {
            player.sendMessage(Text.literal("§cNot enough items."), true);
            return false;
        }

        double pricePerItem = def.getItemSellPrice(ss.getCurrentPrice());
        double earned       = ps.getDailyExchangeEarnings(player.getUuid());
        double remaining    = Math.max(0, DAILY_CAP - earned);
        if (remaining <= 0) {
            player.sendMessage(Text.literal("§7Daily exchange limit reached. Come back tomorrow."), true);
            return false;
        }

        int effectiveQty = (int) Math.min(qty, Math.floor(remaining / pricePerItem));
        if (effectiveQty <= 0) {
            player.sendMessage(Text.literal("§7Daily exchange limit reached. Come back tomorrow."), true);
            return false;
        }

        double grossGain = pricePerItem * effectiveQty;
        double tax       = grossGain * TAX_RATE;
        double netGain   = grossGain - tax;

        removeItems(player, def, effectiveQty);
        ss.applyEventPressure(-effectiveQty * def.basePrice() / def.totalShares() * 2.0);
        ps.getPortfolio(player.getUuid()).addBalance(netGain);
        ps.addDailyExchangeEarnings(player.getUuid(), grossGain);
        player.sendMessage(Text.literal(String.format("§aSold %d %s for §e%.1f¢ §7(5%% tax)", effectiveQty, ticker, netGain)), true);
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
