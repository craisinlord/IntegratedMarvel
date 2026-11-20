package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class VariantPoolData {
    public static final ResourceLocation POOL = IntegratedSuperheroes.id("void_variants/pool.json");
    public static final ResourceLocation MOB_MOVES = IntegratedSuperheroes.id("void_variants/thematic_mob_moves.json");

    public final Map<VariantKind, Double> budgets = new EnumMap<>(VariantKind.class);
    public final Set<String> excluded = new HashSet<>();
    public final Set<String> excludedCollections = new HashSet<>();
    public final Map<String, Double> weights = new HashMap<>();
    public final Map<String, Integer> gates = new HashMap<>();
    public final Set<String> villainCollections = new HashSet<>();
    public final Set<String> villains = new HashSet<>();
    public final Set<String> heroes = new HashSet<>();
    public final Set<String> marvelKits = new HashSet<>();
    public final Set<String> legacySerums = new HashSet<>();
    public final Map<String, String> marvelCostumes = new HashMap<>();
    public final List<EntityEntry> entities = new java.util.ArrayList<>();
    public final Map<String, MobMove> mobMoves = new LinkedHashMap<>();

    public record EntityEntry(String id, VariantKind kind, int gate, VariantAlignment alignment) {
    }

    public record MobMove(double minRange, double maxRange, int cooldownTicks, int weight) {
    }

    public static VariantPoolData load(ResourceManager manager) {
        VariantPoolData data = new VariantPoolData();
        for (VariantKind kind : VariantKind.values()) {
            data.budgets.put(kind, 0.0D);
        }
        read(manager, POOL).ifPresent(data::readPool);
        read(manager, MOB_MOVES).ifPresent(data::readMoves);
        return data;
    }

    private static Optional<JsonObject> read(ResourceManager manager, ResourceLocation id) {
        Optional<Resource> resource = manager.getResource(id);
        if (resource.isEmpty()) {
            IntegratedSuperheroes.LOGGER.warn("Void Variants: {} is missing", id);
            return Optional.empty();
        }
        try (Reader reader = resource.get().openAsReader()) {
            return Optional.of(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception error) {
            IntegratedSuperheroes.LOGGER.error("Void Variants: could not read {}", id, error);
            return Optional.empty();
        }
    }

    private void readPool(JsonObject json) {
        if (json.has("budgets")) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("budgets").entrySet()) {
                VariantKind kind = VariantKind.byKey(entry.getKey());
                if (kind != null) {
                    budgets.put(kind, entry.getValue().getAsDouble());
                }
            }
        }
        strings(json, "exclude", excluded);
        strings(json, "exclude_collections", excludedCollections);
        strings(json, "villain_collections", villainCollections);
        strings(json, "villains", villains);
        strings(json, "heroes", heroes);
        strings(json, "marvel_kits", marvelKits);
        strings(json, "legacy_serums", legacySerums);
        if (json.has("weights")) {
            json.getAsJsonObject("weights").entrySet().forEach(entry -> weights.put(entry.getKey(), entry.getValue().getAsDouble()));
        }
        if (json.has("marvel_costumes")) {
            json.getAsJsonObject("marvel_costumes").entrySet().forEach(entry -> marvelCostumes.put(entry.getKey(), entry.getValue().getAsString()));
        }
        if (json.has("gates")) {
            json.getAsJsonObject("gates").entrySet().forEach(entry -> gates.put(entry.getKey(), entry.getValue().getAsInt()));
        }
        if (json.has("entities")) {
            for (JsonElement element : json.getAsJsonArray("entities")) {
                JsonObject entry = element.getAsJsonObject();
                VariantKind kind = entry.has("source") ? VariantKind.byKey(entry.get("source").getAsString()) : VariantKind.ENTITY;
                entities.add(new EntityEntry(entry.get("id").getAsString(), kind == null ? VariantKind.ENTITY : kind,
                        entry.has("gate") ? entry.get("gate").getAsInt() : 2,
                        VariantAlignment.byKey(entry.has("alignment") ? entry.get("alignment").getAsString() : "villain")));
            }
        }
    }

    private void readMoves(JsonObject json) {
        if (!json.has("moves")) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("moves").entrySet()) {
            JsonObject move = entry.getValue().getAsJsonObject();
            mobMoves.put(entry.getKey(), new MobMove(
                    number(move, "min_range", 0.0D),
                    number(move, "max_range", 16.0D),
                    (int) Math.round(number(move, "cooldown", 6.0D) * 20.0D),
                    (int) number(move, "weight", 10.0D)));
        }
    }

    private static double number(JsonObject json, String key, double fallback) {
        return json.has(key) ? json.get(key).getAsDouble() : fallback;
    }

    private static void strings(JsonObject json, String key, Set<String> into) {
        if (!json.has(key)) {
            return;
        }
        JsonArray array = json.getAsJsonArray(key);
        array.forEach(element -> into.add(element.getAsString()));
    }

    public double weight(String familyKey) {
        return weights.getOrDefault(familyKey, 1.0D);
    }

    public int gate(String familyKey, int fallback) {
        return Math.min(VariantTiers.MAX_GATE, Math.max(0, gates.getOrDefault(familyKey, fallback)));
    }

    public VariantAlignment alignment(String lookKey, String familyKey, VariantAlignment fallback) {
        if (lookKey != null) {
            if (heroes.contains(lookKey)) {
                return VariantAlignment.HERO;
            }
            if (villains.contains(lookKey)) {
                return VariantAlignment.VILLAIN;
            }
        }
        if (heroes.contains(familyKey)) {
            return VariantAlignment.HERO;
        }
        if (villains.contains(familyKey)) {
            return VariantAlignment.VILLAIN;
        }
        return fallback;
    }
}
