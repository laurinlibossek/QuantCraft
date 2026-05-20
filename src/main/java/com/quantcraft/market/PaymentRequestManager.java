package com.quantcraft.market;

import com.quantcraft.persistence.MarketPersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.util.*;

public class PaymentRequestManager {
    private static final PaymentRequestManager INSTANCE = new PaymentRequestManager();
    private final Map<String, PaymentRequest> pendingRequests = new HashMap<>();

    public static PaymentRequestManager getInstance() { return INSTANCE; }

    public String propose(ServerPlayerEntity requester, ServerPlayerEntity target, double amount, MinecraftServer server) {
        if (requester.getUuid().equals(target.getUuid())) {
            return "§cCan't request payment from yourself.";
        }
        if (amount <= 0) {
            return "§cAmount must be positive.";
        }

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        PaymentRequest req = new PaymentRequest(
                requestId,
                requester.getUuid(),
                target.getUuid(),
                amount,
                server.getOverworld().getTime()
        );

        pendingRequests.put(requestId, req);

        // Send notification packet to target
        com.quantcraft.network.ModPackets.sendPaymentRequestToClient(target, req, requester.getName().getString());

        requester.sendMessage(Text.literal(String.format(
                "§7Payment request sent to §f%s §7for §e%.1f¢ §7(ID: §f%s§7)",
                target.getName().getString(), amount, requestId)), false);

        return null; // No error
    }

    public String accept(String requestId, ServerPlayerEntity acceptor, MinecraftServer server) {
        PaymentRequest req = pendingRequests.remove(requestId);
        if (req == null) {
            return "§cRequest not found or expired.";
        }
        if (!req.target().equals(acceptor.getUuid())) {
            return "§cThis request isn't for you.";
        }
        if (req.isExpired(server.getOverworld().getTime())) {
            return "§cRequest has expired.";
        }

        var ps = MarketPersistentState.getOrCreate(server.getOverworld());
        var payerPort = ps.getPortfolio(acceptor.getUuid());
        var recipientPort = ps.getPortfolio(req.requester());

        if (payerPort.getBalance() < req.amount()) {
            pendingRequests.put(requestId, req); // Restore request
            return String.format("§cInsufficient funds. Need §e%.1f¢§c, have §e%.1f¢", req.amount(), payerPort.getBalance());
        }

        // Execute transfer
        payerPort.deductBalance(req.amount());
        recipientPort.addBalance(req.amount());
        ps.markDirty();

        // Notify both players
        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(req.requester());
        String requesterName = requester != null ? requester.getName().getString() : "Unknown";
        acceptor.sendMessage(Text.literal(String.format(
                "§aPaid §e%.1f¢§a to §f%s",
                req.amount(), requesterName)), false);

        if (requester != null) {
            requester.sendMessage(Text.literal(String.format(
                    "§aReceived §e%.1f¢§a from §f%s",
                    req.amount(),
                    acceptor.getName().getString())), false);
        }

        return null; // Success
    }

    public void reject(String requestId, ServerPlayerEntity rejecter, MinecraftServer server) {
        PaymentRequest req = pendingRequests.remove(requestId);
        if (req == null) return;
        if (!req.target().equals(rejecter.getUuid())) return;

        rejecter.sendMessage(Text.literal("§7Payment request declined."), false);

        ServerPlayerEntity requester = server.getPlayerManager().getPlayer(req.requester());
        if (requester != null) {
            requester.sendMessage(Text.literal(String.format(
                    "§c%s declined your payment request for §e%.1f¢",
                    rejecter.getName().getString(), req.amount())), false);
        }
    }

    public void purgeExpired(long currentTime) {
        pendingRequests.entrySet().removeIf(e -> e.getValue().isExpired(currentTime));
    }

    public PaymentRequest getRequest(String requestId) {
        return pendingRequests.get(requestId);
    }
}
