package com.craisinlord.integrated_marvel.gauntlet;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SoulRitualSavedData extends SavedData {
    public static final String DATA_ID = "integrated_marvel_soul_ritual";
    private static final String COOLDOWNS_KEY = "Cooldowns";
    private final Map<UUID, Long> cooldowns = new HashMap<>();

    public static final Factory<SoulRitualSavedData> FACTORY = new Factory<>(
            SoulRitualSavedData::new,
            SoulRitualSavedData::load,
            DataFixTypes.LEVEL
    );

    public static SoulRitualSavedData load(CompoundTag tag, HolderLookup.Provider registries) {
        SoulRitualSavedData data = new SoulRitualSavedData();
        ListTag entries = tag.getList(COOLDOWNS_KEY, Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            try {
                data.cooldowns.put(UUID.fromString(entry.getString("Player")), entry.getLong("Until"));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag entries = new ListTag();
        for (Map.Entry<UUID, Long> cooldown : cooldowns.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString("Player", cooldown.getKey().toString());
            entry.putLong("Until", cooldown.getValue());
            entries.add(entry);
        }
        tag.put(COOLDOWNS_KEY, entries);
        return tag;
    }

    public long cooldownUntil(UUID player) {
        return cooldowns.getOrDefault(player, 0L);
    }

    public void setCooldown(UUID player, long until) {
        cooldowns.put(player, until);
        setDirty();
    }
}
