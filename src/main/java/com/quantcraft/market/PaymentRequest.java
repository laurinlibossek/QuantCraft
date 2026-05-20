package com.quantcraft.market;

import net.minecraft.nbt.NbtCompound;
import java.util.UUID;

public record PaymentRequest(
        String requestId,
        UUID requester,
        UUID target,
        double amount,
        long timestamp
) {
    public static PaymentRequest fromNbt(NbtCompound nbt) {
        return new PaymentRequest(
                nbt.getString("id"),
                nbt.getUuid("requester"),
                nbt.getUuid("target"),
                nbt.getDouble("amount"),
                nbt.getLong("timestamp")
        );
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("id", requestId);
        nbt.putUuid("requester", requester);
        nbt.putUuid("target", target);
        nbt.putDouble("amount", amount);
        nbt.putLong("timestamp", timestamp);
        return nbt;
    }

    public boolean isExpired(long currentTime) {
        return (currentTime - timestamp) > 1200; // 60 seconds (20 ticks/sec)
    }
}
