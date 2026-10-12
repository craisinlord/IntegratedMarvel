package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.MarvelWeaponConfig;
import net.tintankgames.marvel.world.entity.projectile.ThrownStormbreaker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.entity.projectile.ThrownStormbreaker", remap = false)
public abstract class ThrownStormbreakerDamageMixin {
    @Redirect(
            method = "onHitEntity",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/tintankgames/marvel/world/entity/projectile/ThrownStormbreaker;getBaseDamage()D",
                    remap = false
            ),
            remap = false
    )
    private double integratedSuperheroes$setThrownStormbreakerDamage(ThrownStormbreaker projectile) {
        return MarvelWeaponConfig.or(MarvelWeaponConfig.THROWN_STORMBREAKER_DAMAGE, projectile.getBaseDamage());
    }
}
