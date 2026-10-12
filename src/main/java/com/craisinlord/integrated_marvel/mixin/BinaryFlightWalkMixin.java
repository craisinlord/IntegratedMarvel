package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.BinaryFlight;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BinaryFlightWalkMixin {
    @Inject(method = "updateWalkAnimation", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$freezeLegsWhileFlying(float distance, CallbackInfo ci) {
        if (BinaryFlight.isFlying((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
