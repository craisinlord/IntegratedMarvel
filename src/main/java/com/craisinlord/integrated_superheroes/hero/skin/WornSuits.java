package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicLookGroups;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public final class WornSuits {
    private static final EquipmentSlot[] SLOTS = {EquipmentSlot.CHEST, EquipmentSlot.HEAD, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private WornSuits() {
    }

    public static boolean wearsSuit(Player player) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = worn(player, slot);
            if (SerumLooks.ownLook(stack) || ModCompat.THEMATIC && SuitPieces.suit(stack) != null) {
                return true;
            }
        }
        return false;
    }

    public static String thematicSuit(Player player) {
        if (!ModCompat.THEMATIC) {
            return "";
        }
        ItemStack chest = worn(player, EquipmentSlot.CHEST);
        if (SuitPieces.characterItem(chest.getItem())) {
            return BuiltInRegistries.ITEM.getKey(chest.getItem()).toString();
        }
        for (EquipmentSlot slot : SLOTS) {
            Item suit = SuitPieces.suit(worn(player, slot));
            if (suit != null) {
                return BuiltInRegistries.ITEM.getKey(suit).toString();
            }
        }
        return "";
    }

    public static Map<String, String> variants(Player player) {
        Map<String, String> found = new LinkedHashMap<>();
        if (SuitSkins.wearsIronMan(player)) {
            found.putAll(SuitSkins.ironManLooks());
        }
        String suit = thematicSuit(player);
        if (!suit.isEmpty()) {
            for (String member : ThematicLookGroups.members(suit)) {
                found.putIfAbsent(path(member), member);
            }
        }
        return found;
    }

    public static boolean fits(Player player, String costume) {
        return costume != null && !costume.isEmpty() && variants(player).containsValue(costume);
    }

    private static ItemStack worn(Player player, EquipmentSlot slot) {
        return player.getInventory().armor.get(slot.getIndex());
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }
}
