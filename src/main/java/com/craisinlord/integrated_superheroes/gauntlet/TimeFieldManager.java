package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationFieldEntity;
import com.craisinlord.anthology.time.TimeDilationManager;
import com.craisinlord.anthology.time.TimeDilationTags;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public final class TimeFieldManager {
    private static final String KEY_PREFIX = "integrated_superheroes_time_field_";
    private static final List<Field> FIELDS = new CopyOnWriteArrayList<>();
    private static int nextId;

    private TimeFieldManager() {
    }

    public static void create(ServerLevel level, Vec3 center, double radius, double rate, int duration, UUID owner) {
        long expiry = level.getServer().overworld().getGameTime() + duration;
        FIELDS.add(new Field(KEY_PREFIX + nextId++, level.dimension(), center, radius, rate, expiry, owner, new HashSet<>()));
    }

    public static void tick(MinecraftServer server) {
        if (FIELDS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        for (Field field : FIELDS) {
            ServerLevel level = server.getLevel(field.dimension());
            if (level == null || now >= field.expiry()) {
                release(server, field);
                FIELDS.remove(field);
                continue;
            }
            Set<UUID> inside = new HashSet<>();
            double radiusSqr = field.radius() * field.radius();
            for (Entity entity : level.getEntities((Entity) null, new AABB(field.center(), field.center()).inflate(field.radius()),
                    entity -> affectable(entity, field) && entity.distanceToSqr(field.center()) <= radiusSqr)) {
                inside.add(entity.getUUID());
                if (Math.abs(TimeDilationApi.getPersonalRate(entity, field.key()) - field.rate()) > 0.001D) {
                    TimeDilationApi.setPersonalRate(entity, field.key(), field.rate());
                }
                TimeDilationManager.trackPotentiallyAffected(entity);
            }
            for (UUID id : Set.copyOf(field.affected())) {
                if (inside.contains(id)) continue;
                Entity entity = level.getEntity(id);
                if (entity != null) TimeDilationApi.clearPersonalRate(entity, field.key());
                field.affected().remove(id);
            }
            field.affected().addAll(inside);
        }
    }

    private static boolean affectable(Entity entity, Field field) {
        return entity instanceof TimeDilationEntityAccess && !(entity instanceof TimeDilationFieldEntity)
                && !entity.getUUID().equals(field.owner()) && entity.isAlive()
                && !entity.getType().is(TimeDilationTags.TIME_DILATION_IMMUNE);
    }

    private static void release(MinecraftServer server, Field field) {
        ServerLevel level = server.getLevel(field.dimension());
        for (UUID id : field.affected()) {
            Entity entity = level == null ? null : level.getEntity(id);
            if (entity == null) {
                for (ServerLevel candidate : server.getAllLevels()) {
                    entity = candidate.getEntity(id);
                    if (entity != null) break;
                }
            }
            if (entity != null) TimeDilationApi.clearPersonalRate(entity, field.key());
        }
        field.affected().clear();
    }

    public static void clear(MinecraftServer server) {
        for (Field field : FIELDS) release(server, field);
        FIELDS.clear();
    }

    private record Field(String key, ResourceKey<Level> dimension, Vec3 center, double radius, double rate, long expiry,
                         UUID owner, Set<UUID> affected) {
    }
}
