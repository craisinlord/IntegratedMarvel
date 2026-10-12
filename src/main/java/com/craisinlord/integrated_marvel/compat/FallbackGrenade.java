package com.craisinlord.integrated_marvel.compat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.world.level.MarvelGameRules;

public class FallbackGrenade extends PrimedTnt {
    private static final ExplosionDamageCalculator NO_ENTITY_DAMAGE = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }
    };

    private float damage;
    private float radius = 3.0F;
    private boolean fire;
    private boolean flash;

    private FallbackGrenade(Level level, LivingEntity owner, int fuse) {
        super(level, owner.getX(), owner.getEyeY() - 0.1D, owner.getZ(), owner);
        setFuse(fuse);
        setDeltaMovement(Vec3.ZERO);
    }

    public static FallbackGrenade create(Level level, LivingEntity owner, int fuse) {
        return new FallbackGrenade(level, owner, fuse);
    }

    public FallbackGrenade explosive(float damage, float radius) {
        this.damage = damage;
        this.radius = radius;
        return this;
    }

    public FallbackGrenade incendiary() {
        this.fire = true;
        return this;
    }

    public FallbackGrenade flashbang(float radius) {
        this.flash = true;
        this.radius = radius;
        return this;
    }

    public FallbackGrenade throwFrom(LivingEntity thrower, float xRot, float yRot, float velocity) {
        Vec3 motion = Vec3.directionFromRotation(xRot, yRot).scale(velocity);
        Vec3 carried = thrower.getDeltaMovement();
        setDeltaMovement(motion.add(carried.x, thrower.onGround() ? 0.0D : carried.y, carried.z));
        return this;
    }

    @Override
    public void tick() {
        if (!level().isClientSide && getFuse() <= 1) {
            discard();
            detonate((ServerLevel) level());
            return;
        }
        super.tick();
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    private void detonate(ServerLevel level) {
        if (flash) {
            flash(level);
            return;
        }
        boolean griefing = level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
        level.explode(this, null, NO_ENTITY_DAMAGE, getX(), getY(0.0625D), getZ(), radius, fire,
                griefing ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
        if (damage <= 0.0F) {
            return;
        }
        LivingEntity owner = getOwner();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), this::affects)) {
            double distance = Math.sqrt(target.distanceToSqr(this));
            if (distance > radius) {
                continue;
            }
            float scaled = damage * (float) Math.max(0.25D, 1.0D - distance / radius);
            target.invulnerableTime = 0;
            target.hurt(level.damageSources().explosion(this, owner), scaled);
            if (fire) {
                target.igniteForSeconds(5.0F);
            }
        }
    }

    private void flash(ServerLevel level) {
        level.sendParticles(ParticleTypes.FLASH, getX(), getY() + 0.3D, getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.FIREWORK, getX(), getY() + 0.3D, getZ(), 40, 0.4D, 0.4D, 0.4D, 0.25D);
        level.playSound(null, getX(), getY(), getZ(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 2.0F, 1.4F);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(radius), this::affects)) {
            if (target.distanceToSqr(this) > radius * radius || !target.hasLineOfSight(this)) {
                continue;
            }
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0), this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 2), this);
            if (target instanceof Mob mob) {
                mob.setTarget(null);
            }
        }
    }

    private boolean affects(LivingEntity target) {
        LivingEntity owner = getOwner();
        return target.isAlive() && (owner == null || target != owner && !owner.isAlliedTo(target));
    }
}
