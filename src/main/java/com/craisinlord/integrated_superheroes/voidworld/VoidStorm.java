package com.craisinlord.integrated_superheroes.voidworld;

import com.craisinlord.integrated_superheroes.boss.WitherstormContent;
import com.craisinlord.integrated_superheroes.boss.WitherstormEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

public final class VoidStorm {
    private static final TicketType<ChunkPos> TICKET = TicketType.create("integrated_superheroes_void_storm", Comparator.comparingLong(ChunkPos::toLong));
    private static final int ROAM_TICKET_RADIUS = 2;
    private static final int FIGHT_TICKET_RADIUS = 4;
    private static final String DATA = "integrated_superheroes_void_storm";
    private static final Map<ServerLevel, Runtime> RUNTIME = new WeakHashMap<>();

    private VoidStorm() {
    }

    private static final class Runtime {
        @Nullable
        WitherstormEntity storm;
        @Nullable
        ChunkPos ticket;
        int ticketRadius;
        long engagedUntil;
        boolean defeated;
        double keep;
        int orbitSign = 1;
        long nextSpawnTry;
        long nextRoar;
    }

    public static final class Data extends SavedData {
        int stage;
        float health = -1.0F;
        double hunger;
        long blockedUntil;

        static Data load(CompoundTag tag, HolderLookup.Provider registries) {
            Data data = new Data();
            data.stage = tag.getInt("Stage");
            data.health = tag.contains("Health") ? tag.getFloat("Health") : -1.0F;
            data.blockedUntil = tag.getLong("BlockedUntil");
            data.hunger = tag.getDouble("Hunger");
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            tag.putInt("Stage", this.stage);
            tag.putFloat("Health", this.health);
            tag.putLong("BlockedUntil", this.blockedUntil);
            tag.putDouble("Hunger", this.hunger);
            return tag;
        }
    }

    private static Data data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(Data::new, Data::load, null), DATA);
    }

    private static Runtime runtime(ServerLevel level) {
        return RUNTIME.computeIfAbsent(level, key -> new Runtime());
    }

    private static boolean enabled() {
        return !VoidConfig.SPEC.isLoaded() || VoidConfig.STORM_ENABLED.get();
    }

    private static int startStage() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_START_STAGE.get() : 3;
    }

    private static double spawnDistance(ServerLevel level) {
        double wanted = VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_SPAWN_DISTANCE.get() : 160;
        return Math.min(wanted, maxRange(level));
    }

    private static double closest() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_CLOSEST_DISTANCE.get() : 48;
    }

    private static double approachPerSecond() {
        return (VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_APPROACH.get() : 6.0D) / 60.0D;
    }

    private static long respawnTicks() {
        return (VoidConfig.SPEC.isLoaded() ? VoidConfig.STORM_RESPAWN_MINUTES.get() : 60) * 1200L;
    }

    private static double maxRange(ServerLevel level) {
        return Math.max(64.0D, level.getServer().getPlayerList().getViewDistance() * 16.0D - 24.0D);
    }

    private static List<ServerPlayer> players(ServerLevel level) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) out.add(player);
        }
        return out;
    }

    public static void tick(ServerLevel level) {
        Runtime rt = runtime(level);
        WitherstormEntity storm = rt.storm;
        long now = level.getGameTime();
        if (storm != null && (storm.isRemoved() || storm.level() != level)) {
            releaseTicket(level, rt);
            rt.storm = null;
            storm = null;
        }
        boolean wanted = enabled() && !players(level).isEmpty();
        if (!wanted) {
            if (storm != null) despawn(level, rt);
            return;
        }
        if (storm == null) {
            if (now >= rt.nextSpawnTry) {
                rt.nextSpawnTry = now + 100L;
                if (now >= data(level).blockedUntil) spawn(level, rt, null, 0);
            }
            return;
        }
        if (storm.isDeadOrDying()) return;
        if (now % 10L == 0L) updateTicket(level, rt, storm);
        if (now % 20L == 0L) steer(level, rt, storm);
        if (now % 100L == 0L) remember(level, storm);
        if (now >= rt.nextRoar) {
            rt.nextRoar = now + 900L + level.random.nextInt(900);
            roar(level, storm);
        }
    }

    @Nullable
    private static ServerPlayer nearest(ServerLevel level, Vec3 to) {
        ServerPlayer best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : players(level)) {
            double dx = player.getX() - to.x;
            double dz = player.getZ() - to.z;
            double distance = dx * dx + dz * dz;
            if (distance < bestDistance) {
                bestDistance = distance;
                best = player;
            }
        }
        return best;
    }

    @Nullable
    private static WitherstormEntity spawn(ServerLevel level, Runtime rt, @Nullable ServerPlayer near, int forcedStage) {
        List<ServerPlayer> players = players(level);
        if (players.isEmpty()) return null;
        ServerPlayer anchor = near != null ? near : players.get(level.random.nextInt(players.size()));
        double distance = spawnDistance(level);
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        double x = anchor.getX() + Math.cos(angle) * distance;
        double z = anchor.getZ() + Math.sin(angle) * distance;
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        if (!level.hasChunk(bx >> 4, bz >> 4)) return null;
        WitherstormEntity storm = WitherstormContent.WITHERSTORM.get().create(level);
        if (storm == null) return null;
        Data data = data(level);
        int stage = forcedStage > 0 ? forcedStage : data.stage > 1 ? data.stage : startStage();
        storm.setRoaming(true);
        storm.setStage(stage);
        if (forcedStage <= 0 && data.health > 0.0F) storm.setHealth(Math.min(data.health, storm.getMaxHealth()));
        storm.setHunger(forcedStage <= 0 ? data.hunger : 0.0D);
        int ground = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz), 64);
        float yaw = (float) (Mth.atan2(anchor.getZ() - z, anchor.getX() - x) * Mth.RAD_TO_DEG) - 90.0F;
        storm.moveTo(x, ground + storm.hoverOffset(), z, yaw, 0.0F);
        storm.setYBodyRot(yaw);
        storm.setYHeadRot(yaw);
        if (!level.addFreshEntity(storm)) return null;
        rt.storm = storm;
        rt.defeated = false;
        rt.engagedUntil = 0L;
        rt.keep = distance;
        rt.orbitSign = level.random.nextBoolean() ? 1 : -1;
        rt.nextRoar = level.getGameTime() + 200L;
        data.stage = stage;
        data.health = storm.getHealth();
        data.blockedUntil = 0L;
        data.setDirty();
        updateTicket(level, rt, storm);
        return storm;
    }

    private static void steer(ServerLevel level, Runtime rt, WitherstormEntity storm) {
        ServerPlayer anchor = nearest(level, storm.position());
        if (anchor == null) return;
        double dx = storm.getX() - anchor.getX();
        double dz = storm.getZ() - anchor.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double range = maxRange(level);
        rt.keep = Math.max(closest(), rt.keep - approachPerSecond());
        double keep = Math.min(rt.keep, range);
        if (distance > range + 48.0D) {
            double angle = Math.atan2(dz, dx);
            double x = anchor.getX() + Math.cos(angle) * keep;
            double z = anchor.getZ() + Math.sin(angle) * keep;
            if (!level.hasChunk(Mth.floor(x) >> 4, Mth.floor(z) >> 4)) return;
            int ground = Math.max(level.getHeight(Heightmap.Types.WORLD_SURFACE, Mth.floor(x), Mth.floor(z)), 64);
            storm.teleportTo(x, ground + storm.hoverOffset(), z);
            updateTicket(level, rt, storm);
            return;
        }
        if (level.random.nextInt(180) == 0) rt.orbitSign = -rt.orbitSign;
        double radius = Math.min(distance, keep);
        double angle = Math.atan2(dz, dx) + rt.orbitSign * (6.0D / Math.max(radius, 32.0D));
        Vec3 goal = new Vec3(anchor.getX() + Math.cos(angle) * radius, 0.0D, anchor.getZ() + Math.sin(angle) * radius);
        double speed = distance > keep + 32.0D ? 0.35D : 0.12D;
        storm.setRoamGoal(goal, speed);
    }

    private static void updateTicket(ServerLevel level, Runtime rt, WitherstormEntity storm) {
        long now = level.getGameTime();
        if (storm.getTarget() != null) rt.engagedUntil = now + 60L;
        boolean fighting = now < rt.engagedUntil;
        Vec3 anchor = fighting ? storm.position().lerp(storm.mouth(), 0.5D) : storm.position();
        ChunkPos want = new ChunkPos(Mth.floor(anchor.x) >> 4, Mth.floor(anchor.z) >> 4);
        int radius = fighting ? FIGHT_TICKET_RADIUS : ROAM_TICKET_RADIUS;
        if (want.equals(rt.ticket) && radius == rt.ticketRadius) return;
        level.getChunkSource().addRegionTicket(TICKET, want, radius, want);
        releaseTicket(level, rt);
        rt.ticket = want;
        rt.ticketRadius = radius;
    }

    private static void releaseTicket(ServerLevel level, Runtime rt) {
        if (rt.ticket == null) return;
        level.getChunkSource().removeRegionTicket(TICKET, rt.ticket, rt.ticketRadius, rt.ticket);
        rt.ticket = null;
    }

    public static void recordDefeat(ServerLevel level, WitherstormEntity storm) {
        Runtime rt = runtime(level);
        if (rt.storm != storm || rt.defeated) return;
        rt.defeated = true;
        Data data = data(level);
        data.stage = 0;
        data.health = -1.0F;
        data.hunger = 0.0D;
        data.blockedUntil = level.getGameTime() + respawnTicks();
        data.setDirty();
    }

    private static void remember(ServerLevel level, WitherstormEntity storm) {
        Data data = data(level);
        data.stage = storm.getStage();
        data.health = storm.getHealth();
        data.hunger = storm.getHunger();
        data.setDirty();
    }

    private static void despawn(ServerLevel level, Runtime rt) {
        WitherstormEntity storm = rt.storm;
        rt.storm = null;
        if (storm != null) {
            if (storm.isAlive()) remember(level, storm);
            storm.discard();
        }
        releaseTicket(level, rt);
    }

    private static void roar(ServerLevel level, WitherstormEntity storm) {
        storm.roarFromAfar();
        Holder<net.minecraft.sounds.SoundEvent> growl = Holder.direct(SoundEvents.WITHER_SPAWN);
        long seed = level.random.nextLong();
        for (ServerPlayer player : players(level)) {
            Vec3 to = storm.position().subtract(player.position());
            double distance = Math.sqrt(to.x * to.x + to.z * to.z);
            Vec3 dir = distance < 1.0D ? Vec3.ZERO : new Vec3(to.x / distance, 0.0D, to.z / distance);
            float volume = (float) Mth.clamp(3.0D - distance / 240.0D, 1.0D, 3.0D);
            Vec3 at = player.position().add(dir.scale(12.0D)).add(0.0D, 6.0D, 0.0D);
            player.connection.send(new ClientboundSoundPacket(growl, SoundSource.HOSTILE, at.x, at.y, at.z, volume, 0.45F, seed));
        }
    }

    @Nullable
    public static WitherstormEntity current(ServerLevel level) {
        WitherstormEntity storm = runtime(level).storm;
        return storm != null && !storm.isRemoved() ? storm : null;
    }

    @Nullable
    public static WitherstormEntity forceSpawn(ServerLevel level, @Nullable ServerPlayer near, int stage) {
        Runtime rt = runtime(level);
        despawn(level, rt);
        data(level).blockedUntil = 0L;
        return spawn(level, rt, near, stage);
    }

    public static boolean remove(ServerLevel level) {
        Runtime rt = runtime(level);
        boolean had = rt.storm != null;
        despawn(level, rt);
        Data data = data(level);
        data.stage = 0;
        data.health = -1.0F;
        data.hunger = 0.0D;
        data.blockedUntil = level.getGameTime() + respawnTicks();
        data.setDirty();
        return had;
    }

    public static boolean setStage(ServerLevel level, int stage) {
        WitherstormEntity storm = current(level);
        Data data = data(level);
        data.stage = stage;
        data.setDirty();
        if (storm == null) return false;
        storm.setStage(stage);
        remember(level, storm);
        return true;
    }

    public static long minutesUntilSpawn(ServerLevel level) {
        long left = data(level).blockedUntil - level.getGameTime();
        return left <= 0L ? 0L : (left + 1199L) / 1200L;
    }

    public static void clear() {
        RUNTIME.clear();
    }
}
