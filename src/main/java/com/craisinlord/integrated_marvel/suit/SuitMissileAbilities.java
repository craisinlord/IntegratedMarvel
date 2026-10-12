package com.craisinlord.integrated_marvel.suit;

import com.github.alexmodguy.alexscaves.server.misc.ACSoundRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class SuitMissileAbilities {
    private static final float MICRO_MISSILE_SCALE = 0.7F;
    private static final double FAN_DEGREES = 50.0D;

    private SuitMissileAbilities() {
    }

    static SuitAbility.Result microMissiles(SuitContext context) {
        ServerPlayer player = context.player();
        int min = SuitAbilityConfig.MICRO_MISSILES_MIN.get();
        int max = Math.max(min, SuitAbilityConfig.MICRO_MISSILES_MAX.get());
        int count = min + (int) Math.round((max - min) * context.tier());
        List<LivingEntity> targets = SuitTargeting.volley(player, count);
        float damage = context.damage(SuitAbilityConfig.MICRO_MISSILES);

        Vec3 look = player.getLookAngle();
        Vec3 right = right(look);
        Vec3 shoulders = shoulders(player);
        for (int i = 0; i < count; i++) {
            double spread = count == 1 ? 0.0D : (i / (double) (count - 1)) - 0.5D;
            Vec3 direction = look.yRot((float) Math.toRadians(-spread * FAN_DEGREES)).add(0.0D, 0.25D, 0.0D);
            Vec3 from = shoulders.add(right.scale(spread * 0.9D));
            LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
            SuitMissileEntity.launch(player, SuitMissileEntity.Kind.MICRO, from, direction, damage, MICRO_MISSILE_SCALE)
                    .target(target)
                    .splash(SuitAbilityConfig.MISSILE_SPLASH_RADIUS.get(), 0.5F)
                    .straightFor(3 + player.getRandom().nextInt(4))
                    .spawn();
        }
        SuitFx.burst(player.serverLevel(), SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, shoulders, 1, 0.0D, 0.0D);
        SuitFx.sound(player.level(), player.position(), SuitFx.GYROJET_FIRE, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result shoulderRocket(SuitContext context) {
        ServerPlayer player = context.player();
        SuitMissileEntity.launch(player, SuitMissileEntity.Kind.ROCKET, shoulders(player).add(right(player.getLookAngle()).scale(0.4D)),
                        player.getLookAngle(), context.damage(SuitAbilityConfig.SHOULDER_ROCKET), 1.0F)
                .splash(3.0D, 1.0F)
                .spawn();
        playLaunch(player);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result ordnanceDrop(SuitContext context) {
        ServerPlayer player = context.player();
        Vec3 point = aimPoint(player, SuitAbilityConfig.MISSILE_LOCK_RANGE.get());
        if (point == null) {
            player.displayClientMessage(Component.literal("§cNo target in range"), true);
            return SuitAbility.Result.FAILED;
        }
        RandomSource random = player.getRandom();
        float damage = context.damage(SuitAbilityConfig.ORDNANCE_DROP);
        int count = SuitAbilityConfig.ORDNANCE_DROP_MISSILES.get();
        double radius = SuitAbilityConfig.ORDNANCE_DROP_RADIUS.get();
        for (int i = 0; i < count; i++) {
            Vec3 up = new Vec3(random.nextGaussian() * 0.15D, 1.0D, random.nextGaussian() * 0.15D);
            Vec3 spot = point.add(random.nextGaussian() * 2.0D, 0.0D, random.nextGaussian() * 2.0D);
            SuitMissileEntity.launch(player, SuitMissileEntity.Kind.ORDNANCE, shoulders(player).add(0.0D, 0.4D, 0.0D), up, damage, 0.8F)
                    .targetPoint(spot)
                    .splash(radius, 1.0F)
                    .straightFor(14 + i * 2)
                    .spawn();
        }
        playLaunch(player);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result rocketPunch(SuitContext context) {
        ServerPlayer player = context.player();
        Vec3 hand = player.getEyePosition().add(0.0D, -0.4D, 0.0D).add(right(player.getLookAngle()).scale(0.4D));
        SuitMissileEntity.launch(player, SuitMissileEntity.Kind.GAUNTLET, hand, player.getLookAngle(), context.damage(SuitAbilityConfig.ROCKET_PUNCH), 0.8F)
                .spawn();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result nuclearMissile(SuitContext context) {
        ServerPlayer player = context.player();
        LivingEntity target = SuitTargeting.crosshair(player, SuitAbilityConfig.MISSILE_LOCK_RANGE.get() * 2.0D);
        SuitMissileEntity.launch(player, SuitMissileEntity.Kind.NUKE, shoulders(player).add(0.0D, 0.5D, 0.0D),
                        player.getLookAngle().add(0.0D, 0.15D, 0.0D), 0.0F, 1.8F)
                .target(target)
                .straightFor(10)
                .spawn();
        SuitStates.get(player).irradiatedImmuneUntil = player.level().getGameTime() + SuitAbilityConfig.NUCLEAR_IMMUNITY_TICKS.get();
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ACSoundRegistry.NUCLEAR_SIREN.get(), SoundSource.PLAYERS, 4.0F, 1.0F);
        playLaunch(player);
        Component warning = Component.literal("§c§l☢ Nuclear missile launched");
        for (ServerPlayer nearby : player.serverLevel().players()) {
            if (nearby.distanceToSqr(player) < 160.0D * 160.0D) {
                nearby.displayClientMessage(warning, true);
            }
        }
        return SuitAbility.Result.FIRED;
    }

    static Vec3 aimPoint(ServerPlayer player, double range) {
        LivingEntity locked = SuitTargeting.crosshair(player, range);
        if (locked != null) {
            return locked.position();
        }
        Vec3 eye = player.getEyePosition();
        HitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private static Vec3 shoulders(ServerPlayer player) {
        return player.getEyePosition().add(0.0D, -0.35D, 0.0D);
    }

    private static Vec3 right(Vec3 look) {
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        return right.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
    }

    private static void playLaunch(ServerPlayer player) {
        SuitFx.burst(player.serverLevel(), SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, shoulders(player), 1, 0.0D, 0.0D);
        SuitFx.sound(player.level(), player.position(), SuitFx.ROCKET_FIRE, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
    }
}
