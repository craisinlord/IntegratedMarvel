package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.portal.MarvelPortalRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "dev.svportals.client.HexPortalRenderer", remap = false)
abstract class MarvelPortalHexMixin {
    @Inject(method = "gradientDisk", at = @At("HEAD"), cancellable = true, require = 0)
    private static void integratedSuperheroes$skipSurface(BufferBuilder buffer, Matrix4f matrix,
                                                         double z, double radius, double angle,
                                                         int inner, int outer, CallbackInfo callback) {
        if (MarvelPortalRenderer.rimOnly()) {
            callback.cancel();
        }
    }
}
