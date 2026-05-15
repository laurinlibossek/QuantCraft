package com.quantcraft.screen;

import com.quantcraft.market.*;
import com.quantcraft.network.ModPackets;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.registry.ModScreenHandlerTypes;
import net.minecraft.entity.player.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

public class CommodityExchangeScreenHandler extends ScreenHandler {
    // Buttons: 0..N-1 = select, 100=buy1, 101=sell1, 102=buy16, 103=sell16, 104=buy64, 105=sell64

    public record CommodityRow(StockDefinition def, double buyPrice, double sellPrice, int playerHeld) {}

    public final List<CommodityRow> rows = new ArrayList<>();
    public double playerBalance;
    private int selectedIndex = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int get(int i)         { return selectedIndex; }
        @Override public void set(int i, int v) { selectedIndex = v; }
        @Override public int size()             { return 1; }
    };

    public CommodityExchangeScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        super(ModScreenHandlerTypes.COMMODITY_EXCHANGE, syncId);
        addProperties(props);
        if (buf.readableBytes() > 0) readBuf(buf);
    }

    private void readBuf(PacketByteBuf buf) {
        try {
            int n = buf.readInt();
            for (int i = 0; i < n; i++) {
                String          tk   = buf.readString();
                double          bp   = buf.readDouble();
                double          sp   = buf.readDouble();
                int             held = buf.readInt();
                StockDefinition def  = StockRegistry.get(tk);
                if (def != null) rows.add(new CommodityRow(def, bp, sp, held));
            }
            playerBalance = buf.readDouble();
        } catch (Exception ignored) {}
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        if (id < 100) { selectedIndex = id; props.set(0, id); return true; }
        if (!MarketEngine.getInstance().isMarketOpen()) {
            sp.sendMessage(Text.literal("§cThe market is closed. Trading resumes at dawn."), true);
            return false;
        }
        List<StockDefinition> allStocks = StockRegistry.getAll();
        if (selectedIndex >= allStocks.size()) return false;
        String ticker = allStocks.get(selectedIndex).ticker();
        var    ps     = MarketPersistentState.getOrCreate(sp.getServer().getOverworld());
        int    qty    = switch (id) { case 100, 101 -> 1; case 102, 103 -> 16; case 104, 105 -> 64; default -> 1; };
        boolean isBuy = (id % 2 == 0);
        CommodityMarket cm = CommodityMarket.getInstance();
        boolean ok = isBuy ? cm.buyItem(sp, ticker, qty, ps) : cm.sellItem(sp, ticker, qty, ps);
        if (isBuy && !ok) {
            sp.sendMessage(Text.literal("§cInsufficient funds or no inventory space."), true);
        } else if (isBuy) {
            double[] prices = cm.getPrices(ticker);
            sp.sendMessage(Text.literal(String.format("§aBought %d §f%s§a @ §e%.1f¢", qty, ticker, prices[0])), true);
        }
        // sell success message is sent by CommodityMarket.sellItem (includes tax info)
        if (ok) ModPackets.sendPortfolioToClient(sp);
        return ok;
    }

    @Override public boolean canUse(PlayerEntity p) { return true; }
    @Override public net.minecraft.item.ItemStack quickMove(PlayerEntity p, int i) { return net.minecraft.item.ItemStack.EMPTY; }
    public int getSelectedIndex() { return props.get(0); }
}
