package com.quantcraft.network;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.blockentity.QuotronBlockEntity;
import com.quantcraft.item.NewspaperEffects;
import com.quantcraft.item.NewspaperItem;
import com.quantcraft.market.*;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class ModPackets {
    // S2C
    public static final Identifier S2C_MARKET_UPDATE  = id("market_update");
    public static final Identifier S2C_OPEN_QUOTRON   = id("open_quotron");
    public static final Identifier S2C_OPEN_NEWSPAPER = id("open_newspaper");
    // C2S
    public static final Identifier C2S_UPDATE_QUOTRON = id("update_quotron");
    public static final Identifier C2S_NEWSPAPER_READ = id("newspaper_read");

    private static Identifier id(String p) { return new Identifier(QuantCraftMod.MOD_ID, p); }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_UPDATE_QUOTRON, (server, player, handler, buf, resp) -> {
            long pos   = buf.readLong();
            int  count = buf.readInt();
            List<String> tickers = new ArrayList<>();
            for (int i = 0; i < count; i++) tickers.add(buf.readString());
            server.execute(() -> {
                BlockPos bp    = BlockPos.fromLong(pos);
                var      world = server.getOverworld();
                if (world.getBlockEntity(bp) instanceof QuotronBlockEntity qbe)
                    qbe.setTrackedTickers(tickers);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_NEWSPAPER_READ, (server, player, handler, buf, resp) -> {
            int ordinal = buf.readInt();
            server.execute(() -> {
                NewspaperItem.NewspaperType type = NewspaperItem.NewspaperType.values()[ordinal];
                NewspaperEffects.apply(type, server);
                ItemStack held = player.getMainHandStack();
                if (held.getItem() instanceof NewspaperItem) held.decrement(1);
                else {
                    ItemStack off = player.getOffHandStack();
                    if (off.getItem() instanceof NewspaperItem) off.decrement(1);
                }
            });
        });
    }

    public static void broadcastMarketUpdate(MinecraftServer server, Map<String,StockState> snap) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(snap.size());
        for (var e : snap.entrySet()) {
            buf.writeString(e.getKey());
            buf.writeDouble(e.getValue().getCurrentPrice());
            buf.writeDouble(e.getValue().getDailyChangePercent());
        }
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList())
            ServerPlayNetworking.send(p, S2C_MARKET_UPDATE, buf);
    }

    public static void sendOpenQuotron(ServerPlayerEntity player, QuotronBlockEntity qbe, BlockPos pos) {
        var snap = MarketEngine.getInstance().getSnapshot();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(pos.asLong());
        List<String> tracked = qbe.getTrackedTickers();
        buf.writeInt(tracked.size());
        for (String t : tracked) buf.writeString(t);
        buf.writeInt(snap.size());
        for (var e : snap.entrySet()) {
            buf.writeString(e.getKey());
            buf.writeDouble(e.getValue().getCurrentPrice());
            buf.writeDouble(e.getValue().getDailyChangePercent());
        }
        ServerPlayNetworking.send(player, S2C_OPEN_QUOTRON, buf);
    }

    public static void sendOpenNewspaper(ServerPlayerEntity player, NewspaperItem.NewspaperType type) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(type.ordinal());
        ServerPlayNetworking.send(player, S2C_OPEN_NEWSPAPER, buf);
    }
}
