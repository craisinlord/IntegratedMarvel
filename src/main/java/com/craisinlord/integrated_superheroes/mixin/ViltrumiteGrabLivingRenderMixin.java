package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public abstract class ViltrumiteGrabLivingRenderMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.GrabbedEntityRendererMixin", name = "onRenderAnimationFix")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idleRender(LivingEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers,
                                                 int light, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.GrabbedEntityRendererMixin", name = "onSetupTransforms")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idleTransforms(LivingEntity entity, PoseStack pose, float bob, float bodyYaw, float partialTick, float scale,
                                                     CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }
}
