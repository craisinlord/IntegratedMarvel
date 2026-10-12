package com.craisinlord.integrated_marvel.compat;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import com.craisinlord.integrated_marvel.cleanup.Cleanup;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

@JeiPlugin
public final class JeiCleanupPlugin implements IModPlugin {
    private static final ResourceLocation ID = IntegratedMarvel.id("cleanup");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (Cleanup.isHidden(item)) {
                hidden.add(new ItemStack(item));
            }
        }
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
