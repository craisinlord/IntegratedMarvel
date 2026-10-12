package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class HeroGrip {
    private static final Map<UUID, Held> HELD = new HashMap<>();
    private static final Map<Integer, Thrown> THROWN = new HashMap<>();

    private HeroGrip() {
    }

    private record Held(int entityId, double distance, int ticksLeft, ParticleOptions particle, Consumer<LivingEntity> onTick) {
    }

    private record Thrown(UUID owner, int ticksLeft, float damage, Set<Integer> hit) {
    }

    public static boolean isHolding(ServerPlayer player) {
        return HELD.containsKey(player.getUUID());
    }

    public static boolean grab(ServerPlayer player, LivingEntity target, int ticks, ParticleOptions particle, Consumer<LivingEntity> onTick) {
        if (!HeroAbilities.canControl(target)) {
            return false;
        }
        double distance = Math.max(3.0D, Math.min(6.0D, player.distanceTo(target)));
        HELD.put(player.getUUID(), new Held(target.getId(), distance, ticks, particle, onTick));
        return true;
    }

    public static boolean release(ServerPlayer player, double speed, float damage) {
        Held held = HELD.remove(player.getUUID());
        if (held == null) {
            return false;
        }
        Entity entity = player.level().getEntity(held.entityId());
        if (!(entity instanceof LivingEntity target) || !target.isAlive()) {
            return false;
        }
        target.setNoGravity(false);
        target.setDeltaMovement(player.getLookAngle().scale(speed));
        target.hurtMarked = true;
        target.hasImpulse = true;
        THROWN.put(target.getId(), new Thrown(player.getUUID(), 40, damage, new HashSet<>()));
        return true;
    }

    public static void drop(ServerPlayer player) {
        Held held = HELD.remove(player.getUUID());
        if (held != null && player.level().getEntity(held.entityId()) instanceof LivingEntity target) {
            target.setNoGravity(false);
        }
    }

    public static void tick(ServerPlayer player) {
        Held held = HELD.get(player.getUUID());
        if (held == null) {
            return;
        }
        Entity entity = player.level().getEntity(held.entityId());
        if (!(entity instanceof LivingEntity target) || !target.isAlive() || held.ticksLeft() <= 0 || target.distanceToSqr(player) > 48.0D * 48.0D) {
            drop(player);
            return;
        }
        Vec3 point = player.getEyePosition().add(player.getLookAngle().scale(held.distance())).subtract(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        Vec3 pull = point.subtract(target.position());
        target.setNoGravity(true);
        target.setDeltaMovement(pull.scale(0.4D));
        target.hurtMarked = true;
        target.hasImpulse = true;
        target.fallDistance = 0.0F;
        held.onTick().accept(target);
        if (held.particle() != null && player.level() instanceof ServerLevel level && player.tickCount % 2 == 0) {
            Vec3 center = target.getBoundingBox().getCenter();
            level.sendParticles(held.particle(), center.x, center.y, center.z, 4, target.getBbWidth() * 0.6D, target.getBbHeight() * 0.5D,
                    target.getBbWidth() * 0.6D, 0.0D);
        }
        HELD.put(player.getUUID(), new Held(held.entityId(), held.distance(), held.ticksLeft() - 1, held.particle(), held.onTick()));
    }

    public static void tickThrown(ServerLevel level) {
        if (THROWN.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<Integer, Thrown>> iterator = THROWN.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Thrown> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (entity == null) {
                continue;
            }
            Thrown thrown = entry.getValue();
            if (!(entity instanceof LivingEntity target) || !target.isAlive() || thrown.ticksLeft() <= 0) {
                iterator.remove();
                continue;
            }
            Entity owner = level.getEntity(thrown.owner());
            boolean impact = target.horizontalCollision || target.verticalCollision && thrown.ticksLeft() < 37;
            for (LivingEntity other : level.getEntitiesOfClass(LivingEntity.class, target.getBoundingBox().inflate(0.4D),
                    candidate -> candidate != target && candidate != owner && candidate.isAlive() && !thrown.hit().contains(candidate.getId()))) {
                thrown.hit().add(other.getId());
                other.hurt(owner instanceof ServerPlayer player ? player.damageSources().mobAttack(player) : level.damageSources().generic(), thrown.damage());
                impact = true;
            }
            if (impact) {
                target.hurt(owner instanceof ServerPlayer player ? player.damageSources().mobAttack(player) : level.damageSources().flyIntoWall(), thrown.damage());
                iterator.remove();
                continue;
            }
            entry.setValue(new Thrown(thrown.owner(), thrown.ticksLeft() - 1, thrown.damage(), thrown.hit()));
        }
    }

    public static void clear(ServerPlayer player) {
        drop(player);
    }
}
