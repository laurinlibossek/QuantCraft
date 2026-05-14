package com.quantcraft.events;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.market.*;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.WitherEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.tag.BlockTags;

public class MarketEventListener {
    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
            Block b = state.getBlock();
            if (state.isIn(BlockTags.COAL_ORES) || state.isIn(BlockTags.IRON_ORES)
                    || state.isIn(BlockTags.GOLD_ORES) || state.isIn(BlockTags.DIAMOND_ORES)
                    || state.isIn(BlockTags.EMERALD_ORES) || state.isIn(BlockTags.LAPIS_ORES)
                    || state.isIn(BlockTags.REDSTONE_ORES) || state.isIn(BlockTags.COPPER_ORES))
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_MINED_ORE, null);
            else if (b instanceof CropBlock || b == Blocks.MELON || b == Blocks.PUMPKIN
                    || b instanceof StemBlock || b instanceof AttachedStemBlock)
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_HARVESTED_CROP, null);
            else if (state.isIn(BlockTags.LOGS))
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_MINED_WOOD, null);
        });

        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, attacker, killed) -> {
            if (killed instanceof EnderDragonEntity || killed instanceof WitherEntity) {
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_KILLED_BOSS, null);
                QuantCraftMod.LOGGER.info("[QuantCraft] Boss killed — Arcane crash!");
            } else if (killed instanceof MobEntity) {
                MarketEngine.getInstance().fireEvent(WorldMarketEvent.PLAYER_KILLED_MOB, null);
            }
        });

        QuantCraftMod.LOGGER.info("[QuantCraft] Market event listeners registered.");
    }
}
