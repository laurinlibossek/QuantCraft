package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.item.CocaineHighEffect;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEffects {
    public static final StatusEffect COCAINE_HIGH = new CocaineHighEffect();

    public static void register() {
        Registry.register(Registries.STATUS_EFFECT,
                new Identifier(QuantCraftMod.MOD_ID, "cocaine_high"),
                COCAINE_HIGH);
    }
}
