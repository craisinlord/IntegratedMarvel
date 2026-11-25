package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.suit.SuitFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.tintankgames.marvel.world.item.ShoulderTurretItem", remap = false)
public abstract class WarMachineTurretFxMixin {
    @Inject(method = "shootProjectile", at = @At("TAIL"), remap = false)
    private void integratedSuperheroes$turretFx(LivingEntity shooter, Projectile projectile, int index, float velocity, float inaccuracy,
                                        float angle, LivingEntity target, CallbackInfo ci) {
        if (shooter.level() instanceof ServerLevel level) {
            SuitFx.turretShot(level, shooter, true);
        }
    }
}
