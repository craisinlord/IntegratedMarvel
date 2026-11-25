package com.craisinlord.integrated_superheroes.voidworld.pruning;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.Util;
import java.util.concurrent.CompletableFuture;
import com.craisinlord.integrated_superheroes.compat.AntarchyDimensionalTear;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletFx;
import com.craisinlord.integrated_superheroes.voidworld.VoidConfig;
import com.craisinlord.integrated_superheroes.voidworld.VoidPrunePayload;
import com.craisinlord.integrated_superheroes.voidworld.VoidTier;
import com.craisinlord.integrated_superheroes.voidworld.VoidWorld;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

public final class VoidPruning {
    private static final int[] DEFAULT_INTERVALS = {150, 120, 100, 80, 65, 50};
    private static final DustParticleOptions RING = new DustParticleOptions(new Vector3f(1.0F, 0.42F, 0.05F), 2.5F);
    private static final DustParticleOptions RING_INNER = new DustParticleOptions(new Vector3f(1.0F, 0.75F, 0.25F), 1.6F);
    private static final double RING_VISIBLE = 192.0D;
    private static final double EFFECT_VISIBLE = 320.0D;
    private static final double FALLBACK_VISIBLE = 288.0D;
    private static final long WARM_TICKS = 200L;
    private static final Map<ServerLevel, Runtime> RUNTIME = new WeakHashMap<>();

    private VoidPruning() {
    }

    private static final class Runtime {
        final Map<UUID, Long> next = new HashMap<>();
        final List<Drop> drops = new ArrayList<>();
        CompletableFuture<PrunedPiece.Pick> warm;
        int warmTier = -1;
    }

    static final class Drop {
        final int warnTicks;
        final double startY;
        final Vec3 tear;
        PrunedPiece piece;
        int ground;
        int fallTicks;
        int age;
        Entity tearEntity;
        PrunedChunkEntity chunk;
        PrunedLanding landing;
        int linger;
        boolean done;

        Drop(PrunedPiece piece, int warnTicks, double startY, Vec3 tear, int ground) {
            this.piece = piece;
            this.warnTicks = warnTicks;
            this.startY = startY;
            this.tear = tear;
            this.ground = ground;
        }

        void finish() {
            this.done = true;
            if (this.chunk != null && !this.chunk.isRemoved()) {
                this.chunk.discard();
            }
        }

        void cancel() {
            this.finish();
            if (this.tearEntity != null && !this.tearEntity.isRemoved()) {
                this.tearEntity.discard();
            }
        }
    }

    private static Runtime runtime(ServerLevel level) {
        return RUNTIME.computeIfAbsent(level, key -> new Runtime());
    }

    private static boolean enabled() {
        return !VoidConfig.SPEC.isLoaded() || VoidConfig.PRUNE_ENABLED.get();
    }

    private static int warnTicks() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_WARN_TICKS.get() : 100;
    }

    private static int maxConcurrent() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_MAX_CONCURRENT.get() : 3;
    }

    private static int tearHeight() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_TEAR_HEIGHT.get() : 135;
    }

    private static int minDistance() {
        return VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_MIN_DISTANCE.get() : 24;
    }

    private static int maxDistance() {
        int max = VoidConfig.SPEC.isLoaded() ? VoidConfig.PRUNE_MAX_DISTANCE.get() : 80;
        return Math.max(max, minDistance() + 4);
    }

    private static long interval(int tier, RandomSource random) {
        int seconds = DEFAULT_INTERVALS[VoidTier.clamp(tier)];
        double jitter = 0.25D;
        if (VoidConfig.SPEC.isLoaded()) {
            List<? extends Integer> list = VoidConfig.PRUNE_INTERVALS.get();
            if (!list.isEmpty()) {
                seconds = list.get(Math.min(VoidTier.clamp(tier), list.size() - 1));
            }
            jitter = VoidConfig.PRUNE_JITTER.get();
        }
        double scale = 1.0D + (random.nextDouble() * 2.0D - 1.0D) * jitter;
        return Math.max(200L, Math.round(seconds * 20.0D * scale));
    }

    public static void tick(ServerLevel level) {
        Runtime rt = runtime(level);
        Iterator<Drop> iterator = rt.drops.iterator();
        while (iterator.hasNext()) {
            Drop drop = iterator.next();
            tickDrop(level, drop);
            if (drop.done) {
                iterator.remove();
            }
        }
        long now = level.getGameTime();
        if (now % 20L != 0L) {
            return;
        }
        if (!enabled()) {
            rt.next.clear();
            return;
        }
        Set<UUID> present = new HashSet<>();
        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isSpectator()) {
                continue;
            }
            UUID id = player.getUUID();
            present.add(id);
            Long next = rt.next.get(id);
            if (next == null) {
                rt.next.put(id, now + interval(VoidTier.forPlayer(player), level.random));
                continue;
            }
            if (now < next) {
                if (next - now <= WARM_TICKS) {
                    warm(level, rt, VoidTier.forPlayer(player));
                }
                continue;
            }
            if (rt.drops.size() >= maxConcurrent()) {
                rt.next.put(id, now + 200L);
                continue;
            }
            if (start(level, player, false, false) == null) {
                rt.next.put(id, now + 60L);
                continue;
            }
            long interval = interval(VoidTier.forPlayer(player), level.random);
            rt.next.put(id, now + interval);
            spreadOut(level, rt, player, now + interval / 2L);
        }
        rt.next.keySet().retainAll(present);
    }

    private static void spreadOut(ServerLevel level, Runtime rt, ServerPlayer source, long earliest) {
        double range = maxDistance() * 1.5D;
        for (ServerPlayer other : level.players()) {
            if (other == source || other.distanceToSqr(source) > range * range) {
                continue;
            }
            Long next = rt.next.get(other.getUUID());
            if (next != null && next < earliest) {
                rt.next.put(other.getUUID(), earliest);
            }
        }
    }

    public static PrunedPiece force(ServerLevel level, ServerPlayer player, boolean here) {
        return start(level, player, here, true);
    }

    private static PrunedPiece start(ServerLevel level, ServerPlayer player, boolean here, boolean now) {
        if (!VoidWorld.isVoid(level)) {
            return null;
        }
        Runtime rt = runtime(level);
        RandomSource random = level.random;
        int tier = VoidTier.forPlayer(player);
        PrunedPiece.Pick pick;
        if (now) {
            pick = PrunedPiece.pick(level.getServer().getResourceManager(), tier, random);
        } else {
            warm(level, rt, tier);
            if (!rt.warm.isDone()) {
                return null;
            }
            pick = rt.warm.isCompletedExceptionally() ? null : rt.warm.join();
            rt.warm = null;
        }
        if (pick == null) {
            return null;
        }
        int radius = pick.radius();
        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            double distance = here
                    ? radius + 3.0D + random.nextDouble() * 2.0D
                    : minDistance() + radius * 0.5D + random.nextDouble() * Math.max(1.0D, maxDistance() - minDistance());
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            if (!clearOfPlayers(level, x, z, here ? radius : radius + 1) || !clearOfDrops(rt, x, z, radius)) {
                continue;
            }
            int ground = PrunedPiece.groundAt(level, x, z, radius);
            if (ground == Integer.MIN_VALUE) {
                continue;
            }
            PrunedPiece piece = PrunedPiece.prepare(level, pick, tier, new BlockPos(x, ground, z), ground);
            if (piece == null) {
                return null;
            }
            begin(level, rt, piece, ground, random);
            return piece;
        }
        return null;
    }

    private static void warm(ServerLevel level, Runtime rt, int tier) {
        if (rt.warm != null && rt.warmTier == tier) {
            return;
        }
        ResourceManager manager = level.getServer().getResourceManager();
        long seed = level.random.nextLong();
        rt.warmTier = tier;
        rt.warm = CompletableFuture.supplyAsync(() -> PrunedPiece.pick(manager, tier, RandomSource.create(seed)), Util.backgroundExecutor());
    }

    private static boolean clearOfPlayers(ServerLevel level, int x, int z, int radius) {
        for (ServerPlayer other : level.players()) {
            if (other.isSpectator()) {
                continue;
            }
            double dx = other.getX() - (x + 0.5D);
            double dz = other.getZ() - (z + 0.5D);
            if (dx * dx + dz * dz < radius * radius) {
                return false;
            }
        }
        return true;
    }

    private static boolean clearOfDrops(Runtime rt, int x, int z, int radius) {
        for (Drop drop : rt.drops) {
            BlockPos centre = drop.piece.centre();
            double dx = centre.getX() - x;
            double dz = centre.getZ() - z;
            double gap = drop.piece.radius() + radius + 8.0D;
            if (dx * dx + dz * dz < gap * gap) {
                return false;
            }
        }
        return true;
    }

    private static void begin(ServerLevel level, Runtime rt, PrunedPiece piece, int ground, RandomSource random) {
        int warn = warnTicks();
        BlockPos centre = piece.centre();
        double height = tearHeight() + (random.nextDouble() * 2.0D - 1.0D) * 15.0D;
        double startY = Math.min(ground + height, level.getMaxBuildHeight() - piece.box().getYSpan() - 2.0D);
        Vec3 tear = new Vec3(centre.getX() + 0.5D, startY + piece.box().getYSpan() * 0.5D, centre.getZ() + 0.5D);
        Drop drop = new Drop(piece, warn, startY, tear, ground);
        rt.drops.add(drop);
        Vec3 landing = new Vec3(centre.getX() + 0.5D, ground, centre.getZ() + 0.5D);
        VoidPrunePayload.broadcast(level, landing, piece.tier(), piece.seed(), warn);
        int fallTicks = PrunedChunkEntity.ticksToFall(startY - piece.box().minY());
        if (ModCompat.ANTARCHY) {
            try {
                Entity entity = AntarchyDimensionalTear.create(level, tear, random.nextFloat() * 360.0F, warn + fallTicks + 40);
                if (entity != null && level.addFreshEntity(entity)) {
                    drop.tearEntity = entity;
                }
            } catch (RuntimeException | LinkageError ignored) {
                drop.tearEntity = null;
            }
        }
        level.playSound(null, tear.x, tear.y, tear.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.AMBIENT, 6.0F, 0.6F);
        level.playSound(null, landing.x, landing.y, landing.z, SoundEvents.PORTAL_TRIGGER, SoundSource.AMBIENT, 4.0F, 0.5F);
    }

    private static void tickDrop(ServerLevel level, Drop drop) {
        drop.age++;
        BlockPos centre = drop.piece.centre();
        if (!level.hasChunk(centre.getX() >> 4, centre.getZ() >> 4)) {
            drop.cancel();
            return;
        }
        if (drop.landing != null) {
            if (!drop.landing.finished()) {
                drop.landing.step(level);
            } else if (++drop.linger > 2) {
                drop.finish();
            }
            return;
        }
        if (drop.age < drop.warnTicks) {
            float progress = drop.age / (float) drop.warnTicks;
            if (drop.tearEntity == null && drop.age % 2 == 0) {
                fallbackTear(level, drop);
            }
            if (drop.age % 4 == 0) {
                ring(level, drop, progress);
            }
            if (drop.age % 20 == 0) {
                level.playSound(null, centre.getX() + 0.5D, drop.ground + 1.0D, centre.getZ() + 0.5D,
                        SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.AMBIENT, 3.0F, 0.5F + progress * 1.2F);
            }
            return;
        }
        if (drop.chunk == null) {
            if (!resettle(level, drop)) {
                drop.cancel();
                return;
            }
            PrunedChunkEntity chunk = PrunedChunkEntity.create(level, drop.piece, drop, drop.startY);
            if (!level.addFreshEntity(chunk)) {
                drop.cancel();
                return;
            }
            drop.chunk = chunk;
            drop.fallTicks = PrunedChunkEntity.ticksToFall(drop.startY - drop.piece.box().minY());
            level.playSound(null, drop.tear.x, drop.tear.y, drop.tear.z, SoundEvents.WITHER_SHOOT, SoundSource.AMBIENT, 6.0F, 0.4F);
            return;
        }
        int fall = drop.age - drop.warnTicks;
        if (!drop.chunk.isRemoved()) {
            drop.chunk.setFallAge(fall);
        }
        if (fall < drop.fallTicks) {
            if (drop.age % 4 == 0) {
                ring(level, drop, 1.0F);
            }
            if (drop.tearEntity == null && drop.age % 4 == 0 && fall < 40) {
                fallbackTear(level, drop);
            }
            return;
        }
        if (!PrunedLanding.canLand(level, drop.piece)) {
            drop.cancel();
            return;
        }
        drop.landing = new PrunedLanding(drop.piece, drop.chunk);
        drop.landing.impact(level);
        drop.landing.step(level);
    }

    private static boolean resettle(ServerLevel level, Drop drop) {
        BlockPos centre = drop.piece.centre();
        int ground = PrunedPiece.groundAt(level, centre.getX(), centre.getZ(), drop.piece.radius());
        if (ground == Integer.MIN_VALUE) {
            return false;
        }
        if (ground != drop.ground) {
            drop.piece = drop.piece.shift(ground - drop.ground);
            drop.ground = ground;
        }
        return drop.startY > drop.piece.box().minY() + 1.0D;
    }

    private static void fallbackTear(ServerLevel level, Drop drop) {
        Vec3 at = drop.tear;
        double spread = Math.max(3.0D, drop.piece.radius() * 0.6D);
        GauntletFx.farParticles(level, ParticleTypes.REVERSE_PORTAL, at, 24, spread, 0.6D, spread, 0.05D, FALLBACK_VISIBLE);
        GauntletFx.farParticles(level, ParticleTypes.PORTAL, at, 16, spread * 0.8D, 0.4D, spread * 0.8D, 0.6D, FALLBACK_VISIBLE);
        if (drop.age % 10 == 0) {
            GauntletFx.farParticles(level, ParticleTypes.END_ROD, at, 6, spread * 0.5D, 0.2D, spread * 0.5D, 0.02D, FALLBACK_VISIBLE);
        }
        if (drop.age % 40 == 0) {
            level.playSound(null, at.x, at.y, at.z, SoundEvents.PORTAL_AMBIENT, SoundSource.AMBIENT, 6.0F, 0.5F);
        }
    }

    private static void ring(ServerLevel level, Drop drop, float progress) {
        BlockPos centre = drop.piece.centre();
        List<ServerPlayer> viewers = viewers(level, new Vec3(centre.getX() + 0.5D, drop.ground, centre.getZ() + 0.5D), RING_VISIBLE);
        if (viewers.isEmpty()) {
            return;
        }
        double base = drop.piece.radius() + 1.0D;
        double radius = base + (1.0F - progress) * 6.0D;
        int points = Mth.clamp((int) (radius * 2.0D), 16, 40);
        double phase = drop.age * 0.05D;
        for (int i = 0; i < points; i++) {
            double angle = phase + i * Math.PI * 2.0D / points;
            double x = centre.getX() + 0.5D + Math.cos(angle) * radius;
            double z = centre.getZ() + 0.5D + Math.sin(angle) * radius;
            int bx = Mth.floor(x);
            int bz = Mth.floor(z);
            double y = (level.hasChunk(bx >> 4, bz >> 4) ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, bx, bz) : drop.ground) + 0.15D;
            for (ServerPlayer player : viewers) {
                level.sendParticles(player, RING, true, x, y, z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            }
        }
        if (progress > 0.5F) {
            for (ServerPlayer player : viewers) {
                level.sendParticles(player, RING_INNER, true, centre.getX() + 0.5D, drop.ground + 0.5D, centre.getZ() + 0.5D,
                        6, base * 0.5D, 0.1D, base * 0.5D, 0.0D);
            }
        }
    }

    static List<ServerPlayer> viewers(ServerLevel level, Vec3 at, double range) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer player : level.players()) {
            double dx = player.getX() - at.x;
            double dz = player.getZ() - at.z;
            if (dx * dx + dz * dz < range * range) {
                out.add(player);
            }
        }
        return out;
    }

    static void send(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double dx, double dy, double dz, double speed) {
        for (ServerPlayer player : viewers(level, at, EFFECT_VISIBLE)) {
            level.sendParticles(player, particle, true, at.x, at.y, at.z, count, dx, dy, dz, speed);
        }
    }

    public static void clear() {
        RUNTIME.clear();
    }
}
