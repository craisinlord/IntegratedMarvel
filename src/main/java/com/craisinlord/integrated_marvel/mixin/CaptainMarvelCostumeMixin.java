package com.craisinlord.integrated_marvel.mixin;

import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.tintankgames.marvel.world.item.CaptainMarvelSuitItem", remap = false)
public abstract class CaptainMarvelCostumeMixin {
    @Inject(method = "playerTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$disableCostumeFlight(PlayerTickEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}
