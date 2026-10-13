package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.FantasticClient;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class StretchPoseMixin {
    @WrapOperation(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderToBuffer(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V")
    )
    private void integratedSuperheroes$stretch(EntityModel<?> model, PoseStack pose, VertexConsumer consumer, int light, int overlay, int color,
                                      Operation<Void> original, @Local(argsOnly = true) LivingEntity entity, @Local(argsOnly = true, ordinal = 1) float partialTick) {
        if (entity instanceof Player player && model instanceof PlayerModel<?> playerModel) {
            FantasticClient.pose(player, playerModel, partialTick, pose);
            original.call(model, pose, consumer, light, overlay, color);
            FantasticClient.renderNeck(player, playerModel, pose, consumer, light, overlay, color);
            return;
        }
        original.call(model, pose, consumer, light, overlay, color);
    }

    @Inject(
            method = "render(Lnet/minecraft/world/entity/LivingEntity;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("RETURN")
    )
    private void integratedSuperheroes$unstretch(LivingEntity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                                        CallbackInfo ci) {
        if (entity instanceof Player && ((LivingEntityRenderer<?, ?>) (Object) this).getModel() instanceof PlayerModel<?> model) {
            FantasticClient.reset(model);
        }
    }
}
