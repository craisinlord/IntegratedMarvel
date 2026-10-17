package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

@Mixin(value = GeoEntityRenderer.class, priority = 1500)
public abstract class ViltrumiteGrabGeckoRenderMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.GeckoLibGrabRendererMixin", name = "onGeckoRenderFix")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idleGecko(Entity entity, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light,
                                                CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }
}
