package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroHudClientConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.event.ClientHandler", remap = false)
public abstract class ThematicEquipPreviewMixin {
    @Inject(method = "lambda$register$0", at = @At("HEAD"), cancellable = true, require = 0)
    private static void integratedSuperheroes$hideEquipPreview(CallbackInfo ci) {
        if (HeroHudClientConfig.hideSuitEquipPreview()) {
            ci.cancel();
        }
    }
}
