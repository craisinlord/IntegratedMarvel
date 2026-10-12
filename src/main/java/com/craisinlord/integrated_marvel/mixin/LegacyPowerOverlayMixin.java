package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.HeroSlotsClient;
import com.stxllarplanes.marvellegacy.client.gui.overlay.PowerOverlay;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PowerOverlay.class, remap = false)
public abstract class LegacyPowerOverlayMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void integratedMarvel$tenSlotOverlay(GuiGraphics graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
        HeroSlotsClient.renderOverlay(graphics);
        ci.cancel();
    }
}
