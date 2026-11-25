package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicLookOptions;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicSerumLooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MarvelSerumLooks {
    private static final String[] PIECES = {"boots", "leggings", "chestplate", "helmet"};
    private static final ItemStack[] NOT_A_SET = {};
    private static final Map<Item, ItemStack> STACKS = new ConcurrentHashMap<>();
    private static final Map<String, ItemStack[]> SETS = new ConcurrentHashMap<>();
    private static Entity drawing;
    private static boolean checking;

    private MarvelSerumLooks() {
    }

    private static ItemStack[] set(String costume) {
        if (!SerumLooks.marvelSet(costume)) {
            return NOT_A_SET;
        }
        ItemStack[] set = new ItemStack[PIECES.length];
        for (int i = 0; i < PIECES.length; i++) {
            Item piece = SerumLooks.piece(costume, PIECES[i]);
            set[i] = piece == null ? ItemStack.EMPTY : STACKS.computeIfAbsent(piece, ItemStack::new);
        }
        return set;
    }

    public static boolean overriding(Entity entity) {
        return entity != null && drawing == entity && entity instanceof Player player && override(player, EquipmentSlot.CHEST) != null;
    }

    public static Entity begin(Entity entity) {
        Entity previous = drawing;
        drawing = entity;
        return previous;
    }

    public static void end(Entity previous) {
        drawing = previous;
    }

    public static ItemStack override(Player player, EquipmentSlot slot) {
        if (checking || drawing != player || slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR || !player.level().isClientSide) {
            return null;
        }
        checking = true;
        try {
            if (player.isInvisible()) {
                return null;
            }
        } finally {
            checking = false;
        }
        String costume = SkinData.costume(player);
        if (costume.isEmpty()) {
            return null;
        }
        if (!(player == Minecraft.getInstance().player ? ThematicLookOptions.showOwn() : ThematicLookOptions.showOthers())) {
            return null;
        }
        ItemStack[] set = SETS.computeIfAbsent(costume, MarvelSerumLooks::set);
        if (set != NOT_A_SET) {
            return set[slot.getIndex()];
        }
        return ModCompat.THEMATIC && ThematicSerumLooks.covers(player) ? ItemStack.EMPTY : null;
    }
}
