package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;

public final class PymHelmets {
    public enum Kind {
        ANT,
        WASP
    }

    private PymHelmets() {
    }

    public static Kind kind(Player player) {
        if (!ModCompat.ANTARCHY || player.isSpectator()) {
            return null;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(player.getItemBySlot(EquipmentSlot.HEAD).getItem());
        if (!"marvel".equals(key.getNamespace())) {
            return null;
        }
        return switch (key.getPath()) {
            case "ant_man_helmet", "ant_man_upgraded_helmet" -> Kind.ANT;
            case "wasp_helmet" -> Kind.WASP;
            default -> null;
        };
    }
}
