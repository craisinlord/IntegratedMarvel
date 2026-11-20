package com.craisinlord.integrated_superheroes.voidworld.vehicles;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

final class VoidVehicleData extends SavedData {
    private static final String NAME = "integrated_superheroes_void_vehicles";
    private static final int CAP = 256;
    final Map<UUID, Entry> tracked = new LinkedHashMap<>();

    static final class Entry {
        long lastSeen;
        long farSince = -1L;

        Entry(long lastSeen) {
            this.lastSeen = lastSeen;
        }
    }

    static VoidVehicleData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(VoidVehicleData::new, VoidVehicleData::load, null), NAME);
    }

    void track(UUID id, long now) {
        tracked.put(id, new Entry(now));
        Iterator<UUID> oldest = tracked.keySet().iterator();
        while (tracked.size() > CAP && oldest.hasNext()) {
            oldest.next();
            oldest.remove();
        }
        setDirty();
    }

    private static VoidVehicleData load(CompoundTag tag, HolderLookup.Provider registries) {
        VoidVehicleData data = new VoidVehicleData();
        ListTag list = tag.getList("vehicles", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            if (entry.hasUUID("id")) {
                data.tracked.put(entry.getUUID("id"), new Entry(entry.getLong("seen")));
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag list = new ListTag();
        tracked.forEach((id, entry) -> {
            CompoundTag item = new CompoundTag();
            item.putUUID("id", id);
            item.putLong("seen", entry.lastSeen);
            list.add(item);
        });
        tag.put("vehicles", list);
        return tag;
    }
}
