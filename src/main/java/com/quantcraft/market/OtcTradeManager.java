package com.quantcraft.market;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.network.ModPackets;
import com.quantcraft.persistence.MarketPersistentState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.*;

public final class OtcTradeManager {

    private static final OtcTradeManager INSTANCE = new OtcTradeManager();
    public static OtcTradeManager getInstance() { return INSTANCE; }
    private OtcTradeManager() {}

    private final Map<UUID, OtcTradeOffer> pending = new LinkedHashMap<>();

    public String propose(ServerPlayerEntity proposer,
                          ServerPlayerEntity target,
                          String ticker,
                          int shares,
                          double pricePerShare,
                          MinecraftServer server) {

        if (proposer.getUuid().equals(target.getUuid()))
            return "§cYou cannot offer a trade to yourself.";
        if (shares <= 0)
            return "§cShares must be positive.";
        if (pricePerShare <= 0)
            return "§cPrice per share must be positive.";

        MarketEngine engine = MarketEngine.getInstance();
        if (engine.getState(ticker) == null)
            return "§cUnknown ticker: " + ticker;

        MarketPersistentState ps = MarketPersistentState.getOrCreate(server.getOverworld());
        PlayerPortfolio propPortfolio = ps.getPortfolio(proposer.getUuid());
        int propShares = propPortfolio.getHolding(ticker);
        if (propShares < shares)
            return String.format("§cYou only hold %d shares of %s.", propShares, ticker);

        PlayerPortfolio targPortfolio = ps.getPortfolio(target.getUuid());
        double totalCost = (double) shares * pricePerShare;
        double totalWithTax = totalCost + totalCost * QuantCraftConfig.getTaxRate();
        if (targPortfolio.getBalance() < totalWithTax)
            return String.format("§c%s cannot afford %.2f¢ for this trade (including tax).",
                    target.getName().getString(), totalWithTax);

        boolean dupExists = pending.values().stream().anyMatch(o ->
                o.getProposerUuid().equals(proposer.getUuid()) &&
                o.getTargetUuid().equals(target.getUuid()) &&
                o.getTicker().equals(ticker.toUpperCase()));
        if (dupExists)
            return "§cYou already have a pending offer with that player for " + ticker + ".";

        double listingFee = totalCost * QuantCraftConfig.getTaxRate();
        if (propPortfolio.getBalance() < listingFee)
            return String.format("§cYou need at least %.2f¢ to cover the OTC listing fee.", listingFee);
        propPortfolio.deductBalance(listingFee);
        ps.markDirty();

        OtcTradeOffer offer = new OtcTradeOffer(
                proposer.getUuid(), proposer.getName().getString(),
                target.getUuid(),
                ticker, shares, pricePerShare,
                server.getOverworld().getTime());

        pending.put(offer.getOfferId(), offer);

        proposer.sendMessage(Text.literal(String.format(
                "§aOffer sent to §f%s§a: §f%d x %s @ %.2f §a(total §f%.2f§a, fee §f%.2f§a). Expires in 60s.",
                target.getName().getString(), shares, ticker,
                pricePerShare, offer.totalCost(), listingFee)), false);

        ModPackets.sendOtcOfferToClient(target, offer);

        QuantCraftMod.LOGGER.info("[OTC] {}", offer);
        return null;
    }

    public void accept(ServerPlayerEntity target, String offerIdStr, MinecraftServer server) {
        UUID offerId;
        try { offerId = UUID.fromString(offerIdStr); }
        catch (IllegalArgumentException e) {
            target.sendMessage(Text.literal("§cInvalid offer ID."), false);
            return;
        }

        OtcTradeOffer offer = pending.get(offerId);
        if (offer == null) {
            target.sendMessage(Text.literal("§cOffer not found (may have expired or been withdrawn)."), false);
            return;
        }
        if (!offer.getTargetUuid().equals(target.getUuid())) {
            target.sendMessage(Text.literal("§cThis offer was not addressed to you."), false);
            return;
        }
        if (offer.isExpired(server.getOverworld().getTime())) {
            pending.remove(offerId);
            target.sendMessage(Text.literal("§cThat offer has expired."), false);
            return;
        }

        MarketPersistentState ps = MarketPersistentState.getOrCreate(server.getOverworld());
        PlayerPortfolio buyer = ps.getPortfolio(target.getUuid());
        ServerPlayerEntity proposer = server.getPlayerManager().getPlayer(offer.getProposerUuid());

        if (proposer == null) {
            pending.remove(offerId);
            target.sendMessage(Text.literal("§cThe proposer is no longer online. Trade cancelled."), false);
            return;
        }

        PlayerPortfolio seller = ps.getPortfolio(proposer.getUuid());
        double cost = offer.totalCost();

        double tax = cost * QuantCraftConfig.getTaxRate();
        if (buyer.getBalance() < cost + tax) {
            pending.remove(offerId);
            target.sendMessage(Text.literal("§cYou no longer have enough funds for this trade (including tax)."), false);
            proposer.sendMessage(Text.literal("§eOTC trade failed — buyer has insufficient funds."), false);
            return;
        }
        if (seller.getHolding(offer.getTicker()) < offer.getShares()) {
            pending.remove(offerId);
            target.sendMessage(Text.literal("§cThe seller no longer holds enough shares. Trade cancelled."), false);
            proposer.sendMessage(Text.literal("§eOTC trade failed — you no longer hold enough shares."), false);
            return;
        }

        seller.removeShares(offer.getTicker(), offer.getShares());
        seller.addBalance(cost - tax);

        buyer.addShares(offer.getTicker(), offer.getShares(), offer.getPricePerShare());
        buyer.deductBalance(cost + tax);

        ps.markDirty();
        pending.remove(offerId);

        String confirmation = String.format(
                "§a✓ OTC trade complete: §f%d x %s §aat §f%.2f §a(total §f%.2f§a).",
                offer.getShares(), offer.getTicker(),
                offer.getPricePerShare(), cost);
        target.sendMessage(Text.literal(confirmation), false);
        proposer.sendMessage(Text.literal(confirmation), false);

        QuantCraftMod.LOGGER.info("[OTC] Executed {}", offer);
    }

    public void reject(ServerPlayerEntity target, String offerIdStr) {
        UUID offerId;
        try { offerId = UUID.fromString(offerIdStr); }
        catch (IllegalArgumentException e) { return; }

        OtcTradeOffer offer = pending.remove(offerId);
        if (offer == null) return;

        target.sendMessage(Text.literal("§7Trade offer rejected."), false);

        if (target.getServer() != null) {
            ServerPlayerEntity proposer = target.getServer().getPlayerManager()
                                                .getPlayer(offer.getProposerUuid());
            if (proposer != null) {
                proposer.sendMessage(Text.literal(
                        "§e" + target.getName().getString() +
                        " rejected your OTC offer for " + offer.getTicker() + "."), false);
            }
        }
    }

    public void purgeExpired(long currentTick) {
        pending.entrySet().removeIf(entry -> entry.getValue().isExpired(currentTick));
    }

    public List<OtcTradeOffer> getOffersFor(UUID targetUuid) {
        return pending.values().stream()
                .filter(o -> o.getTargetUuid().equals(targetUuid))
                .toList();
    }
}
