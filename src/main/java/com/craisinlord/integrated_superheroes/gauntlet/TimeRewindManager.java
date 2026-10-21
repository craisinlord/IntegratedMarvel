package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.villain.VillainBrain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeRewindManager {
    private static final int SAMPLE_INTERVAL = 5;
    private static final int PLAYBACK_TICKS = 20;
    private static final double RECORD_RADIUS = 48.0D;
    private static final int MAX_BLOCK_CHANGES = 50000;
    private static final int MAX_DEATHS = 512;
    private static final Map<UUID, FrameBuffer> FRAMES = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> RECORDERS = new ConcurrentHashMap<>();
    private static final Deque<BlockChange> BLOCKS = new ArrayDeque<>();
    private static final Deque<Death> DEATHS = new ArrayDeque<>();
    private static final List<Playback> PLAYBACKS = new ArrayList<>();
    private static final Set<UUID> REWINDING = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Long> LOOP_READY = new ConcurrentHashMap<>();

    private TimeRewindManager() {
    }

    private static long window() {
        return GauntletConfig.REWIND_SECONDS.get() * 20L;
    }

    public static float fill(ServerPlayer player) {
        Long since = RECORDERS.get(player.getUUID());
        if (since == null) return 0.0F;
        return Math.clamp((player.server.overworld().getGameTime() - since) / (float) window(), 0.0F, 1.0F);
    }

    private static boolean recordsTime(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        return !gauntlet.isEmpty() && InfinityGauntletStones.hasStone(gauntlet, "time") && GauntletConfig.isEnabled("time", 4);
    }

    private static boolean recorded(Level level, Vec3 position) {
        if (!(level instanceof ServerLevel serverLevel) || RECORDERS.isEmpty()) return false;
        for (ServerPlayer player : serverLevel.players()) {
            if (RECORDERS.containsKey(player.getUUID()) && player.distanceToSqr(position) <= RECORD_RADIUS * RECORD_RADIUS) return true;
        }
        return false;
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (recordsTime(player)) {
                RECORDERS.putIfAbsent(player.getUUID(), now);
            } else {
                RECORDERS.remove(player.getUUID());
            }
        }
        RECORDERS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
        if (now % SAMPLE_INTERVAL == 0L) sample(server, now);
        long cutoff = now - window();
        for (Iterator<Map.Entry<UUID, FrameBuffer>> iterator = FRAMES.entrySet().iterator(); iterator.hasNext();) {
            FrameBuffer frames = iterator.next().getValue();
            frames.trimBefore(cutoff);
            if (frames.isEmpty()) iterator.remove();
        }
        synchronized (BLOCKS) {
            while (!BLOCKS.isEmpty() && (BLOCKS.peekFirst().tick() < cutoff || BLOCKS.size() > MAX_BLOCK_CHANGES)) BLOCKS.pollFirst();
        }
        synchronized (DEATHS) {
            while (!DEATHS.isEmpty() && (DEATHS.peekFirst().tick() < cutoff || DEATHS.size() > MAX_DEATHS)) DEATHS.pollFirst();
        }
        for (Iterator<Playback> iterator = PLAYBACKS.iterator(); iterator.hasNext();) {
            Playback playback = iterator.next();
            ServerLevel level = server.getLevel(playback.dimension());
            if (level == null || step(level, playback)) {
                iterator.remove();
                playback.tracks().forEach(track -> REWINDING.remove(track.id()));
            }
        }
    }

    private static void sample(MinecraftServer server, long now) {
        if (RECORDERS.isEmpty()) return;
        Set<UUID> seen = new HashSet<>();
        for (UUID recorderId : RECORDERS.keySet()) {
            ServerPlayer recorder = server.getPlayerList().getPlayer(recorderId);
            if (recorder == null) continue;
            ServerLevel level = recorder.serverLevel();
            for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, recorder.getBoundingBox().inflate(32.0D),
                    entity -> entity.isAlive() && !entity.isSpectator())) {
                if (!seen.add(living.getUUID()) || REWINDING.contains(living.getUUID())) continue;
                FRAMES.computeIfAbsent(living.getUUID(), key -> new FrameBuffer()).add(now, level.dimension(),
                        living.getX(), living.getY(), living.getZ(), living.getYRot(), living.getXRot(), living.getHealth());
            }
        }
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (GauntletGrief.probing()) return;
        if (!(event.getLevel() instanceof ServerLevel level) || !recorded(level, Vec3.atCenterOf(event.getPos()))) return;
        record(level, event.getPos(), event.getState());
    }

    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !recorded(level, Vec3.atCenterOf(event.getPos()))) return;
        synchronized (BLOCKS) {
            BLOCKS.addLast(new BlockChange(level.getServer().overworld().getGameTime(), level.dimension(), event.getPos().immutable(),
                    event.getBlockSnapshot().getState(), null));
        }
    }

    public static void onExplosion(ExplosionEvent.Detonate event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        Vec3 center = event.getExplosion().center();
        if (!recorded(level, center)) return;
        for (BlockPos pos : event.getAffectedBlocks()) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) record(level, pos, state);
        }
    }

    private static void record(ServerLevel level, BlockPos pos, BlockState state) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        CompoundTag data = blockEntity == null || blockEntity instanceof net.minecraft.world.Container ? null
                : blockEntity.saveWithFullMetadata(level.registryAccess());
        synchronized (BLOCKS) {
            BLOCKS.addLast(new BlockChange(level.getServer().overworld().getGameTime(), level.dimension(), pos.immutable(), state, data));
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity instanceof Player || !(entity.level() instanceof ServerLevel level) || !recorded(level, entity.position())) return;
        CompoundTag snapshot = SnapStone.rewindSnapshot();
        CompoundTag tag;
        if (snapshot != null) {
            tag = snapshot.copy();
            tag.remove("integrated_superheroes_dimension");
        } else {
            tag = new CompoundTag();
            if (!entity.saveAsPassenger(tag)) return;
            tag.remove("Passengers");
            stripLoot(tag);
        }
        tag.putFloat("Health", entity.getMaxHealth());
        tag.remove("DeathTime");
        VillainBrain.resetFight(tag);
        FrameBuffer frames = FRAMES.get(entity.getUUID());
        Vec3 position = frames == null || frames.isEmpty() ? entity.position() : frames.first().position();
        synchronized (DEATHS) {
            DEATHS.addLast(new Death(level.getServer().overworld().getGameTime(), level.dimension(), entity.getUUID(), position, tag));
        }
    }

    public static void stripLoot(CompoundTag tag) {
        for (String key : new String[]{"ArmorItems", "HandItems", "body_armor_item", "Items", "Inventory", "SaddleItem",
                "DecorItem", "ArmorItem", "Saddle", "ChestedHorse"}) {
            tag.remove(key);
        }
        tag.putBoolean("PersistenceRequired", true);
    }

    public static boolean tryTimeLoop(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty() || !GauntletAbilities.infinity(gauntlet) || !InfinityGauntletStones.hasStone(gauntlet, "time")) return false;
        long now = player.server.overworld().getGameTime();
        if (LOOP_READY.getOrDefault(player.getUUID(), 0L) > now) return false;
        FrameBuffer frames = FRAMES.get(player.getUUID());
        Frame origin = frames == null || frames.isEmpty() ? null : frames.first();
        LOOP_READY.put(player.getUUID(), now + GauntletConfig.TIME_LOOP_COOLDOWN.get());
        player.setHealth(Math.max(player.getMaxHealth() * 0.5F, origin == null ? 0.0F : origin.health()));
        player.removeAllEffects();
        player.clearFire();
        player.resetFallDistance();
        if (origin != null && origin.dimension().equals(player.serverLevel().dimension())) {
            player.connection.teleport(origin.position().x, origin.position().y, origin.position().z, origin.yRot(), origin.xRot());
        }
        ServerLevel level = player.serverLevel();
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0D, player.getZ(), 200, 0.6D, 1.0D, 0.6D, 0.3D);
        GauntletFx.sphere(level, player.position().add(0.0D, 1.0D, 0.0D), 2.5D, "time", 120);
        GauntletFx.sound(level, player.position(), SoundEvents.TOTEM_USE, 1.0F, 0.5F);
        player.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.time_loop"), true);
        return true;
    }

    public static void rewind(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        double radius = cast.infinity() ? 64.0D : 32.0D;
        Vec3 center = player.position();
        List<Track> tracks = new ArrayList<>();
        for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                Entity::isAlive)) {
            if (living == player && !cast.crouching()) continue;
            FrameBuffer frames = FRAMES.get(living.getUUID());
            if (frames == null || frames.size() < 2) continue;
            List<Frame> usable = frames.inDimension(level.dimension());
            if (usable.size() < 2) continue;
            tracks.add(new Track(living.getUUID(), usable));
            REWINDING.add(living.getUUID());
        }
        List<BlockChange> blocks = new ArrayList<>();
        synchronized (BLOCKS) {
            for (Iterator<BlockChange> iterator = BLOCKS.descendingIterator(); iterator.hasNext();) {
                BlockChange change = iterator.next();
                if (change.dimension().equals(level.dimension()) && Vec3.atCenterOf(change.pos()).distanceToSqr(center) <= radius * radius) {
                    blocks.add(change);
                    iterator.remove();
                }
            }
        }
        List<Death> deaths = new ArrayList<>();
        synchronized (DEATHS) {
            for (Iterator<Death> iterator = DEATHS.iterator(); iterator.hasNext();) {
                Death death = iterator.next();
                if (death.dimension().equals(level.dimension()) && death.position().distanceToSqr(center) <= radius * radius) {
                    deaths.add(death);
                    iterator.remove();
                }
            }
        }
        if (tracks.isEmpty() && blocks.isEmpty() && deaths.isEmpty()) {
            cast.message("time_rewind_empty");
            return;
        }
        PLAYBACKS.add(new Playback(level.dimension(), tracks, blocks, deaths, new int[1], center, radius));
        GauntletFx.cast(player, "time", SoundEvents.BEACON_DEACTIVATE, 0.3F);
        GauntletFx.shake(level, center, 0.8F, PLAYBACK_TICKS, (float) radius);
        cast.message("time_rewind", tracks.size(), blocks.size(), deaths.size());
        cast.cooldown();
    }

    private static boolean step(ServerLevel level, Playback playback) {
        int step = playback.step()[0]++;
        double progress = Math.min(1.0D, step / (double) (PLAYBACK_TICKS - 1));
        for (Track track : playback.tracks()) {
            Entity entity = level.getEntity(track.id());
            if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
            int index = (int) Math.round((track.frames().size() - 1) * (1.0D - progress));
            Frame frame = track.frames().get(Math.clamp(index, 0, track.frames().size() - 1));
            move(living, frame);
            if (step % 2 == 0) {
                level.sendParticles(ParticleTypes.REVERSE_PORTAL, living.getX(), living.getY() + living.getBbHeight() * 0.5D,
                        living.getZ(), 6, 0.3D, 0.4D, 0.3D, 0.05D);
            }
        }
        if (step % 4 == 0) {
            GauntletFx.ring(level, playback.center(), Math.min(24.0D, playback.radius() * (1.0D - progress) + 2.0D), "time");
            GauntletFx.sound(level, playback.center(), SoundEvents.NOTE_BLOCK_HAT.value(), 1.0F, (float) (2.0D - progress * 1.5D));
        }
        if (progress < 1.0D) return false;
        for (Track track : playback.tracks()) {
            Entity entity = level.getEntity(track.id());
            if (entity instanceof LivingEntity living && living.isAlive()) {
                Frame oldest = track.frames().getFirst();
                living.setHealth(Math.min(living.getMaxHealth(), Math.max(1.0F, oldest.health())));
                living.clearFire();
            }
            FRAMES.remove(track.id());
        }
        List<BlockEditQueue.Edit> edits = new ArrayList<>(playback.blocks().size());
        for (BlockChange change : playback.blocks()) edits.add(new BlockEditQueue.Edit(change.pos(), change.state(), change.data()));
        BlockEditQueue.submit(level, edits, null, 3).withoutUndo().onComplete(job -> {
            revive(level, playback.deaths());
            GauntletFx.sound(level, playback.center(), SoundEvents.BELL_RESONATE, 2.0F, 0.5F);
        });
        return true;
    }

    private static void revive(ServerLevel level, List<Death> deaths) {
        for (Death death : deaths) {
            if (level.getEntity(death.id()) != null) continue;
            Entity revived = EntityType.loadEntityRecursive(death.data().copy(), level, entity -> {
                entity.moveTo(death.position().x, death.position().y, death.position().z, entity.getYRot(), entity.getXRot());
                return entity;
            });
            if (revived != null) {
                level.addFreshEntity(revived);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, revived.getX(), revived.getY() + 1.0D, revived.getZ(), 30,
                        0.4D, 0.6D, 0.4D, 0.3D);
            }
        }
    }

    private static void move(LivingEntity living, Frame frame) {
        Vec3 position = frame.position();
        if (living instanceof ServerPlayer player) {
            player.connection.teleport(position.x, position.y, position.z, frame.yRot(), frame.xRot());
        } else {
            living.moveTo(position.x, position.y, position.z, frame.yRot(), frame.xRot());
            living.setDeltaMovement(Vec3.ZERO);
            living.hurtMarked = true;
        }
        living.resetFallDistance();
    }

    public static void clearPlayer(UUID player) {
        RECORDERS.remove(player);
    }

    public static void clear() {
        FRAMES.clear();
        RECORDERS.clear();
        synchronized (BLOCKS) {
            BLOCKS.clear();
        }
        synchronized (DEATHS) {
            DEATHS.clear();
        }
        PLAYBACKS.clear();
        REWINDING.clear();
        LOOP_READY.clear();
    }

    private record Frame(long tick, ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot, float health) {
    }

    private static final class FrameBuffer {
        private long[] ticks = new long[8];
        private Object[] dimensions = new Object[8];
        private double[] positions = new double[24];
        private float[] values = new float[24];
        private int head;
        private int size;

        private void add(long tick, ResourceKey<Level> dimension, double x, double y, double z, float yRot, float xRot, float health) {
            if (size == ticks.length) grow();
            int slot = (head + size) % ticks.length;
            ticks[slot] = tick;
            dimensions[slot] = dimension;
            positions[slot * 3] = x;
            positions[slot * 3 + 1] = y;
            positions[slot * 3 + 2] = z;
            values[slot * 3] = yRot;
            values[slot * 3 + 1] = xRot;
            values[slot * 3 + 2] = health;
            size++;
        }

        private void grow() {
            int capacity = ticks.length * 2;
            long[] nextTicks = new long[capacity];
            Object[] nextDimensions = new Object[capacity];
            double[] nextPositions = new double[capacity * 3];
            float[] nextValues = new float[capacity * 3];
            for (int index = 0; index < size; index++) {
                int slot = (head + index) % ticks.length;
                nextTicks[index] = ticks[slot];
                nextDimensions[index] = dimensions[slot];
                System.arraycopy(positions, slot * 3, nextPositions, index * 3, 3);
                System.arraycopy(values, slot * 3, nextValues, index * 3, 3);
            }
            ticks = nextTicks;
            dimensions = nextDimensions;
            positions = nextPositions;
            values = nextValues;
            head = 0;
        }

        private void trimBefore(long cutoff) {
            while (size > 0 && ticks[head] < cutoff) {
                dimensions[head] = null;
                head = (head + 1) % ticks.length;
                size--;
            }
        }

        private boolean isEmpty() {
            return size == 0;
        }

        private int size() {
            return size;
        }

        private Frame first() {
            return frame(head);
        }

        @SuppressWarnings("unchecked")
        private Frame frame(int slot) {
            return new Frame(ticks[slot], (ResourceKey<Level>) dimensions[slot],
                    new Vec3(positions[slot * 3], positions[slot * 3 + 1], positions[slot * 3 + 2]),
                    values[slot * 3], values[slot * 3 + 1], values[slot * 3 + 2]);
        }

        private List<Frame> inDimension(ResourceKey<Level> dimension) {
            List<Frame> frames = new ArrayList<>(size);
            for (int index = 0; index < size; index++) {
                int slot = (head + index) % ticks.length;
                if (dimension.equals(dimensions[slot])) frames.add(frame(slot));
            }
            return frames;
        }
    }

    private record Track(UUID id, List<Frame> frames) {
    }

    private record BlockChange(long tick, ResourceKey<Level> dimension, BlockPos pos, BlockState state, CompoundTag data) {
    }

    private record Death(long tick, ResourceKey<Level> dimension, UUID id, Vec3 position, CompoundTag data) {
    }

    private record Playback(ResourceKey<Level> dimension, List<Track> tracks, List<BlockChange> blocks, List<Death> deaths,
                            int[] step, Vec3 center, double radius) {
    }
}
