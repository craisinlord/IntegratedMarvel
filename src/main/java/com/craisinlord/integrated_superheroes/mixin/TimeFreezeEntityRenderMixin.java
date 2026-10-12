package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.TimeFreezeState;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public abstract class TimeFreezeEntityRenderMixin {
    @WrapOperation(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;renderEntity(Lnet/minecraft/world/entity/Entity;DDDFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;)V"))
    private void integratedSuperheroes$holdFrozenFrame(LevelRenderer renderer, Entity entity, double camX, double camY, double camZ,
                                                       float partialTick, PoseStack poseStack, MultiBufferSource buffers,
                                                       Operation<Void> original) {
        boolean frozen = TimeFreezeState.clientActive() && TimeFreezeState.isFrozen(entity);
        original.call(renderer, entity, camX, camY, camZ, frozen ? 1.0F : partialTick, poseStack, buffers);
    }
}
