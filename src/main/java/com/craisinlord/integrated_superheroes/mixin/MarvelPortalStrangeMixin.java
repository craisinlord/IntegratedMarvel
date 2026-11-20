package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.portal.MarvelPortalRenderer;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "dev.svportals.client.StrangePortalRenderer", remap = false)
abstract class MarvelPortalStrangeMixin {
    @ModifyExpressionValue(method = "draw", at = @At(value = "INVOKE",
            target = "Ldev/svportals/client/preview/PreviewTextures;has(Ljava/util/UUID;)Z"), require = 0)
    private static boolean integratedSuperheroes$skipSurface(boolean original) {
        return original || MarvelPortalRenderer.rimOnly();
    }
}
