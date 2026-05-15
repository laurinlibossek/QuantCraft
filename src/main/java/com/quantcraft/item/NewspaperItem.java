package com.quantcraft.item;

import com.quantcraft.network.ModPackets;
import com.quantcraft.registry.ModItems;
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
        DIAMOND_DISCOVERY, TRADE_WAR, MINING_BOOM, LUMBER_SHORTAGE,
        GOLD_RUSH, HARVEST_FESTIVAL, ARCANE_ANOMALY, LIVESTOCK_PLAGUE, EMERALD_CARTEL
    }

    private final NewspaperType type;

    public NewspaperItem(NewspaperType type, Settings s) {
        super(s.maxCount(16));
        this.type = type;
    }

    public NewspaperType getType() { return type; }

    public static Item getItemForType(NewspaperType type) {
        return switch (type) {
            case DIAMOND_DISCOVERY -> ModItems.NP_DIAMOND_DISCOVERY;
            case TRADE_WAR         -> ModItems.NP_TRADE_WAR;
            case MINING_BOOM       -> ModItems.NP_MINING_BOOM;
            case LUMBER_SHORTAGE   -> ModItems.NP_LUMBER_SHORTAGE;
            case GOLD_RUSH         -> ModItems.NP_GOLD_RUSH;
            case HARVEST_FESTIVAL  -> ModItems.NP_HARVEST_FESTIVAL;
            case ARCANE_ANOMALY    -> ModItems.NP_ARCANE_ANOMALY;
            case LIVESTOCK_PLAGUE  -> ModItems.NP_LIVESTOCK_PLAGUE;
            case EMERALD_CARTEL    -> ModItems.NP_EMERALD_CARTEL;
        };
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!world.isClient && user instanceof ServerPlayerEntity sp)
            ModPackets.sendOpenNewspaper(sp, type);
        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Already read.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Right-click to re-read the article.").formatted(Formatting.DARK_GRAY));
    }
}
