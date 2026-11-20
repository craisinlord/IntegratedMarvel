package com.craisinlord.integrated_superheroes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(targets = "dev.emi.emi.EmiUtil", remap = false)
public abstract class EmiMarvelModNameMixin {
    @Unique
    private static final Set<String> integratedSuperheroes$MARVEL_MODS = Set.of("marvellegacy", "marvel", "infinity_gauntlet", "integrated_superheroes", "thematic");

    @Inject(method = "getModName", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$integratedMarvelName(String modId, CallbackInfoReturnable<String> cir) {
        if (integratedSuperheroes$MARVEL_MODS.contains(modId)) {
            cir.setReturnValue("Integrated Superheroes");
        }
    }
}
