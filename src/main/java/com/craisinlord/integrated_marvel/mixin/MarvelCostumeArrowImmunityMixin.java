package com.craisinlord.integrated_marvel.mixin;

import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.tintankgames.marvel.world.item.BlackPantherSuitItem",
        "net.tintankgames.marvel.world.item.KineticBlackPantherSuitItem",
        "net.tintankgames.marvel.world.item.CaptainMarvelSuitItem"
}, remap = false)
public abstract class MarvelCostumeArrowImmunityMixin {
    @Inject(method = "arrowImmunity", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$disableCostumeArrowImmunity(LivingIncomingDamageEvent event, CallbackInfo ci) {
        ci.cancel();
    }
}
