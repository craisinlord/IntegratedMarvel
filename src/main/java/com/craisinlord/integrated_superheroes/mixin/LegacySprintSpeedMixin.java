package com.craisinlord.integrated_superheroes.mixin;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "com.stxllarplanes.marvellegacy.power.SpiderPowers",
        "com.stxllarplanes.marvellegacy.power.WolverinePowers"
}, remap = false)
public abstract class LegacySprintSpeedMixin {
    @Inject(method = "speedModifier", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$disableSprintSpeed(PlayerTickEvent.Pre event, CallbackInfo ci) {
        ci.cancel();
    }
}
