package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class SuitPieceIds {
    private static final String[] PIECES = {"suit_helmet", "suit_leggings", "suit_boots"};

    private SuitPieceIds() {
    }

    public static DataComponentType<?> component() {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.get(IntegratedSuperheroes.id("suit"));
    }

    public static List<Item> items() {
        List<Item> items = new ArrayList<>(PIECES.length);
        for (String piece : PIECES) {
            Item item = BuiltInRegistries.ITEM.get(IntegratedSuperheroes.id(piece));
            if (item != Items.AIR) {
                items.add(item);
            }
        }
        return items;
    }

    public static List<ItemStack> allPieces() {
        List<ItemStack> pieces = new ArrayList<>();
        for (Item item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!com.craisinlord.integrated_superheroes.cleanup.Cleanup.isHidden(item)) {
                pieces.addAll(com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab.followers.apply(new ItemStack(item)));
            }
        }
        return pieces;
    }

    public static Item suitItem(ItemStack piece) {
        DataComponentType<?> suit = component();
        Object id = suit == null ? null : piece.get(suit);
        return id instanceof net.minecraft.resources.ResourceLocation location ? BuiltInRegistries.ITEM.get(location) : null;
    }

    public static boolean bare(ItemStack stack) {
        DataComponentType<?> suit = component();
        return suit != null && items().contains(stack.getItem()) && !stack.has(suit);
    }
}
