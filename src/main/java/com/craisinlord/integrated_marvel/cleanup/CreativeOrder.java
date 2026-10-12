package com.craisinlord.integrated_marvel.cleanup;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CreativeOrder {
    private static final String FILE = "/assets/integrated_marvel/creative_order.json";
    private static final List<String> SLOTS = List.of("_sleeveless_chestplate", "_slim_chestplate", "_helmet", "_chestplate", "_leggings", "_boots", "_necklace");
    private static final List<Integer> SLOT_RANK = List.of(2, 2, 0, 1, 3, 4, 5);
    private static CreativeOrder instance;

    private final List<String> sections = new ArrayList<>();
    private final Map<ResourceLocation, int[]> exact = new HashMap<>();
    private final List<Prefix> prefixes = new ArrayList<>();

    private CreativeOrder() {
        try (InputStream stream = CreativeOrder.class.getResourceAsStream(FILE)) {
            if (stream == null) {
                return;
            }
            JsonObject root = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement sectionElement : root.getAsJsonArray("sections")) {
                JsonObject section = sectionElement.getAsJsonObject();
                int sectionIndex = this.sections.size();
                this.sections.add(section.get("id").getAsString());
                int entryIndex = 0;
                for (JsonElement entry : section.getAsJsonArray("entries")) {
                    String value = entry.getAsString();
                    if (value.endsWith("*")) {
                        ResourceLocation id = ResourceLocation.parse(value.substring(0, value.length() - 1));
                        this.prefixes.add(new Prefix(id.getNamespace(), id.getPath(), sectionIndex, entryIndex));
                    } else {
                        this.exact.putIfAbsent(ResourceLocation.parse(value), new int[]{sectionIndex, entryIndex});
                    }
                    entryIndex++;
                }
            }
            this.prefixes.sort(Comparator.comparingInt((Prefix prefix) -> prefix.path.length()).reversed());
        } catch (Exception exception) {
            IntegratedMarvel.LOGGER.error("Couldn't read the creative tab order", exception);
        }
    }

    static CreativeOrder get() {
        if (instance == null) {
            instance = new CreativeOrder();
        }
        return instance;
    }

    Key key(ResourceLocation id, String fallbackSection) {
        int[] found = this.exact.get(id);
        if (found != null) {
            return new Key(found[0], found[1], "", 0);
        }
        String path = id.getPath();
        int slot = 0;
        String base = path;
        for (int i = 0; i < SLOTS.size(); i++) {
            if (path.endsWith(SLOTS.get(i))) {
                slot = SLOT_RANK.get(i);
                base = path.substring(0, path.length() - SLOTS.get(i).length());
                break;
            }
        }
        for (Prefix prefix : this.prefixes) {
            if (prefix.namespace.equals(id.getNamespace()) && path.startsWith(prefix.path)) {
                return new Key(prefix.section, prefix.entry, base, slot);
            }
        }
        int section = this.sections.indexOf(fallbackSection);
        if (section < 0) {
            section = this.sections.size();
        }
        return new Key(section, Integer.MAX_VALUE, base, slot);
    }

    record Key(int section, int entry, String base, int slot) implements Comparable<Key> {
        @Override
        public int compareTo(Key other) {
            int result = Integer.compare(this.section, other.section);
            if (result == 0) {
                result = Integer.compare(this.entry, other.entry);
            }
            if (result == 0) {
                result = natural(this.base, other.base);
            }
            if (result == 0) {
                result = Integer.compare(this.slot, other.slot);
            }
            return result;
        }
    }

    static int natural(String a, String b) {
        int i = 0;
        int j = 0;
        while (i < a.length() && j < b.length()) {
            char ca = a.charAt(i);
            char cb = b.charAt(j);
            if (Character.isDigit(ca) && Character.isDigit(cb)) {
                int startA = i;
                int startB = j;
                while (i < a.length() && Character.isDigit(a.charAt(i))) {
                    i++;
                }
                while (j < b.length() && Character.isDigit(b.charAt(j))) {
                    j++;
                }
                int result = Long.compare(Long.parseLong(a.substring(startA, i)), Long.parseLong(b.substring(startB, j)));
                if (result != 0) {
                    return result;
                }
            } else {
                if (ca != cb) {
                    return Character.compare(ca, cb);
                }
                i++;
                j++;
            }
        }
        return Integer.compare(a.length() - i, b.length() - j);
    }

    private record Prefix(String namespace, String path, int section, int entry) {
    }
}
