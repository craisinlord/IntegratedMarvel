package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.TimeFreezeState;
import net.minecraft.client.multiplayer.ClientLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class TimeFreezeAmbientMixin {
    @Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$pauseAmbientParticles(int x, int y, int z, CallbackInfo ci) {
        if (TimeFreezeState.clientActive()) ci.cancel();
    }
}
