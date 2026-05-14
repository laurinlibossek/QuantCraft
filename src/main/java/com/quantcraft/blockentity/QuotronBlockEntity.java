package com.quantcraft.blockentity;

import com.quantcraft.registry.ModBlockEntityTypes;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.util.math.BlockPos;
import java.util.*;

public class QuotronBlockEntity extends BlockEntity {
    public static final int MAX_TRACKED = 5;
    private static final String NBT_KEY = "TrackedTickers";

    private final List<String> trackedTickers = new ArrayList<>();

    public QuotronBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.QUOTRON, pos, state);
    }

    public List<String> getTrackedTickers() { return Collections.unmodifiableList(trackedTickers); }

    public void setTrackedTickers(List<String> tickers) {
        trackedTickers.clear();
        tickers.stream().limit(MAX_TRACKED).forEach(trackedTickers::add);
        markDirty();
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        NbtList list = new NbtList();
        for (String t : trackedTickers) list.add(NbtString.of(t));
        nbt.put(NBT_KEY, list);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        trackedTickers.clear();
        NbtList list = nbt.getList(NBT_KEY, 8);
        for (int i = 0; i < list.size(); i++) trackedTickers.add(list.getString(i));
    }
}
