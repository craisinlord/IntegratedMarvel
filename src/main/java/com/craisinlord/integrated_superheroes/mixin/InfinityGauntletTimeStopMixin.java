package com.craisinlord.integrated_superheroes.mixin;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.redgitreds.infinitygauntlet.procedures.TimeStopProcProcedure", remap = false)
public abstract class InfinityGauntletTimeStopMixin {
    @Inject(method = "onEntityTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$disableTimeStop(EntityTickEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }
}
