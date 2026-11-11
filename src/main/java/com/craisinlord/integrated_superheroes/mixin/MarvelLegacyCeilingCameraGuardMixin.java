package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.client.event.ViewportEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The ceiling-crawl camera roll casts the camera entity to Player without checking. Skipped for non-player cameras.
 * With flipped gravity the roll needs no change: NeoForge feeds it to Camera.setRotation as a roll around the view
 * axis and Anthology turns the whole camera over afterwards, so the 180 roll is relative to the flipped view and
 * leaves a player crawling on the world floor looking world-upright.
 */
@Mixin(targets = "com.stxllarplanes.marvellegacy.power.SpiderPowers", remap = false)
public abstract class MarvelLegacyCeilingCameraGuardMixin {
    @Inject(method = "ceilingCameraRotate", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipNonPlayerCamera(ViewportEvent.ComputeCameraAngles event, CallbackInfo ci) {
        if (event.getCamera() == null || !(event.getCamera().getEntity() instanceof Player)) {
            ci.cancel();
        }
    }
}
