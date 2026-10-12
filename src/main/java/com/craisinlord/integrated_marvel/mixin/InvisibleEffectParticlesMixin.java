package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.InvisibleWomanPower;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntity.class)
public abstract class InvisibleEffectParticlesMixin {
    @WrapOperation(method = "tickEffects", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V"))
    private void integratedMarvel$hideSwirls(Level level, ParticleOptions particle, double x, double y, double z, double dx, double dy, double dz,
                                         Operation<Void> original) {
        if (!((Object) this instanceof Player player && InvisibleWomanPower.isInvisible(player))) {
            original.call(level, particle, x, y, z, dx, dy, dz);
        }
    }
}
