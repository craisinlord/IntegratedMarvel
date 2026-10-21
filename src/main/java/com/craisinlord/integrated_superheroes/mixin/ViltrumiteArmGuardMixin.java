package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PlayerRenderer.class, priority = 1500)
public abstract class ViltrumiteArmGuardMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.PlayerRendererMixin", name = "fixRightArmFP")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$keepRightArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                                  CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteGuards.styled(player)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.PlayerRendererMixin", name = "fixLeftArmFP")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$keepLeftArm(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                                 CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteGuards.styled(player)) {
            ci.cancel();
        }
    }
}
