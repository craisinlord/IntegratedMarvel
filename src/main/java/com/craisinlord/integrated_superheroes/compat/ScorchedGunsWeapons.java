package com.craisinlord.integrated_superheroes.compat;

import net.minecraft.server.level.ServerLevel;
import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import top.ribs.scguns.entity.projectile.ProjectileEntity;
import top.ribs.scguns.entity.projectile.TraumaHookEntity;
import top.ribs.scguns.entity.throwable.ThrowableGrenadeEntity;
import top.ribs.scguns.entity.throwable.ThrowableHellfireBombEntity;
import top.ribs.scguns.entity.throwable.ThrowableStunGrenadeEntity;
import top.ribs.scguns.init.ModEntities;
import top.ribs.scguns.item.GunItem;

public final class ScorchedGunsWeapons {
    private ScorchedGunsWeapons() {
    }

    public static boolean isGun(ItemStack stack) {
        return stack.getItem() instanceof GunItem;
    }

    public static boolean isBullet(Entity entity) {
        return entity instanceof ProjectileEntity;
    }

    public static boolean isThrowable(Entity entity) {
        return entity instanceof top.ribs.scguns.entity.throwable.ThrowableItemEntity;
    }

    public static LivingEntity bulletShooter(Entity entity) {
        return entity instanceof ProjectileEntity bullet ? bullet.getShooter() : null;
    }

    public static Projectile grenade(ServerLevel level, LivingEntity owner, int fuse, float damage, float radius) {
        ThrowableGrenadeEntity grenade = HeroCaster.griefs(owner) ? new ThrowableGrenadeEntity(level, owner, fuse) : new SafeGrenade(level, owner, fuse, damage, radius);
        grenade.setExplosionDamage(damage);
        grenade.setExplosionRadius(radius);
        return grenade;
    }

    public static Projectile hellfireBomb(ServerLevel level, LivingEntity owner, int fuse) {
        return HeroCaster.griefs(owner) ? new ThrowableHellfireBombEntity(level, owner, fuse) : new SafeHellfireBomb(level, owner, fuse);
    }

    public static Projectile flashbang(ServerLevel level, LivingEntity owner, int fuse) {
        return new ThrowableStunGrenadeEntity(level, owner, fuse);
    }

    public static void traumaHook(LivingEntity player, Vec3 hand, Vec3 target) {
        TraumaHookEntity hook = new TraumaHookEntity(ModEntities.TRAUMA_HOOK.get(), player, player.level());
        hook.moveTo(hand.x, hand.y, hand.z, player.getYRot(), player.getXRot());
        hook.setDeltaMovement(target.subtract(hand).normalize().scale(2.5D));
        player.level().addFreshEntity(hook);
    }

    private static final class SafeGrenade extends ThrowableGrenadeEntity {
        private final float blastDamage;
        private final float blastRadius;

        private SafeGrenade(ServerLevel level, LivingEntity owner, int fuse, float damage, float radius) {
            super(level, owner, fuse);
            this.blastDamage = damage;
            this.blastRadius = radius;
        }

        @Override
        public void onDeath() {
            ProjectileEntity.createRocketExplosion(this, blastRadius, blastDamage, true);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }
    }

    private static final class SafeHellfireBomb extends ThrowableHellfireBombEntity {
        private static final float RADIUS = 4.0F;

        private SafeHellfireBomb(ServerLevel level, LivingEntity owner, int fuse) {
            super(level, owner, fuse);
        }

        @Override
        public void onDeath() {
            if (!(level() instanceof ServerLevel level)) {
                return;
            }
            double y = getY() + getType().getDimensions().height() * 0.5D;
            level.playSound(null, getX(), y, getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 2.0F, 1.0F);
            level.playSound(null, getX(), y, getZ(), SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 2.0F, 1.0F);
            level.explode(this, damageSources().explosion(this, getOwner()), null, getX(), getY(), getZ(), RADIUS * 0.5F, false,
                    Level.ExplosionInteraction.NONE);
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(RADIUS))) {
                double distance = living.distanceTo(this);
                if (distance <= RADIUS && living != getOwner()) {
                    living.igniteForSeconds(8.0F);
                    living.hurt(level.damageSources().onFire(), (float) (4.0D * (1.0D - distance / RADIUS)));
                }
            }
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, getX(), getY() + 0.25D, getZ(), 48, 2.0D, 0.4D, 2.0D, 0.05D);
            level.sendParticles(ParticleTypes.SOUL, getX(), getY() + 0.25D, getZ(), 16, 0.8D, 0.4D, 0.8D, 0.04D);
        }

        @Override
        public boolean shouldBeSaved() {
            return false;
        }
    }
}
