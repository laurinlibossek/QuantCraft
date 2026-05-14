package com.quantcraft.block;

import com.quantcraft.blockentity.TradingPostBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TradingPostBlock extends BlockWithEntity {
    public TradingPostBlock(Settings s) { super(s); }

    @Override public MapCodec<? extends BlockWithEntity> getCodec() { throw new UnsupportedOperationException(); }

    @Override public BlockRenderType getRenderType(BlockState s) { return BlockRenderType.MODEL; }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new TradingPostBlockEntity(pos, state);
    }

    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos,
                                        PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof TradingPostBlockEntity tp) player.openHandledScreen(tp);
        }
        return ActionResult.SUCCESS;
    }
}
