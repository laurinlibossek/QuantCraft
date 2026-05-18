package com.quantcraft.mixin;

import com.quantcraft.item.CocaineCrashTracker;
import com.quantcraft.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public class CocaineCrashMixin {

    @Inject(method = "onStatusEffectRemoved", at = @At("HEAD"))
    private void onCocaineHighRemoved(StatusEffectInstance instance, CallbackInfo ci) {
        if (!instance.getEffectType().equals(ModEffects.COCAINE_HIGH)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,       2400, 1, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,       2400, 0, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 2400, 0, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,         2400, 0, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER,         2400, 0, false, true, true));

        CocaineCrashTracker.CRASHING.add(self.getUuid());
    }

    @Inject(method = "onStatusEffectRemoved", at = @At("HEAD"))
    private void onCrashEffectRemoved(StatusEffectInstance instance, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        StatusEffect effect = instance.getEffectType();

        // If any crash effect is removed, check if all crash effects are gone
        if (effect == StatusEffects.SLOWNESS ||
            effect == StatusEffects.WEAKNESS ||
            effect == StatusEffects.MINING_FATIGUE ||
            effect == StatusEffects.NAUSEA ||
            effect == StatusEffects.HUNGER) {

            // Check if all crash effects are now gone
            boolean stillCrashing = self.hasStatusEffect(StatusEffects.SLOWNESS) ||
                                   self.hasStatusEffect(StatusEffects.WEAKNESS) ||
                                   self.hasStatusEffect(StatusEffects.MINING_FATIGUE) ||
                                   self.hasStatusEffect(StatusEffects.NAUSEA) ||
                                   self.hasStatusEffect(StatusEffects.HUNGER);

            if (!stillCrashing && self instanceof PlayerEntity player) {
                CocaineCrashTracker.clearCrash(player.getUuid());
            }
        }
    }
}
