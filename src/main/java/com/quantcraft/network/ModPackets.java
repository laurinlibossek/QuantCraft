package com.quantcraft.network;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.blockentity.QuotronBlockEntity;
import com.quantcraft.item.NewspaperItem;
import com.quantcraft.market.*;
import com.quantcraft.screen.TradeMessage;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class ModPackets {
    // S2C
    public static final Identifier S2C_MARKET_UPDATE     = id("market_update");
    public static final Identifier S2C_OPEN_QUOTRON      = id("open_quotron");
    public static final Identifier S2C_OPEN_NEWSPAPER    = id("open_newspaper");
    public static final Identifier S2C_OTC_OFFER         = id("otc_offer");
    public static final Identifier S2C_PORTFOLIO_DATA    = id("portfolio_data");
    public static final Identifier S2C_PAYMENT_REQUEST   = id("payment_request");
    // C2S
    public static final Identifier C2S_UPDATE_QUOTRON    = id("update_quotron");
    public static final Identifier C2S_NEWSPAPER_READ    = id("newspaper_read");
    public static final Identifier C2S_BUTTON_CLICK      = id("button_click");
    public static final Identifier C2S_OTC_RESPONSE      = id("otc_response");
    public static final Identifier C2S_PAYMENT_RESPONSE  = id("payment_response");
    public static final Identifier C2S_WITHDRAW          = id("withdraw");

    private static Identifier id(String p) { return new Identifier(QuantCraftMod.MOD_ID, p); }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(C2S_UPDATE_QUOTRON, (server, player, handler, buf, resp) -> {
            long pos   = buf.readLong();
            int  count = buf.readInt();
            if (count < 0 || count > QuotronBlockEntity.MAX_TRACKED) return;
            List<String> tickers = new ArrayList<>();
            for (int i = 0; i < count; i++) tickers.add(buf.readString(32));
            server.execute(() -> {
                BlockPos bp = BlockPos.fromLong(pos);
                if (player.getBlockPos().getSquaredDistance(bp) > 64) return;
                if (player.getServerWorld().getBlockEntity(bp) instanceof QuotronBlockEntity qbe)
                    qbe.setTrackedTickers(tickers);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_NEWSPAPER_READ, (server, player, handler, buf, resp) -> {
            buf.readInt();
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_BUTTON_CLICK, (server, player, handler, buf, resp) -> {
            int syncId = buf.readInt();
            int buttonId = buf.readInt();
            server.execute(() -> {
                if (player.currentScreenHandler.syncId == syncId) {
                    player.currentScreenHandler.onButtonClick(player, buttonId);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_OTC_RESPONSE, (server, player, handler, buf, resp) -> {
            String offerId = buf.readString(64);
            boolean accepted = buf.readBoolean();
            server.execute(() -> {
                if (accepted) OtcTradeManager.getInstance().accept(player, offerId, server);
                else          OtcTradeManager.getInstance().reject(player, offerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_PAYMENT_RESPONSE, (server, player, handler, buf, resp) -> {
            String requestId = buf.readString(64);
            boolean accepted = buf.readBoolean();
            server.execute(() -> {
                if (accepted) {
                    String err = PaymentRequestManager.getInstance().accept(requestId, player, server);
                    if (err != null) player.sendMessage(net.minecraft.text.Text.literal(err), false);
                } else {
                    PaymentRequestManager.getInstance().reject(requestId, player, server);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(C2S_WITHDRAW, (server, player, handler, buf, resp) -> {
            int requestedAmount = buf.readInt();
            server.execute(() -> {
                if (!(player.currentScreenHandler instanceof com.quantcraft.screen.StockExchangeScreenHandler)) return;
                var ps = com.quantcraft.persistence.MarketPersistentState.getOrCreate(player.getServer().getOverworld());
                PlayerPortfolio port = ps.getPortfolio(player.getUuid());
                int balance = (int) Math.floor(port.getBalance());
                int amount = Math.max(0, Math.min(requestedAmount, balance));
                if (amount <= 0) {
                    player.sendMessage(net.minecraft.text.Text.literal("§cNo funds to withdraw."), false);
                    return;
                }
                // Check capacity
                int capacity = 0;
                for (int s = 0; s < 36; s++) {
                    net.minecraft.item.ItemStack slot = player.getInventory().main.get(s);
                    if (slot.isEmpty()) {
                        capacity += 64;
                    } else if (slot.getItem() == com.quantcraft.registry.ModItems.DOLLAR_BILL && slot.getCount() < 64) {
                        capacity += 64 - slot.getCount();
                    }
                }
                if (amount > capacity) {
                    player.sendMessage(net.minecraft.text.Text.literal(
                            String.format("§cCan only fit %d¢. Free up inventory space.", capacity)), false);
                    return;
                }
                // Insert
                int given = 0;
                while (given < amount) {
                    int stackSize = Math.min(64, amount - given);
                    net.minecraft.item.ItemStack dollarStack = new net.minecraft.item.ItemStack(
                            com.quantcraft.registry.ModItems.DOLLAR_BILL, stackSize);
                    player.getInventory().insertStack(dollarStack);
                    given += stackSize - dollarStack.getCount();
                    if (dollarStack.getCount() > 0) break;
                }
                port.deductBalance(given);
                ps.addPlayerMessage(player.getUuid(),
                        String.format("Withdrew %d¢ (%.1f¢ remaining)", given, port.getBalance()),
                        com.quantcraft.screen.TradeMessage.MessageType.INFO);
                ps.markDirty();
                for (int s = 0; s < player.getInventory().size(); s++) {
                    player.networkHandler.sendPacket(new net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket(
                            -2, 0, s, player.getInventory().getStack(s)));
                }
                sendPortfolioToClient(player);
                player.sendMessage(net.minecraft.text.Text.literal(String.format(
                        "§aWithdrew §e%d¢ §7(%.1f¢ remaining in account).", given, port.getBalance())), false);
            });
        });
    }

    public static void broadcastMarketUpdate(MinecraftServer server, Map<String,StockState> snap) {
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeInt(snap.size());
            for (var e : snap.entrySet()) {
                buf.writeString(e.getKey());
                buf.writeDouble(e.getValue().getCurrentPrice());
                buf.writeDouble(e.getValue().getDailyChangePercent());
            }
            ServerPlayNetworking.send(p, S2C_MARKET_UPDATE, buf);
        }
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
            List<Double> history = e.getValue().getPriceHistory();
            buf.writeInt(history.size());
            for (double h : history) buf.writeDouble(h);
        }
        ServerPlayNetworking.send(player, S2C_OPEN_QUOTRON, buf);
    }

    public static void sendOpenNewspaper(ServerPlayerEntity player, NewspaperItem.NewspaperType type) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(type.ordinal());
        ServerPlayNetworking.send(player, S2C_OPEN_NEWSPAPER, buf);
    }

    public static void sendOtcOfferToClient(ServerPlayerEntity target, OtcTradeOffer offer) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(offer.getOfferId().toString());
        buf.writeString(offer.getProposerName());
        buf.writeString(offer.getTicker());
        buf.writeInt(offer.getShares());
        buf.writeDouble(offer.getPricePerShare());
        ServerPlayNetworking.send(target, S2C_OTC_OFFER, buf);
    }

    public static void sendPortfolioToClient(ServerPlayerEntity player) {
        var ps = com.quantcraft.persistence.MarketPersistentState.getOrCreate(
                player.getServerWorld());
        PlayerPortfolio port = ps.getPortfolio(player.getUuid());
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(port.getBalance());
        Map<String, Integer> holdings = port.getHoldings();
        Map<String, Double> avgCosts = port.getAvgCosts();
        buf.writeInt(holdings.size());
        for (var e : holdings.entrySet()) {
            buf.writeString(e.getKey());
            buf.writeInt(e.getValue());
            buf.writeDouble(avgCosts.getOrDefault(e.getKey(), 0.0));
        }
        List<TradeMessage> messages = ps.getPlayerMessages(player.getUuid());
        buf.writeInt(messages.size());
        for (TradeMessage msg : messages) {
            buf.writeLong(msg.timestamp());
            buf.writeString(msg.text());
            buf.writeEnumConstant(msg.type());
        }
        ServerPlayNetworking.send(player, S2C_PORTFOLIO_DATA, buf);
    }

    public static void sendPaymentRequestToClient(ServerPlayerEntity target, PaymentRequest request, String requesterName) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(request.requestId());
        buf.writeString(requesterName);
        buf.writeDouble(request.amount());
        ServerPlayNetworking.send(target, S2C_PAYMENT_REQUEST, buf);
    }
}
