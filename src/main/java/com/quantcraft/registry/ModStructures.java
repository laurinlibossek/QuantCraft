package com.quantcraft.registry;

import com.quantcraft.QuantCraftMod;
import com.quantcraft.structure.*;
import net.minecraft.registry.*;
import net.minecraft.structure.StructurePieceType;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.structure.StructureType;

public class ModStructures {
    public static StructureType<InvestmentCenterGenerator> INVESTMENT_CENTER;
    public static StructurePieceType INVESTMENT_CENTER_PIECE;

    public static void register() {
        INVESTMENT_CENTER_PIECE = Registry.register(Registries.STRUCTURE_PIECE,
                new Identifier(QuantCraftMod.MOD_ID, "investment_center_piece"),
                InvestmentCenterPiece::new);

        INVESTMENT_CENTER = Registry.register(Registries.STRUCTURE_TYPE,
                new Identifier(QuantCraftMod.MOD_ID, "investment_center"),
                (StructureType<InvestmentCenterGenerator>) () -> InvestmentCenterGenerator.CODEC);
    }
}
