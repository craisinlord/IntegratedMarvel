package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.HulkClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HulkPortalOverlayMixin {
    @Inject(method = "renderPortalOverlay", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$noPortalTintWhileRaging(GuiGraphics graphics, float alpha, CallbackInfo callback) {
        if (HulkClient.isWarping()) {
            callback.cancel();
        }
    }
}
