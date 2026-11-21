package com.craisinlord.integrated_superheroes.voidrealm.variant.moves;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ThematicKits {
    private static volatile Map<String, List<String>> kits = Map.of();

    private ThematicKits() {
    }

    public static void load(ResourceManager manager) {
        Map<String, List<String>> out = new HashMap<>();
        Map<ResourceLocation, Resource> files = manager.listResources("void_variants/thematic_kits",
                id -> id.getNamespace().equals(IntegratedSuperheroes.MODID) && id.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            try (Reader reader = file.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject map = json.has("kits") ? json.getAsJsonObject("kits") : json;
                for (Map.Entry<String, JsonElement> entry : map.entrySet()) {
                    if (!entry.getValue().isJsonArray()) {
                        continue;
                    }
                    List<String> moves = new ArrayList<>();
                    for (JsonElement move : entry.getValue().getAsJsonArray()) {
                        String id = move.getAsString();
                        if (VariantMoves.get(id) != null) {
                            moves.add(id);
                        } else {
                            IntegratedSuperheroes.LOGGER.warn("Void Variants: unknown move {} for {} in {}", id, entry.getKey(), file.getKey());
                        }
                    }
                    if (!moves.isEmpty()) {
                        out.put(entry.getKey(), List.copyOf(moves));
                    }
                }
            } catch (Exception error) {
                IntegratedSuperheroes.LOGGER.error("Void Variants: could not read {}", file.getKey(), error);
            }
        }
        kits = Map.copyOf(out);
        IntegratedSuperheroes.LOGGER.info("Void Variants: {} Thematic suit kits loaded", out.size());
    }

    public static List<String> kitFor(String family, String look) {
        Map<String, List<String>> current = kits;
        List<String> kit = current.get("look:" + look);
        if (kit == null) {
            kit = current.get(look);
        }
        if (kit == null) {
            kit = current.get(family);
        }
        return kit == null ? List.of() : kit;
    }
}
