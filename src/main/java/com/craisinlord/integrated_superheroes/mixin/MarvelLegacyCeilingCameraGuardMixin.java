package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.stxllarplanes.marvellegacy.power.SpiderPowers", remap = false)
public abstract class MarvelLegacyCeilingCameraGuardMixin {
    @Inject(method = "ceilingCameraRotate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipNonPlayerCamera(ViewportEvent.ComputeCameraAngles event, CallbackInfo ci) {
        if (event.getCamera() == null || !(event.getCamera().getEntity() instanceof Player)) {
            ci.cancel();
        }
    }
}
