package com.quantcraft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quantcraft.config.QuantCraftConfig;
import com.quantcraft.registry.ModStructures;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.*;
import java.util.Optional;

public class InvestmentCenterGenerator extends Structure {
    public static final Codec<InvestmentCenterGenerator> CODEC =
            RecordCodecBuilder.create(i -> i.group(
                Structure.configCodecBuilder(i)
            ).apply(i, InvestmentCenterGenerator::new));

    public InvestmentCenterGenerator(Config c) { super(c); }

    @Override
    public Optional<StructurePosition> getStructurePosition(Context ctx) {
        if (!QuantCraftConfig.isSpawnTradingHuts()) return Optional.empty();
        return getStructurePosition(ctx, Heightmap.Type.WORLD_SURFACE_WG,
                collector -> collector.addPiece(new InvestmentCenterPiece(
                        ModStructures.INVESTMENT_CENTER_PIECE,
                        new BlockPos(ctx.chunkPos().getStartX(), 64, ctx.chunkPos().getStartZ()))));
    }

    @Override public StructureType<?> getType() { return ModStructures.INVESTMENT_CENTER; }
}
