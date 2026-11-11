package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.FlightClientConfig;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbstractClientPlayer.class, priority = 1500)
public abstract class ViltrumiteFovMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.AbstractClientPlayerMixin", name = "onGetFovMultiplier")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD",
            target = "Ldev/baranhan/viltrumiteflight/config/ViltrumiteConfigClient;enableFovEffect:Z"), remap = false)
    private boolean integratedSuperheroes$fovEffect(boolean enabled) {
        return enabled && FlightClientConfig.fovEffect();
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.client.mixin.AbstractClientPlayerMixin", name = "onGetFovMultiplier")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "FIELD",
            target = "Ldev/baranhan/viltrumiteflight/config/ViltrumiteConfigClient;fovMultiplier:F"), remap = false)
    private float integratedSuperheroes$fovScale(float multiplier) {
        return multiplier * FlightClientConfig.fovScale();
    }
}
