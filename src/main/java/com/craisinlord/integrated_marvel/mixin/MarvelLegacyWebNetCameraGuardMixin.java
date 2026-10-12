package com.craisinlord.integrated_marvel.mixin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "com.stxllarplanes.marvellegacy.client.renderer.WebNetRenderer",
        "com.stxllarplanes.marvellegacy.client.renderer.WebLineRenderer"
}, remap = false)
public abstract class MarvelLegacyWebNetCameraGuardMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedMarvel$skipNonPlayerCamera(RenderLevelStageEvent event, CallbackInfo ci) {
        if (event.getCamera() == null || !(event.getCamera().getEntity() instanceof Player)) {
            ci.cancel();
        }
    }
}
