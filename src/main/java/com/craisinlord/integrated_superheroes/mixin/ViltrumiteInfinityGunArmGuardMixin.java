package com.craisinlord.integrated_superheroes.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Viltrumite Core's Infinity Gun draws the player's arms on the gun by casting the local player's
 * renderer to {@link PlayerRenderer} and its model to {@link PlayerModel} with no check. When the
 * player is morphed, suited or otherwise using a replaced renderer, that cast throws and crashes the
 * client (reported when opening the inventory with the gun held). Skip the arm overlay in that case.
 */
@Pseudo
@Mixin(targets = "dev.baranhan.viltrumitecore.client.render.InfinityGunRenderer", remap = false)
public abstract class ViltrumiteInfinityGunArmGuardMixin {
    @Inject(method = "renderPendingArms", at = @At("HEAD"), cancellable = true, require = 0)
    private void integratedSuperheroes$guardArmRenderer(PoseStack poseStack, MultiBufferSource buffers, int light, int overlay, CallbackInfo ci) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        EntityRenderer<?> renderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
        if (!(renderer instanceof PlayerRenderer playerRenderer) || !(playerRenderer.getModel() instanceof PlayerModel<?>)) {
            ci.cancel();
        }
    }
}
