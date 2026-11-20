package com.craisinlord.integrated_superheroes.cleanup;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.resources.ResourceLocation;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class CleanupRecipes {
    private static final Set<String> RESULT_KEYS = Set.of("result", "results", "output", "outputs");
    private static final Set<String> ITEM_KEYS = Set.of("item", "id", "items");

    private CleanupRecipes() {
    }

    public static void apply(Map<ResourceLocation, JsonElement> recipes) {
        int dropped = 0;
        int rewritten = 0;
        Iterator<Map.Entry<ResourceLocation, JsonElement>> iterator = recipes.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<ResourceLocation, JsonElement> entry = iterator.next();
            if (!entry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject recipe = entry.getValue().getAsJsonObject();
            JsonElement type = recipe.get("type");
            if (CatalogCleanup.dropsRecipe(recipe) || Cleanup.dropsRecipe(entry.getKey()) || producesHidden(recipe)
                    || (type != null && type.isJsonPrimitive() && Cleanup.dropsRecipeType(type.getAsString()))) {
                iterator.remove();
                dropped++;
                continue;
            }
            State state = new State();
            state.changed = CatalogCleanup.rewriteLegacyIngredients(recipe);
            state.changed |= CosmeticMerges.rewriteProgressionRecipe(recipe);
            for (Map.Entry<String, JsonElement> field : recipe.entrySet()) {
                if (!RESULT_KEYS.contains(field.getKey())) {
                    field.setValue(rewrite(field.getKey(), field.getValue(), state));
                }
            }
            if (state.dead) {
                iterator.remove();
                dropped++;
            } else if (state.changed) {
                rewritten++;
            }
        }
        if (dropped > 0 || rewritten > 0) {
            IntegratedSuperheroes.LOGGER.info("Dropped {} recipes and rewrote {} for removed Marvel items", dropped, rewritten);
        }
    }

    private static boolean producesHidden(JsonObject recipe) {
        for (String key : RESULT_KEYS) {
            if (recipe.has(key) && outputsHidden(recipe.get(key))) {
                return true;
            }
        }
        return false;
    }

    private static boolean outputsHidden(JsonElement element) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            ResourceLocation id = ResourceLocation.tryParse(element.getAsString());
            return id != null && Cleanup.dropsRecipesFor(id);
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                if (outputsHidden(child)) {
                    return true;
                }
            }
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
                if (ITEM_KEYS.contains(field.getKey()) && outputsHidden(field.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    private static JsonElement rewrite(String key, JsonElement element, State state) {
        if (element.isJsonObject()) {
            JsonObject dyed = dyed(element.getAsJsonObject());
            if (dyed != null) {
                state.changed = true;
                return dyed;
            }
            for (Map.Entry<String, JsonElement> field : element.getAsJsonObject().entrySet()) {
                field.setValue(rewrite(field.getKey(), field.getValue(), state));
            }
            return element;
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            for (int i = 0; i < array.size(); i++) {
                array.set(i, rewrite(key, array.get(i), state));
            }
            return element;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            return element;
        }
        String value = element.getAsString();
        if ("tag".equals(key)) {
            String swapped = Cleanup.tagSwaps().get(value);
            if (swapped != null) {
                state.changed = true;
                return new JsonPrimitive(swapped);
            }
            return element;
        }
        if (!ITEM_KEYS.contains(key)) {
            return element;
        }
        ResourceLocation id = ResourceLocation.tryParse(value);
        if (id == null) {
            return element;
        }
        ResourceLocation replacement = Cleanup.replacements().get(id);
        if (replacement != null) {
            state.changed = true;
            return new JsonPrimitive(replacement.toString());
        }
        if (Cleanup.removals().contains(id)) {
            state.dead = true;
        }
        return element;
    }

    private static JsonObject dyed(JsonObject object) {
        JsonElement item = object.get("item");
        if (object.size() != 1 || item == null || !item.isJsonPrimitive() || !item.getAsJsonPrimitive().isString()) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(item.getAsString());
        Integer rgb = id == null ? null : Cleanup.dyes().get(id);
        ResourceLocation target = rgb == null ? null : Cleanup.replacements().get(id);
        if (target == null) {
            return null;
        }
        JsonObject color = new JsonObject();
        color.addProperty("rgb", rgb);
        JsonObject components = new JsonObject();
        components.add("minecraft:dyed_color", color);
        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("type", "neoforge:components");
        ingredient.addProperty("items", target.toString());
        ingredient.add("components", components);
        return ingredient;
    }

    private static final class State {
        boolean changed;
        boolean dead;
    }
}
