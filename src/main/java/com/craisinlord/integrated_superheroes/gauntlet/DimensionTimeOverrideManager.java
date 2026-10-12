package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.antarchy.content.time.TimeDilationApi;
import com.craisinlord.antarchy.content.time.TimeDilationEntityAccess;
import com.craisinlord.antarchy.content.time.TimeDilationManager;
import com.craisinlord.antarchy.content.time.TimeDilationMath;
import com.craisinlord.antarchy.content.time.TimeDilationFieldEntity;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class DimensionTimeOverrideManager {
    private static final long FIELD_DURATION = 400L;
    private static final double FIELD_RATE = 0.25D;
    private static final Map<ResourceKey<Level>, Override> ACTIVE = new ConcurrentHashMap<>();

    private DimensionTimeOverrideManager() {}

    public static boolean create(ServerLevel level, UUID owner) {
        long now = level.getServer().overworld().getGameTime();
        Override override = ACTIVE.compute(level.dimension(), (key, current) -> {
            if (current == null) return new Override(now + FIELD_DURATION);
            current.expiresAt = now + FIELD_DURATION;
            return current;
        });
        applyLevel(level, override);
        return true;
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Map.Entry<ResourceKey<Level>, Override> entry : ACTIVE.entrySet()) {
            Override override = entry.getValue();
            if (now >= override.expiresAt) {
                if (ACTIVE.remove(entry.getKey(), override)) restore(override);
                continue;
            }
            ServerLevel level = server.getLevel(entry.getKey());
            if (level != null) {
                pruneMovedEntities(override, entry.getKey());
                if (now % 10L == 0L) applyLevel(level, override);
            }
        }
    }

    public static void clear(MinecraftServer server) {
        for (Override override : ACTIVE.values()) restore(override);
        ACTIVE.clear();
    }

    private static void applyLevel(ServerLevel level, Override override) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof TimeDilationFieldEntity || !(entity instanceof TimeDilationEntityAccess access)
                    || entity.getType().is(com.craisinlord.antarchy.content.AntarchyTags.Entities.TIME_DILATION_IMMUNE)) continue;
            RateState state = override.entities.computeIfAbsent(entity.getUUID(), ignored ->
                    new RateState(entity, level.dimension(), access.antarchy$getInheritedTimeDilationRate(), Double.NaN));
            double current = access.antarchy$getInheritedTimeDilationRate();
            if (Double.isFinite(state.appliedRate) && Math.abs(current - state.appliedRate) > 0.001D) {
                state.originalRate = current;
            }
            double applied = TimeDilationMath.clampRate(state.originalRate * FIELD_RATE);
            if (Math.abs(current - applied) > 0.001D) {
                TimeDilationApi.setInheritedRate(entity, applied);
                TimeDilationManager.trackPotentiallyAffected(entity);
            }
            state.appliedRate = applied;
        }
    }

    private static void restore(Override override) {
        for (RateState state : override.entities.values()) {
            Entity entity = state.entity;
            if (!(entity instanceof TimeDilationEntityAccess access)) continue;
            double current = access.antarchy$getInheritedTimeDilationRate();
            if (Double.isFinite(state.appliedRate) && Math.abs(current - state.appliedRate) <= 0.001D) {
                TimeDilationApi.setInheritedRate(entity, state.originalRate);
                TimeDilationManager.trackPotentiallyAffected(entity);
            }
        }
        override.entities.clear();
    }

    private static void pruneMovedEntities(Override override, ResourceKey<Level> dimension) {
        var iterator = override.entities.entrySet().iterator();
        while (iterator.hasNext()) {
            RateState state = iterator.next().getValue();
            if (!state.entity.isRemoved() && !state.entity.level().dimension().equals(dimension)) {
                restoreEntity(state);
                iterator.remove();
            }
        }
    }

    private static void restoreEntity(RateState state) {
        if (!(state.entity instanceof TimeDilationEntityAccess access)) return;
        double current = access.antarchy$getInheritedTimeDilationRate();
        if (Double.isFinite(state.appliedRate) && Math.abs(current - state.appliedRate) <= 0.001D) {
            TimeDilationApi.setInheritedRate(state.entity, state.originalRate);
            TimeDilationManager.trackPotentiallyAffected(state.entity);
        }
    }

    private static final class Override {
        private final Map<UUID, RateState> entities = new HashMap<>();
        private long expiresAt;

        private Override(long expiresAt) {
            this.expiresAt = expiresAt;
        }
    }

    private static final class RateState {
        private final Entity entity;
        private final ResourceKey<Level> dimension;
        private double originalRate;
        private double appliedRate;

        private RateState(Entity entity, ResourceKey<Level> dimension, double originalRate, double appliedRate) {
            this.entity = entity;
            this.dimension = dimension;
            this.originalRate = originalRate;
            this.appliedRate = appliedRate;
        }
    }
}
