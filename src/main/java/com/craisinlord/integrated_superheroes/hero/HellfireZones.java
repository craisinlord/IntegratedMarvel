package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class HellfireZones {
    private static final Map<UUID, List<Zone>> ZONES = ServerState.map(new HashMap<>());
    private static final Map<UUID, Burn> BURNS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Map<UUID, Long>> LAST_HIT = ServerState.map(new HashMap<>());

    private HellfireZones() {
    }

    private record Zone(ResourceKey<Level> dimension, Vec3 center, long until) {
    }

    private static final class Burn {
        private final UUID owner;
        private final ResourceKey<Level> dimension;
        private long until;

        private Burn(UUID owner, ResourceKey<Level> dimension, long until) {
            this.owner = owner;
            this.dimension = dimension;
            this.until = until;
        }
    }

    public static boolean harmable(LivingEntity owner, Entity entity) {
        if (!(entity instanceof LivingEntity living) || living == owner || !living.isAlive() || living instanceof ArmorStand) {
            return false;
        }
        if (HellrideHorse.isHellride(living)) {
            return false;
        }
        if (!(owner instanceof Player rider)) {
            return HeroCaster.foe(owner, living);
        }
        if (living instanceof OwnableEntity ownable && owner.getUUID().equals(ownable.getOwnerUUID())) {
            return false;
        }
        if (living instanceof Player other) {
            return !other.isCreative() && !other.isSpectator() && rider.canHarmPlayer(other);
        }
        return !living.isAlliedTo(owner);
    }

    public static boolean aggressive(ServerPlayer owner, LivingEntity entity) {
        if (!harmable(owner, entity)) {
            return false;
        }
        if (entity instanceof Enemy || entity instanceof Player) {
            return true;
        }
        return entity instanceof Mob mob && mob.getTarget() != null
                && (mob.getTarget() == owner || mob.getTarget() instanceof Player || mob.getTarget().isAlliedTo(owner));
    }

    public static boolean hit(ServerPlayer owner, LivingEntity victim, double baseDamage) {
        if (!harmable(owner, victim) || baseDamage <= 0.0D) {
            return false;
        }
        long now = owner.serverLevel().getGameTime();
        Map<UUID, Long> hits = LAST_HIT.computeIfAbsent(owner.getUUID(), ignored -> new HashMap<>());
        Long last = hits.get(victim.getUUID());
        if (last != null && now - last < GhostRiderConfig.HELLFIRE_INTERVAL_TICKS.get()) {
            return false;
        }
        hits.put(victim.getUUID(), now);
        double damage = baseDamage;
        if (victim.fireImmune()) {
            damage *= GhostRiderConfig.FIRE_IMMUNE_MULTIPLIER.get();
        }
        if (victim.hasEffect(MobEffects.FIRE_RESISTANCE)) {
            damage *= GhostRiderConfig.FIRE_RESISTANCE_MULTIPLIER.get();
        }
        if (damage <= 0.0D) {
            return false;
        }
        float amount = (float) damage;
        HeroAbilities.runAsAbilityDamage(() -> HeroHits.force(owner, victim, owner.damageSources().playerAttack(owner), amount));
        Vec3 center = victim.getBoundingBox().getCenter();
        owner.serverLevel().sendParticles(ParticleTypes.SOUL_FIRE_FLAME, center.x, center.y, center.z, 4,
                victim.getBbWidth() * 0.4D, victim.getBbHeight() * 0.4D, victim.getBbWidth() * 0.4D, 0.01D);
        return true;
    }

    public static void ignite(ServerPlayer owner, LivingEntity victim) {
        int ticks = GhostRiderConfig.BURN_TICKS.get();
        if (ticks <= 0 || !harmable(owner, victim)) {
            return;
        }
        long until = owner.serverLevel().getGameTime() + ticks;
        Burn burn = BURNS.get(victim.getUUID());
        if (burn != null && burn.owner.equals(owner.getUUID())) {
            burn.until = Math.max(burn.until, until);
        } else {
            BURNS.put(victim.getUUID(), new Burn(owner.getUUID(), owner.serverLevel().dimension(), until));
        }
    }

    public static void drop(ServerPlayer owner, Vec3 at) {
        List<Zone> zones = ZONES.computeIfAbsent(owner.getUUID(), ignored -> new ArrayList<>());
        int max = GhostRiderConfig.TRAIL_MAX_ZONES.get();
        while (zones.size() >= max) {
            zones.remove(0);
        }
        zones.add(new Zone(owner.serverLevel().dimension(), at, owner.serverLevel().getGameTime() + GhostRiderConfig.TRAIL_ZONE_TICKS.get()));
    }

    public static void tick(MinecraftServer server) {
        if (ZONES.isEmpty() && BURNS.isEmpty()) {
            return;
        }
        tickBurns(server);
        tickZones(server);
    }

    private static void tickBurns(MinecraftServer server) {
        for (Map.Entry<UUID, Burn> entry : List.copyOf(BURNS.entrySet())) {
            Burn burn = entry.getValue();
            if (BURNS.get(entry.getKey()) != burn) {
                continue;
            }
            ServerLevel level = server.getLevel(burn.dimension);
            ServerPlayer owner = server.getPlayerList().getPlayer(burn.owner);
            if (level == null || owner == null || owner.level() != level || level.getGameTime() > burn.until) {
                BURNS.remove(entry.getKey());
                continue;
            }
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity victim) || !victim.isAlive() || victim.isInWaterRainOrBubble()) {
                BURNS.remove(entry.getKey());
                continue;
            }
            if (level.getGameTime() % 4 == 0) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, victim.getX(), victim.getY() + victim.getBbHeight() * 0.5D, victim.getZ(), 2,
                        victim.getBbWidth() * 0.35D, victim.getBbHeight() * 0.35D, victim.getBbWidth() * 0.35D, 0.01D);
            }
            hit(owner, victim, GhostRiderConfig.BURN_DAMAGE.get());
        }
    }

    private static void tickZones(MinecraftServer server) {
        double radius = GhostRiderConfig.TRAIL_RADIUS.get();
        for (Map.Entry<UUID, List<Zone>> entry : List.copyOf(ZONES.entrySet())) {
            if (ZONES.get(entry.getKey()) != entry.getValue()) {
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            if (owner == null) {
                ZONES.remove(entry.getKey());
                continue;
            }
            ServerLevel level = owner.serverLevel();
            long now = level.getGameTime();
            entry.getValue().removeIf(zone -> zone.dimension() != level.dimension() || now > zone.until());
            if (entry.getValue().isEmpty()) {
                ZONES.remove(entry.getKey());
                continue;
            }
            for (Zone zone : List.copyOf(entry.getValue())) {
                if (ZONES.get(entry.getKey()) != entry.getValue()) {
                    break;
                }
                Vec3 c = zone.center();
                if (now % 3 == 0) {
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, c.x, c.y + 0.1D, c.z, 2, radius * 0.4D, 0.05D, radius * 0.4D, 0.01D);
                }
                if (now % 6 == 0) {
                    level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.1D, c.z, 1, radius * 0.4D, 0.05D, radius * 0.4D, 0.005D);
                }
                AABB box = new AABB(c.x - radius, c.y - 0.5D, c.z - radius, c.x + radius, c.y + 2.0D, c.z + radius);
                for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box, entity -> aggressive(owner, entity))) {
                    hit(owner, victim, GhostRiderConfig.TRAIL_DAMAGE.get());
                }
            }
        }
    }

    public static void clear(ServerPlayer owner) {
        UUID id = owner.getUUID();
        ZONES.remove(id);
        LAST_HIT.remove(id);
        BURNS.values().removeIf(burn -> burn.owner.equals(id));
    }

    public static void clearAll() {
        ZONES.clear();
        BURNS.clear();
        LAST_HIT.clear();
    }

    public static void prune(long now, ServerPlayer owner) {
        Map<UUID, Long> hits = LAST_HIT.get(owner.getUUID());
        if (hits != null) {
            hits.values().removeIf(tick -> now - tick > 200L);
            if (hits.isEmpty()) {
                LAST_HIT.remove(owner.getUUID());
            }
        }
    }
}
