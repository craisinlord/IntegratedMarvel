package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.IronManEnergy;
import net.minecraft.world.item.ItemStack;
import net.tintankgames.marvel.world.item.IronManSuitItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = IronManSuitItem.class, remap = false)
public abstract class IronManLightningChargeMixin {
    @Redirect(method = "lambda$lightning$2", at = @At(value = "INVOKE",
            target = "Lnet/tintankgames/marvel/world/item/IronManSuitItem;addEnergy(Lnet/minecraft/world/item/ItemStack;F)V"), remap = false)
    private static void integratedSuperheroes$lightningCharge(ItemStack stack, float power) {
        IronManEnergy.addFE(stack, IronManEnergy.capacity(stack) * power / 100.0D);
    }
}
