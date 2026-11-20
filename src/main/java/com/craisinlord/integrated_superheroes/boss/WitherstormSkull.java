package com.craisinlord.integrated_superheroes.boss;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;

public class WitherstormSkull extends WitherSkull {
    private final float damage;

    public WitherstormSkull(Level level, LivingEntity owner, Vec3 movement, float damage, boolean blue) {
        super(level, owner, movement);
        this.damage = damage;
        this.setDangerous(blue);
    }

    public WitherstormSkull(EntityType<? extends WitherSkull> type, Level level) {
        super(type, level);
        this.damage = 8.0F;
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        Entity owner = this.getOwner();
        if (owner != null) {
            if (target == owner) return false;
            if (target instanceof PartEntity<?> part && part.getParent() == owner) return false;
            if (target instanceof WitherstormSkull) return false;
        }
        return super.canHitEntity(target);
    }

    @Override
    protected void onHit(HitResult result) {
        if (result.getType() == HitResult.Type.ENTITY) {
            this.onHitEntity((EntityHitResult) result);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            this.onHitBlock((BlockHitResult) result);
        }
        if (!this.level().isClientSide) {
            this.level().explode(this, this.getX(), this.getY(), this.getZ(), this.isDangerous() ? 3.0F : 1.0F, false, Level.ExplosionInteraction.MOB);
            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) return;
        Entity target = result.getEntity();
        Entity owner = this.getOwner();
        if (!(owner instanceof LivingEntity living)) return;
        if (target.hurt(this.damageSources().witherSkull(this, living), this.damage) && target instanceof LivingEntity hit) {
            hit.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1), living);
        }
    }
}
