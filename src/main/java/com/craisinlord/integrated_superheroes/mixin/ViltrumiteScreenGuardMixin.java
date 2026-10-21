package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PostChain;
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

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.GameRendererMixin", name = "shatterFovLimit")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$vanillaFovOutsideFlight(CallbackInfo original, CallbackInfo ci) {
        if (ViltrumiteGuards.visuals() && fov <= 1.5F && !ViltrumiteBridge.inFlight(Minecraft.getInstance().player)) {
            ci.cancel();
        }
    }
}
