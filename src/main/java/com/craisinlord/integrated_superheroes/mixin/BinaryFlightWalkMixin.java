package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.BinaryFlight;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BinaryFlightWalkMixin {
    @Inject(method = "updateWalkAnimation", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$freezeLegsWhileFlying(float distance, CallbackInfo ci) {
        LivingEntity living = (LivingEntity) (Object) this;
        if (BinaryFlight.isFlying(living)) {
            living.walkAnimation.update(0.0F, 0.4F);
            ci.cancel();
        }
    }
}
