package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.tintankgames.marvel.client.renderer.MarvelRenderTypes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MarvelRenderTypes.class, remap = false)
public abstract class MarvelEmissiveShaderCompatMixin {
    @Inject(method = "entityEmissive(Lnet/minecraft/resources/ResourceLocation;Z)Lnet/minecraft/client/renderer/RenderType;", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$vanillaEmissive(ResourceLocation texture, boolean outline, CallbackInfoReturnable<RenderType> cir) {
        cir.setReturnValue(RenderType.entityTranslucentEmissive(texture, outline));
    }
}
