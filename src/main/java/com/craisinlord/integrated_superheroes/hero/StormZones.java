package com.craisinlord.integrated_superheroes.hero;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class StormZones {
    private static final Map<ResourceKey<Level>, Map<UUID, Zone>> SERVER = new ConcurrentHashMap<>();
    private static volatile List<Zone> client = List.of();
    private static volatile long clientTime;
    private static volatile Vec3 clientFocus;

    private StormZones() {
    }

    public record Zone(double x, double z, double radius, boolean snow, long expires) {
        public static final StreamCodec<ByteBuf, Zone> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, Zone::x,
                ByteBufCodecs.DOUBLE, Zone::z,
                ByteBufCodecs.DOUBLE, Zone::radius,
                ByteBufCodecs.BOOL, Zone::snow,
                ByteBufCodecs.VAR_LONG, Zone::expires,
                Zone::new
        );

        private boolean covers(double px, double pz) {
            double dx = px - this.x;
            double dz = pz - this.z;
            return dx * dx + dz * dz <= this.radius * this.radius;
        }
    }

    public static Boolean forcedAt(double x, double z) {
        if (!SERVER.isEmpty()) {
            for (Map<UUID, Zone> zones : SERVER.values()) {
                for (Zone zone : zones.values()) {
                    if (zone.covers(x, z)) {
                        return zone.snow();
                    }
                }
            }
        }
        List<Zone> zones = client;
        if (zones.isEmpty()) {
            return null;
        }
        long now = clientTime;
        for (Zone zone : zones) {
            if (zone.expires() > now && zone.covers(x, z)) {
                return zone.snow();
            }
        }
        return null;
    }

    public static boolean focusForced() {
        Vec3 focus = clientFocus;
        List<Zone> zones = client;
        if (focus == null || zones.isEmpty()) {
            return false;
        }
        long now = clientTime;
        for (Zone zone : zones) {
            if (zone.expires() > now && zone.covers(focus.x, focus.z)) {
                return true;
            }
        }
        return false;
    }

    public static void force(ServerPlayer player, boolean snow) {
        ServerLevel level = player.serverLevel();
        long expires = level.getGameTime() + MarvelBalanceConfig.FORCED_WEATHER_SECONDS.get() * 20L;
        SERVER.computeIfAbsent(level.dimension(), ignored -> new ConcurrentHashMap<>())
                .put(player.getUUID(), new Zone(player.getX(), player.getZ(), MarvelBalanceConfig.FORCED_WEATHER_RADIUS.get(), snow, expires));
        sync(level);
    }

    public static void release(ServerPlayer player) {
        Map<UUID, Zone> zones = SERVER.get(player.serverLevel().dimension());
        if (zones != null && zones.remove(player.getUUID()) != null) {
            sync(player.serverLevel());
        }
    }

    public static void tick(ServerLevel level) {
        Map<UUID, Zone> zones = SERVER.get(level.dimension());
        if (zones == null || zones.isEmpty() || level.getGameTime() % 20 != 0) {
            return;
        }
        long now = level.getGameTime();
        if (zones.values().removeIf(zone -> zone.expires() <= now)) {
            sync(level);
        }
    }

    private static List<Zone> zonesFor(ServerLevel level) {
        Map<UUID, Zone> zones = SERVER.get(level.dimension());
        return zones == null ? List.of() : new ArrayList<>(zones.values());
    }

    public static void sync(ServerLevel level) {
        PacketDistributor.sendToPlayersInDimension(level, new StormZonesPayload(zonesFor(level)));
    }

    public static void syncTo(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new StormZonesPayload(zonesFor(player.serverLevel())));
    }

    public static void setClient(List<Zone> zones) {
        client = List.copyOf(zones);
    }

    public static void tickClient(long time, Vec3 focus) {
        clientTime = time;
        clientFocus = focus;
    }

    public static void clearClient() {
        client = List.of();
        clientFocus = null;
    }
}
