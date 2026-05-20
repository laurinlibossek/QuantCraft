package com.quantcraft.screen;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.market.*;
import com.quantcraft.network.ModPackets;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.registry.ModScreenHandlerTypes;
import net.minecraft.entity.player.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

public class StockExchangeScreenHandler extends ScreenHandler {
    // Button IDs: 0..N-1 = select row
    // 100=market buy 1, 101=market sell 1, 102=market buy 64, 103=market sell 64

    public record StockDisplayData(
            StockDefinition definition,
            double price,
            double changePercent,
            List<Double> history,
            List<double[]> candles,
            int totalShares,
            int sharesHeld) {}

    public record ShortDisplayData(
            String ticker, int shares, double openPrice,
            double currentPrice, double pnl, double accruedFee, double marginReserve) {}

    public final List<StockDisplayData> stocks        = new ArrayList<>();
    public double                       playerBalance;
    public final Map<String,Integer>    playerHoldings = new LinkedHashMap<>();
    public final List<String>           recentNews     = new ArrayList<>();
    public final List<ShortDisplayData> openShorts     = new ArrayList<>();
    public final List<TradeMessage>     messageHistory = new ArrayList<>();
    private int selectedIndex = 0;

    private final PropertyDelegate props = new PropertyDelegate() {
        @Override public int get(int i)          { return selectedIndex; }
        @Override public void set(int i, int v)  { selectedIndex = v; }
        @Override public int size()              { return 1; }
    };

    public StockExchangeScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        super(ModScreenHandlerTypes.STOCK_EXCHANGE, syncId);
        addProperties(props);
        if (buf.readableBytes() > 0) readBuf(buf);
    }

    private void readBuf(PacketByteBuf buf) {
        try {
            int n = buf.readInt();
            for (int i = 0; i < n; i++) {
                String       tk    = buf.readString();
                double       price = buf.readDouble();
                double       chg   = buf.readDouble();
                int          hs    = buf.readInt();
                List<Double> hist  = new ArrayList<>();
                for (int j = 0; j < hs; j++) hist.add(buf.readDouble());
                int              cs      = buf.readInt();
                List<double[]>   candles = new ArrayList<>();
                for (int j = 0; j < cs; j++)
                    candles.add(new double[]{
                            buf.readDouble(), buf.readDouble(),
                            buf.readDouble(), buf.readDouble(),
                            buf.readBoolean() ? 1.0 : 0.0});
                int             total = buf.readInt();
                int             held  = buf.readInt();
                StockDefinition def   = StockRegistry.get(tk);
                if (def != null) stocks.add(new StockDisplayData(def, price, chg, hist, candles, total, held));
            }
            playerBalance = buf.readDouble();
            int hc = buf.readInt();
            for (int i = 0; i < hc; i++) playerHoldings.put(buf.readString(), buf.readInt());
            int nc = buf.readInt();
            for (int i = 0; i < nc; i++) recentNews.add(buf.readString());
            int sc = buf.readInt();
            for (int i = 0; i < sc; i++) {
                String tk     = buf.readString();
                int    shares = buf.readInt();
                double open   = buf.readDouble();
                double cur    = buf.readDouble();
                double pnl    = buf.readDouble();
                double fee    = buf.readDouble();
                double margin = buf.readDouble();
                openShorts.add(new ShortDisplayData(tk, shares, open, cur, pnl, fee, margin));
            }
            int msgCount = buf.readInt();
            for (int i = 0; i < msgCount; i++) {
                long timestamp = buf.readLong();
                String text = buf.readString();
                TradeMessage.MessageType type = buf.readEnumConstant(TradeMessage.MessageType.class);
                messageHistory.add(new TradeMessage(timestamp, text, type));
            }
        } catch (Exception e) {
            QuantCraftMod.LOGGER.warn("[QuantCraft] StockExchangeScreenHandler failed to read opening data: {}", e.getMessage());
        }
        if (stocks.isEmpty()) {
            QuantCraftMod.LOGGER.warn("[QuantCraft] StockExchangeScreenHandler: stocks list is empty after readBuf — data may not have arrived.");
        }
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity sp)) return false;
        if (id < 100) { selectedIndex = id; props.set(0, id); return true; }
        // Withdraw is handled by C2S_WITHDRAW packet (supports custom amounts)

        if (!MarketEngine.getInstance().isMarketOpen()) {
            sp.sendMessage(Text.literal("§cThe market is closed. Trading resumes at dawn."), true);
            return false;
        }
        List<StockDefinition> allStocks = StockRegistry.getAll();
        if (selectedIndex >= allStocks.size()) return false;
        String ticker = allStocks.get(selectedIndex).ticker();
        var    ps     = MarketPersistentState.getOrCreate(sp.getServer().getOverworld());
        int    qty    = switch (id) { case 100, 101 -> 1; case 102, 103 -> 64; default -> 1; };
        boolean isBuy = (id == 100 || id == 102);
        boolean ok    = isBuy
                ? MarketEngine.getInstance().executeMarketBuy(ticker, qty, player.getUuid(), ps)
                : MarketEngine.getInstance().executeMarketSell(ticker, qty, player.getUuid(), ps);
        StockState ss    = MarketEngine.getInstance().getState(ticker);
        double     price = ss != null ? ss.getCurrentPrice() : 0;
        if (ok) {
            double cost = price * qty;
            ps.addPlayerMessage(player.getUuid(),
                String.format("%s %dx %s @ %.1f¢", isBuy ? "Bought" : "Sold", qty, ticker, price),
                TradeMessage.MessageType.TRADE);
            sp.sendMessage(Text.literal(String.format("§a%s %d %s @ §e%.1f¢", isBuy ? "Bought" : "Sold", qty, ticker, price)), true);
            ModPackets.sendPortfolioToClient(sp);
        } else {
            ps.addPlayerMessage(player.getUuid(),
                isBuy ? "Failed: insufficient funds for " + ticker : "Failed: not enough " + ticker + " shares",
                TradeMessage.MessageType.ERROR);
            sp.sendMessage(Text.literal(isBuy ? "§cInsufficient funds or no shares available." : "§cNot enough shares."), true);
        }
        return ok;
    }

    @Override public boolean canUse(PlayerEntity p) { return true; }
    @Override public net.minecraft.item.ItemStack quickMove(PlayerEntity p, int i) { return net.minecraft.item.ItemStack.EMPTY; }
    public int              getSelectedIndex()      { return selectedIndex; }
    public PropertyDelegate getPropertyDelegate()   { return props; }
}
