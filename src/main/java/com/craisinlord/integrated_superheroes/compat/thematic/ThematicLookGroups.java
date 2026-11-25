package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.registries.armors.armor.CharacterAltItem;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class ThematicLookGroups {
    private static final Map<String, List<String>> MEMBERS = new ConcurrentHashMap<>();

    private ThematicLookGroups() {
    }

    public static void clearCache() {
        MEMBERS.clear();
    }

    public static boolean isSuit(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        return location != null && BuiltInRegistries.ITEM.containsKey(location)
                && BuiltInRegistries.ITEM.get(location) instanceof CharacterItem;
    }

    public static List<String> members(String base) {
        return MEMBERS.computeIfAbsent(base, ThematicLookGroups::findMembers);
    }

    private static List<String> findMembers(String base) {
        ResourceLocation location = ResourceLocation.tryParse(base);
        if (location == null) {
            return List.of();
        }
        List<ResourceLocation> found = new ArrayList<>();
        TagKey<Item> group = TagKey.create(Registries.ITEM, IntegratedSuperheroes.id("thematic_skin/" + location.getPath()));
        BuiltInRegistries.ITEM.getTag(group).ifPresent(tag -> tag.forEach(holder -> {
            if (holder.value() instanceof CharacterItem) {
                found.add(BuiltInRegistries.ITEM.getKey(holder.value()));
            }
        }));
        if (found.isEmpty()) {
            String prefix = location.getPath() + "_";
            for (Item item : BuiltInRegistries.ITEM) {
                if (!(item instanceof CharacterAltItem)) {
                    continue;
                }
                ResourceLocation key = BuiltInRegistries.ITEM.getKey(item);
                if (key.getNamespace().equals(location.getNamespace()) && key.getPath().startsWith(prefix)) {
                    found.add(key);
                }
            }
        }
        found.sort(Comparator.comparing(ResourceLocation::getPath));
        return found.stream().map(ResourceLocation::toString).toList();
    }
}
