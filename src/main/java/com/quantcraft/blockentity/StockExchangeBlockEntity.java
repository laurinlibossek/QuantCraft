package com.quantcraft.blockentity;

import com.quantcraft.market.*;
import com.quantcraft.persistence.MarketPersistentState;
import com.quantcraft.registry.ModBlockEntityTypes;
import com.quantcraft.screen.StockExchangeScreenHandler;
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
import java.util.Map;

public class StockExchangeBlockEntity extends BlockEntity implements ExtendedScreenHandlerFactory {
    public StockExchangeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.STOCK_EXCHANGE, pos, state);
    }

    @Override public Text getDisplayName() { return Text.translatable("container.quantcraft.stock_exchange"); }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        Map<String,StockState> snap = MarketEngine.getInstance().getSnapshot();
        buf.writeInt(snap.size());
        for (var e : snap.entrySet()) {
            StockState s = e.getValue();
            buf.writeString(e.getKey());
            buf.writeDouble(s.getCurrentPrice());
            buf.writeDouble(s.getDailyChangePercent());
            var hist = s.getPriceHistory();
            buf.writeInt(hist.size());
            for (double p : hist) buf.writeDouble(p);
            var candles = s.getCandleHistory().getCandles();
            buf.writeInt(candles.size());
            for (var c : candles) {
                buf.writeDouble(c.getOpen());
                buf.writeDouble(c.getHigh());
                buf.writeDouble(c.getLow());
                buf.writeDouble(c.getClose());
                buf.writeBoolean(c.isBullish());
            }
            StockDefinition def = StockRegistry.get(e.getKey());
            buf.writeInt(def != null ? def.totalShares() : 0);
            buf.writeInt(s.getSharesHeld());
        }
        if (world != null && world.getServer() != null) {
            var ps = MarketPersistentState.getOrCreate(world.getServer().getOverworld());
            PlayerPortfolio p = ps.getPortfolio(player.getUuid());
            buf.writeDouble(p.getBalance());
            buf.writeInt(p.getHoldings().size());
            p.getHoldings().forEach((t, q) -> { buf.writeString(t); buf.writeInt(q); });
        } else {
            buf.writeDouble(0); buf.writeInt(0);
        }
        var news = MarketEngine.getInstance().getRecentNews();
        buf.writeInt(news.size());
        news.forEach(buf::writeString);
        if (world != null && world.getServer() != null) {
            var ps     = MarketPersistentState.getOrCreate(world.getServer().getOverworld());
            var shorts = ps.getShorts(player.getUuid());
            buf.writeInt(shorts.size());
            for (var sp : shorts) {
                StockState ss  = MarketEngine.getInstance().getState(sp.getTicker());
                double     cur = ss != null ? ss.getCurrentPrice() : sp.getOpenPrice();
                buf.writeString(sp.getTicker());
                buf.writeInt(sp.getShares());
                buf.writeDouble(sp.getOpenPrice());
                buf.writeDouble(cur);
                buf.writeDouble(sp.getCurrentPnL(cur));
                buf.writeDouble(sp.getAccruedFee());
                buf.writeDouble(sp.getMarginReserve());
            }
        } else {
            buf.writeInt(0);
        }
        if (world != null && world.getServer() != null) {
            var ps = MarketPersistentState.getOrCreate(world.getServer().getOverworld());
            var messages = ps.getPlayerMessages(player.getUuid());
            buf.writeInt(messages.size());
            for (var msg : messages) {
                buf.writeLong(msg.timestamp());
                buf.writeString(msg.text());
                buf.writeEnumConstant(msg.type());
            }
        } else {
            buf.writeInt(0);
        }
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new StockExchangeScreenHandler(syncId, inv, new PacketByteBuf(Unpooled.buffer()));
    }
}
