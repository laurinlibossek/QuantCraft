package com.quantcraft.structure;

import com.quantcraft.block.QuotronBlock;
import com.quantcraft.registry.ModBlocks;
import com.quantcraft.registry.ModStructures;
import net.minecraft.block.*;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.structure.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class InvestmentCenterPiece extends StructurePiece {
    private static final Identifier LOOT = new Identifier("quantcraft", "chests/investment_center");

    public InvestmentCenterPiece(StructurePieceType t, BlockPos pos) {
        super(t, 0, new BlockBox(pos.getX(), pos.getY(), pos.getZ(),
                pos.getX() + 8, pos.getY() + 5, pos.getZ() + 8));
    }

    public InvestmentCenterPiece(StructureContext ctx, NbtCompound nbt) {
        super(ModStructures.INVESTMENT_CENTER_PIECE, nbt);
    }

    @Override
    protected void writeNbt(StructureContext ctx, NbtCompound nbt) {}

    @Override
    public void generate(StructureWorldAccess world, StructureAccessor acc, ChunkGenerator gen,
                         Random rand, BlockBox box, ChunkPos cp, BlockPos pivot) {
        BlockState plank   = Blocks.DARK_OAK_PLANKS.getDefaultState();
        BlockState log     = Blocks.DARK_OAK_LOG.getDefaultState();
        BlockState slab    = Blocks.DARK_OAK_SLAB.getDefaultState();
        BlockState stone   = Blocks.POLISHED_ANDESITE.getDefaultState();
        BlockState glass   = Blocks.GLASS_PANE.getDefaultState();
        BlockState air     = Blocks.AIR.getDefaultState();
        BlockState lantern = Blocks.LANTERN.getDefaultState().with(LanternBlock.HANGING, true);

        // Floor
        fillWithOutline(world, box, 0, 0, 0, 7, 0, 7, stone, stone, false);
        // Walls (hollow: outer shell = plank, interior = air)
        fillWithOutline(world, box, 0, 1, 0, 7, 4, 7, plank, air, false);
        // Clear interior
        fill(world, box, 1, 1, 1, 6, 3, 6);
        // Corner logs
        for (int cx : new int[]{0, 7})
            for (int cz : new int[]{0, 7})
                for (int y = 1; y <= 4; y++)
                    addBlock(world, log, cx, y, cz, box);
        // Roof
        fillWithOutline(world, box, 0, 5, 0, 7, 5, 7, slab, slab, false);

        // Windows and door
        addBlock(world, glass, 2, 2, 0, box);
        addBlock(world, glass, 5, 2, 0, box);
        addBlock(world, air,   3, 1, 0, box);
        addBlock(world, air,   3, 2, 0, box);
        addBlock(world, Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(DoorBlock.HALF, DoubleBlockHalf.LOWER)
                .with(DoorBlock.FACING, Direction.NORTH), 3, 1, 0, box);
        addBlock(world, Blocks.DARK_OAK_DOOR.getDefaultState()
                .with(DoorBlock.HALF, DoubleBlockHalf.UPPER)
                .with(DoorBlock.FACING, Direction.NORTH), 3, 2, 0, box);

        // Functional blocks
        addBlock(world, ModBlocks.TRADING_POST.getDefaultState(),        3, 1, 4, box);
        addBlock(world, ModBlocks.COMMODITY_EXCHANGE.getDefaultState(),  5, 1, 4, box);
        addBlock(world, ModBlocks.QUOTRON.getDefaultState()
                .with(QuotronBlock.FACING, Direction.SOUTH), 1, 2, 7, box);

        // Decoration
        addBlock(world, lantern, 2, 4, 2, box);
        addBlock(world, lantern, 5, 4, 5, box);
        addBlock(world, Blocks.BARREL.getDefaultState(), 1, 1, 6, box);
        addBlock(world, Blocks.BARREL.getDefaultState(), 6, 1, 6, box);

        // Chests with loot
        addChest(world, box, rand, 1, 1, 1, LOOT);
        addChest(world, box, rand, 6, 1, 1, LOOT);
    }
}
