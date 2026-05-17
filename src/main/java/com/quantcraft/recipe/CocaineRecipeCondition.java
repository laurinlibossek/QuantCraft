package com.quantcraft.recipe;

import com.quantcraft.config.QuantCraftConfig;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.util.Identifier;

public class CocaineRecipeCondition {
    public static final Identifier ID = new Identifier("quantcraft", "cocaine_enabled");

    public static void register() {
        ResourceConditions.register(ID, jsonObject -> QuantCraftConfig.isCocaineCraftingEnabled());
    }
}
