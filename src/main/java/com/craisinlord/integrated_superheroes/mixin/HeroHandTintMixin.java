package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroTint;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerRenderer.class)
public abstract class HeroHandTintMixin {
    @WrapOperation(
            method = "renderHand",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/geom/ModelPart;render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;II)V")
    )
    private void integratedSuperheroes$heroHandTint(ModelPart part, PoseStack pose, VertexConsumer consumer, int light, int overlay,
                                           Operation<Void> original, @Local(argsOnly = true) AbstractClientPlayer player) {
        int color = HeroTint.colorFor(player);
        if (color == -1) {
            original.call(part, pose, consumer, light, overlay);
        } else {
            part.render(pose, consumer, light, overlay, color);
        }
    }
}
