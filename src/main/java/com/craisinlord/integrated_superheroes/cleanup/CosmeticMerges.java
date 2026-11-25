package com.craisinlord.integrated_superheroes.cleanup;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CosmeticMerges {
    private static final JsonObject POLICY = load();
    private static final Map<String, WingProfile> WING_PROFILES = buildWingProfiles();
    private static final String RECIPE_MARKER = "integrated_superheroes:cosmetic_merge";

    public record WingProfile(String id, ResourceLocation geo, ResourceLocation texture, String left, String right) {}

    private CosmeticMerges() {}

    private static JsonObject load() {
        try (var stream = CosmeticMerges.class.getResourceAsStream("/data/integrated_superheroes/cosmetic_merges.json")) {
            if (stream == null) throw new IllegalStateException("Missing cosmetic merge policy");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read cosmetic merge policy", error);
        }
    }

    public static boolean enabled() {
        return CatalogCleanup.enabled() && CleanupConfig.on(CleanupConfig.COSMETIC_MERGES);
    }

    public static List<String> looks(String power) {
        if (!enabled()) return List.of();
        var values = POLICY.getAsJsonObject("serumLooks").get(power);
        if (values == null) return List.of();
        List<String> result = new ArrayList<>();
        values.getAsJsonArray().forEach(value -> result.add(value.getAsString()));
        return List.copyOf(result);
    }

    public static boolean serumCostume(String id) {
        return enabled() && serumListed(id);
    }

    public static boolean serumListed(String id) {
        return POLICY.getAsJsonObject("serumLooks").entrySet().stream()
                .anyMatch(entry -> contains(entry.getValue(), id));
    }

    public static boolean retiredSuit(String id) {
        return enabled() && contains(POLICY.get("retiredSuits"), id);
    }

    public static ResourceLocation baseSuit(ResourceLocation id) {
        if (!enabled() || id == null) return null;
        JsonElement base = POLICY.getAsJsonObject("suitAliases").get(id.toString());
        return base == null ? null : ResourceLocation.parse(base.getAsString());
    }

    public static boolean hidden(ResourceLocation id) {
        if (!CatalogCleanup.enabled() || id == null) return false;
        return serumCostume(id.toString()) || retiredSuit(id.toString()) || baseSuit(id) != null
                || CleanupConfig.on(CleanupConfig.HIDE_UNUSED_STATIONS) && contains(POLICY.get("hiddenStationProps"), id.toString());
    }

    private static boolean contains(JsonElement values, String id) {
        if (values == null) return false;
        for (var value : values.getAsJsonArray()) if (value.getAsString().equals(id)) return true;
        return false;
    }

    public static Map<ResourceLocation, JsonObject> applySuitDefinitions(Map<ResourceLocation, JsonObject> definitions, String folder) {
        if (!enabled() || !folder.equals("armors")) return definitions;
        Map<ResourceLocation, JsonObject> result = new LinkedHashMap<>(definitions);
        for (var entry : POLICY.getAsJsonObject("suitAliases").entrySet()) {
            ResourceLocation alias = definitionKey(definitions, ResourceLocation.parse(entry.getKey()));
            ResourceLocation base = definitionKey(definitions, ResourceLocation.parse(entry.getValue().getAsString()));
            JsonObject original = definitions.get(alias);
            JsonObject parent = definitions.get(base);
            if (original == null || parent == null) continue;
            JsonObject merged = original.deepCopy();
            for (String field : Set.of("abilities", "stats", "tier", "flight_type", "flight_profile", "fighting_type")) {
                if (parent.has(field)) merged.add(field, parent.get(field).deepCopy());
                else merged.remove(field);
            }
            result.put(alias, merged);
        }
        return result;
    }

    private static ResourceLocation definitionKey(Map<ResourceLocation, JsonObject> definitions, ResourceLocation id) {
        if (definitions.containsKey(id)) return id;
        return definitions.keySet().stream().filter(key -> key.getNamespace().equals(id.getNamespace())
                && key.getPath().endsWith("/" + id.getPath())).findFirst().orElse(id);
    }

    public static boolean rewriteProgressionRecipe(JsonObject recipe) {
        if (!recipe.has("type") || !recipe.get("type").getAsString().equals("marvel:suit_upgrading")) return false;
        JsonElement result = recipe.has("results") ? recipe.get("results") : recipe.get("result");
        if (result == null || !result.isJsonObject()) return false;
        JsonObject output = result.getAsJsonObject();
        if (!output.has("id") || !output.has("components")) return false;
        JsonElement suit = output.getAsJsonObject("components").get("integrated_superheroes:suit");
        if (suit == null || !suit.getAsString().equals("thematic:ultron")) return false;
        String slot = output.get("id").getAsString().replace("integrated_superheroes:suit_", "");
        if (!Set.of("helmet", "leggings", "boots").contains(slot)) return false;
        boolean requireIronMan = CatalogCleanup.enabled() && CleanupConfig.on(CleanupConfig.ULTRON_REQUIRES_IRON_MAN);
        JsonObject base = new JsonObject();
        base.addProperty("item", (requireIronMan ? "marvel:iron_man_mark_7_" : "minecraft:leather_") + slot);
        if (base.equals(recipe.get("suit"))) return false;
        recipe.add("suit", base);
        return true;
    }

    public static boolean dropsRecipe(JsonObject recipe) {
        if (recipe.has(RECIPE_MARKER) && recipe.get(RECIPE_MARKER).getAsBoolean()) return !enabled();
        if (!enabled()) return false;
        for (String key : Set.of("result", "results", "output", "outputs")) {
            if (recipe.has(key) && aliasOutput(recipe.get(key))) return true;
        }
        return false;
    }

    private static boolean aliasOutput(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            return POLICY.getAsJsonObject("suitAliases").has(value.getAsString());
        }
        if (value.isJsonArray()) {
            for (var child : value.getAsJsonArray()) if (aliasOutput(child)) return true;
        } else if (value.isJsonObject()) {
            var object = value.getAsJsonObject();
            if (object.has("components") && object.get("components").isJsonObject()) {
                var suit = object.getAsJsonObject("components").get("integrated_superheroes:suit");
                if (suit != null && aliasOutput(suit)) return true;
            }
            for (String key : Set.of("id", "item", "items")) if (object.has(key) && aliasOutput(object.get(key))) return true;
        }
        return false;
    }

    private static Map<String, WingProfile> buildWingProfiles() {
        Map<String, WingProfile> result = new LinkedHashMap<>();
        POLICY.getAsJsonObject("wingProfiles").entrySet().forEach(entry -> {
            var value = entry.getValue().getAsJsonObject();
            result.put(entry.getKey(), new WingProfile(entry.getKey(), ResourceLocation.parse(value.get("geo").getAsString()),
                    ResourceLocation.parse(value.get("texture").getAsString()), value.get("left").getAsString(), value.get("right").getAsString()));
        });
        return Map.copyOf(result);
    }

    public static Map<String, WingProfile> wingProfiles() {
        return WING_PROFILES;
    }

    public static WingProfile wingProfile(String costume) {
        var profiles = WING_PROFILES;
        return enabled() ? profiles.getOrDefault(costume, profiles.get("angel")) : profiles.get("angel");
    }
}
