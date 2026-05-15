package com.quantcraft.market;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

public final class OtcTradeOffer {

    public static final long EXPIRY_TICKS = 20L * 60;

    private final UUID   offerId;
    private final UUID   proposerUuid;
    private final String proposerName;
    private final UUID   targetUuid;
    private final String ticker;
    private final int    shares;
    private final double pricePerShare;
    private final long   createdAt;

    public OtcTradeOffer(UUID proposerUuid, String proposerName,
                         UUID targetUuid,
                         String ticker, int shares, double pricePerShare,
                         long createdAt) {
        this.offerId       = UUID.randomUUID();
        this.proposerUuid  = proposerUuid;
        this.proposerName  = proposerName;
        this.targetUuid    = targetUuid;
        this.ticker        = ticker.toUpperCase();
        this.shares        = shares;
        this.pricePerShare = pricePerShare;
        this.createdAt     = createdAt;
    }

    private OtcTradeOffer(UUID offerId, UUID proposerUuid, String proposerName,
                          UUID targetUuid, String ticker, int shares,
                          double pricePerShare, long createdAt) {
        this.offerId       = offerId;
        this.proposerUuid  = proposerUuid;
        this.proposerName  = proposerName;
        this.targetUuid    = targetUuid;
        this.ticker        = ticker;
        this.shares        = shares;
        this.pricePerShare = pricePerShare;
        this.createdAt     = createdAt;
    }

    public double  totalCost()                    { return shares * pricePerShare; }
    public boolean isExpired(long currentTick)    { return currentTick - createdAt >= EXPIRY_TICKS; }

    public UUID   getOfferId()       { return offerId; }
    public UUID   getProposerUuid()  { return proposerUuid; }
    public String getProposerName()  { return proposerName; }
    public UUID   getTargetUuid()    { return targetUuid; }
    public String getTicker()        { return ticker; }
    public int    getShares()        { return shares; }
    public double getPricePerShare() { return pricePerShare; }
    public long   getCreatedAt()     { return createdAt; }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("offerId",       offerId.toString());
        nbt.putString("proposerUuid",  proposerUuid.toString());
        nbt.putString("proposerName",  proposerName);
        nbt.putString("targetUuid",    targetUuid.toString());
        nbt.putString("ticker",        ticker);
        nbt.putInt   ("shares",        shares);
        nbt.putDouble("pricePerShare", pricePerShare);
        nbt.putLong  ("createdAt",     createdAt);
        return nbt;
    }

    public static OtcTradeOffer fromNbt(NbtCompound nbt) {
        return new OtcTradeOffer(
            UUID.fromString(nbt.getString("offerId")),
            UUID.fromString(nbt.getString("proposerUuid")),
            nbt.getString("proposerName"),
            UUID.fromString(nbt.getString("targetUuid")),
            nbt.getString("ticker"),
            nbt.getInt("shares"),
            nbt.getDouble("pricePerShare"),
            nbt.getLong("createdAt")
        );
    }

    @Override
    public String toString() {
        return String.format("OtcOffer[%s -> %s: %d x %s @ %.2f = %.2f]",
                proposerName, targetUuid, shares, ticker, pricePerShare, totalCost());
    }
}
