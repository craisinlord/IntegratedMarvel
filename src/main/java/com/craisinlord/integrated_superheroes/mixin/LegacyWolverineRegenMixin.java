package com.craisinlord.integrated_superheroes.mixin;

import com.stxllarplanes.marvellegacy.power.WolverinePowers;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WolverinePowers.class, remap = false)
public abstract class LegacyWolverineRegenMixin {
    @Inject(method = "regenerationTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$ownRegeneration(PlayerTickEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}
