package com.quantcraft.network;

import com.quantcraft.item.NewspaperItem;
import com.quantcraft.screen.*;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class ModPacketsClient {
    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ModPackets.S2C_OPEN_QUOTRON, (client, handler, buf, resp) -> {
            long pos   = buf.readLong();
            int  count = buf.readInt();
            List<String> tracked = new ArrayList<>();
            for (int i = 0; i < count; i++) tracked.add(buf.readString());
            Map<String,Double> prices  = new LinkedHashMap<>();
            Map<String,Double> changes = new LinkedHashMap<>();
            Map<String,List<Double>> histories = new LinkedHashMap<>();
            int stockCount = buf.readInt();
            for (int i = 0; i < stockCount; i++) {
                String t = buf.readString();
                prices.put(t, buf.readDouble());
                changes.put(t, buf.readDouble());
                int hSize = buf.readInt();
                List<Double> history = new ArrayList<>(hSize);
                for (int j = 0; j < hSize; j++) history.add(buf.readDouble());
                histories.put(t, history);
            }
            client.execute(() -> client.setScreen(
                    new QuotronScreen(tracked, prices, changes, BlockPos.fromLong(pos), 0.0, histories)));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.S2C_OPEN_NEWSPAPER, (client, handler, buf, resp) -> {
            int ordinal = buf.readInt();
            client.execute(() -> client.setScreen(
                    new NewspaperScreen(NewspaperItem.NewspaperType.values()[ordinal])));
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.S2C_MARKET_UPDATE, (client, handler, buf, resp) -> {
            int count = buf.readInt();
            Map<String, double[]> incoming = new HashMap<>();
            for (int i = 0; i < count; i++) {
                String t      = buf.readString();
                double price  = buf.readDouble();
                double change = buf.readDouble();
                incoming.put(t, new double[]{price, change});
            }
            client.execute(() -> {
                incoming.forEach((t, v) -> ClientMarketCache.update(t, v[0], v[1]));
                if (client.currentScreen instanceof StockExchangeScreen tps)
                    tps.onMarketUpdate(ClientMarketCache.getAll());
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.S2C_OTC_OFFER, (client, handler, buf, resp) -> {
            String offerId      = buf.readString();
            String proposerName = buf.readString();
            String ticker       = buf.readString();
            int    shares       = buf.readInt();
            double pricePerShare = buf.readDouble();
            client.execute(() -> {
                double mktPrice = ClientMarketCache.getPrice(ticker);
                client.setScreen(new OtcTradeScreen(offerId, proposerName, ticker, shares, pricePerShare, mktPrice));
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModPackets.S2C_PORTFOLIO_DATA, (client, handler, buf, resp) -> {
            double balance = buf.readDouble();
            int count = buf.readInt();
            Map<String, Integer> holdings = new LinkedHashMap<>();
            Map<String, Double> avgCosts = new LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                String tk = buf.readString();
                holdings.put(tk, buf.readInt());
                avgCosts.put(tk, buf.readDouble());
            }
            client.execute(() -> {
                if (client.currentScreen instanceof StockExchangeScreen tps) {
                    tps.onPortfolioUpdate(balance, holdings);
                } else if (client.currentScreen instanceof CommodityExchangeScreen ces) {
                    ces.onPortfolioUpdate(balance);
                } else {
                    client.setScreen(new PortfolioScreen(balance, holdings, avgCosts));
                }
            });
        });
    }

    public static void sendUpdateQuotron(List<String> tickers, BlockPos pos) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeLong(pos.asLong());
        buf.writeInt(tickers.size());
        for (String t : tickers) buf.writeString(t);
        ClientPlayNetworking.send(ModPackets.C2S_UPDATE_QUOTRON, buf);
    }

    public static void sendButtonClick(int syncId, int buttonId) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeInt(syncId);
        buf.writeInt(buttonId);
        ClientPlayNetworking.send(ModPackets.C2S_BUTTON_CLICK, buf);
    }

    public static void sendOtcResponse(String offerId, boolean accepted) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeString(offerId, 64);
        buf.writeBoolean(accepted);
        ClientPlayNetworking.send(ModPackets.C2S_OTC_RESPONSE, buf);
    }
}
