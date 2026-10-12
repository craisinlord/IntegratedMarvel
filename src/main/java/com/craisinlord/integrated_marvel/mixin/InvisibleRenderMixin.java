package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.FantasticClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class InvisibleRenderMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$hideInvisible(Entity entity, double x, double y, double z, float yaw, float partialTick, PoseStack pose,
                                            MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (FantasticClient.hidden(entity)) {
            ci.cancel();
        }
    }
}
