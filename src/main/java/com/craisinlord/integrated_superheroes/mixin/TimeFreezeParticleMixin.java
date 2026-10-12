package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.TimeFreezeState;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ParticleEngine.class)
public abstract class TimeFreezeParticleMixin {
    @Inject(method = "tickParticle", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$freezeParticle(Particle particle, CallbackInfo ci) {
        if (TimeFreezeState.clientActive()) ci.cancel();
    }

    @ModifyVariable(method = "render(Lnet/minecraft/client/renderer/LightTexture;Lnet/minecraft/client/Camera;FLnet/minecraft/client/renderer/culling/Frustum;Ljava/util/function/Predicate;)V",
            at = @At("HEAD"), argsOnly = true)
    private float integratedSuperheroes$holdParticleFrame(float partialTick) {
        return TimeFreezeState.clientActive() ? 1.0F : partialTick;
    }
}
