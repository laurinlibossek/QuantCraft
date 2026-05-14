package com.quantcraft.blockentity;

import com.quantcraft.market.*;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.registry.ModBlockEntityTypes;
import com.quantcraft.screen.CommodityExchangeScreenHandler;
import io.netty.buffer.Unpooled;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.*;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class CommodityExchangeBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    public CommodityExchangeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.COMMODITY_EXCHANGE, pos, state);
    }

    @Override public Text getDisplayName() { return Text.translatable("container.quantcraft.commodity_exchange"); }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        var snap = MarketEngine.getInstance().getSnapshot();
        buf.writeInt(StockRegistry.getAll().size());
        for (StockDefinition def : StockRegistry.getAll()) {
            StockState ss = snap.get(def.ticker());
            double sp = ss != null ? ss.getCurrentPrice() : def.basePrice();
            buf.writeString(def.ticker());
            buf.writeDouble(def.getItemBuyPrice(sp));
            buf.writeDouble(def.getItemSellPrice(sp));
            int held = (int) player.getInventory().main.stream()
                    .filter(s -> !s.isEmpty() && s.getItem() == def.getItem())
                    .mapToInt(net.minecraft.item.ItemStack::getCount).sum();
            buf.writeInt(held);
        }
        if (world != null && world.getServer() != null) {
            var ps = MarketPersistentState.getOrCreate(world.getServer().getOverworld());
            buf.writeDouble(ps.getPortfolio(player.getUuid()).getCoinBalance());
        } else {
            buf.writeDouble(0);
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new CommodityExchangeScreenHandler(syncId, inv, new PacketByteBuf(Unpooled.buffer()));
    }
}
