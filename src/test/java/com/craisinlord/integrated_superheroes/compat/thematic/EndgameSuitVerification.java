package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.ability.script.AbilityScriptCompiler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class EndgameSuitVerification {
    public static void main(String[] args) throws Exception {
        Path resources = Path.of("src/main/resources");
        Path data = resources.resolve("data/thematic");
        Map<String, Path> scripts = new HashMap<>();
        try (var paths = Files.walk(data.resolve("ability"))) {
            paths.filter(path -> path.toString().endsWith(".js")).forEach(path -> scripts.put(path.getFileName().toString().replace(".js", ""), path));
        }
        Map<String, JsonObject> stats = new HashMap<>();
        try (var paths = Files.walk(data.resolve("ability_stats"))) {
            for (Path path : paths.filter(path -> path.toString().endsWith(".json")).toList()) {
                JsonParser.parseString(Files.readString(path)).getAsJsonObject().entrySet().forEach(entry -> stats.put(entry.getKey(), entry.getValue().getAsJsonObject()));
            }
        }
        for (String relative : List.of("brotherhood/apocalypse", "justice_league_dark/dr_fate", "cosmic_threats/darkseid")) {
            JsonObject armor = JsonParser.parseString(Files.readString(data.resolve("armors/" + relative + ".json"))).getAsJsonObject();
            if (armor.get("tier").getAsInt() != 10) {
                throw new AssertionError(relative + " must be tier 10");
            }
            Set<String> keys = new HashSet<>();
            Set<String> ids = new HashSet<>();
            int dedicated = 0;
            for (var element : armor.getAsJsonArray("abilities")) {
                JsonObject ability = element.getAsJsonObject();
                if (ability.has("remove") && ability.get("remove").getAsBoolean()) {
                    continue;
                }
                String id = ability.get("identifier").getAsString();
                if (!ids.add(id)) {
                    throw new AssertionError("Duplicate power " + id);
                }
                String key = ability.has("keybind") ? ability.get("keybind").getAsString() : "key.thematic.passive";
                if (!key.equals("key.thematic.passive")) {
                    if (!keys.add(key)) {
                        throw new AssertionError("Multiple keyed powers on " + key + " in " + relative);
                    }
                }
                if (id.startsWith("apocalypse_") || id.startsWith("fate_") || id.startsWith("darkseid_")) {
                    dedicated++;
                    Path script = scripts.get(id);
                    if (script == null || !stats.containsKey(id)) {
                        throw new AssertionError("Missing dedicated script or tuning: " + id);
                    }
                    JsonObject defaults = stats.get(id);
                    if (defaults.has("disabled") && defaults.get("disabled").getAsBoolean()) {
                        throw new AssertionError("Disabled power " + id);
                    }
                    List<String> errors = new ArrayList<>();
                    JsonObject compiled = AbilityScriptCompiler.compile(id, Files.readString(script), errors::add);
                    if (compiled == null || compiled.isEmpty() || !errors.isEmpty()) {
                        throw new AssertionError(id + ": " + errors);
                    }
                    Path output = Path.of("build/endgame-verification/" + id + ".json");
                    Files.createDirectories(output.getParent());
                    Files.writeString(output, compiled.toString());
                    System.out.println("Compiled " + id);
                }
            }
            if (keys.size() > 4 || keys.size() < 2 || !keys.contains("key.thematic.ultimate") || dedicated < 3) {
                throw new AssertionError("Expected compact dedicated endgame kit: " + relative + " " + keys);
            }
            System.out.println("Verified " + relative + ": " + keys.size() + " controls");
        }
    }
}

