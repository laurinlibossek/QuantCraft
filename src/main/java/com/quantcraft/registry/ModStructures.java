package com.quantcraft.registry;

import com.quantcraft.structure.InvestmentCenterGenerator;
import com.quantcraft.structure.InvestmentCenterPiece;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

public final class ModStructures {

    public static StructureType<InvestmentCenterGenerator> INVESTMENT_CENTER;
    public static StructurePieceType INVESTMENT_CENTER_PIECE;

    public static void register() {
        INVESTMENT_CENTER = Registry.register(
                Registries.STRUCTURE_TYPE,
                new Identifier("quantcraft", "investment_center"),
                () -> InvestmentCenterGenerator.CODEC);

        INVESTMENT_CENTER_PIECE = Registry.register(
                Registries.STRUCTURE_PIECE,
                new Identifier("quantcraft", "icep"),
                InvestmentCenterPiece::new);
    }

    private ModStructures() {}
}
