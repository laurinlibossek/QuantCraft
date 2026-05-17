package com.quantcraft.mixin;

import com.quantcraft.item.CocaineCrashTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
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
    private static final ThreadLocal<List<StatusEffectInstance>> CRASH_SNAPSHOT =
            ThreadLocal.withInitial(ArrayList::new);

    @Unique
    private static final Set<StatusEffect> CRASH_EFFECTS = Set.of(
            StatusEffects.SLOWNESS,
            StatusEffects.WEAKNESS,
            StatusEffects.MINING_FATIGUE,
            StatusEffects.NAUSEA,
            StatusEffects.HUNGER
    );

    @Inject(method = "finishUsing", at = @At("HEAD"))
    private void snapshotCrashEffects(ItemStack stack, World world, LivingEntity user,
                                      CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        if (!(user instanceof PlayerEntity player)) return;
        if (!CocaineCrashTracker.CRASHING.contains(player.getUuid())) return;

        List<StatusEffectInstance> snapshot = CRASH_SNAPSHOT.get();
        snapshot.clear();
        for (StatusEffectInstance instance : player.getStatusEffects()) {
            if (CRASH_EFFECTS.contains(instance.getEffectType())) {
                snapshot.add(new StatusEffectInstance(
                        instance.getEffectType(),
                        instance.getDuration(),
                        instance.getAmplifier(),
                        instance.isAmbient(),
                        instance.shouldShowParticles(),
                        instance.shouldShowIcon()
                ));
            }
        }
    }

    @Inject(method = "finishUsing", at = @At("RETURN"))
    private void reapplyCrashEffects(ItemStack stack, World world, LivingEntity user,
                                     CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        if (!(user instanceof ServerPlayerEntity sp)) return;

        List<StatusEffectInstance> snapshot = CRASH_SNAPSHOT.get();
        if (snapshot.isEmpty()) return;

        for (StatusEffectInstance instance : snapshot) {
            sp.addStatusEffect(instance);
        }
        snapshot.clear();

        sp.sendMessage(Text.literal("§c...unfortunately, that doesn't work here. §o(Trust us, we tried)"), true);
    }
}
