package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManSuits;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.entity.projectile.Flame", remap = false)
public abstract class IronManFlameDamageMixin {
    @Redirect(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            remap = false
    )
    private boolean integratedMarvel$scaleFlameDamage(Entity target, DamageSource source, float amount) {
        if (!IronManSuits.isLoaded()) {
            return target.hurt(source, amount);
        }
        return target.hurt(source, amount * (float) (IronManSuits.FLAMETHROWER_DAMAGE.get() / 2.0D));
    }
}
