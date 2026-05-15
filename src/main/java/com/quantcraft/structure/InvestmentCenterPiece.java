package com.quantcraft.structure;

import com.quantcraft.registry.ModStructures;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.world.ServerWorldAccess;

public class InvestmentCenterPiece extends SimpleStructurePiece {

    private static final Identifier TEMPLATE = new Identifier("quantcraft", "investors_center");

    public InvestmentCenterPiece(StructureTemplateManager manager, BlockPos pos, BlockRotation rotation) {
        super(ModStructures.INVESTMENT_CENTER_PIECE, 0, manager, TEMPLATE,
                TEMPLATE.toString(), makePlacement(rotation), pos);
    }

    public InvestmentCenterPiece(StructureContext context, NbtCompound nbt) {
        super(ModStructures.INVESTMENT_CENTER_PIECE, nbt, context.structureTemplateManager(),
                id -> makePlacement(BlockRotation.valueOf(nbt.getString("Rot"))));
    }

    private static StructurePlacementData makePlacement(BlockRotation rotation) {
        return new StructurePlacementData().setRotation(rotation);
    }

    @Override
    protected void writeNbt(StructureContext context, NbtCompound nbt) {
        super.writeNbt(context, nbt);
        nbt.putString("Rot", this.placementData.getRotation().name());
    }

    @Override
    protected void handleMetadata(String metadata, BlockPos pos, ServerWorldAccess world,
                                  net.minecraft.util.math.random.Random random,
                                  BlockBox boundingBox) {}
}
