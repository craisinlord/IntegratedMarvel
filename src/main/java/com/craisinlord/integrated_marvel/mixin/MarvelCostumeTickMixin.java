package com.craisinlord.integrated_marvel.mixin;

import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.tintankgames.marvel.world.item.SpiderManSuitItem",
        "net.tintankgames.marvel.world.item.WolverineSuitItem",
        "net.tintankgames.marvel.world.item.QuicksilverSuitItem"
}, remap = false)
public abstract class MarvelCostumeTickMixin {
    @Inject(method = "livingTick", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$disableCostumeSetBonus(EntityTickEvent.Post event, CallbackInfo ci) {
        ci.cancel();
    }
}
