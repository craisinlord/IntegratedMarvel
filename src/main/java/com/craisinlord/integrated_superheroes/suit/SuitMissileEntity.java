package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.compat.AlexsCavesSuits;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.registry.SuitEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.ExplosionDamageCalculator;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class SuitMissileEntity extends Projectile {
    public enum Kind {
        MICRO(0.8D, 1.8D, 0.08D, -1.0D, 100),
        ROCKET(0.6D, 1.2D, 0.03D, 0.0D, 80),
        ORDNANCE(0.6D, 2.0D, 0.10D, 15.0D, 140),
        GAUNTLET(1.6D, 1.6D, 0.0D, 0.0D, 100),
        NUKE(0.6D, 1.2D, 0.02D, 3.0D, 300);

        final double startSpeed;
        final double maxSpeed;
        final double acceleration;

        final double turnDegrees;
        final int lifetime;

        Kind(double startSpeed, double maxSpeed, double acceleration, double turnDegrees, int lifetime) {
            this.startSpeed = startSpeed;
            this.maxSpeed = maxSpeed;
            this.acceleration = acceleration;
            this.turnDegrees = turnDegrees;
            this.lifetime = lifetime;
        }
    }

    private static final EntityDataAccessor<Float> SCALE = SynchedEntityData.defineId(SuitMissileEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Byte> KIND = SynchedEntityData.defineId(SuitMissileEntity.class, EntityDataSerializers.BYTE);
    private static final ResourceLocation ROCKET_TRAIL = ResourceLocation.fromNamespaceAndPath("scguns", "rocket_trail");
    private static final ResourceLocation ROCKET_EXPLOSION = ResourceLocation.fromNamespaceAndPath("scguns", "rocket_explosion");
    private static final double GAUNTLET_RANGE = 20.0D;

    private LivingEntity target;
    private Vec3 targetPoint;
    private float damage;
    private double splashRadius;
    private float splashFactor;
    private int straightTicks;
    private int life;
    private Vec3 launchPos;
    private boolean returning;
    private boolean reloaded;
    private final Set<UUID> pierced = new HashSet<>();

    public SuitMissileEntity(EntityType<? extends SuitMissileEntity> type, Level level) {
        super(type, level);
    }

    public static SuitMissileEntity launch(ServerPlayer owner, Kind kind, Vec3 from, Vec3 direction, float damage, float scale) {
        SuitMissileEntity missile = new SuitMissileEntity(SuitEntities.SUIT_MISSILE.get(), owner.level());
        missile.setOwner(owner);
        missile.setPos(from);
        missile.launchPos = from;
        missile.setDeltaMovement(direction.normalize().scale(kind.startSpeed));
        missile.faceMotion();
        missile.damage = damage;
        missile.entityData.set(KIND, (byte) kind.ordinal());
        missile.entityData.set(SCALE, scale);
        return missile;
    }

    public SuitMissileEntity target(LivingEntity target) {
        this.target = target;
        return this;
    }

    public SuitMissileEntity targetPoint(Vec3 point) {
        this.targetPoint = point;
        return this;
    }

    public SuitMissileEntity splash(double radius, float factor) {
        this.splashRadius = radius;
        this.splashFactor = factor;
        return this;
    }

    public SuitMissileEntity straightFor(int ticks) {
        this.straightTicks = ticks;
        return this;
    }

    public void spawn() {
        level().addFreshEntity(this);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(SCALE, 1.0F);
        builder.define(KIND, (byte) 0);
    }

    public float scale() {
        return entityData.get(SCALE);
    }

    public Kind kind() {
        Kind[] kinds = Kind.values();
        int index = entityData.get(KIND);
        return index >= 0 && index < kinds.length ? kinds[index] : Kind.MICRO;
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        if (!level().isClientSide) {
            if (reloaded) {
                discard();
                return;
            }
            if (++life > kind().lifetime) {
                if (kind() == Kind.GAUNTLET) {
                    discard();
                } else {
                    detonate(null);
                }
                return;
            }
            motion = kind() == Kind.GAUNTLET ? steerGauntlet(motion) : steer(motion);
            if (isRemoved()) {
                return;
            }
            setDeltaMovement(motion);
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(this, this::canHitEntity);
            if (hit.getType() != HitResult.Type.MISS) {
                onHit(hit);
                if (isRemoved()) {
                    return;
                }
            }
        } else {
            trail(motion);
        }
        setPos(position().add(motion));
        faceMotion();
    }

    private Vec3 steer(Vec3 motion) {
        Kind kind = kind();
        double speed = Math.min(kind.maxSpeed, motion.length() + kind.acceleration);
        Vec3 heading = heading(motion);
        if (straightTicks > 0) {
            straightTicks--;
            return heading.scale(speed);
        }
        Vec3 aim = null;
        if (targetPoint != null) {
            aim = targetPoint;
        } else {
            if ((target == null || !target.isAlive() || target.isRemoved()) && kind != Kind.ROCKET && getOwner() instanceof ServerPlayer owner) {
                target = SuitTargeting.nearest(owner, position(), 16.0D);
            }
            if (target != null) {
                aim = target.getBoundingBox().getCenter();
            }
        }
        double turn = kind.turnDegrees < 0.0D ? SuitAbilityConfig.MISSILE_TURN_DEGREES.get() : kind.turnDegrees;
        return turnToward(heading, aim, turn).scale(speed);
    }

    private Vec3 steerGauntlet(Vec3 motion) {
        Entity owner = getOwner();
        if (owner == null || !owner.isAlive() || owner.level() != level()) {
            discard();
            return motion;
        }
        if (!returning && launchPos != null && position().distanceToSqr(launchPos) > GAUNTLET_RANGE * GAUNTLET_RANGE) {
            startReturning();
        }
        if (!returning) {
            return motion;
        }
        Vec3 home = owner.getBoundingBox().getCenter();
        if (position().distanceToSqr(home) < 2.25D) {
            discard();
            return motion;
        }
        return turnToward(heading(motion), home, 30.0D).scale(Kind.GAUNTLET.maxSpeed);
    }

    private void startReturning() {
        returning = true;
        pierced.clear();
    }

    private Vec3 heading(Vec3 motion) {
        return motion.lengthSqr() > 1.0E-6D ? motion.normalize() : Vec3.directionFromRotation(getXRot(), getYRot());
    }

    private Vec3 turnToward(Vec3 heading, Vec3 aim, double maxDegrees) {
        if (aim == null || maxDegrees <= 0.0D) {
            return heading;
        }
        Vec3 wanted = aim.subtract(position());
        if (wanted.lengthSqr() < 1.0E-6D) {
            return heading;
        }
        wanted = wanted.normalize();
        double angle = Math.acos(Mth.clamp(heading.dot(wanted), -1.0D, 1.0D));
        if (angle < 1.0E-4D) {
            return heading;
        }
        double t = Math.min(1.0D, Math.toRadians(maxDegrees) / angle);
        return heading.lerp(wanted, t).normalize();
    }

    private void faceMotion() {
        Vec3 motion = getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        setXRot((float) (Mth.atan2(motion.y, motion.horizontalDistance()) * Mth.RAD_TO_DEG));
    }

    private void trail(Vec3 motion) {
        Vec3 back = motion.lengthSqr() > 1.0E-6D ? motion.normalize().scale(-0.4D * scale()) : Vec3.ZERO;
        double x = getX() + back.x;
        double y = getY() + back.y;
        double z = getZ() + back.z;
        if (kind() == Kind.GAUNTLET) {
            if (random.nextInt(2) == 0) {
                level().addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        level().addParticle(particle(ROCKET_TRAIL, ParticleTypes.SMOKE), x, y, z, 0.0D, 0.0D, 0.0D);
        if (random.nextInt(3) == 0 || kind() == Kind.NUKE) {
            level().addParticle(ParticleTypes.FLAME, x, y, z, 0.0D, 0.0D, 0.0D);
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return super.canHitEntity(entity) && getOwner() instanceof ServerPlayer owner && SuitTargeting.canHit(owner, entity)
                && !pierced.contains(entity.getUUID());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (kind() == Kind.GAUNTLET) {
            if (getOwner() instanceof ServerPlayer owner && result.getEntity() instanceof LivingEntity living && damage > 0.0F) {
                pierced.add(living.getUUID());
                hurt(owner, living, damage);
                living.knockback(0.6D, -getDeltaMovement().x, -getDeltaMovement().z);
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.4F, 1.6F);
            }
            return;
        }
        detonate(result.getEntity());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        if (kind() == Kind.GAUNTLET) {
            if (!returning) {
                startReturning();
            } else {
                discard();
            }
            return;
        }
        detonate(null);
    }

    @Override
    public boolean isPickable() {
        return kind() == Kind.NUKE;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (kind() == Kind.NUKE && !level().isClientSide && !isRemoved()) {
            detonate(null);
            return true;
        }
        return false;
    }

    private void detonate(Entity direct) {
        if (!(level() instanceof ServerLevel level) || isRemoved()) {
            return;
        }
        if (kind() == Kind.NUKE) {
            nuclearDetonation(level);
            discard();
            return;
        }
        if (damage > 0.0F && getOwner() instanceof ServerPlayer owner) {
            if (direct instanceof LivingEntity living) {
                hurt(owner, living, damage);
            }
            if (splashRadius > 0.0D) {
                for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(splashRadius),
                        entity -> entity != direct && SuitTargeting.canHit(owner, entity) && entity.distanceToSqr(this) <= splashRadius * splashRadius)) {
                    hurt(owner, nearby, damage * splashFactor);
                }
            }
        }
        if (kind() == Kind.ROCKET) {
            boolean griefing = level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
            level.explode(this, null, NO_ENTITY_DAMAGE, getX(), getY(), getZ(), 2.0F, false,
                    griefing ? Level.ExplosionInteraction.TNT : Level.ExplosionInteraction.NONE);
            level.sendParticles(particle(ROCKET_EXPLOSION, ParticleTypes.EXPLOSION), getX(), getY(), getZ(), 3, 0.6D, 0.6D, 0.6D, 0.0D);
        } else if (kind() == Kind.ORDNANCE) {
            level.sendParticles(particle(ROCKET_EXPLOSION, ParticleTypes.EXPLOSION), getX(), getY(), getZ(), 3,
                    splashRadius * 0.3D, 0.4D, splashRadius * 0.3D, 0.0D);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 1.2F, 0.9F + random.nextFloat() * 0.2F);
        } else {
            level.sendParticles(particle(ROCKET_EXPLOSION, ParticleTypes.EXPLOSION), getX(), getY(), getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
            level.playSound(null, getX(), getY(), getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS,
                    0.6F * Math.max(0.5F, scale()), 1.3F + random.nextFloat() * 0.3F);
        }
        discard();
    }

    private void nuclearDetonation(ServerLevel level) {
        if (!ModCompat.ALEXS_CAVES || !AlexsCavesSuits.nuke(level, this)) {
            level.explode(this, getX(), getY(), getZ(), 16.0F, true, Level.ExplosionInteraction.TNT);
        }
    }

    private static final ExplosionDamageCalculator NO_ENTITY_DAMAGE = new ExplosionDamageCalculator() {
        @Override
        public boolean shouldDamageEntity(Explosion explosion, Entity entity) {
            return false;
        }

        @Override
        public float getKnockbackMultiplier(Entity entity) {
            return 0.0F;
        }
    };

    private static void hurt(ServerPlayer owner, LivingEntity target, float amount) {
        target.invulnerableTime = 0;
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(owner.damageSources().playerAttack(owner), amount));
    }

    private static ParticleOptions particle(ResourceLocation id, SimpleParticleType fallback) {
        return BuiltInRegistries.PARTICLE_TYPE.get(id) instanceof SimpleParticleType type ? type : fallback;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        reloaded = true;
    }
}
