package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.KittyPower;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(LevelRenderer.class)
public abstract class PhaseCullingMixin {
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/LevelRenderer;setupRender(Lnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;ZZ)V"),
            index = 3)
    private boolean integratedMarvel$phaseSeesThrough(boolean spectator) {
        Minecraft minecraft = Minecraft.getInstance();
        return spectator || minecraft.player != null && KittyPower.isPhasing(minecraft.player);
    }
}
