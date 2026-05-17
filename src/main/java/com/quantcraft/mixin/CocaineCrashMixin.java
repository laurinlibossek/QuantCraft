package com.quantcraft.mixin;

import com.quantcraft.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mixin(LivingEntity.class)
public class CocaineCrashMixin {

    @Unique
    public static final Set<UUID> CRASHING = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Inject(method = "onStatusEffectRemoved", at = @At("HEAD"))
    private void onCocaineHighRemoved(StatusEffectInstance instance, CallbackInfo ci) {
        if (!instance.getEffectType().equals(ModEffects.COCAINE_HIGH)) return;

        LivingEntity self = (LivingEntity) (Object) this;
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS,       2400, 1, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS,       2400, 0, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 2400, 0, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.NAUSEA,         2400, 0, false, true, true));
        self.addStatusEffect(new StatusEffectInstance(StatusEffects.HUNGER,         2400, 0, false, true, true));

        CRASHING.add(self.getUuid());
    }
}
