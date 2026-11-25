package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
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
        LivingEntity caster = context.caster();
        int min = SuitAbilityConfig.MICRO_MISSILES_MIN.get();
        int max = Math.max(min, SuitAbilityConfig.MICRO_MISSILES_MAX.get());
        int count = min + (int) Math.round((max - min) * context.tier());
        List<LivingEntity> targets = SuitTargeting.volley(caster, count);
        float damage = context.damage(SuitAbilityConfig.MICRO_MISSILES);

        Vec3 look = caster.getLookAngle();
        Vec3 right = right(look);
        Vec3 shoulders = shoulders(caster);
        for (int i = 0; i < count; i++) {
            double spread = count == 1 ? 0.0D : (i / (double) (count - 1)) - 0.5D;
            Vec3 direction = look.yRot((float) Math.toRadians(-spread * FAN_DEGREES)).add(0.0D, 0.25D, 0.0D);
            Vec3 from = shoulders.add(right.scale(spread * 0.9D));
            LivingEntity target = targets.isEmpty() ? null : targets.get(i % targets.size());
            SuitMissileEntity.launch(caster, SuitMissileEntity.Kind.MICRO, from, direction, damage, MICRO_MISSILE_SCALE)
                    .target(target)
                    .splash(SuitAbilityConfig.MISSILE_SPLASH_RADIUS.get(), 0.5F)
                    .straightFor(3 + caster.getRandom().nextInt(4))
                    .spawn();
        }
        SuitFx.burst(context.level(), SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, shoulders, 1, 0.0D, 0.0D);
        SuitFx.sound(caster.level(), caster.position(), SuitFx.GYROJET_FIRE, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result shoulderRocket(SuitContext context) {
        LivingEntity caster = context.caster();
        SuitMissileEntity.launch(caster, SuitMissileEntity.Kind.ROCKET, shoulders(caster).add(right(caster.getLookAngle()).scale(0.4D)),
                        caster.getLookAngle(), context.damage(SuitAbilityConfig.SHOULDER_ROCKET), 1.0F)
                .splash(3.0D, 1.0F)
                .spawn();
        playLaunch(caster);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result ordnanceDrop(SuitContext context) {
        LivingEntity caster = context.caster();
        Vec3 point = aimPoint(caster, SuitAbilityConfig.MISSILE_LOCK_RANGE.get());
        if (point == null) {
            HeroCaster.message(caster, Component.literal("§cNo target in range"));
            return SuitAbility.Result.FAILED;
        }
        RandomSource random = caster.getRandom();
        float damage = context.damage(SuitAbilityConfig.ORDNANCE_DROP);
        int count = SuitAbilityConfig.ORDNANCE_DROP_MISSILES.get();
        double radius = SuitAbilityConfig.ORDNANCE_DROP_RADIUS.get();
        for (int i = 0; i < count; i++) {
            Vec3 up = new Vec3(random.nextGaussian() * 0.15D, 1.0D, random.nextGaussian() * 0.15D);
            Vec3 spot = point.add(random.nextGaussian() * 2.0D, 0.0D, random.nextGaussian() * 2.0D);
            SuitMissileEntity.launch(caster, SuitMissileEntity.Kind.ORDNANCE, shoulders(caster).add(0.0D, 0.4D, 0.0D), up, damage, 0.8F)
                    .targetPoint(spot)
                    .splash(radius, 1.0F)
                    .straightFor(14 + i * 2)
                    .spawn();
        }
        playLaunch(caster);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result rocketPunch(SuitContext context) {
        LivingEntity caster = context.caster();
        Vec3 hand = caster.getEyePosition().add(0.0D, -0.4D, 0.0D).add(right(caster.getLookAngle()).scale(0.4D));
        SuitMissileEntity.launch(caster, SuitMissileEntity.Kind.GAUNTLET, hand, caster.getLookAngle(), context.damage(SuitAbilityConfig.ROCKET_PUNCH), 0.8F)
                .spawn();
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.0F, 1.4F);
        return SuitAbility.Result.FIRED;
    }

    static SuitAbility.Result nuclearMissile(SuitContext context) {
        LivingEntity caster = context.caster();
        LivingEntity target = SuitTargeting.crosshair(caster, SuitAbilityConfig.MISSILE_LOCK_RANGE.get() * 2.0D);
        SuitMissileEntity.launch(caster, SuitMissileEntity.Kind.NUKE, shoulders(caster).add(0.0D, 0.5D, 0.0D),
                        caster.getLookAngle().add(0.0D, 0.15D, 0.0D), 0.0F, 1.8F)
                .target(target)
                .straightFor(10)
                .spawn();
        SuitStates.get(caster).irradiatedImmuneUntil = caster.level().getGameTime() + SuitAbilityConfig.NUCLEAR_IMMUNITY_TICKS.get();
        caster.level().playSound(null, caster.getX(), caster.getY(), caster.getZ(), HeroFx.sound("alexscaves:nuclear_siren", SoundEvents.BELL_RESONATE), SoundSource.PLAYERS, 4.0F, 1.0F);
        playLaunch(caster);
        Component warning = Component.literal("§c§l☢ Nuclear missile launched");
        for (ServerPlayer nearby : context.level().players()) {
            if (nearby.distanceToSqr(caster) < 160.0D * 160.0D) {
                nearby.displayClientMessage(warning, true);
            }
        }
        return SuitAbility.Result.FIRED;
    }

    static Vec3 aimPoint(LivingEntity caster, double range) {
        LivingEntity locked = SuitTargeting.crosshair(caster, range);
        if (locked != null) {
            return locked.position();
        }
        Vec3 eye = caster.getEyePosition();
        HitResult hit = caster.level().clip(new ClipContext(eye, eye.add(caster.getLookAngle().scale(range)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        return hit.getType() == HitResult.Type.MISS ? null : hit.getLocation();
    }

    private static Vec3 shoulders(LivingEntity caster) {
        return caster.getEyePosition().add(0.0D, -0.35D, 0.0D);
    }

    private static Vec3 right(Vec3 look) {
        Vec3 right = look.cross(new Vec3(0.0D, 1.0D, 0.0D));
        return right.lengthSqr() < 1.0E-6D ? new Vec3(1.0D, 0.0D, 0.0D) : right.normalize();
    }

    private static void playLaunch(LivingEntity caster) {
        if (HeroFx.vfx(HeroCaster.level(caster), shoulders(caster), "ember_flare", 0, 0.6F)) {
            HeroFx.cue(caster, "thematic:ultron.deathray_shot", 1.0F, 1.0F);
        } else {
            SuitFx.burst(HeroCaster.level(caster), SuitFx.MUZZLE_FLASH, ParticleTypes.FLAME, shoulders(caster), 1, 0.0D, 0.0D);
            SuitFx.sound(caster.level(), caster.position(), SuitFx.ROCKET_FIRE, SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.0F);
        }
    }
}
