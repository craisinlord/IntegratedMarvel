package com.craisinlord.integrated_superheroes.cleanup;

import com.craisinlord.integrated_superheroes.compat.SuitPieceIds;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public final class CatalogCleanup {
    private static final JsonObject MANIFEST = load();
    private static final Set<String> COSTUMES = ids("costumes");
    private static final Set<String> INERT = ids("inertSuits");
    private static final Set<String> GENERATED = ids("generatedEquipment");
    private static final Set<String> COVERS = ids("comicCovers");
    private static final Set<String> LEGACY_GADGETS = ids("legacyGadgets");
    private static final Set<String> UNCRAFTABLE_WEAPONS = ids("uncraftableWeapons");
    private static final Set<String> INTERMEDIATES = ids("recipeIntermediates");

    private CatalogCleanup() {}

    private static JsonObject load() {
        try (var stream = CatalogCleanup.class.getResourceAsStream("/data/integrated_superheroes/catalog_cleanup.json")) {
            if (stream == null) throw new IllegalStateException("Missing catalog cleanup manifest");
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read catalog cleanup manifest", e);
        }
    }

    private static Set<String> ids(String key) {
        Set<String> ids = new HashSet<>();
        MANIFEST.getAsJsonArray(key).forEach(value -> ids.add(value.getAsString()));
        return Set.copyOf(ids);
    }

    public static boolean enabled() {
        return CleanupConfig.on(CleanupConfig.CATALOG_ENABLED);
    }

    public static boolean retiredLegacyGadget(ResourceLocation id) {
        return enabled() && CleanupConfig.on(CleanupConfig.RETIRE_LEGACY_GADGETS)
                && id != null && LEGACY_GADGETS.contains(id.toString());
    }

    public static boolean protectedSuit(ResourceLocation id) {
        return id != null && CleanupConfig.strings(CleanupConfig.KEEP_SUITS).contains(id.toString());
    }

    private static boolean costume(ResourceLocation id) {
        if (id == null || protectedSuit(id)) return false;
        String value = id.toString();
        return CosmeticMerges.serumCostume(value) || CosmeticMerges.retiredSuit(value) || COSTUMES.contains(value) || INERT.contains(value)
                || CleanupConfig.strings(CleanupConfig.EXTRA_RETIRED_SUITS).contains(value);
    }

    public static boolean civilianLook(ResourceLocation id) {
        if (!enabled() || id == null || protectedSuit(id)) return false;
        String value = id.toString();
        if (CosmeticMerges.serumCostume(value) || COSTUMES.contains(value)) return false;
        return INERT.contains(value) || CosmeticMerges.retiredSuit(value) || CleanupConfig.strings(CleanupConfig.EXTRA_RETIRED_SUITS).contains(value);
    }

    public static boolean hidden(ItemStack stack) {
        if (!enabled() || stack.isEmpty()) return false;
        Item parent = SuitPieceIds.suitItem(stack);
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(parent == null ? stack.getItem() : parent);
        return hidden(id);
    }

    public static boolean hidden(ResourceLocation id) {
        if (!enabled() || id == null || protectedSuit(id)) return false;
        return CosmeticMerges.hidden(id) || retiredLegacyGadget(id) || (CleanupConfig.on(CleanupConfig.HIDE_INERT_COSTUMES) && costume(id))
                || (CleanupConfig.on(CleanupConfig.HIDE_GENERATED_EQUIPMENT) && GENERATED.contains(id.toString()))
                || (CleanupConfig.on(CleanupConfig.HIDE_UNCRAFTABLE_WEAPONS) && UNCRAFTABLE_WEAPONS.contains(id.toString()))
                || (CleanupConfig.on(CleanupConfig.HIDE_COMIC_COVERS) && COVERS.contains(id.toString()))
                || (CleanupConfig.on(CleanupConfig.HIDE_UNCRAFTABLE_SUITS) && uncraftableSuit(id));
    }

    private static final net.minecraft.tags.TagKey<Item> CRAFTABLE_SUITS = net.minecraft.tags.TagKey.create(
            net.minecraft.core.registries.Registries.ITEM, ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "craftable_suits"));

    public static boolean uncraftableSuit(ResourceLocation id) {
        if (!"thematic".equals(id.getNamespace()) || !BuiltInRegistries.ITEM.containsKey(id)) return false;
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!com.craisinlord.integrated_superheroes.compat.thematic.ThematicLookGroups.isSuit(id.toString())) return false;
        return !item.builtInRegistryHolder().is(CRAFTABLE_SUITS);
    }

    public static boolean rewriteLegacyIngredients(JsonObject recipe) {
        if (!enabled() || !CleanupConfig.on(CleanupConfig.RETIRE_LEGACY_GADGETS)) return false;
        boolean changed = false;
        for (var field : recipe.entrySet()) {
            if (Set.of("result", "results", "output", "outputs", "neoforge:conditions").contains(field.getKey())) continue;
            JsonElement replacement = replaceLegacyIngredient(field.getValue());
            if (replacement != field.getValue()) {
                field.setValue(replacement);
                changed = true;
            }
        }
        return changed;
    }

    private static JsonElement replaceLegacyIngredient(JsonElement value) {
        if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
            JsonElement replacement = MANIFEST.getAsJsonObject("legacyGadgetIngredients").get(value.getAsString());
            return replacement == null ? value : replacement.deepCopy();
        }
        if (value.isJsonArray()) {
            var replacement = new com.google.gson.JsonArray();
            boolean changed = false;
            for (JsonElement child : value.getAsJsonArray()) {
                JsonElement updated = replaceLegacyIngredient(child);
                replacement.add(updated);
                changed |= updated != child;
            }
            return changed ? replacement : value;
        }
        if (value.isJsonObject()) {
            var replacement = new JsonObject();
            boolean changed = false;
            for (var field : value.getAsJsonObject().entrySet()) {
                JsonElement updated = replaceLegacyIngredient(field.getValue());
                boolean replacedTag = field.getKey().equals("tag") && updated != field.getValue()
                        && updated.isJsonPrimitive() && updated.getAsJsonPrimitive().isString();
                replacement.add(replacedTag ? "item" : field.getKey(), updated);
                changed |= updated != field.getValue();
            }
            return changed ? replacement : value;
        }
        return value;
    }

    public static boolean dropsRecipe(JsonObject recipe) {
        if (CosmeticMerges.dropsRecipe(recipe)) return true;
        if (!enabled()) return false;
        if (recipe.has("type") && recipe.get("type").isJsonPrimitive()
                && "marvel:suit_repairing".equals(recipe.get("type").getAsString()) && recipe.has("suit")) {
            JsonElement suit = recipe.get("suit");
            if (dropsOutput(suit)) return true;
            if (suit.isJsonObject() && suit.getAsJsonObject().has("tag")) {
                JsonElement tag = suit.getAsJsonObject().get("tag");
                if (tag.isJsonPrimitive() && tag.getAsJsonPrimitive().isString() && dropsId(tag.getAsString())) return true;
            }
        }
        for (String key : Set.of("result", "results", "output", "outputs")) {
            if (recipe.has(key) && dropsOutput(recipe.get(key))) return true;
        }
        return false;
    }

    private static boolean dropsId(String value) {
        ResourceLocation id = ResourceLocation.tryParse(value);
        return retiredLegacyGadget(id) || id != null && !protectedSuit(id)
                && ((CleanupConfig.on(CleanupConfig.DISABLE_COSTUME_RECIPES) && costume(id) && (!INTERMEDIATES.contains(value)
                        || CleanupConfig.strings(CleanupConfig.EXTRA_RETIRED_SUITS).contains(value)))
                || (CleanupConfig.on(CleanupConfig.DISABLE_GENERATED_RECIPES) && GENERATED.contains(value)));
    }

    private static boolean dropsOutput(JsonElement output) {
        if (output.isJsonPrimitive() && output.getAsJsonPrimitive().isString()) return dropsId(output.getAsString());
        if (output.isJsonArray()) {
            for (JsonElement entry : output.getAsJsonArray()) if (dropsOutput(entry)) return true;
        } else if (output.isJsonObject()) {
            JsonObject object = output.getAsJsonObject();
            JsonElement components = object.get("components");
            if (components != null && components.isJsonObject()) {
                JsonElement suit = components.getAsJsonObject().get("integrated_superheroes:suit");
                if (suit != null && suit.isJsonPrimitive() && suit.getAsJsonPrimitive().isString()
                        && dropsId(suit.getAsString())) return true;
            }
            for (String key : Set.of("id", "item", "items")) {
                if (object.has(key) && dropsOutput(object.get(key))) return true;
            }
        }
        return false;
    }
}
