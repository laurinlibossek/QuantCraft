package com.quantcraft.events;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.market.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import java.util.*;

public class MarketEventListener {
    private static final int MAX_EVENTS_PER_TICK = 20;
    private static final Map<UUID, Map<WorldMarketEvent, Integer>> eventCounts = new HashMap<>();

    // Raid cooldown: don't fire another raid event within 25 market ticks (~25 min) of the last one
    private static long lastRaidEventMarketTick = Long.MIN_VALUE;
    private static boolean wasRaining = false;

    public static void resetEventCounts() { eventCounts.clear(); }

    private static boolean canFire(UUID player, WorldMarketEvent type) {
        Map<WorldMarketEvent, Integer> counts = eventCounts.computeIfAbsent(player, k -> new EnumMap<>(WorldMarketEvent.class));
        int current = counts.getOrDefault(type, 0);
        if (current >= MAX_EVENTS_PER_TICK) return false;
        counts.put(type, current + 1);
        return true;
    }

    public static void register() {
        // Block-break events: supply-side pressure on resource sectors
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            if (!(player instanceof ServerPlayerEntity sp)) return;
            Block b = state.getBlock();
            if (state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES)
                    || state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.DIAMOND_ORES)
                    || state.isIn(BlockTags.EMERALD_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                    || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.COPPER_ORES)) {
                if (canFire(sp.getUuid(), WorldMarketEvent.PLAYER_MINED_ORE))
                    MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_MINED_ORE, null);
            } else if (b instanceof CropBlock || b == Blocks.MELON || b == Blocks.PUMPKIN
                    || b instanceof StemBlock || b instanceof AttachedStemBlock) {
                if (canFire(sp.getUuid(), WorldMarketEvent.PLAYER_HARVESTED_CROP))
                    MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_HARVESTED_CROP, null);
            } else if (state.isIn(BlockTags.LOGS)) {
                if (canFire(sp.getUuid(), WorldMarketEvent.PLAYER_MINED_WOOD))
                    MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_MINED_WOOD, null);
            }
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, attacker, killed) -> {
            MinecraftServer server = world.getServer();

            if (killed instanceof EnderDragonEntity || killed instanceof WitherEntity) {
                if (server != null) {
                    MarketEngine engine = MarketEngine.getInstance();
                    if (killed instanceof EnderDragonEntity) {
                        // Dragon death: rare server milestone — sharp Arcane spike down then quick recovery.
                        // Instant pressure creates the spike; short sustained event is the trailing bleed.
                        engine.applySectorPressure(MarketSector.ARCANE, -10.0);
                        engine.addActiveEvent(new ActiveMarketEvent(
                                "Ender Dragon slain — Arcane market in freefall",
                                null, MarketSector.ARCANE, -2.0, 5, server.getTicks()), null);
                    } else {
                        // Wither: still a big event but less dramatic than the dragon
                        engine.addActiveEvent(new ActiveMarketEvent(
                                "Wither slain — Arcane sector rattled",
                                null, MarketSector.ARCANE, -2.0, 8, server.getTicks()), null);
                    }
                    QuantCraftMod.LOGGER.info("[QuantCraft] Boss killed — Arcane crash event triggered.");
                }
            } else if (killed instanceof RaiderEntity raider && raider.getRaid() != null) {
                // Raid detected: fire market event once per raid, not once per mob killed
                long currentTick = MarketEngine.getInstance().getMarketTickCount();
                if (currentTick - lastRaidEventMarketTick > 25) {
                    lastRaidEventMarketTick = currentTick;
                    if (server != null) {
                        // Crops trampled -> Agrarian supply disrupted for ~20 min (raid duration)
                        MarketEngine.getInstance().addActiveEvent(new ActiveMarketEvent(
                                "Village raid — crop fields trampled, Agrarian supply disrupted",
                                null, MarketSector.AGRARIAN, -1.5, 20, server.getTicks()), null);
                        // Villagers buy weapons/armor -> Manufactured demand spikes briefly
                        MarketEngine.getInstance().addActiveEvent(new ActiveMarketEvent(
                                "Village raid — defensive goods in high demand",
                                null, MarketSector.MANUFACTURED, +1.0, 20, server.getTicks()), null);
                        QuantCraftMod.LOGGER.info("[QuantCraft] Raid detected — Agrarian/Manufactured events triggered.");
                    }
                }
            } else if (killed instanceof MobEntity) {
                if (attacker instanceof ServerPlayerEntity sp) {
                    if (canFire(sp.getUuid(), WorldMarketEvent.PLAYER_KILLED_MOB))
                        MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_KILLED_MOB, null);
                }
            }
        });

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> {
            boolean enteredEnd = destination.getRegistryKey().equals(World.END);
            boolean leftEnd    = origin.getRegistryKey().equals(World.END);
            if (!enteredEnd && !leftEnd) return;
            String name = player.getName().getString();
            Text msg = enteredEnd
                    ? Text.literal("§5§l[ MARKET ALERT ] §r§7" + name + " has entered the tax-free zone.")
                    : Text.literal("§7" + name + " has returned to taxable territory.");
            player.getServer().getPlayerManager().getPlayerList()
                    .forEach(p -> p.sendMessage(msg));
        });

        // Clear weather: market sentiment improves when the sun comes back out
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            boolean isRaining = server.getOverworld().isRaining();
            if (!isRaining && wasRaining)
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.CLEAR_WEATHER, null);
            wasRaining = isRaining;
        });

        QuantCraftMod.LOGGER.info("[QuantCraft] Market event listeners registered.");
    }
}
