package com.quantcraft.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.*;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import java.util.List;

public class DollarBillItem extends Item {
    public DollarBillItem(Settings s) { super(s); }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Currency of the QuantCraft Exchange").formatted(Formatting.GOLD));
        tooltip.add(Text.literal("9 Nuggets = 9 Coins = 1 Ingot").formatted(Formatting.DARK_GRAY));
    }
}
