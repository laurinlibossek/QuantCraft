package com.quantcraft.block;

import com.quantcraft.blockentity.StockExchangeBlockEntity;
import com.quantcraft.item.DollarBillItem;
import com.quantcraft.persistence.MarketPersistentState;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class StockExchangeBlock extends BlockWithEntity {
    public StockExchangeBlock(Settings s) { super(s); }

    @Override public MapCodec<? extends BlockWithEntity> getCodec() { throw new UnsupportedOperationException(); }

    @Override public BlockRenderType getRenderType(BlockState s) { return BlockRenderType.MODEL; }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new StockExchangeBlockEntity(pos, state);
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
        if (world.isClient) return ActionResult.SUCCESS;
        if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

        // Holding dollar bills → deposit all bills from inventory
        ItemStack mainHand = player.getMainHandStack();
        if (!mainHand.isEmpty() && mainHand.getItem() instanceof DollarBillItem) {
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.SUCCESS;
            int totalDeposit = 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof DollarBillItem) {
                    totalDeposit += stack.getCount();
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
            var ps = MarketPersistentState.getOrCreate(world.getServer().getOverworld());
            var portfolio = ps.getPortfolio(player.getUuid());
            portfolio.addBalance(totalDeposit);
            ps.markDirty();
            sp.sendMessage(Text.literal(String.format(
                    "§aDeposited §e%d¢ §ainto your account. Balance: §e%.1f¢", totalDeposit, portfolio.getBalance())), false);
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof StockExchangeBlockEntity se) player.openHandledScreen(se);
        return ActionResult.SUCCESS;
    }
}
