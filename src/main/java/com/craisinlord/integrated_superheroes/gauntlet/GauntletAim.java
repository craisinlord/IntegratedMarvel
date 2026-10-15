package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class GauntletAim {
    private GauntletAim() {
    }

    public static BlockHitResult block(ServerPlayer player, double range, ClipContext.Block mode) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        return player.serverLevel().clip(new ClipContext(start, end, mode, ClipContext.Fluid.NONE, player));
    }

    public static BlockHitResult block(ServerPlayer player, double range) {
        return block(player, range, ClipContext.Block.COLLIDER);
    }

    public static LivingEntity living(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        BlockHitResult blockHit = block(player, range);
        double maxDistance = blockHit.getType() == HitResult.Type.MISS ? range : start.distanceTo(blockHit.getLocation());
        AABB search = player.getBoundingBox().expandTowards(end.subtract(start)).inflate(1.0D);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(player, start, end, search,
                entity -> entity instanceof LivingEntity && entity.isPickable() && entity != player,
                maxDistance * maxDistance);
        return hit != null && hit.getEntity() instanceof LivingEntity living
                && start.distanceToSqr(hit.getLocation()) <= maxDistance * maxDistance ? living : null;
    }

    public static Vec3 point(ServerPlayer player, double range) {
        LivingEntity living = living(player, range);
        if (living != null) return living.position();
        BlockHitResult hit = block(player, range);
        if (hit.getType() == HitResult.Type.BLOCK) return hit.getLocation();
        return player.getEyePosition().add(player.getLookAngle().normalize().scale(range * 0.5D));
    }

    public static List<LivingEntity> cone(ServerPlayer player, double range, double degrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle().normalize();
        double cos = Math.cos(Math.toRadians(degrees));
        return player.serverLevel().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range),
                entity -> {
                    if (entity == player || !entity.isAlive()) return false;
                    Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
                    double length = to.length();
                    return length <= range && length > 0.0D && to.scale(1.0D / length).dot(look) >= cos
                            && player.hasLineOfSight(entity);
                });
    }

    public static boolean isProtectedEntity(Entity entity) {
        return entity instanceof ServerPlayer player && (player.isCreative() || player.isSpectator());
    }
}
