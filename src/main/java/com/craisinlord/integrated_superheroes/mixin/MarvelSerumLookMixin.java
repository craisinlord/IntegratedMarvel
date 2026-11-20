package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.skin.MarvelSerumLooks;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(PlayerRenderer.class)
public abstract class MarvelSerumLookMixin {
    @WrapMethod(method = "render(Lnet/minecraft/client/player/AbstractClientPlayer;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V")
    private void integratedSuperheroes$serumLook(AbstractClientPlayer player, float yaw, float partialTick, PoseStack pose,
                                                 MultiBufferSource buffers, int light, Operation<Void> original) {
        Entity previous = MarvelSerumLooks.begin(player);
        try {
            original.call(player, yaw, partialTick, pose, buffers, light);
        } finally {
            MarvelSerumLooks.end(previous);
        }
    }

    @WrapMethod(method = "renderRightHand")
    private void integratedSuperheroes$serumLookRightHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                                          Operation<Void> original) {
        Entity previous = MarvelSerumLooks.begin(player);
        try {
            original.call(pose, buffers, light, player);
        } finally {
            MarvelSerumLooks.end(previous);
        }
    }

    @WrapMethod(method = "renderLeftHand")
    private void integratedSuperheroes$serumLookLeftHand(PoseStack pose, MultiBufferSource buffers, int light, AbstractClientPlayer player,
                                                         Operation<Void> original) {
        Entity previous = MarvelSerumLooks.begin(player);
        try {
            original.call(pose, buffers, light, player);
        } finally {
            MarvelSerumLooks.end(previous);
        }
    }
}
