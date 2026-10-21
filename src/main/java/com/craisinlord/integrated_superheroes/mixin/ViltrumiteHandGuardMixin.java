package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.baranhan.viltrumiteflight.client.PoseDataManager;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ItemInHandRenderer.class, priority = 1500)
public abstract class ViltrumiteHandGuardMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.ItemInHandRendererMixin", name = "onRenderFirstPersonItem")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$onlyViltrumiteHands(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand, float swingProgress,
                                                         ItemStack stack, float equipProgress, PoseStack pose, MultiBufferSource buffers, int light,
                                                         CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteGuards.visuals()) {
            return;
        }
        boolean untouched = PoseDataManager.FP.translateX == 0.0F && PoseDataManager.FP.translateY == 0.0F && PoseDataManager.FP.translateZ == 0.0F
                && PoseDataManager.FP.rotateX == 0.0F && PoseDataManager.FP.rotateY == 0.0F && PoseDataManager.FP.rotateZ == 0.0F;
        if (untouched || !ViltrumiteGuards.styled(player)) {
            ci.cancel();
        }
    }
}
