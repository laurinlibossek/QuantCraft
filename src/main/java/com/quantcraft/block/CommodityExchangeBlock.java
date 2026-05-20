package com.quantcraft.block;

import com.quantcraft.blockentity.CommodityExchangeBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class CommodityExchangeBlock extends BlockWithEntity {
    public CommodityExchangeBlock(Settings s) { super(s); }

    @Override public MapCodec<? extends BlockWithEntity> getCodec() { throw new UnsupportedOperationException(); }

    @Override public BlockRenderType getRenderType(BlockState s) { return BlockRenderType.MODEL; }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CommodityExchangeBlockEntity(pos, state);
    }

    @Override
    public void onStacksDropped(BlockState state, ServerWorld world, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.onStacksDropped(state, world, pos, tool, dropExperience);
        if (dropExperience && EnchantmentHelper.getLevel(Enchantments.SILK_TOUCH, tool) == 0) {
            dropExperience(world, pos, 3 + world.getRandom().nextInt(4));
        }
    }

    @Override public ActionResult onUse(BlockState state, World world, BlockPos pos,
                                        PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CommodityExchangeBlockEntity ce) player.openHandledScreen(ce);
        }
        return ActionResult.SUCCESS;
    }
}
