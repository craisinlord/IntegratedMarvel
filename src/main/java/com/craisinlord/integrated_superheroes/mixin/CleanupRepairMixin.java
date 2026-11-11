package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.cleanup.Cleanup;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public abstract class CleanupRepairMixin {
    @Inject(method = "isValidRepairItem", at = @At("RETURN"), cancellable = true)
    private void integratedSuperheroes$repairWithReplacement(ItemStack toRepair, ItemStack repair, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && Cleanup.repairsAs(((ArmorItem) (Object) this).getMaterial().value().repairIngredient().get(), repair)) {
            cir.setReturnValue(true);
        }
    }
}
