package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public abstract class ThematicSuitEnchantabilityMixin {
    @Inject(method = "getEnchantmentValue", at = @At("RETURN"), cancellable = true)
    private void integratedSuperheroes$suitEnchantability(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof CharacterItem) {
            cir.setReturnValue(SuitPieces.MATERIAL.get().enchantmentValue());
        }
    }
}
