package com.quantcraft.network;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.item.NewspaperItem;
import com.quantcraft.screen.NewspaperScreen;
import com.quantcraft.screen.QuotronScreen;
import com.quantcraft.screen.TradingPostScreen;
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
            int stockCount = buf.readInt();
            for (int i = 0; i < stockCount; i++) {
                String t = buf.readString();
                prices.put(t,  buf.readDouble());
                changes.put(t, buf.readDouble());
            }
            client.execute(() -> client.setScreen(
                    new QuotronScreen(tracked, prices, changes, BlockPos.fromLong(pos))));
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
                if (client.currentScreen instanceof TradingPostScreen tps)
                    tps.onMarketUpdate(ClientMarketCache.getAll());
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

    public static void sendNewspaperRead(NewspaperItem.NewspaperType type) {
        PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeInt(type.ordinal());
        ClientPlayNetworking.send(ModPackets.C2S_NEWSPAPER_READ, buf);
    }
}
