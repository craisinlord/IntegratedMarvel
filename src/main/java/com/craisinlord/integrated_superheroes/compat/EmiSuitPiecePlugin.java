package com.craisinlord.integrated_superheroes.compat;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.Comparison;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;

@EmiEntrypoint
public final class EmiSuitPiecePlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        java.util.Set<Item> variants = new java.util.HashSet<>(VariantHiding.variants(registry.getRecipeManager(),
                level != null ? level.registryAccess() : net.minecraft.core.RegistryAccess.EMPTY));
        if (com.craisinlord.integrated_superheroes.cleanup.CleanupConfig.hideSerumCostumes()) {
            variants.addAll(com.craisinlord.integrated_superheroes.hero.skin.SerumLooks.costumeItems());
        }
        java.util.Map<String, EmiStack> firstPiece = new java.util.HashMap<>();
        registry.removeEmiStacks(stack -> {
            ItemStack item = stack.getItemStack();
            if (item.isEmpty()) {
                return false;
            }
            Item parentSuit = SuitPieceIds.suitItem(item);
            if (com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.protectedSuit(
                    BuiltInRegistries.ITEM.getKey(parentSuit == null ? item.getItem() : parentSuit))) return false;
            if (com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.hidden(item)
                    || (parentSuit != null && com.craisinlord.integrated_superheroes.cleanup.Cleanup.isHidden(parentSuit))
                    || com.craisinlord.integrated_superheroes.cleanup.Cleanup.isHidden(item.getItem()) || SuitPieceIds.bare(item)
                    || VariantHiding.hidden(item, variants)) {
                return true;
            }
            Item suit = SuitPieceIds.suitItem(item);
            if (suit == null) {
                return false;
            }
            String key = BuiltInRegistries.ITEM.getKey(item.getItem()) + "|" + BuiltInRegistries.ITEM.getKey(suit);
            EmiStack first = firstPiece.putIfAbsent(key, stack);
            return first != null && first != stack;
        });
        if (SuitPieceIds.component() == null || SuitPieceIds.items().isEmpty()) {
            return;
        }
        for (Item item : SuitPieceIds.items()) {
            registry.setDefaultComparison(item, Comparison.compareComponents());
        }
        boolean listed = net.minecraft.world.item.CreativeModeTabs.searchTab().getSearchTabDisplayItems().stream()
                .anyMatch(stack -> SuitPieceIds.suitItem(stack) != null);
        if (listed) {
            return;
        }
        java.util.List<ItemStack> pieces = new java.util.ArrayList<>(SuitPieceIds.allPieces());
        java.util.Collections.reverse(pieces);
        for (ItemStack piece : pieces) {
            Item suit = SuitPieceIds.suitItem(piece);
            boolean keep = suit != null && com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.protectedSuit(
                    BuiltInRegistries.ITEM.getKey(suit));
            if (!keep && (com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.hidden(piece)
                    || VariantHiding.hidden(piece, variants))) continue;
            EmiStack emi = EmiStack.of(piece);
            if (suit == null) {
                registry.addEmiStack(emi);
            } else {
                registry.addEmiStackAfter(emi, existing -> existing.getItemStack().is(suit));
            }
        }
    }
}
