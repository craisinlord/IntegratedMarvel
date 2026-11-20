package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicLookOptions;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MarvelSerumLooks {
    private static final Map<Item, ItemStack> STACKS = new ConcurrentHashMap<>();
    private static Entity drawing;
    private static boolean checking;

    private MarvelSerumLooks() {
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
        if (SuitSkins.ironManLook(costume) && SuitSkins.wearsIronMan(player)) {
            return ItemStack.EMPTY;
        }
        if (!SerumLooks.marvelSet(costume)) {
            return null;
        }
        if (ownGear(worn(player, EquipmentSlot.CHEST)) || ownGear(worn(player, slot))) {
            return null;
        }
        String name = switch (slot) {
            case HEAD -> "helmet";
            case CHEST -> "chestplate";
            case LEGS -> "leggings";
            case FEET -> "boots";
            default -> null;
        };
        Item piece = name == null ? null : SerumLooks.piece(costume, name);
        return piece == null ? null : STACKS.computeIfAbsent(piece, ItemStack::new);
    }

    private static ItemStack worn(Player player, EquipmentSlot slot) {
        return player.getInventory().armor.get(slot.getIndex());
    }

    private static boolean ownGear(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return !ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace());
    }
}
