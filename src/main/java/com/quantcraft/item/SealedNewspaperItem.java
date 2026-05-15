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
import java.util.concurrent.ThreadLocalRandom;

public class SealedNewspaperItem extends Item {

    private static final NewspaperItem.NewspaperType[] TYPES = NewspaperItem.NewspaperType.values();

    public SealedNewspaperItem(Settings settings) {
        super(settings.maxCount(16));
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient && user instanceof ServerPlayerEntity sp) {
            NewspaperItem.NewspaperType type = TYPES[ThreadLocalRandom.current().nextInt(TYPES.length)];

            NewspaperEffects.apply(type, sp.getServer(), sp);

            Item revealedItem = NewspaperItem.getItemForType(type);
            ItemStack revealed = new ItemStack(revealedItem, 1);
            stack.decrement(1);
            if (stack.isEmpty()) {
                user.setStackInHand(hand, revealed);
            } else {
                if (!user.getInventory().insertStack(revealed)) {
                    user.dropItem(revealed, false);
                }
            }

            ModPackets.sendOpenNewspaper(sp, type);
        }

        return TypedActionResult.success(user.getStackInHand(hand), world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Sealed edition.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Right-click to open and trigger a market event.").formatted(Formatting.DARK_GRAY));
    }
}
