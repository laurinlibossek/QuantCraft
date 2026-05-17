package com.quantcraft.item;

import com.quantcraft.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

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
    public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
        if (!world.isClient) {
            user.addStatusEffect(new StatusEffectInstance(ModEffects.COCAINE_HIGH,    1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED,        1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.STRENGTH,     1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.HASTE,        1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.JUMP_BOOST,   1200, 1, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 0, false, true, true));
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1200, 0, false, true, true));
        }
        return super.finishUsing(stack, world, user);
    }
}
