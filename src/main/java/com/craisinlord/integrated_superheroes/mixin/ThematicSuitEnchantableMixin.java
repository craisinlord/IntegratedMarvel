package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ThematicSuitEnchantableMixin {
    @Inject(method = "isEnchantable", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$suitEnchantable(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof CharacterItem && stack.getMaxStackSize() == 1) {
            cir.setReturnValue(true);
        }
    }
}
