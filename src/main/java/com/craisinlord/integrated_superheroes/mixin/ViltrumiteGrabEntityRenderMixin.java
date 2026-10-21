package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = EntityRenderer.class, priority = 1500)
public abstract class ViltrumiteGrabEntityRenderMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.GrabbedEntityRendererMixin2", name = "forceRenderWhenGrabbed")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idleCulling(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> original,
                                                  CallbackInfo ci) {
        if (!ViltrumiteCompat.active() || !ViltrumiteGuards.mayBeGrabbed(entity)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.GrabbedEntityRendererMixin2", name = "fixGrabbedNametagSpace")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idleNametag(Entity entity, Component name, PoseStack pose, MultiBufferSource buffers, int light, float partialTick,
                                                  CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active() || !ViltrumiteGuards.mayBeGrabbed(entity)) {
            ci.cancel();
        }
    }
}
