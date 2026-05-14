package com.quantcraft.block;

import com.quantcraft.blockentity.TradingPostBlockEntity;
import com.quantcraft.item.DollarBillItem;
import com.quantcraft.persistence.MarketPersistentState;
import com.mojang.serialization.MapCodec;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
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
        if (world.isClient) return ActionResult.SUCCESS;

        if (player.isSneaking()) {
            if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.SUCCESS;
            int totalCoins = 0;
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof DollarBillItem) {
                    totalCoins += stack.getCount();
                }
            }
            if (totalCoins == 0) {
                sp.sendMessage(Text.literal("§7No bills to deposit. Hold Dollar Bills and sneak+right-click to deposit."), true);
                return ActionResult.SUCCESS;
            }
            // Clear all bill stacks
            for (int i = 0; i < player.getInventory().size(); i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof DollarBillItem) {
                    player.getInventory().setStack(i, ItemStack.EMPTY);
                }
            }
            var ps = MarketPersistentState.getOrCreate(world.getServer().getOverworld());
            var portfolio = ps.getPortfolio(player.getUuid());
            portfolio.addCoins(totalCoins);
            ps.markDirty();
            sp.sendMessage(Text.literal(String.format(
                    "§aDeposited §e%d¢ §7into your account. Balance: §e%.1f¢", totalCoins, portfolio.getCoinBalance())), true);
            return ActionResult.SUCCESS;
        }

        BlockEntity be = world.getBlockEntity(pos);
        if (be instanceof TradingPostBlockEntity tp) player.openHandledScreen(tp);
        return ActionResult.SUCCESS;
    }
}
