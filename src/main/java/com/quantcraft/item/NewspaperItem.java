package com.quantcraft.item;

import com.quantcraft.network.ModPackets;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.world.World;
import java.util.List;

public class NewspaperItem extends Item {
    public enum NewspaperType {
        DIAMOND_DISCOVERY, DRAGON_SLAIN, TRADE_WAR, MINING_BOOM, LUMBER_SHORTAGE,
        GOLD_RUSH, HARVEST_FESTIVAL, ARCANE_ANOMALY, LIVESTOCK_PLAGUE, EMERALD_CARTEL
    }

    private final NewspaperType type;

    public NewspaperItem(NewspaperType type, Settings s) {
        super(s.maxCount(16));
        this.type = type;
    }

    public NewspaperType getType() { return type; }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp)
            ModPackets.sendOpenNewspaper(sp, type);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Right-click to read").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Triggers a market event when read").formatted(Formatting.DARK_GRAY));
    }
}
