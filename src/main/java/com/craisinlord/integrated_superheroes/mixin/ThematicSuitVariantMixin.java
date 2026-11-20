package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.ability.ThematicAbility;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.util.StackData;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.tintankgames.marvel.world.item.crafting.SuitVariantRecipe", remap = false)
public abstract class ThematicSuitVariantMixin {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$reassignThematicAbilities(SingleRecipeInput input, HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        ItemStack source = input.getItem(0);
        if (!(result.getItem() instanceof CharacterItem target) || !StackData.contains(result, "ArmorUUID")) {
            return;
        }
        StackData.update(result, tag -> {
            if (source.getItem() instanceof CharacterItem previous) {
                for (ThematicAbility ability : previous.getAbilities()) {
                    tag.remove(ability.getId());
                }
            }
            target.assignAbilities(tag);
        });
    }
}
