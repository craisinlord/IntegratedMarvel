package com.craisinlord.integrated_marvel.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.tintankgames.marvel.core.particles.EmissiveDustParticleOptions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class HeroAbilities {
    private static final Map<UUID, Dash> DASHES = new HashMap<>();
    private static final ThreadLocal<Boolean> ABILITY_DAMAGE = ThreadLocal.withInitial(() -> false);

    private HeroAbilities() {
    }

    public static void runAsAbilityDamage(Runnable action) {
        boolean previous = ABILITY_DAMAGE.get();
        ABILITY_DAMAGE.set(true);
        try {
            action.run();
        } finally {
            ABILITY_DAMAGE.set(previous);
        }
    }

    public static boolean isAbilityDamage() {
        return ABILITY_DAMAGE.get();
    }

    public static void startDash(ServerPlayer player, Vec3 velocity, int ticks, double damage, double knockback) {
        startDash(player, velocity, ticks, damage, knockback, entity -> true);
    }

    public static void startDash(ServerPlayer player, Vec3 velocity, int ticks, double damage, double knockback, Predicate<LivingEntity> canHit) {
        player.setDeltaMovement(velocity);
        player.hurtMarked = true;
        DASHES.put(player.getUUID(), new Dash(ticks, damage, knockback, new HashSet<>(), canHit));
    }

    public static void tickDash(ServerPlayer player) {
        Dash dash = DASHES.get(player.getUUID());
        if (dash == null) {
            return;
        }
        Vec3 motion = player.getDeltaMovement();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(0.8D),
                entity -> entity != player && entity.isAlive() && !dash.hit().contains(entity.getUUID()) && dash.canHit().test(entity))) {
            dash.hit().add(target.getUUID());
            target.hurt(player.damageSources().mobAttack(player), (float) dash.damage());
            Vec3 push = motion.lengthSqr() > 1.0E-4D ? motion.normalize() : player.getLookAngle();
            target.knockback(dash.knockback(), -push.x, -push.z);
        }
        if (dash.ticksLeft() <= 1) {
            DASHES.remove(player.getUUID());
        } else {
            DASHES.put(player.getUUID(), new Dash(dash.ticksLeft() - 1, dash.damage(), dash.knockback(), dash.hit(), dash.canHit()));
        }
    }

    public static void clearDash(ServerPlayer player) {
        DASHES.remove(player.getUUID());
    }

    public static boolean canControl(LivingEntity entity) {
        if (entity.getType().is(net.neoforged.neoforge.common.Tags.EntityTypes.BOSSES)) {
            return false;
        }
        return !(entity instanceof net.minecraft.world.entity.player.Player player) || !player.isCreative() && !player.isSpectator();
    }

    public static List<LivingEntity> cone(ServerPlayer player, double range, double halfAngleDegrees) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double minDot = Math.cos(Math.toRadians(halfAngleDegrees));
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(range), entity -> {
            if (entity == player || !entity.isAlive()) {
                return false;
            }
            Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
            double distance = to.length();
            return distance <= range && (distance < 1.0E-3D || to.normalize().dot(look) >= minDot) && player.hasLineOfSight(entity);
        });
    }

    public static List<LivingEntity> sphere(ServerPlayer player, double radius) {
        return player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && entity.distanceToSqr(player) <= radius * radius);
    }

    public static Beam beam(ServerPlayer player, Vec3 direction, double range, int maxTargets) {
        Vec3 start = player.getEyePosition();
        Vec3 end = start.add(direction.scale(range));
        HitResult blockHit = player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() != HitResult.Type.MISS) {
            end = blockHit.getLocation();
        }
        Vec3 finalEnd = end;
        AABB area = new AABB(start, end).inflate(1.0D);
        List<Entity> candidates = new ArrayList<>(player.level().getEntities(player, area,
                entity -> entity instanceof LivingEntity && entity.isAlive()
                        && entity.getBoundingBox().inflate(0.3D).clip(start, finalEnd).isPresent()));
        candidates.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(start)));
        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : candidates) {
            if (targets.size() >= maxTargets) {
                break;
            }
            targets.add((LivingEntity) entity);
        }
        Vec3 beamEnd = !targets.isEmpty() && maxTargets == 1 ? targets.get(0).getBoundingBox().getCenter() : finalEnd;
        return new Beam(start, beamEnd, targets, blockHit.getType() != HitResult.Type.MISS);
    }

    public static void particleLine(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
        double length = from.distanceTo(to);
        Vec3 step = to.subtract(from).normalize();
        for (double d = 0.0D; d < length; d += spacing) {
            Vec3 point = from.add(step.scale(d));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static ParticleOptions emissive(int color, float size) {
        return new EmissiveDustParticleOptions(color, size);
    }

    public static Power slotPower(ServerPlayer player, String abilityName) {
        for (Power power : HeroPowers.data(player).getActivePowers()) {
            if (power != null && abilityName.equals(power.getName())) {
                return power;
            }
        }
        return null;
    }

    public static void setCooldown(ServerPlayer player, String abilityName, double seconds) {
        Power power = slotPower(player, abilityName);
        if (power != null) {
            power.setCooldown(seconds);
            PlayerData.sync(player);
        }
    }

    public static void setActive(ServerPlayer player, String abilityName, boolean active) {
        Power power = slotPower(player, abilityName);
        if (power != null) {
            power.setActive(active);
            PlayerData.sync(player);
        }
    }

    public record Beam(Vec3 start, Vec3 end, List<LivingEntity> targets, boolean hitBlock) {
    }

    private record Dash(int ticksLeft, double damage, double knockback, Set<UUID> hit, Predicate<LivingEntity> canHit) {
    }
}
