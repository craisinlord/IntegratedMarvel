package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.registry.HeroEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class KineticCharge extends ThrowableItemProjectile {
    private static final EntityDataAccessor<Integer> KINDS = SynchedEntityData.defineId(KineticCharge.class, EntityDataSerializers.INT);
    private float damage;
    private float radius;
    private float impactDamage;
    private boolean dropBack;
    private UUID homingTarget;
    private int maxLife = 100;
    private boolean detonated;

    public KineticCharge(EntityType<? extends KineticCharge> type, Level level) {
        super(type, level);
    }

    private KineticCharge(Level level, LivingEntity owner) {
        super(HeroEntities.KINETIC_CHARGE.get(), owner, level);
    }

    public static KineticCharge create(ServerPlayer owner, ItemStack stack, GambitItems.Profile profile, float impactDamage, boolean dropBack) {
        KineticCharge charge = new KineticCharge(owner.serverLevel(), owner);
        charge.setItem(stack.isEmpty() ? new ItemStack(Items.PAPER) : stack);
        charge.entityData.set(KINDS, profile.kinds());
        charge.damage = profile.damage();
        charge.radius = profile.radius();
        charge.impactDamage = impactDamage;
        charge.dropBack = dropBack;
        return charge;
    }

    public KineticCharge homing(LivingEntity target) {
        this.homingTarget = target.getUUID();
        this.maxLife = 160;
        return this;
    }

    public int kinds() {
        return this.entityData.get(KINDS);
    }

    public boolean isCard() {
        return (kinds() & GambitItems.CARD) != 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KINDS, 0);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.PAPER;
    }

    @Override
    protected double getDefaultGravity() {
        return this.homingTarget != null ? 0.0D : GambitItems.gravity(kinds());
    }

    @Override
    public boolean isCurrentlyGlowing() {
        return this.level().isClientSide() || super.isCurrentlyGlowing();
    }

    @Override
    public int getTeamColor() {
        return KineticBurst.PURPLE;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            Vec3 at = this.position().add(0.0D, this.getBbHeight() * 0.5D, 0.0D);
            this.level().addParticle(HeroAbilities.emissive(this.random.nextInt(3) == 0 ? KineticBurst.PINK : KineticBurst.PURPLE, 0.7F),
                    at.x, at.y, at.z, 0.0D, 0.0D, 0.0D);
            if (this.tickCount % 2 == 0) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z,
                        (this.random.nextDouble() - 0.5D) * 0.05D, (this.random.nextDouble() - 0.5D) * 0.05D, (this.random.nextDouble() - 0.5D) * 0.05D);
            }
            if (this.tickCount % 3 == 0) {
                this.level().addParticle(ParticleTypes.ELECTRIC_SPARK, at.x, at.y, at.z, 0.0D, 0.0D, 0.0D);
            }
            return;
        }
        if (this.homingTarget != null && this.level() instanceof ServerLevel level
                && level.getEntity(this.homingTarget) instanceof LivingEntity target && target.isAlive()) {
            Vec3 velocity = this.getDeltaMovement();
            double speed = Math.max(1.2D, velocity.length());
            Vec3 desired = target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(speed);
            this.setDeltaMovement(velocity.add(desired.subtract(velocity).scale(0.35D)).normalize().scale(speed));
            this.hurtMarked = true;
        }
        if (this.tickCount > this.maxLife) {
            detonate(this.position());
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (this.level().isClientSide() || this.detonated) {
            return;
        }
        Entity hit = result.getEntity();
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer player ? player : null;
        if (hit instanceof LivingEntity living && KineticBurst.affects(owner, living) && this.impactDamage > 0.0F) {
            HeroAbilities.runAsAbilityDamage(() -> living.hurt(this.damageSources().thrown(this, owner), this.impactDamage));
        }
        detonate(hit.getBoundingBox().getCenter());
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        if (this.level().isClientSide() || this.detonated) {
            return;
        }
        Vec3 back = this.getDeltaMovement().lengthSqr() > 1.0E-4D ? this.getDeltaMovement().normalize().scale(-0.25D) : Vec3.ZERO;
        detonate(result.getLocation().add(back));
    }

    private void detonate(Vec3 at) {
        if (this.detonated || !(this.level() instanceof ServerLevel level)) {
            return;
        }
        this.detonated = true;
        ServerPlayer owner = this.getOwner() instanceof ServerPlayer player ? player : null;
        ItemStack stack = this.getItem().copy();
        KineticBurst.burst(level, at, this, owner, this.damage, this.radius, kinds(), stack);
        if (this.dropBack && !stack.isEmpty()) {
            int cost = MarvelBalanceConfig.RETURN_DURABILITY_COST.get();
            if (cost > 0 && stack.isDamageableItem()) {
                stack.hurtAndBreak(cost, level, owner, item -> {
                });
            }
            if (!stack.isEmpty()) {
                ItemEntity drop = new ItemEntity(level, at.x, at.y, at.z, stack);
                drop.setDeltaMovement(0.0D, 0.15D, 0.0D);
                drop.setDefaultPickUpDelay();
                drop.setGlowingTag(false);
                level.addFreshEntity(drop);
                level.sendParticles(HeroAbilities.emissive(KineticBurst.PURPLE, 0.8F), at.x, at.y, at.z, 10, 0.2D, 0.2D, 0.2D, 0.0D);
            }
        }
        this.discard();
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Kinds", kinds());
        tag.putFloat("BurstDamage", this.damage);
        tag.putFloat("BurstRadius", this.radius);
        tag.putFloat("ImpactDamage", this.impactDamage);
        tag.putBoolean("DropBack", this.dropBack);
        tag.putInt("MaxLife", this.maxLife);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(KINDS, tag.getInt("Kinds"));
        this.damage = tag.getFloat("BurstDamage");
        this.radius = tag.getFloat("BurstRadius");
        this.impactDamage = tag.getFloat("ImpactDamage");
        this.dropBack = tag.getBoolean("DropBack");
        this.maxLife = tag.contains("MaxLife") ? tag.getInt("MaxLife") : 100;
    }
}
