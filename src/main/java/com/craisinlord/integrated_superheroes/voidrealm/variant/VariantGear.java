package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.cleanup.Cleanup;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariantGear;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class VariantGear {
    public static final ResourceLocation GEAR = IntegratedSuperheroes.id("void_variants/gear.json");
    private static final String[][] GUN_FALLBACKS = {
            {"m870", "scguns:combat_shotgun"}, {"m590", "scguns:combat_shotgun"}, {"spas", "scguns:combat_shotgun"},
            {"shotgun", "scguns:combat_shotgun"}, {"gm6", "scguns:m3_marksman"}, {"lynx", "scguns:m3_marksman"},
            {"ump", "scguns:greaser_smg"}, {"mp7", "scguns:greaser_smg"}, {"p90", "scguns:greaser_smg"},
            {"tmp", "scguns:greaser_smg"}, {"mp5", "scguns:greaser_smg"}, {"m4", "scguns:m3_carabine"},
            {"star15", "scguns:m3_carabine"}, {"g36", "scguns:m3_carabine"}, {"xm29", "scguns:m3_carabine"},
            {"c14", "scguns:m3_carabine"}, {"dc15c", "scguns:m3_carabine"}
    };
    private static final String DEFAULT_GUN = "scguns:defender_pistol";
    private static final String[] GUN_WORDS = {"gun", "revolver", "deagle", "pistol", "rifle", "carbine", "shotgun", "dc15", "dc17", "westar", "ee3"};

    private static volatile ResourceManager loadedFrom;
    private static volatile Map<String, Entry> entries = Map.of();

    private record Entry(List<String> mainhand, List<String> offhand) {
    }

    private VariantGear() {
    }

    public static void equip(Mob mob, VariantProfile profile) {
        if (profile == null || profile.kind() == VariantKind.ENTITY) {
            return;
        }
        if (!mob.getMainHandItem().isEmpty() || !mob.getOffhandItem().isEmpty()) {
            return;
        }
        MinecraftServer server = mob.level().getServer();
        if (server == null) {
            return;
        }
        Map<String, Entry> data = data(server);
        RandomSource random = mob.getRandom();
        if (apply(mob, data.get("look:" + profile.look()), random)) {
            return;
        }
        String familyKey = profile.kind().key() + ":" + profile.family();
        switch (profile.kind()) {
            case THEMATIC -> {
                if (applyDerived(mob, profile.look()) || apply(mob, data.get(familyKey), random)) {
                    return;
                }
                applyDerived(mob, profile.family());
            }
            case SERUM, VILTRUMITE -> {
                if (!apply(mob, data.get(familyKey), random)) {
                    applyDerived(mob, VoidVariants.wornLook(profile));
                }
            }
            default -> apply(mob, data.get(familyKey), random);
        }
    }

    private static Map<String, Entry> data(MinecraftServer server) {
        ResourceManager manager = server.getResourceManager();
        if (manager != loadedFrom) {
            synchronized (VariantGear.class) {
                if (manager != loadedFrom) {
                    entries = load(manager);
                    loadedFrom = manager;
                }
            }
        }
        return entries;
    }

    private static Map<String, Entry> load(ResourceManager manager) {
        Map<String, Entry> out = new HashMap<>();
        Optional<Resource> resource = manager.getResource(GEAR);
        if (resource.isEmpty()) {
            IntegratedSuperheroes.LOGGER.warn("Void Variants: {} is missing", GEAR);
            return out;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            JsonObject gear = json.has("gear") ? json.getAsJsonObject("gear") : json;
            for (Map.Entry<String, JsonElement> entry : gear.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    JsonObject hands = entry.getValue().getAsJsonObject();
                    out.put(entry.getKey(), new Entry(ids(hands, "mainhand"), ids(hands, "offhand")));
                }
            }
        } catch (Exception error) {
            IntegratedSuperheroes.LOGGER.error("Void Variants: could not read {}", GEAR, error);
        }
        return out;
    }

    private static List<String> ids(JsonObject hands, String key) {
        List<String> out = new ArrayList<>();
        if (!hands.has(key)) {
            return out;
        }
        JsonElement value = hands.get(key);
        if (value.isJsonArray()) {
            value.getAsJsonArray().forEach(element -> out.add(element.getAsString()));
        } else if (value.isJsonPrimitive()) {
            out.add(value.getAsString());
        }
        return out;
    }

    private static boolean apply(Mob mob, Entry entry, RandomSource random) {
        if (entry == null) {
            return false;
        }
        return hold(mob, pick(entry.mainhand(), random), pick(entry.offhand(), random));
    }

    private static Item pick(List<String> ids, RandomSource random) {
        List<Item> usable = new ArrayList<>();
        for (String id : ids) {
            Item item = resolve(id);
            if (item != null) {
                usable.add(item);
            }
        }
        return usable.isEmpty() ? null : usable.get(random.nextInt(usable.size()));
    }

    private static boolean applyDerived(Mob mob, String look) {
        if (!ModCompat.THEMATIC || look == null || look.isEmpty()) {
            return false;
        }
        List<String> ids;
        try {
            ids = ThematicVariantGear.equipItems(look);
        } catch (RuntimeException | LinkageError error) {
            return false;
        }
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        List<Item> weapons = new ArrayList<>();
        Item shield = null;
        for (String id : ids) {
            Item item = resolve(id);
            if (item == null || weapons.contains(item) || item == shield || utility(item)) {
                continue;
            }
            if (!shield(item)) {
                weapons.add(item);
            } else if (shield == null) {
                shield = item;
            }
        }
        if (weapons.isEmpty()) {
            return hold(mob, shield, null);
        }
        Item main = weapons.get(0);
        if (gun(main)) {
            for (Item item : weapons) {
                if (!gun(item) && !(item instanceof ProjectileWeaponItem)) {
                    main = item;
                    break;
                }
            }
        }
        Item off = shield;
        if (off == null) {
            for (Item item : weapons) {
                if (item != main) {
                    off = item;
                    break;
                }
            }
        }
        return hold(mob, main, off);
    }

    private static boolean hold(Mob mob, Item main, Item off) {
        if (main == null && off == null) {
            return false;
        }
        if (main != null) {
            mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(main));
        }
        if (off != null && off != main) {
            mob.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(off));
        }
        return true;
    }

    private static Item resolve(String raw) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            return null;
        }
        Map<ResourceLocation, ResourceLocation> replacements = Cleanup.replacements();
        for (int i = 0; i < 4 && replacements.containsKey(id); i++) {
            id = replacements.get(id);
        }
        if (Cleanup.isGone(id)) {
            return null;
        }
        if (!BuiltInRegistries.ITEM.containsKey(id)) {
            return "pointblank".equals(id.getNamespace()) ? gunFallback(id.getPath()) : null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    private static Item gunFallback(String path) {
        String pick = DEFAULT_GUN;
        for (String[] fallback : GUN_FALLBACKS) {
            if (path.contains(fallback[0])) {
                pick = fallback[1];
                break;
            }
        }
        ResourceLocation id = ResourceLocation.tryParse(pick);
        if (id == null || !BuiltInRegistries.ITEM.containsKey(id) || Cleanup.isGone(id)) {
            return null;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == Items.AIR ? null : item;
    }

    private static boolean utility(Item item) {
        String path = BuiltInRegistries.ITEM.getKey(item).getPath();
        return path.contains("grapple") || path.endsWith("ring");
    }

    private static boolean shield(Item item) {
        return item instanceof ShieldItem || BuiltInRegistries.ITEM.getKey(item).getPath().contains("shield");
    }

    private static boolean gun(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if ("scguns".equals(id.getNamespace()) || "pointblank".equals(id.getNamespace())) {
            return true;
        }
        for (String word : GUN_WORDS) {
            if (id.getPath().contains(word)) {
                return true;
            }
        }
        return false;
    }
}
