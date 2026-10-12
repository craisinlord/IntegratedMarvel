package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.DaredevilClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class RadarGlowMixin {
    @Inject(method = "shouldEntityAppearGlowing", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$radarGlow(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (DaredevilClient.highlights(entity)) {
            cir.setReturnValue(true);
        }
    }
}
