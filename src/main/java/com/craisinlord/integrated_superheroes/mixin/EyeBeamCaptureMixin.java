package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.EyeBeamClient;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class EyeBeamCaptureMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V"),
            require = 0
    )
    private void integratedSuperheroes$captureEyes(T living, float entityYaw, float partialTick, PoseStack poseStack,
                                                   MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!EyeBeamClient.wantsEyes(living.getId()) || !(this.model instanceof HumanoidModel<?> humanoid)
                || RenderSystem.getProjectionMatrix().m33() > 0.5F) {
            return;
        }
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        poseStack.pushPose();
        humanoid.head.translateAndRotate(poseStack);
        Vec3 left = integratedSuperheroes$point(poseStack, 2.0F, camera);
        Vec3 right = integratedSuperheroes$point(poseStack, -2.0F, camera);
        poseStack.popPose();
        EyeBeamClient.captureEyes(living.getId(), left, right);
    }

    private static Vec3 integratedSuperheroes$point(PoseStack poseStack, float x, Vec3 camera) {
        Vector4f point = new Vector4f(x / 16.0F, -3.5F / 16.0F, -4.2F / 16.0F, 1.0F);
        poseStack.last().pose().transform(point);
        return new Vec3(point.x() + camera.x, point.y() + camera.y, point.z() + camera.z);
    }
}
