package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.tintankgames.marvel.world.item.crafting.SuitUpgradingRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SuitUpgradingRecipe.class, remap = false)
public abstract class SuitUpgradeEnchantmentMixin {
    @Shadow
    @Final
    Ingredient suit;

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$keepEnchantments(CraftingInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result.isEmpty()) {
            return;
        }
        for (int i = 0; i < input.size(); i++) {
            ItemStack old = input.getItem(i);
            if (old.isEmpty() || !suit.test(old)) {
                continue;
            }
            ItemEnchantments enchantments = old.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
            if (!enchantments.isEmpty()) {
                result.set(DataComponents.ENCHANTMENTS, enchantments);
                result.set(DataComponents.REPAIR_COST, old.getOrDefault(DataComponents.REPAIR_COST, 0));
            }
            return;
        }
    }
}
