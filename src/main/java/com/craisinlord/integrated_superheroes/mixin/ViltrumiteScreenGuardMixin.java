package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.FlightClientConfig;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GameRenderer.class, priority = 1500)
public abstract class ViltrumiteScreenGuardMixin {
    @Shadow
    private float fov;

    @Shadow
    public abstract PostChain currentEffect();

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.GameRendererMixin", name = "onRender")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$keepOtherShaders(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteGuards.visuals()) {
            return;
        }
        PostChain effect = currentEffect();
        if (effect != null && !ViltrumiteGuards.SONIC_BOOM_SHADER.equals(effect.getName())) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.GameRendererMixin", name = "onRender")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/resources/ResourceLocation;fromNamespaceAndPath(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"), remap = false)
    private ResourceLocation integratedSuperheroes$scalableShader(ResourceLocation shader) {
        return ViltrumiteGuards.SONIC_BOOM_ID;
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.GameRendererMixin", name = "onRender")
    @Inject(method = "@MixinSquared:Handler", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$scaleShake(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo original, CallbackInfo ci) {
        PostChain effect = currentEffect();
        if (effect == null || !ViltrumiteGuards.SONIC_BOOM_SHADER.equals(effect.getName())) {
            return;
        }
        float scale = FlightClientConfig.shakeScale();
        for (PostPass pass : ((ViltrumitePostChainAccessor) effect).integratedSuperheroes$getPasses()) {
            pass.getEffect().safeGetUniform("ShakeScale").set(scale);
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.GameRendererMixin", name = "shatterFovLimit")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$vanillaFovOutsideFlight(CallbackInfo original, CallbackInfo ci) {
        if (!FlightClientConfig.fovEffect() || ViltrumiteGuards.visuals() && fov <= 1.5F && !ViltrumiteBridge.inFlight(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }
}
