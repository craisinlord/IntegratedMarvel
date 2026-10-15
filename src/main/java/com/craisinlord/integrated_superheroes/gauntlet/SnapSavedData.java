package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public final class SnapSavedData extends SavedData {
    public static final String DATA_ID = "integrated_superheroes_snap";
    private static final String ENTRIES_KEY = "Snapped";
    private static final int MAX_ENTRIES = 4096;
    private final List<CompoundTag> entries = new ArrayList<>();

    public static final Factory<SnapSavedData> FACTORY = new Factory<>(
            SnapSavedData::new,
            SnapSavedData::load,
            DataFixTypes.LEVEL
    );

    public static SnapSavedData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, DATA_ID);
    }

    public static SnapSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SnapSavedData data = new SnapSavedData();
        ListTag list = tag.getList(ENTRIES_KEY, Tag.TAG_COMPOUND);
        for (int index = 0; index < list.size(); index++) data.entries.add(list.getCompound(index));
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        list.addAll(entries);
        tag.put(ENTRIES_KEY, list);
        return tag;
    }

    public void replace(List<CompoundTag> snapped) {
        entries.clear();
        entries.addAll(snapped.subList(0, Math.min(snapped.size(), MAX_ENTRIES)));
        setDirty();
    }

    public void add(CompoundTag tag) {
        if (entries.size() >= MAX_ENTRIES) return;
        entries.add(tag);
        setDirty();
    }

    public List<CompoundTag> drain() {
        List<CompoundTag> drained = List.copyOf(entries);
        entries.clear();
        setDirty();
        return drained;
    }

    public int size() {
        return entries.size();
    }
}
