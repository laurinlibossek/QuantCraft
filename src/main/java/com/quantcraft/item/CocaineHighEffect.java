package com.quantcraft.item;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.AttributeContainer;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;

public class CocaineHighEffect extends StatusEffect {

    public CocaineHighEffect() {
        super(StatusEffectType.BENEFICIAL, 0xE8A000);
    }

    @Override
    public void onRemoved(LivingEntity entity, AttributeContainer attributes, int amplifier) {
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,       2400, 1, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,       2400, 0, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 2400, 0, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,         2400, 0, false, true, true));
        entity.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER,         2400, 0, false, true, true));

        if (entity instanceof PlayerEntity player) {
            player.getCustomData().putBoolean("CocaineCrash", true);
        }
    }
}
