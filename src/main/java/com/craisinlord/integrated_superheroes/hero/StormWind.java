package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.AntarchyWindVortex;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class StormWind {
    private static final double GALE_RANGE = 16.0D;
    private static final double GALE_CONE = 0.55D;
    private static final double GALE_STRENGTH = 3.2D;

    private StormWind() {
    }

    public static void takeoff(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (ModCompat.ANTARCHY) {
            AntarchyWindVortex.takeoff(level, player);
            return;
        }
        Vec3 at = player.position().subtract(0.0D, 0.5D, 0.0D);
        level.explode(player, null, AbstractWindCharge.EXPLOSION_DAMAGE_CALCULATOR, at.x, at.y, at.z, 2.5F, false,
                Level.ExplosionInteraction.TRIGGER, ParticleTypes.GUST_EMITTER_SMALL, ParticleTypes.GUST_EMITTER_LARGE,
                SoundEvents.WIND_CHARGE_BURST);
    }

    public static void gale(ServerPlayer player, Vec3 origin, Vec3 look) {
        ServerLevel level = player.serverLevel();
        if (ModCompat.ANTARCHY) {
            AntarchyWindVortex.gale(level, origin, look, player);
            return;
        }
        Vec3 direction = look.normalize();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(GALE_RANGE),
                entity -> entity != player && entity.isAlive() && !entity.isSpectator())) {
            Vec3 offset = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D).subtract(origin);
            double distance = offset.length();
            if (distance > GALE_RANGE || distance < 1.0E-4D || offset.normalize().dot(direction) < GALE_CONE) {
                continue;
            }
            double strength = GALE_STRENGTH * (1.0D - distance / (GALE_RANGE * 1.25D)) * (1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE) * 0.5D);
            target.setDeltaMovement(target.getDeltaMovement().add(direction.x * strength, 0.35D + Math.max(direction.y, 0.0D) * strength, direction.z * strength));
            target.hurtMarked = true;
            target.hasImpulse = true;
        }
        for (int i = 1; i <= 8; i++) {
            Vec3 point = origin.add(direction.scale(i * 2.0D));
            double spread = i * 0.35D;
            level.sendParticles(ParticleTypes.GUST, point.x, point.y, point.z, 2, spread, spread * 0.5D, spread, 0.0D);
            level.sendParticles(ParticleTypes.SMALL_GUST, point.x, point.y, point.z, 4, spread, spread * 0.5D, spread, 0.02D);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_BURST, player.getSoundSource(), 1.2F, 0.8F);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BREEZE_WIND_CHARGE_BURST, player.getSoundSource(), 1.0F, 0.7F);
    }
}
