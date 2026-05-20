package com.quantcraft.mixin;

import com.quantcraft.item.CocaineCrashTracker;
import com.quantcraft.registry.ModEffects;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MilkBucketItem;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(MilkBucketItem.class)
public class MilkMixin {

    @Unique
    private static final ThreadLocal<List<StatusEffectInstance>> COCAINE_SNAPSHOT =
            ThreadLocal.withInitial(ArrayList::new);

    @Unique
    private static final Set<net.minecraft.entity.effect.StatusEffect> HIGH_EFFECTS = Set.of(
            StatusEffects.SPEED,
            StatusEffects.STRENGTH,
            StatusEffects.HASTE,
            StatusEffects.JUMP_BOOST,
            StatusEffects.NIGHT_VISION,
            StatusEffects.POISON
    );

    @Unique
    private static final Set<net.minecraft.entity.effect.StatusEffect> CRASH_EFFECTS = Set.of(
            StatusEffects.SLOWNESS,
            StatusEffects.WEAKNESS,
            StatusEffects.MINING_FATIGUE,
            StatusEffects.NAUSEA,
            StatusEffects.HUNGER
    );

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void snapshotCocaineEffects(ItemStack stack, World world, LivingEntity user,
                                        CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        if (!(user instanceof ServerPlayerEntity player)) return;

        boolean isHigh = CocaineCrashTracker.HIGH.contains(player.getUuid());
        boolean isCrashing = CocaineCrashTracker.CRASHING.contains(player.getUuid());
        if (!isHigh && !isCrashing) return;

        List<StatusEffectInstance> snapshot = COCAINE_SNAPSHOT.get();
        snapshot.clear();

        if (player.hasStatusEffect(ModEffects.COCAINE_HIGH)) {
            StatusEffectInstance hi = player.getStatusEffect(ModEffects.COCAINE_HIGH);
            snapshot.add(new StatusEffectInstance(hi.getEffectType(), hi.getDuration(),
                    hi.getAmplifier(), hi.isAmbient(), hi.shouldShowParticles(), hi.shouldShowIcon()));
        }

        for (StatusEffectInstance instance : player.getStatusEffects()) {
            if ((isHigh && HIGH_EFFECTS.contains(instance.getEffectType())) ||
                (isCrashing && CRASH_EFFECTS.contains(instance.getEffectType()))) {
                snapshot.add(new StatusEffectInstance(instance.getEffectType(), instance.getDuration(),
                        instance.getAmplifier(), instance.isAmbient(), instance.shouldShowParticles(),
                        instance.shouldShowIcon()));
            }
        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void reapplyCocaineEffects(ItemStack stack, World world, LivingEntity user,
                                       CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;

        List<StatusEffectInstance> snapshot = COCAINE_SNAPSHOT.get();
        if (snapshot.isEmpty()) {
            COCAINE_SNAPSHOT.remove();
            return;
        }

        if (!(user instanceof ServerPlayerEntity sp) || sp.isDisconnected()) {
            COCAINE_SNAPSHOT.remove();
            return;
        }

        for (StatusEffectInstance instance : snapshot) {
            sp.addStatusEffect(instance);
        }
        COCAINE_SNAPSHOT.remove();

        sp.sendMessage(Text.literal("§cMilk can't wash this away..."), true);
    }
}
