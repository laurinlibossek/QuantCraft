package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.blockentity.*;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

public class ModBlockEntityTypes {
    public static BlockEntityType<StockExchangeBlockEntity>     STOCK_EXCHANGE;
    public static BlockEntityType<CommodityExchangeBlockEntity> COMMODITY_EXCHANGE;
    public static BlockEntityType<QuotronBlockEntity>           QUOTRON;

    public static void register() {
        STOCK_EXCHANGE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("stock_exchange"),
                FabricBlockEntityTypeBuilder.create(StockExchangeBlockEntity::new, ModBlocks.STOCK_EXCHANGE).build());

        COMMODITY_EXCHANGE = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("commodity_exchange"),
                FabricBlockEntityTypeBuilder.create(CommodityExchangeBlockEntity::new, ModBlocks.COMMODITY_EXCHANGE).build());

        QUOTRON = Registry.register(Registries.BLOCK_ENTITY_TYPE, id("quotron"),
                FabricBlockEntityTypeBuilder.create(QuotronBlockEntity::new, ModBlocks.QUOTRON).build());
    }

    private static Identifier id(String p) { return new Identifier(QuantCraftMod.MOD_ID, p); }
}
