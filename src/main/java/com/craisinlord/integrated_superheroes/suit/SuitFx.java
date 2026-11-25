package com.craisinlord.integrated_superheroes.suit;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class SuitFx {
    public static final String GYROJET_FIRE = "item.gyrojet.fire";
    public static final String ROCKET_FIRE = "item.rocket.fire";
    public static final String ROCKET_RIFLE_FIRE = "item.rocket_rifle.fire";
    public static final String JETPACK_FIRE = "item.jetpack.fire";
    public static final String LASER_FIRE = "item.laser.fire";
    public static final String BEAM_FIRE = "item.beam.fire";
    public static final String PLASMA_FIRE = "item.plasma.fire";
    public static final String GRENADE_LAUNCHER_FIRE = "item.grenade_launcher.fire";
    public static final String GRENADE_PIN = "item.grenade.pin";

    public static final String ROCKET_TRAIL = "rocket_trail";
    public static final String ROCKET_EXPLOSION = "rocket_explosion";
    public static final String PLASMA_RING = "plasma_ring";
    public static final String PLASMA_EXPLOSION = "plasma_explosion";
    public static final String SONIC_BLAST = "sonic_blast";
    public static final String MUZZLE_FLASH = "turret_muzzle_flash";
    public static final String BRASS_CASING = "brass_casing";
    public static final String LASER = "laser";
    public static final String SMALL_LASER = "small_laser";

    private SuitFx() {
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("scguns", path);
    }

    public static ParticleOptions particle(String name, SimpleParticleType fallback) {
        return BuiltInRegistries.PARTICLE_TYPE.get(id(name)) instanceof SimpleParticleType type ? type : fallback;
    }

    public static void sound(Level level, Vec3 at, String name, Holder<SoundEvent> fallback, float volume, float pitch) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id(name)).orElse(fallback.value());
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static void sound(Level level, Vec3 at, String name, SoundEvent fallback, float volume, float pitch) {
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getOptional(id(name)).orElse(fallback);
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    public static Vec3 turretBarrel(LivingEntity shooter) {
        Vec3 look = shooter.getLookAngle();
        return shooter.getEyePosition().add(0.0D, 0.15D, 0.0D).add(right(look).scale(0.45D)).add(look.scale(0.9D));
    }

    public static void turretShot(ServerLevel level, LivingEntity shooter, boolean casing) {
        Vec3 look = shooter.getLookAngle();
        Vec3 barrel = turretBarrel(shooter);
        burst(level, MUZZLE_FLASH, ParticleTypes.FLAME, barrel, 1, 0.0D, 0.0D);
        if (casing) {
            Vec3 right = right(look);
            Vec3 ejection = barrel.subtract(look.scale(0.6D));
            level.sendParticles(particle(BRASS_CASING, ParticleTypes.CRIT), ejection.x, ejection.y, ejection.z, 0,
                    right.x * 0.3D, 0.25D, right.z * 0.3D, 1.0D);
        }
    }

    private static Vec3 right(Vec3 look) {
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        return right.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
    }

    public static void burst(ServerLevel level, String name, SimpleParticleType fallback, Vec3 at, int count, double spread, double speed) {
        level.sendParticles(particle(name, fallback), at.x, at.y, at.z, count, spread, spread, spread, speed);
    }
}
