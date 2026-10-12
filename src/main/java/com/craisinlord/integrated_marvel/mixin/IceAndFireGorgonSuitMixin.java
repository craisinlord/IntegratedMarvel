package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.suit.SuitPassives;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "com.iafenvoy.iceandfire.entity.GorgonEntity", remap = false)
public abstract class IceAndFireGorgonSuitMixin {
    @Inject(method = "isBlindfolded", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$suitHelmetBlindfold(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (SuitPassives.gorgonProof(entity)) {
            cir.setReturnValue(true);
        }
    }
}
