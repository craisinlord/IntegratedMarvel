package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.antarchy.content.AntarchySoundEvents;
import com.craisinlord.antarchy.content.entity.portal.DimensionalTearEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpacePortalManager {
    private static final int PORTAL_LIFETIME_TICKS = 1200;
    private static final double CENTER_HEIGHT = 0.9D;
    private static final double EXIT_OFFSET = 1.4D;
    private static final double HALF_WIDTH = 1.15D;
    private static final double HALF_HEIGHT = 1.55D;
    private static final Map<UUID, PortalPair> PORTAL_PAIRS = new ConcurrentHashMap<>();
    private static final Map<UUID, Tracked> TRACKED = new HashMap<>();

    private SpacePortalManager() {
    }

    public static boolean create(ServerLevel source, Vec3 sourcePosition, Vec3 normal, Vec3 openerPosition,
                                 ServerLevel destination, BlockPos destinationBlock, ServerPlayer owner, long now) {
        normal = new Vec3(normal.x, 0.0D, normal.z);
        normal = normal.lengthSqr() < 1.0E-6D ? new Vec3(0.0D, 0.0D, 1.0D) : normal.normalize();

        Vec3 sourceCenter = groundSnap(source, sourcePosition.add(normal.scale(0.75D)));
        double openerSide = Math.signum(openerPosition.subtract(sourceCenter).dot(normal));
        Vec3 towardOpener = normal.scale(openerSide == 0.0D ? 1.0D : openerSide);
        Vec3 returnArrival = returnArrival(owner, source, sourceCenter, towardOpener, openerPosition);

        Vec3 arrival = SpaceTravel.arrivalPosition(owner, destination, destinationBlock);
        Vec3 exitNormal = exitNormal(destination, arrival, towardOpener);
        Vec3 exitCenter = arrival.add(exitNormal.scale(EXIT_OFFSET)).add(0.0D, CENTER_HEIGHT, 0.0D);

        Endpoint first = new Endpoint(source.dimension(), sourceCenter, normal, returnArrival, towardOpener);
        Endpoint second = new Endpoint(destination.dimension(), exitCenter, exitNormal, arrival, exitNormal.scale(-1.0D));
        DimensionalTearEntity firstVisual = createVisual(source, first);
        DimensionalTearEntity secondVisual = createVisual(destination, second);
        if (!source.addFreshEntity(firstVisual)) {
            firstVisual.discard();
            return false;
        }
        if (!destination.addFreshEntity(secondVisual)) {
            firstVisual.discard();
            secondVisual.discard();
            return false;
        }
        SpaceTravel.keepLoaded(source, BlockPos.containing(sourceCenter));
        SpaceTravel.keepLoaded(destination, BlockPos.containing(exitCenter));
        PortalPair previous = PORTAL_PAIRS.put(owner.getUUID(), new PortalPair(first, second, firstVisual.getUUID(),
                secondVisual.getUUID(), now + PORTAL_LIFETIME_TICKS));
        if (previous != null) {
            discardVisuals(source.getServer(), previous);
        }
        return true;
    }

    private static Vec3 groundSnap(ServerLevel level, Vec3 center) {
        BlockPos start = BlockPos.containing(center);
        for (int depth = 0; depth <= 6; depth++) {
            BlockPos feet = start.below(depth);
            if (!level.hasChunkAt(feet)) break;
            if (SpaceTravel.isOpen(level, feet) && !SpaceTravel.isOpen(level, feet.below())) {
                return new Vec3(center.x, feet.getY() + CENTER_HEIGHT, center.z);
            }
        }
        return center;
    }

    private static Vec3 returnArrival(ServerPlayer owner, ServerLevel source, Vec3 sourceCenter, Vec3 towardOpener,
                                      Vec3 openerPosition) {
        Vec3 inFront = sourceCenter.add(towardOpener.scale(EXIT_OFFSET)).subtract(0.0D, CENTER_HEIGHT, 0.0D);
        BlockPos feet = BlockPos.containing(inFront);
        if (SpaceTravel.isOpen(source, feet) && SpaceTravel.isOpen(source, feet.above())) {
            return new Vec3(inFront.x, feet.getY(), inFront.z);
        }
        return SpaceTravel.arrivalPosition(owner, source, BlockPos.containing(openerPosition));
    }

    private static Vec3 exitNormal(ServerLevel level, Vec3 arrival, Vec3 preferred) {
        Vec3 side = new Vec3(-preferred.z, 0.0D, preferred.x);
        Vec3[] candidates = {preferred.scale(-1.0D), preferred, side, side.scale(-1.0D)};
        for (Vec3 candidate : candidates) {
            BlockPos feet = BlockPos.containing(arrival.add(candidate.scale(EXIT_OFFSET)));
            if (SpaceTravel.isOpen(level, feet) && SpaceTravel.isOpen(level, feet.above())) return candidate;
        }
        return candidates[0];
    }

    private static DimensionalTearEntity createVisual(ServerLevel level, Endpoint endpoint) {
        Vec3 normal = endpoint.normal();
        float yaw = (float) Math.toDegrees(Math.atan2(normal.x, -normal.z));
        return DimensionalTearEntity.createGauntletVisual(level, endpoint.center(), yaw, PORTAL_LIFETIME_TICKS);
    }

    public static void tick(MinecraftServer server) {
        if (PORTAL_PAIRS.isEmpty()) {
            TRACKED.clear();
            return;
        }
        long now = server.overworld().getGameTime();
        Map<UUID, Tracked> seen = new HashMap<>();
        for (Map.Entry<UUID, PortalPair> entry : PORTAL_PAIRS.entrySet()) {
            PortalPair pair = entry.getValue();
            if (now >= pair.expiresAt()) {
                if (PORTAL_PAIRS.remove(entry.getKey(), pair)) {
                    discardVisuals(server, pair);
                }
                continue;
            }
            tickEndpoint(server, pair.first(), pair.second(), now, seen);
            tickEndpoint(server, pair.second(), pair.first(), now, seen);
        }
        TRACKED.clear();
        TRACKED.putAll(seen);
    }

    public static void clear(MinecraftServer server) {
        for (PortalPair pair : PORTAL_PAIRS.values()) {
            discardVisuals(server, pair);
        }
        PORTAL_PAIRS.clear();
        TRACKED.clear();
    }

    private static void discardVisuals(MinecraftServer server, PortalPair pair) {
        discardVisual(server, pair.first().dimension(), pair.firstVisual());
        discardVisual(server, pair.second().dimension(), pair.secondVisual());
    }

    private static void discardVisual(MinecraftServer server, ResourceKey<Level> dimension, UUID id) {
        ServerLevel level = server.getLevel(dimension);
        if (level != null && level.getEntity(id) instanceof DimensionalTearEntity tear) {
            tear.discard();
        }
    }

    private static void tickEndpoint(MinecraftServer server, Endpoint endpoint, Endpoint exit, long now,
                                     Map<UUID, Tracked> seen) {
        ServerLevel level = server.getLevel(endpoint.dimension());
        ServerLevel destination = server.getLevel(exit.dimension());
        if (level == null || destination == null) {
            return;
        }
        if (now % 20L == 0L) {
            SpaceTravel.keepLoaded(level, BlockPos.containing(endpoint.center()));
        }
        if (now % 80L == 0L) {
            level.playSound(null, BlockPos.containing(endpoint.center()), AntarchySoundEvents.DIMENSIONAL_TEAR_IDLE.get(),
                    SoundSource.AMBIENT, 0.45F, 0.95F);
        }
        AABB bounds = AABB.ofSize(endpoint.center(), 8.0D, 6.0D, 8.0D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds, SpacePortalManager::canTravel)) {
            Vec3 current = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
            Tracked previous = TRACKED.get(entity.getUUID());
            seen.put(entity.getUUID(), new Tracked(level.dimension(), current));
            if (previous == null || !previous.dimension().equals(level.dimension()) || entity.isOnPortalCooldown()) {
                continue;
            }
            if (!crosses(previous.position(), current, endpoint)) {
                continue;
            }
            Vec3 arrival = SpaceTravel.confirmArrival(entity, destination, exit.arrival());
            float yaw = (float) Math.toDegrees(Math.atan2(-exit.facing().x, exit.facing().z));
            if (SpaceTravel.teleport(entity, destination, arrival, yaw)) {
                seen.remove(entity.getUUID());
            }
        }
    }

    private static boolean canTravel(Entity entity) {
        return entity.isAlive() && !entity.isSpectator() && !entity.isPassenger()
                && !(entity instanceof DimensionalTearEntity);
    }

    private static boolean crosses(Vec3 previous, Vec3 current, Endpoint endpoint) {
        Vec3 center = endpoint.center();
        Vec3 normal = endpoint.normal();
        double previousDepth = previous.subtract(center).dot(normal);
        double currentDepth = current.subtract(center).dot(normal);
        boolean entering = previousDepth > 0.0D && currentDepth <= 0.0D || previousDepth < 0.0D && currentDepth >= 0.0D;
        if (!entering) {
            return false;
        }
        double fraction = previousDepth / (previousDepth - currentDepth);
        Vec3 crossing = previous.add(current.subtract(previous).scale(fraction));
        Vec3 right = new Vec3(-normal.z, 0.0D, normal.x);
        Vec3 relative = crossing.subtract(center);
        return Math.abs(relative.dot(right)) <= HALF_WIDTH && Math.abs(relative.y) <= HALF_HEIGHT;
    }

    private record Endpoint(ResourceKey<Level> dimension, Vec3 center, Vec3 normal, Vec3 arrival, Vec3 facing) {
    }

    private record PortalPair(Endpoint first, Endpoint second, UUID firstVisual, UUID secondVisual, long expiresAt) {
    }

    private record Tracked(ResourceKey<Level> dimension, Vec3 position) {
    }
}
