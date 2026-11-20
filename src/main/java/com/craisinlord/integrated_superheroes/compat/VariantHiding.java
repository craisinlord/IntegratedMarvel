package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.cleanup.CleanupConfig;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class VariantHiding {
    public static final ResourceLocation VARIANT = ResourceLocation.fromNamespaceAndPath("marvel", "suit_variant");

    private VariantHiding() {
    }

    public static Set<Item> variants(RecipeManager manager, HolderLookup.Provider registries) {
        Set<Item> swapped = new HashSet<>();
        Set<Item> crafted = new HashSet<>();
        if (manager == null || !CleanupConfig.hideVariants()) {
            return swapped;
        }
        List<Item> pieces = SuitPieceIds.items();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            Recipe<?> recipe = holder.value();
            ItemStack result;
            try {
                result = recipe.getResultItem(registries);
            } catch (Throwable error) {
                continue;
            }
            if (result == null || result.isEmpty() || pieces.contains(result.getItem())) {
                continue;
            }
            if (VARIANT.equals(BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType()))) {
                swapped.add(result.getItem());
            } else {
                crafted.add(result.getItem());
            }
        }
        swapped.removeAll(crafted);
        IntegratedSuperheroes.LOGGER.info("Hiding {} cosmetic suit variants from recipe viewers", swapped.size());
        return swapped;
    }

    public static boolean hidden(ItemStack stack, Set<Item> variants) {
        if (variants.isEmpty() || stack.isEmpty()) {
            return false;
        }
        if (variants.contains(stack.getItem())) {
            return true;
        }
        Item suit = SuitPieceIds.suitItem(stack);
        return suit != null && variants.contains(suit);
    }

    public static List<ItemStack> stacks(Set<Item> variants) {
        List<ItemStack> stacks = new java.util.ArrayList<>();
        for (Item item : variants) {
            ItemStack stack = new ItemStack(item);
            stacks.add(stack);
            stacks.addAll(com.craisinlord.integrated_superheroes.cleanup.UnifiedCreativeTab.followers.apply(stack));
        }
        return stacks;
    }
}
