package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.BinaryFlight;
import com.craisinlord.integrated_superheroes.hero.BinaryFlightTilt;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class BinaryFlightPoseMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Shadow
    protected M model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", shift = At.Shift.AFTER)
    )
    private void integratedSuperheroes$binaryFlightPose(T living, float entityYaw, float partialTick, PoseStack poseStack,
                                               MultiBufferSource buffers, int light, CallbackInfo ci) {
        if (!BinaryFlight.isFlying(living)) {
            return;
        }
        BinaryFlightTilt.Tilt tilt = BinaryFlightTilt.sample(living, partialTick);
        double forward = tilt.forward();
        double side = tilt.side();
        double climb = tilt.climb();
        if (this.model instanceof HumanoidModel<?> humanoid) {
            humanoid.head.xRot -= (float) forward;
            humanoid.hat.xRot -= (float) forward;
            if (forward > 0.2D) {
                var arm = living.getMainArm() == HumanoidArm.LEFT ? humanoid.leftArm : humanoid.rightArm;
                arm.xRot = (float) (-Math.PI * forward);
                arm.zRot = 0.0F;
            }
        }
        poseStack.mulPose(Axis.XP.rotationDegrees((float) (forward * 90.0D - climb * Math.max(forward, 0.0D) * 90.0D)));
        poseStack.mulPose(Axis.ZN.rotationDegrees((float) (side * 30.0D)));
    }
}
