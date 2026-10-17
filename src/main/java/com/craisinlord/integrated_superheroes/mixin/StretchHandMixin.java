package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.FantasticClient;
import com.craisinlord.integrated_superheroes.hero.HulkProportions;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerRenderer.class)
public abstract class StretchHandMixin {
    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$hideStretchedHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
                                                ModelPart sleeve, CallbackInfo ci) {
        if (FantasticClient.hidesHand(player, arm == ((PlayerRenderer) (Object) this).getModel().rightArm)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderHand", at = @At("HEAD"))
    private void integratedSuperheroes$hulkFist(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
                                                ModelPart sleeve, CallbackInfo ci) {
        float width = HulkProportions.handWidth(player);
        if (width != 1.0F) {
            arm.xScale = width;
            arm.zScale = width;
            sleeve.xScale = width;
            sleeve.zScale = width;
        }
    }

    @Inject(method = "renderHand", at = @At("RETURN"))
    private void integratedSuperheroes$resetFist(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
                                                 ModelPart sleeve, CallbackInfo ci) {
        arm.xScale = 1.0F;
        arm.zScale = 1.0F;
        sleeve.xScale = 1.0F;
        sleeve.zScale = 1.0F;
    }
}
