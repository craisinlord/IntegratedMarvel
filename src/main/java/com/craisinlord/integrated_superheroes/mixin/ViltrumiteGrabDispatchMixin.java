package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EntityRenderDispatcher.class, priority = 1500)
public abstract class ViltrumiteGrabDispatchMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.EntityShadowMixin", name = "disableGrabbedShadow")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void integratedSuperheroes$idleShadow(PoseStack pose, MultiBufferSource buffers, Entity entity, float opacity, float partialTick,
                                                        LevelReader level, float size, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active() || !ViltrumiteGuards.mayBeGrabbed(entity)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.GrabbedEntityPositionMixin", name = "onRenderAbsolutePositionFix")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idlePosition(Entity entity, double x, double y, double z, float yaw, float partialTick, PoseStack pose,
                                                   MultiBufferSource buffers, int light, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active() || !ViltrumiteGuards.mayBeGrabbed(entity)) {
            ci.cancel();
        }
    }
}
