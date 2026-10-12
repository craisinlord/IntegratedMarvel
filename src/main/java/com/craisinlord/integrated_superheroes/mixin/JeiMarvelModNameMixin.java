package com.craisinlord.integrated_superheroes.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(targets = "mezz.jei.neoforge.platform.ModHelper", remap = false)
public abstract class JeiMarvelModNameMixin {
    @Unique
    private static final Set<String> integratedSuperheroes$MARVEL_MODS = Set.of("marvellegacy", "marvel", "infinity_gauntlet", "integrated_superheroes");

    @Inject(method = "computeModNameForModId", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$integratedMarvelName(String modId, CallbackInfoReturnable<String> cir) {
        if (integratedSuperheroes$MARVEL_MODS.contains(modId)) {
            cir.setReturnValue("Integrated Superheroes");
        }
    }
}
