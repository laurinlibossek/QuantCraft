package com.quantcraft.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.quantcraft.registry.ModStructures;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureType;

import java.util.Optional;

public class InvestmentCenterGenerator extends Structure {

    public static final Codec<InvestmentCenterGenerator> CODEC =
            RecordCodecBuilder.create(instance ->
                    instance.group(Structure.configCodecBuilder(instance))
                            .apply(instance, InvestmentCenterGenerator::new));

    public InvestmentCenterGenerator(Config config) {
        super(config);
    }

    @Override
    public Optional<StructurePosition> getStructurePosition(Context context) {
        return getStructurePosition(context, Heightmap.Type.WORLD_SURFACE_WG, collector -> {
            int x = context.chunkPos().getCenterX();
            int z = context.chunkPos().getCenterZ();
            int y = context.chunkGenerator().getHeightOnGround(
                    x, z, Heightmap.Type.WORLD_SURFACE_WG,
                    context.world(), context.noiseConfig());

            BlockRotation rotation = BlockRotation.random(context.random());
            StructureTemplateManager manager = context.structureTemplateManager();

            collector.addPiece(new InvestmentCenterPiece(manager, new BlockPos(x, y, z), rotation));
        });
    }

    @Override
    public StructureType<?> getType() {
        return ModStructures.INVESTMENT_CENTER;
    }
}
