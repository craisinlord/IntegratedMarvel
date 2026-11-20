package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.content.ability.script.AbilityScriptCompiler;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DoomVerification {
    public static void main(String[] args) throws Exception {
        Path resources = Path.of("src/main/resources");
        List<String> errors = new ArrayList<>();
        for (String id : List.of("doom_gauntlet", "doom_aegis", "doom_ascendant")) {
            String script = Files.readString(resources.resolve("data/thematic/ability/doom/" + id + ".js"));
            JsonObject result = AbilityScriptCompiler.compile(id, script, errors::add);
            if (result == null || result.isEmpty() || !errors.isEmpty()) {
                throw new AssertionError(id + ": " + errors);
            }
            Path output = Path.of("build/doom-verification/" + id + ".json");
            Files.createDirectories(output.getParent());
            Files.writeString(output, result.toString());
            if (id.equals("doom_aegis")) {
                String compiled = result.toString();
                if (!compiled.contains("doom_recall") || !compiled.contains("press") || !compiled.contains("doom_end_aegis")) {
                    throw new AssertionError("Aegis lost its second press or cleanup");
                }
            }
            System.out.println("Compiled " + id);
        }
        JsonObject armor = JsonParser.parseString(Files.readString(resources.resolve("data/thematic/armors/marvel_villains/dr_doom.json"))).getAsJsonObject();
        Set<String> keys = new HashSet<>();
        armor.getAsJsonArray("abilities").forEach(element -> keys.add(element.getAsJsonObject().get("keybind").getAsString()));
        if (!keys.equals(Set.of("key.thematic.ability_0", "key.thematic.ability_1", "key.thematic.ability_2", "key.thematic.ultimate"))) {
            throw new AssertionError("Doom must have exactly four controls: " + keys);
        }
        JsonObject stats = JsonParser.parseString(Files.readString(resources.resolve("data/thematic/ability_stats/doom.json"))).getAsJsonObject();
        if (stats.size() != 3 || stats.entrySet().stream().anyMatch(entry -> entry.getValue().getAsJsonObject().get("disabled").getAsBoolean())) {
            throw new AssertionError("Dedicated powers must be enabled");
        }
        System.out.println("Verified four controls, dedicated enabled powers, and compiled recall cleanup");
    }
}
