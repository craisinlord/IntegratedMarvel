package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.antarchy.content.portal.CrossDimensionTravelApi;
import com.craisinlord.antarchy.content.entity.portal.DimensionalTearEntity;
import com.craisinlord.antarchy.content.AntarchySoundEvents;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class SpacePortalManager {
    private static final int PORTAL_LIFETIME_TICKS = 1200;
    private static final Map<UUID, PortalPair> PORTAL_PAIRS = new ConcurrentHashMap<>();

    private SpacePortalManager() {
    }

    public static boolean create(ServerLevel source, BlockPos sourceBlock, Vec3 sourcePosition, Vec3 normal,
                                 ServerLevel destination, BlockPos destinationBlock, UUID owner, long now) {
        normal = new Vec3(normal.x, 0.0D, normal.z).normalize();
        if (normal.lengthSqr() < 1.0E-6D) {
            normal = new Vec3(0.0D, 0.0D, 1.0D);
        }

        Vec3 sourceCenter = sourcePosition.add(normal.scale(0.75D));
        ServerPlayer probe = source.getServer().getPlayerList().getPlayer(owner);
        Vec3 safeDestination = probe == null
                ? null
                : CrossDimensionTravelApi.findSafePosition(probe, destination, destinationBlock);
        if (safeDestination == null) {
            return false;
        }
        Endpoint first = new Endpoint(source.dimension(), sourceCenter, normal, sourceBlock);

        Endpoint second = new Endpoint(destination.dimension(), safeDestination.add(0.0D, 0.9D, 0.0D),
                normal.scale(-1.0D), BlockPos.containing(safeDestination));
        DimensionalTearEntity firstVisual = createVisual(source, first);
        DimensionalTearEntity secondVisual = createVisual(destination, second);
        if (!source.addFreshEntity(firstVisual) || !destination.addFreshEntity(secondVisual)) {
            firstVisual.discard();
            secondVisual.discard();
            return false;
        }
        PortalPair previous = PORTAL_PAIRS.put(owner, new PortalPair(first, second, firstVisual.getUUID(),
                secondVisual.getUUID(), now + PORTAL_LIFETIME_TICKS));
        if (previous != null) {
            discardVisuals(source.getServer(), previous);
        }
        return true;
    }

    private static DimensionalTearEntity createVisual(ServerLevel level, Endpoint endpoint) {
        Vec3 normal = endpoint.normal();
        float yaw = (float) Math.toDegrees(Math.atan2(normal.x, -normal.z));
        return DimensionalTearEntity.createGauntletVisual(level, endpoint.center(), yaw, PORTAL_LIFETIME_TICKS);
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, PortalPair> entry : PORTAL_PAIRS.entrySet()) {
            PortalPair pair = entry.getValue();
            if (now >= pair.expiresAt()) {
                if (PORTAL_PAIRS.remove(entry.getKey(), pair)) {
                    discardVisuals(server, pair);
                }
                continue;
            }
            tickEndpoint(server, pair.first(), pair.second(), now);
            tickEndpoint(server, pair.second(), pair.first(), now);
        }
    }

    public static void clear(MinecraftServer server) {
        for (PortalPair pair : PORTAL_PAIRS.values()) {
            discardVisuals(server, pair);
        }
        PORTAL_PAIRS.clear();
    }

    private static void discardVisuals(MinecraftServer server, PortalPair pair) {
        discardVisual(server, pair.first().dimension(), pair.firstVisual());
        discardVisual(server, pair.second().dimension(), pair.secondVisual());
    }

    private static void discardVisual(MinecraftServer server, net.minecraft.resources.ResourceKey<Level> dimension, UUID id) {
        ServerLevel level = server.getLevel(dimension);
        if (level != null && level.getEntity(id) instanceof DimensionalTearEntity tear) {
            tear.discard();
        }
    }

    private static void tickEndpoint(MinecraftServer server, Endpoint endpoint, Endpoint exit, long now) {
        ServerLevel level = server.getLevel(endpoint.dimension());
        ServerLevel destination = server.getLevel(exit.dimension());
        if (level == null || destination == null) {
            return;
        }
        if (now % 80L == 0L) {
            level.playSound(null, BlockPos.containing(endpoint.center()), AntarchySoundEvents.DIMENSIONAL_TEAR_IDLE.get(),
                    SoundSource.AMBIENT, 0.45F, 0.95F);
        }
        AABB bounds = AABB.ofSize(endpoint.center(), 3.2D, 3.6D, 3.2D);
        for (Entity entity : level.getEntitiesOfClass(Entity.class, bounds,
                candidate -> candidate.isAlive() && !(candidate instanceof Player player && player.isSpectator())
                        && !candidate.isOnPortalCooldown())) {
            if (!crosses(entity, endpoint)) {
                continue;
            }
            if (CrossDimensionTravelApi.transferToSafePosition(entity, destination, exit.safeBlock())) {
                entity.setPortalCooldown();
            }
        }
    }

    private static boolean crosses(Entity entity, Endpoint endpoint) {
        Vec3 center = endpoint.center();
        Vec3 normal = endpoint.normal();
        Vec3 previous = new Vec3(entity.xo, entity.yo + entity.getBbHeight() * 0.5D, entity.zo);
        Vec3 current = entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D);
        double previousDepth = previous.subtract(center).dot(normal);
        double currentDepth = current.subtract(center).dot(normal);
        if (previousDepth * currentDepth > 0.0D || Math.abs(previousDepth - currentDepth) < 1.0E-5D) {
            return false;
        }
        double fraction = previousDepth / (previousDepth - currentDepth);
        Vec3 crossing = previous.add(current.subtract(previous).scale(fraction));
        Vec3 right = new Vec3(-normal.z, 0.0D, normal.x);
        Vec3 relative = crossing.subtract(center);
        return Math.abs(relative.dot(right)) <= 1.15D && Math.abs(relative.y) <= 1.55D;
    }

    private record Endpoint(net.minecraft.resources.ResourceKey<Level> dimension, Vec3 center, Vec3 normal, BlockPos safeBlock) {
    }

    private record PortalPair(Endpoint first, Endpoint second, UUID firstVisual, UUID secondVisual, long expiresAt) {
    }
}
