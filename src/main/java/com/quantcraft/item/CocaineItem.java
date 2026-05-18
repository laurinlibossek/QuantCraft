package com.quantcraft.item;

import com.quantcraft.registry.ModEffects;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import java.util.List;

public class CocaineItem extends Item {

    public CocaineItem() {
        super(new Settings()
                .maxCount(16)
                .food(new FoodComponent.Builder()
                        .alwaysEdible()
                        .hunger(0)
                        .saturationModifier(0f)
                        .build()));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext ctx) {
        tooltip.add(Text.literal("Wall Street's favourite breakfast.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Right-click to consume. Results may vary.").formatted(Formatting.DARK_GRAY));
    }

    @Override
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            // FEATURE: Cocaine can be consumed repeatedly to extend effect duration - this is intentional
            // Each consumption adds another 60 seconds (1200 ticks) to all effects
            user.addStatusEffect(new StatusEffectInstance(ModEffects.COCAINE_HIGH,    1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,     1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,        1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,   1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.POISON,       300,  0, false, true, true)); // 15 seconds weak poison
        }
        return super.finishUsing(stack, world, user);
    }
}
