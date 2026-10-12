package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
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

import java.util.Comparator;
import java.util.List;

public final class SuitTargeting {
    public static final TagKey<EntityType<?>> IGNORED =
            TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "suit_missile_ignored"));

    private SuitTargeting() {
    }

    public static boolean canHit(ServerPlayer shooter, Entity entity) {
        if (!(entity instanceof LivingEntity living) || entity == shooter || !living.isAlive() || entity.isSpectator()
                || entity instanceof ArmorStand || entity.getType().is(IGNORED) || shooter.isAlliedTo(entity)) {
            return false;
        }
        if (entity instanceof OwnableEntity owned && shooter.getUUID().equals(owned.getOwnerUUID())) {
            return false;
        }
        if (entity instanceof Player player) {
            return SuitAbilityConfig.MISSILES_TARGET_PLAYERS.get() && shooter.server.isPvpAllowed()
                    && !player.isCreative() && shooter.canHarmPlayer(player);
        }
        return true;
    }

    public static boolean shouldSeek(ServerPlayer shooter, Entity entity) {
        return canHit(shooter, entity) && (entity instanceof Enemy || entity instanceof Player
                || (entity instanceof Mob mob && mob.getTarget() == shooter));
    }

    public static LivingEntity crosshair(ServerPlayer shooter, double range) {
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

    public static List<LivingEntity> volley(ServerPlayer shooter, int count) {
        LivingEntity locked = crosshair(shooter, SuitAbilityConfig.MISSILE_LOCK_RANGE.get());
        if (locked != null) {
            return List.of(locked);
        }
        return HeroAbilities.cone(shooter, SuitAbilityConfig.MISSILE_CONE_RANGE.get(), SuitAbilityConfig.MISSILE_CONE_DEGREES.get())
                .stream()
                .filter(entity -> shouldSeek(shooter, entity))
                .sorted(Comparator.comparingDouble(entity -> entity.distanceToSqr(shooter)))
                .limit(count)
                .toList();
    }

    public static LivingEntity nearest(ServerPlayer shooter, Vec3 from, double radius) {
        return shooter.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(radius),
                        entity -> shouldSeek(shooter, entity) && entity.distanceToSqr(from) <= radius * radius)
                .stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)))
                .orElse(null);
    }
}
