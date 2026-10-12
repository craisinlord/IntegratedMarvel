package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.MarvelWeaponConfig;
import net.tintankgames.marvel.world.entity.projectile.ThrownMjolnir;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.entity.projectile.ThrownMjolnir", remap = false)
public abstract class ThrownMjolnirDamageMixin {
    @Redirect(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/tintankgames/marvel/world/entity/projectile/ThrownMjolnir;getBaseDamage()D",
                    remap = false
            ),
            remap = false
    )
    private double integratedMarvel$setThrownMjolnirDamage(ThrownMjolnir projectile) {
        return MarvelWeaponConfig.or(MarvelWeaponConfig.THROWN_MJOLNIR_DAMAGE, projectile.getBaseDamage());
    }
}
