package com.quantcraft.item;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CocaineCrashTracker {
    public static final Set<UUID> HIGH = Collections.newSetFromMap(new ConcurrentHashMap<>());
    public static final Set<UUID> CRASHING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private CocaineCrashTracker() {}

    public static void tick(ServerPlayerEntity player) {
        UUID uuid = player.getUuid();

        if (HIGH.contains(uuid) && !player.hasStatusEffect(com.quantcraft.registry.ModEffects.COCAINE_HIGH)) {
            HIGH.remove(uuid);
            CRASHING.add(uuid);
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,       2400, 1, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,       2400, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 2400, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,         2400, 0, false, true, true));
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER,         2400, 0, false, true, true));
        }

        if (CRASHING.contains(uuid)) {
            boolean stillCrashing = player.hasStatusEffect(StatusEffects.SLOWNESS) ||
                                    player.hasStatusEffect(StatusEffects.WEAKNESS) ||
                                    player.hasStatusEffect(StatusEffects.MINING_FATIGUE) ||
                                    player.hasStatusEffect(StatusEffects.NAUSEA) ||
                                    player.hasStatusEffect(StatusEffects.HUNGER);
            if (!stillCrashing) {
                CRASHING.remove(uuid);
            }
        }
    }

    public static void clearCrash(UUID playerId) {
        CRASHING.remove(playerId);
    }
}
