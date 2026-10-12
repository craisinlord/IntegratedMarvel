package com.craisinlord.integrated_marvel.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(targets = "mezz.jei.neoforge.platform.ModHelper", remap = false)
public abstract class JeiMarvelModNameMixin {
    @Unique
    private static final Set<String> integratedMarvel$MARVEL_MODS = Set.of("marvellegacy", "marvel", "infinity_gauntlet", "integrated_marvel");

    @Inject(method = "computeModNameForModId", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$integratedMarvelName(String modId, CallbackInfoReturnable<String> cir) {
        if (integratedMarvel$MARVEL_MODS.contains(modId)) {
            cir.setReturnValue("Integrated Marvel");
        }
    }
}
