package com.craisinlord.integrated_superheroes.compat.thematic;

import bond.thematic.api.registries.armors.armor.ArmorRegistry;
import bond.thematic.api.registries.armors.armor.CharacterAltItem;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;

public final class ThematicVariantGear {
    private ThematicVariantGear() {
    }

    public static List<String> equipItems(String look) {
        if (look == null || look.isEmpty()) {
            return null;
        }
        Item item = ThematicVariants.lookItem(look);
        if (!(item instanceof CharacterItem suit)) {
            return null;
        }
        List<String> own = fromDefinition(suit);
        if (own != null) {
            return own;
        }
        if (suit instanceof CharacterAltItem alt && alt.getMainArmor() != null) {
            return fromDefinition(alt.getMainArmor());
        }
        return null;
    }

    private static List<String> fromDefinition(CharacterItem suit) {
        JsonObject json;
        try {
            json = ArmorRegistry.resolvedDefinition(suit.getIdentifier());
        } catch (RuntimeException error) {
            return null;
        }
        if (json == null || !json.has("abilities") || !json.get("abilities").isJsonArray()) {
            return null;
        }
        List<String> out = null;
        for (JsonElement element : json.getAsJsonArray("abilities")) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject ability = element.getAsJsonObject();
            if (!ability.has("identifier") || !"equip".equals(ability.get("identifier").getAsString())) {
                continue;
            }
            if (out == null) {
                out = new ArrayList<>();
            }
            JsonObject fields = fields(ability);
            if (fields == null || !fields.has("items") || !fields.get("items").isJsonArray()) {
                continue;
            }
            for (JsonElement entry : fields.getAsJsonArray("items")) {
                String id = entry.isJsonObject() && entry.getAsJsonObject().has("item")
                        ? entry.getAsJsonObject().get("item").getAsString()
                        : entry.isJsonPrimitive() ? entry.getAsString() : null;
                if (id != null && !id.isEmpty() && !out.contains(id)) {
                    out.add(id);
                }
            }
        }
        return out;
    }

    private static JsonObject fields(JsonObject ability) {
        if (!ability.has("options") || !ability.get("options").isJsonObject()) {
            return null;
        }
        JsonObject options = ability.getAsJsonObject("options");
        return options.has("fields") && options.get("fields").isJsonObject() ? options.getAsJsonObject("fields") : null;
    }
}
