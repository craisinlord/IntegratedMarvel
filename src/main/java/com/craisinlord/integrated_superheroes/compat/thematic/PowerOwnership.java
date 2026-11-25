package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.cleanup.CosmeticMerges;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PowerOwnership {
    private static final JsonObject POLICY = load();
    private static final Set<String> COSTUMES = Set.copyOf(POLICY.getAsJsonObject("costumes").keySet());
    private static final Map<String, List<String>> LOOKS = looks();
    private static final Map<Item, Kind> KINDS = new ConcurrentHashMap<>();

    private enum Kind { POWERED, COSTUME, SERUM_COSTUME }

    private PowerOwnership() {
    }

    public static boolean costume(Item item) {
        Kind kind = KINDS.get(item);
        if (kind == null) {
            String id = BuiltInRegistries.ITEM.getKey(item).toString();
            kind = COSTUMES.contains(id) ? Kind.COSTUME : CosmeticMerges.serumListed(id) ? Kind.SERUM_COSTUME : Kind.POWERED;
            KINDS.put(item, kind);
        }
        return kind == Kind.COSTUME || kind == Kind.SERUM_COSTUME && CosmeticMerges.enabled();
    }

    public static boolean costume(String suit) {
        return COSTUMES.contains(suit) || CosmeticMerges.serumCostume(suit);
    }

    public static List<String> looks(String power) {
        List<String> result = new ArrayList<>(LOOKS.getOrDefault(power, List.of()));
        result.addAll(CosmeticMerges.looks(power));
        return List.copyOf(result);
    }

    private static JsonObject load() {
        try (InputStream input = PowerOwnership.class.getResourceAsStream("/data/integrated_superheroes/power_ownership.json")) {
            if (input == null) {
                throw new IllegalStateException("Missing power ownership policy");
            }
            return JsonParser.parseReader(new InputStreamReader(input, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load power ownership policy", error);
        }
    }

    private static Map<String, List<String>> looks() {
        Map<String, List<String>> result = new LinkedHashMap<>();
        POLICY.getAsJsonObject("looks").entrySet().forEach(entry -> {
            List<String> members = new ArrayList<>();
            entry.getValue().getAsJsonArray().forEach(member -> {
                String suit = member.getAsString();
                if (!COSTUMES.contains(suit)) {
                    throw new IllegalStateException("Powered appearance is not a costume: " + suit);
                }
                members.add(suit);
            });
            result.put(entry.getKey(), List.copyOf(members));
        });
        return Map.copyOf(result);
    }
}
