package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.SurferBoardClient;
import com.craisinlord.integrated_superheroes.hero.SurferBoardRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class SilverSurferBoardPoseMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Unique
    private static final float INTEGRATED_SUPERHEROES$STANCE = 90.0F;
    @Unique
    private static final float INTEGRATED_SUPERHEROES$HEAD_RETURN = 75.0F * ((float) Math.PI / 180.0F);
    @Unique
    private static final float INTEGRATED_SUPERHEROES$FEET = 1.501F;

    @Shadow
    protected M model;

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;setupAnim(Lnet/minecraft/world/entity/Entity;FFFFF)V", shift = At.Shift.AFTER)
    )
    private void integratedSuperheroes$silverSurferBoard(T living, float entityYaw, float partialTick, PoseStack poseStack,
                                                        MultiBufferSource buffers, int light, CallbackInfo ci) {
        SurferBoardClient.View view = SurferBoardClient.view(living, partialTick);
        if (view == null) {
            return;
        }
        if (this.model instanceof HumanoidModel<?> humanoid) {
            integratedSuperheroes$stance(humanoid);
        }
        poseStack.translate(0.0F, INTEGRATED_SUPERHEROES$FEET, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-view.bank()));
        poseStack.translate(0.0F, -SurferBoardRenderer.RIDE_HEIGHT, 0.0F);
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || !living.isInvisibleTo(minecraft.player)) {
            poseStack.pushPose();
            poseStack.scale(-1.0F, -1.0F, 1.0F);
            SurferBoardRenderer.draw(poseStack, buffers, light, OverlayTexture.NO_OVERLAY);
            poseStack.popPose();
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(INTEGRATED_SUPERHEROES$STANCE));
        poseStack.translate(0.0F, -INTEGRATED_SUPERHEROES$FEET, 0.0F);
    }

    @Unique
    private static void integratedSuperheroes$stance(HumanoidModel<?> humanoid) {
        humanoid.head.yRot -= INTEGRATED_SUPERHEROES$HEAD_RETURN;
        humanoid.hat.copyFrom(humanoid.head);
        if (humanoid.attackTime <= 0.0F) {
            humanoid.leftArm.xRot = -0.25F;
            humanoid.leftArm.yRot = 0.0F;
            humanoid.leftArm.zRot = -1.15F;
            humanoid.rightArm.xRot = 0.15F;
            humanoid.rightArm.yRot = 0.0F;
            humanoid.rightArm.zRot = 0.65F;
        }
        humanoid.leftLeg.xRot = 0.0F;
        humanoid.leftLeg.yRot = -0.12F;
        humanoid.leftLeg.zRot = -0.26F;
        humanoid.rightLeg.xRot = 0.0F;
        humanoid.rightLeg.yRot = 0.12F;
        humanoid.rightLeg.zRot = 0.26F;
        if (humanoid instanceof PlayerModel<?> player) {
            player.leftSleeve.copyFrom(player.leftArm);
            player.rightSleeve.copyFrom(player.rightArm);
            player.leftPants.copyFrom(player.leftLeg);
            player.rightPants.copyFrom(player.rightLeg);
        }
    }
}
