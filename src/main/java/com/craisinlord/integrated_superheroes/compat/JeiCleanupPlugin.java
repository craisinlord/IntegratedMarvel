package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.cleanup.Cleanup;
import com.craisinlord.integrated_superheroes.cleanup.CleanupConfig;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.subtypes.ISubtypeInterpreter;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class JeiCleanupPlugin implements IModPlugin {
    private static final ResourceLocation ID = IntegratedSuperheroes.id("cleanup");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        DataComponentType<?> suit = SuitPieceIds.component();
        if (suit == null) {
            return;
        }
        ISubtypeInterpreter<ItemStack> interpreter = new ISubtypeInterpreter<>() {
            @Override
            public Object getSubtypeData(ItemStack stack, UidContext context) {
                return stack.get(suit);
            }

            @Override
            public String getLegacyStringSubtypeInfo(ItemStack stack, UidContext context) {
                Object value = stack.get(suit);
                return value == null ? "" : value.toString();
            }
        };
        for (Item item : SuitPieceIds.items()) {
            registration.registerSubtypeInterpreter(item, interpreter);
        }
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (Cleanup.isHidden(item)) {
                hidden.add(new ItemStack(item));
            }
        }
        for (Item item : SuitPieceIds.items()) {
            if (item != Items.AIR) {
                hidden.add(new ItemStack(item));
            }
        }
        net.minecraft.client.multiplayer.ClientLevel level = net.minecraft.client.Minecraft.getInstance().level;
        java.util.Set<Item> variants = level == null ? java.util.Set.of()
                : VariantHiding.variants(level.getRecipeManager(), level.registryAccess());
        hidden.addAll(VariantHiding.stacks(variants));
        if (CleanupConfig.hideSerumCostumes()) {
            java.util.Set<Item> costumes = new java.util.HashSet<>(com.craisinlord.integrated_superheroes.hero.skin.SerumLooks.costumeItems());
            costumes.removeAll(variants);
            hidden.addAll(VariantHiding.stacks(costumes));
        }
        for (ItemStack stack : runtime.getIngredientManager().getAllIngredients(VanillaTypes.ITEM_STACK)) {
            Item parent = SuitPieceIds.suitItem(stack);
            boolean keep = com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.protectedSuit(
                    BuiltInRegistries.ITEM.getKey(parent == null ? stack.getItem() : parent));
            if (!keep && (com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.hidden(stack)
                    || (parent != null && Cleanup.isHidden(parent))
                    || VariantHiding.hidden(stack, variants))) {
                hidden.add(stack);
            }
        }
        hidden.removeIf(stack -> {
            Item parent = SuitPieceIds.suitItem(stack);
            return com.craisinlord.integrated_superheroes.cleanup.CatalogCleanup.protectedSuit(
                    BuiltInRegistries.ITEM.getKey(parent == null ? stack.getItem() : parent));
        });
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
