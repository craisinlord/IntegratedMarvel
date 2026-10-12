package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.FantasticClient;
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
    private void integratedMarvel$hideStretchedHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player, ModelPart arm,
                                                ModelPart sleeve, CallbackInfo ci) {
        if (FantasticClient.hidesHand(player, arm == ((PlayerRenderer) (Object) this).getModel().rightArm)) {
            ci.cancel();
        }
    }
}
