package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicSerumLooks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.client.SuitModelProxies", remap = false)
public abstract class ThematicSerumLookMixin {
    @Inject(method = "standIn", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$serumLook(LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack look = ThematicSerumLooks.standIn(entity);
        if (look == null) {
            look = com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces.standIn(entity);
        }
        if (look != null) {
            cir.setReturnValue(look);
        }
    }
}
