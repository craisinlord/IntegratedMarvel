package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.HeroPowers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "com.stxllarplanes.marvellegacy.event.PlayerLandingEvent", remap = false)
public abstract class LegacyHeroLandingMixin {
    @Redirect(
            method = "landAnimation",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z", remap = false),
            remap = false
    )
    private static boolean integratedMarvel$anyHeroLands(String primaryPower, Object spiderId) {
        return primaryPower != null
                && (primaryPower.equals(spiderId) || "weapon_x_logan".equals(primaryPower) || HeroPowers.get(primaryPower) != null);
    }
}
