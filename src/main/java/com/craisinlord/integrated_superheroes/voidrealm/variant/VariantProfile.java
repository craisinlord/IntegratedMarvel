package com.craisinlord.integrated_superheroes.voidrealm.variant;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.List;

public record VariantProfile(VariantKind kind, String family, String look, int tier, VariantAlignment alignment, List<String> moves) {
    public static final String KEY = "integrated_superheroes_variant";

    public VariantProfile {
        moves = List.copyOf(moves);
    }

    public VariantProfile withMoves(List<String> picked) {
        return new VariantProfile(kind, family, look, tier, alignment, picked);
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("Kind", kind.key());
        tag.putString("Family", family);
        tag.putString("Look", look);
        tag.putInt("Tier", tier);
        tag.putString("Alignment", alignment.key());
        ListTag list = new ListTag();
        moves.forEach(move -> list.add(StringTag.valueOf(move)));
        tag.put("Moves", list);
        return tag;
    }

    public static VariantProfile load(CompoundTag tag) {
        VariantKind kind = VariantKind.byKey(tag.getString("Kind"));
        if (kind == null || tag.getString("Family").isEmpty()) {
            return null;
        }
        List<String> moves = new ArrayList<>();
        ListTag list = tag.getList("Moves", Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            moves.add(list.getString(i));
        }
        return new VariantProfile(kind, tag.getString("Family"), tag.getString("Look"), tag.getInt("Tier"),
                VariantAlignment.byKey(tag.getString("Alignment")), moves);
    }

    public static VariantProfile of(Entity entity) {
        CompoundTag root = entity.getPersistentData();
        return root.contains(KEY, Tag.TAG_COMPOUND) ? load(root.getCompound(KEY)) : null;
    }

    public void write(Entity entity) {
        entity.getPersistentData().put(KEY, save());
    }
}
