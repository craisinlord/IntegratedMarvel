package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import net.minecraft.world.item.ArmorItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public abstract class IronManSuitEnchantabilityMixin {
    @Inject(method = "getEnchantmentValue", at = @At("RETURN"), cancellable = true)
    private void integratedSuperheroes$suitEnchantability(CallbackInfoReturnable<Integer> cir) {
        if (IronManSuits.isLoaded() && IronManSuits.suitId((ArmorItem) (Object) this) != null) {
            cir.setReturnValue(IronManSuits.ENCHANTABILITY.get());
        }
    }
}
