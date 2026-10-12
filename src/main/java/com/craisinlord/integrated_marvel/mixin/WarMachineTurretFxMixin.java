package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.suit.SuitFx;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.tintankgames.marvel.world.item.ShoulderTurretItem", remap = false)
public abstract class WarMachineTurretFxMixin {
    @Inject(method = "shootProjectile", at = @At("TAIL"), remap = false)
    private void integratedMarvel$turretFx(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy,
                                        float angle, LivingEntity target, CallbackInfo ci) {
        if (!(shooter.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 look = shooter.getLookAngle();
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        right = right.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
        Vec3 barrel = shooter.getEyePosition().add(0.0D, 0.15D, 0.0D).add(right.scale(0.45D)).add(look.scale(0.9D));
        SuitFx.burst(level, SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, barrel, 1, 0.0D, 0.0D);
        Vec3 ejection = barrel.subtract(look.scale(0.6D));
        level.sendParticles(SuitFx.particle(SuitFx.BRASS_CASING, ParticleTypes.CRIT), ejection.x, ejection.y, ejection.z, 0,
                right.x * 0.3D, 0.25D, right.z * 0.3D, 1.0D);
    }
}
