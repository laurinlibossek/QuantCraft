package com.quantcraft;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.quantcraft.config.QuantCraftConfigScreen;

public class QuantCraftModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return QuantCraftConfigScreen::create;
    }
}
