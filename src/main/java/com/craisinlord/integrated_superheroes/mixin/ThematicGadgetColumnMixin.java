package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroHudClientConfig;
import com.craisinlord.integrated_superheroes.hero.HeroSlotsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.hud.HudLayout", remap = false)
public abstract class ThematicGadgetColumnMixin {
    @Inject(method = "loadout", at = @At("HEAD"), cancellable = true, require = 0)
    private static void integratedSuperheroes$gadgetsInAbilityHud(CallbackInfo ci) {
        if (HeroHudClientConfig.suitGadgetsInHud() && HeroSlotsClient.hudShown()) {
            ci.cancel();
        }
    }
}
