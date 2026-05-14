package com.quantcraft.config;

import com.google.gson.*;
import com.quantcraft.QuantCraftMod;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;

public class QuantCraftConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
            FabricLoader.getInstance().getConfigDir().resolve("quantcraft.json");
    private static ConfigData data = new ConfigData();

    public static void load() {
        if (Files.exists(CONFIG_PATH)) {
            try (Reader r = Files.newBufferedReader(CONFIG_PATH)) {
                data = GSON.fromJson(r, ConfigData.class);
                if (data == null) data = new ConfigData();
            } catch (IOException e) {
                QuantCraftMod.LOGGER.warn("[QuantCraft] Failed to read config", e);
                data = new ConfigData();
            }
        }
        save();
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(CONFIG_PATH)) {
            GSON.toJson(data, w);
        } catch (IOException e) {
            QuantCraftMod.LOGGER.warn("[QuantCraft] Failed to write config", e);
        }
    }

    // Getters
    public static int     getStartingBalance()             { return data.startingBalance; }
    public static int     getMarketTickInterval()          { return data.marketTickInterval; }
    public static float   getGlobalVolatilityMultiplier()  { return data.globalVolatilityMultiplier; }
    public static boolean isEventPressureEnabled()         { return data.eventPressureEnabled; }
    public static boolean isMarketNewsEnabled()            { return data.marketNewsEnabled; }
    public static boolean isSpawnTradingHuts()             { return data.spawnTradingHuts; }
    public static int     getHutSpawnRarity()              { return data.hutSpawnRarity; }

    // Setters
    public static void setStartingBalance(int v)                 { data.startingBalance = v; }
    public static void setMarketTickInterval(int v)              { data.marketTickInterval = v; }
    public static void setGlobalVolatilityMultiplier(float v)    { data.globalVolatilityMultiplier = v; }
    public static void setEventPressureEnabled(boolean v)        { data.eventPressureEnabled = v; }
    public static void setMarketNewsEnabled(boolean v)           { data.marketNewsEnabled = v; }
    public static void setSpawnTradingHuts(boolean v)            { data.spawnTradingHuts = v; }
    public static void setHutSpawnRarity(int v)                  { data.hutSpawnRarity = v; }

    private static class ConfigData {
        int     startingBalance           = 500;
        int     marketTickInterval        = 1200;
        float   globalVolatilityMultiplier= 1.0f;
        boolean eventPressureEnabled      = true;
        boolean marketNewsEnabled         = true;
        boolean spawnTradingHuts          = true;
        int     hutSpawnRarity            = 32;
    }
}
