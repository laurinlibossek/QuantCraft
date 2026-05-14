package com.quantcraft.events;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.market.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import java.util.*;

public class MarketEventListener {
    private static final int MAX_EVENTS_PER_TICK = 20;
    private static final Map<UUID, Map<WorldMarketEvent, Integer>> eventCounts = new HashMap<>();

    public static void resetEventCounts() { eventCounts.clear(); }

    private static boolean canFire(UUID player, WorldMarketEvent type) {
        Map<WorldMarketEvent, Integer> counts = eventCounts.computeIfAbsent(player, k -> new EnumMap<>(WorldMarketEvent.class));
        int current = counts.getOrDefault(type, 0);
        if (current >= MAX_EVENTS_PER_TICK) return false;
        counts.put(type, current + 1);
        return true;
    }

    public static void register() {
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
            if (killed instanceof EnderDragonEntity || killed instanceof WitherEntity) {
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_KILLED_BOSS, null);
                QuantCraftMod.LOGGER.info("[QuantCraft] Boss killed — Arcane crash!");
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

        QuantCraftMod.LOGGER.info("[QuantCraft] Market event listeners registered.");
    }
}
