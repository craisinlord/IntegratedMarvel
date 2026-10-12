package com.craisinlord.integrated_superheroes.mixin;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.redgitreds.infinitygauntlet.procedures.BaseUIProcProcedure", remap = false)
public abstract class InfinityGauntletBaseUiMixin {
    @Inject(method = "onPlayerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$disableBaseUi(PlayerTickEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}
