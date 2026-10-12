package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.craisinlord.integrated_superheroes.suit.SuitPassives;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.entity.projectile.Repulsor", remap = false)
public abstract class IronManRepulsorDamageMixin {
    @Redirect(
            method = "onHitEntity",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z"),
            remap = false
    )
    private boolean integratedSuperheroes$scaleRepulsorDamage(Entity target, DamageSource source, float amount) {
        IronManSuits.Stats stats = IronManSuits.isLoaded() ? IronManSuits.forWearer(((Projectile) (Object) this).getOwner()) : null;
        if (stats == null || stats.repulsorDamage().get() <= 0.0D) {
            return target.hurt(source, amount);
        }
        if (target instanceof LivingEntity living) {
            living.invulnerableTime = 0;
        }
        boolean hurt = target.hurt(source, stats.repulsorDamage().get().floatValue());
        if (hurt) {
            SuitPassives.onRepulsorHit(((Projectile) (Object) this).getOwner(), target);
        }
        return hurt;
    }
}
