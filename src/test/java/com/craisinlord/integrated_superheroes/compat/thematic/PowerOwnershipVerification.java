package com.craisinlord.integrated_superheroes.compat.thematic;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public final class PowerOwnershipVerification {
    private static int checks;

    public static void main(String[] args) throws Exception {
        Path resources = Path.of("src/main/resources/data/integrated_superheroes");
        JsonObject policy = read(resources.resolve("power_ownership.json"));
        JsonObject costumes = policy.getAsJsonObject("costumes");
        require(costumes.size() == 397, "Reviewed costume inventory changed");
        for (String suit : Set.of("clayface", "circe", "human_torch", "invisible_woman", "mr_fantastic", "the_thing",
                "gentleman_ghost", "kabal", "the_deep", "bruce_banner", "batson", "soldier_boy", "thanos", "batman_dkos")) {
            require(PowerOwnership.costume("thematic:" + suit), "Wearable power source escaped: " + suit);
        }
        for (String suit : Set.of("batman", "batman_arkham", "iron_man", "war_machine", "ant_man", "wasp", "dr_doom",
                "dr_fate", "hal_jordan", "sinestro", "villain_joker", "villain_ultron", "general_zod")) {
            require(!PowerOwnership.costume("thematic:" + suit), "Equipment/NPC kit was blocked: " + suit);
        }
        require(!PowerOwnership.costume("minecraft:diamond_chestplate"), "Vanilla armor was blocked");
        require(PowerOwnership.looks("unknown").isEmpty(), "Unknown powers gained appearances");
        Set<String> catalog = strings(read(resources.resolve("catalog_cleanup.json")).getAsJsonArray("costumes"));
        Set<String> costumeTag = strings(read(resources.resolve("tags/item/thematic_costume.json")).getAsJsonArray("values"));
        for (String suit : costumes.keySet()) {
            require(PowerOwnership.costume(suit), "Policy did not load " + suit);
            require(catalog.contains(suit), "Catalog escaped " + suit);
            require(costumeTag.contains(suit), "Costume classification escaped " + suit);
        }
        for (var entry : policy.getAsJsonObject("looks").entrySet()) {
            require(PowerOwnership.looks(entry.getKey()).containsAll(strings(entry.getValue().getAsJsonArray())), "Missing look group " + entry.getKey());
            for (JsonElement element : entry.getValue().getAsJsonArray()) {
                String suit = element.getAsString();
                require(strings(costumes.getAsJsonArray(suit)).contains(entry.getKey()), "Appearance assigned to wrong serum " + suit);
                Path group = resources.resolve("tags/item/thematic_skin/" + suit.substring(suit.indexOf(':') + 1) + ".json");
                require(strings(read(group).getAsJsonArray("values")).contains(suit), "Missing explicit appearance group " + suit);
            }
        }
        require(PowerOwnership.looks("chaos_magic_wanda_maximoff").contains("thematic:circe"), "Circe magic route missing");
        require(PowerOwnership.looks("super_soldier_steve_rogers").contains("thematic:soldier_boy"), "Soldier Boy route missing");
        require(!PowerOwnership.looks("kryptonian_clark_kent").contains("thematic:batman"), "Ordinary Batman became Kryptonian");
        ClassNode mixin = new ClassNode();
        try (var input = PowerOwnershipVerification.class.getResourceAsStream("/com/craisinlord/integrated_superheroes/mixin/ThematicFullSetMixin.class")) {
            require(input != null, "Ownership mixin missing");
            new ClassReader(input).accept(mixin, 0);
        }
        var stackGate = mixin.methods.stream().filter(method -> method.name.equals("integratedSuperheroes$serumCostumeStack")).findFirst().orElseThrow();
        boolean playerGate = false;
        boolean policyGate = false;
        boolean returnOverride = false;
        for (var instruction : stackGate.instructions) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == Opcodes.INSTANCEOF && type.desc.equals("net/minecraft/world/entity/player/Player")) {
                playerGate = true;
            }
            if (instruction instanceof MethodInsnNode call) {
                policyGate |= call.owner.endsWith("/PowerOwnership") && call.name.equals("costume");
                returnOverride |= call.name.equals("setReturnValue");
            }
        }
        require(playerGate && policyGate && returnOverride, "Armor-stack gate lost player-only ownership enforcement");
        require(stackGate.visibleAnnotations.stream().anyMatch(annotation -> annotation.values.toString().contains("getArmorStack(Lnet/minecraft/world/entity/LivingEntity;)")), "Wrong stack overload targeted");
        ClassNode fastMixin = new ClassNode();
        try (var input = PowerOwnershipVerification.class.getResourceAsStream("/com/craisinlord/integrated_superheroes/mixin/ThematicArmorStackFastMixin.class")) {
            require(input != null, "Armor stack fast path missing");
            new ClassReader(input).accept(fastMixin, 0);
        }
        var fastGate = fastMixin.methods.stream().filter(method -> method.name.equals("integratedSuperheroes$chestOnly")).findFirst().orElseThrow();
        boolean fastPolicy = false;
        for (var instruction : fastGate.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.endsWith("/PowerOwnership") && call.name.equals("costume")) {
                fastPolicy = true;
            }
        }
        require(fastPolicy, "Cancelled HEAD fast path bypasses costume ownership");
        require(args.length == 1, "Supply the pack suit-band resource to verify");
        JsonObject bands = read(Path.of(args[0]));
        for (String suit : costumes.keySet()) {
            require(!bands.has(suit), "Cosmetic still grants pack armor/health bands: " + suit);
        }
        require(bands.has("thematic:dr_doom"), "Doom armor band lost");
        System.out.println("Verified " + checks + " ownership, appearance, catalog, attribute-band and player-only hook checks");
    }

    private static JsonObject read(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static Set<String> strings(Iterable<JsonElement> values) {
        Set<String> result = new HashSet<>();
        values.forEach(value -> result.add(value.isJsonObject() ? value.getAsJsonObject().get("id").getAsString() : value.getAsString()));
        return result;
    }

    private static void require(boolean condition, String message) {
        checks++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
