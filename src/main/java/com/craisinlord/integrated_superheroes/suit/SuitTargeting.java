package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class SuitTargeting {
    public static final TagKey<EntityType<?>> IGNORED =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "suit_missile_ignored"));
    private static final double MOB_VOLLEY_SPREAD = 8.0D;

    private SuitTargeting() {
    }

    public static DamageSource attack(LivingEntity attacker) {
        return attacker instanceof Player player ? attacker.damageSources().playerAttack(player) : attacker.damageSources().mobAttack(attacker);
    }

    public static void hit(LivingEntity attacker, LivingEntity target, float amount) {
        target.invulnerableTime = 0;
        HeroAbilities.runAsSuitDamage(() -> target.hurt(attack(attacker), amount));
    }

    public static boolean canHit(LivingEntity shooter, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == shooter || !living.isAlive() || entity.isSpectator()
                || entity instanceof ArmorStand || entity.getType().is(IGNORED) || shooter.isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof OwnableEntity owned && shooter.getUUID().equals(owned.getOwnerUUID())) {
            return false;
        }
        PymColony.Member member = PymColony.member(entity);
        if (member != null && member.owner.equals(shooter.getUUID())) {
            return false;
        }
        if (!(shooter instanceof ServerPlayer player)) {
            return HeroCaster.foe(shooter, entity);
        }
        if (entity instanceof Player target) {
            return SuitAbilityConfig.MISSILES_TARGET_PLAYERS.get() && player.server.isPvpAllowed()
                    && !target.isCreative() && player.canHarmPlayer(target);
        }
        return true;
    }

    public static boolean shouldSeek(LivingEntity shooter, Entity entity) {
        if (!canHit(shooter, entity)) {
            return false;
        }
        if (entity instanceof Mob mob && mob.getTarget() == shooter || entity instanceof Player) {
            return true;
        }
        if (shooter instanceof ServerPlayer) {
            return entity instanceof Enemy;
        }
        return entity == HeroCaster.target(shooter) || entity instanceof OwnableEntity owned && owned.getOwner() instanceof Player;
    }

    public static LivingEntity crosshair(LivingEntity shooter, double range) {
        if (!(shooter instanceof ServerPlayer)) {
            LivingEntity target = HeroCaster.target(shooter);
            if (target != null && canHit(shooter, target) && target.distanceToSqr(shooter) <= range * range && shooter.hasLineOfSight(target)) {
                return target;
            }
        }
        Vec3 eye = shooter.getEyePosition();
        Vec3 end = eye.add(shooter.getLookAngle().scale(range));
        HitResult block = shooter.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, shooter));
        if (block.getType() != HitResult.Type.MISS) {
            end = block.getLocation();
        }
        AABB area = shooter.getBoundingBox().expandTowards(end.subtract(eye)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(shooter, eye, end, area, entity -> canHit(shooter, entity), eye.distanceToSqr(end));
        return hit != null && hit.getEntity() instanceof LivingEntity living ? living : null;
    }

    public static List<LivingEntity> volley(LivingEntity shooter, int count) {
        LivingEntity locked = crosshair(shooter, SuitAbilityConfig.MISSILE_LOCK_RANGE.get());
        if (!(shooter instanceof ServerPlayer)) {
            List<LivingEntity> targets = new ArrayList<>();
            if (locked != null) {
                targets.add(locked);
                shooter.level().getEntitiesOfClass(LivingEntity.class, locked.getBoundingBox().inflate(MOB_VOLLEY_SPREAD),
                                entity -> entity != locked && shouldSeek(shooter, entity))
                        .stream()
                        .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(locked)))
                        .limit(Math.max(0, count - 1))
                        .forEach(targets::add);
                return targets;
            }
        } else if (locked != null) {
            return List.of(locked);
        }
        return HeroAbilities.cone(shooter, SuitAbilityConfig.MISSILE_CONE_RANGE.get(), SuitAbilityConfig.MISSILE_CONE_DEGREES.get())
                .stream()
                .filter(entity -> shouldSeek(shooter, entity))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(shooter)))
                .limit(count)
                .toList();
    }

    public static LivingEntity nearest(LivingEntity shooter, Vec3 from, double radius) {
        return shooter.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(radius),
                        entity -> shouldSeek(shooter, entity) && entity.distanceToSqr(from) <= radius * radius)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)))
                .orElse(null);
    }
}
