package com.craisinlord.integrated_superheroes.voidrealm.variant;

import com.craisinlord.integrated_superheroes.portal.MarvelPortal;
import com.craisinlord.integrated_superheroes.compat.AntarchyDimensionalTear;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicVariants;
import com.craisinlord.integrated_superheroes.gauntlet.GauntletFx;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

public final class VariantTears {
    public static final String TEAR_TAG = "integrated_superheroes_variant_tear";
    public static final String ESCAPE_TRIED_TAG = "integrated_superheroes_variant_escape_tried";
    public static final int OPEN_TICKS = 24;
    private static final String NEXUS_READY_KEY = "integrated_superheroes_nexus_ready";
    private static final int ARRIVAL_LIFETIME = 72;
    private static final int ESCAPE_LIFETIME = 130;
    private static final int ESCAPE_COOLDOWN = 400;
    private static final int ESCAPE_TIMEOUT = 120;
    private static final int RECENT_HIT_TICKS = 100;
    private static final double NEXUS_PLAYER_RANGE = 48.0D;
    private static final double LIFT = 0.25D;
    private static final double NEXUS_LIFT = 0.4D;
    private static final double CENTER = 1.3D;
    private static final double EXIT = 1.2D;
    private static final Vector3f HERO_A = new Vector3f(0.35F, 0.62F, 1.0F);
    private static final Vector3f HERO_B = new Vector3f(1.0F, 0.82F, 0.3F);
    private static final Vector3f VILLAIN_A = new Vector3f(0.95F, 0.16F, 0.18F);
    private static final Vector3f VILLAIN_B = new Vector3f(0.3F, 0.95F, 0.35F);
    private static final Vector3f NEXUS_A = new Vector3f(0.62F, 0.25F, 0.95F);
    private static final Vector3f NEXUS_B = new Vector3f(0.2F, 0.06F, 0.35F);
    private static final double FAR_RANGE = 160.0D;
    private static final int MAX_TEARS = 64;
    private static final long MAX_FROZEN = 6000L;
    private static final int VERIFY_TICKS = 40;
    private static final String DATA_NAME = "integrated_superheroes_variant_tears";
    private static final SavedData.Factory<Store> FACTORY = new SavedData.Factory<>(Store::new, Store::load, null);
    private static final Map<ResourceKey<Level>, Store> STORES = new HashMap<>();
    private static final Set<UUID> LIVE = new HashSet<>();
    private static final Map<UUID, Escape> ESCAPES = new HashMap<>();
    private static final Map<ResourceKey<Level>, Long> ESCAPE_READY = new HashMap<>();

    private VariantTears() {
    }

    private static final class Tear {
        final Vec3 base;
        final Vec3 ground;
        final Vec3 facing;
        long opened;
        long closes;
        long expires;
        long frozenAt = -1L;
        int verify;
        final boolean nexus;
        final VariantAlignment alignment;
        final List<UUID> visuals = new ArrayList<>();
        final List<Arrival> arrivals = new ArrayList<>();
        final List<Visual> queued = new ArrayList<>();
        UUID owner;
        int tier;
        int broodLeft;
        long nextWave;
        long waveGap;
        long nextGrowl;

        Tear(Vec3 base, Vec3 ground, Vec3 facing, long opened, long closes, boolean nexus, @Nullable VariantAlignment alignment) {
            this.base = base;
            this.ground = ground;
            this.facing = facing;
            this.opened = opened;
            this.closes = closes;
            this.expires = closes + MAX_FROZEN;
            this.nexus = nexus;
            this.alignment = alignment;
        }

        void delay(long ticks) {
            opened += ticks;
            closes = Math.min(closes + ticks, expires);
            nextWave += ticks;
            nextGrowl += ticks;
            for (Arrival arrival : arrivals) {
                arrival.at += ticks;
            }
            for (Visual visual : queued) {
                visual.due += ticks;
            }
        }

        CompoundTag save() {
            CompoundTag tag = new CompoundTag();
            putVec(tag, "Base", base);
            putVec(tag, "Ground", ground);
            putVec(tag, "Facing", facing);
            tag.putLong("Opened", opened);
            tag.putLong("Closes", closes);
            tag.putLong("Expires", expires);
            tag.putLong("FrozenAt", frozenAt);
            tag.putBoolean("Nexus", nexus);
            if (alignment != null) {
                tag.putString("Alignment", alignment.key());
            }
            ListTag ids = new ListTag();
            for (int i = 0; i < Math.min(visuals.size(), 4); i++) {
                ids.add(NbtUtils.createUUID(visuals.get(i)));
            }
            tag.put("Visuals", ids);
            ListTag pending = new ListTag();
            for (int i = 0; i < Math.min(arrivals.size(), 4); i++) {
                Arrival arrival = arrivals.get(i);
                CompoundTag entry = new CompoundTag();
                entry.put("Profile", arrival.profile.save());
                putVec(entry, "Spot", arrival.spot);
                entry.putLong("At", arrival.at);
                pending.add(entry);
            }
            tag.put("Arrivals", pending);
            ListTag later = new ListTag();
            for (int i = 0; i < Math.min(queued.size(), 4); i++) {
                Visual visual = queued.get(i);
                CompoundTag entry = new CompoundTag();
                putVec(entry, "At", visual.at);
                putVec(entry, "Facing", visual.facing);
                entry.putLong("Due", visual.due);
                entry.putInt("Lifetime", visual.lifetime);
                later.add(entry);
            }
            tag.put("Queued", later);
            if (owner != null) {
                tag.putUUID("Owner", owner);
            }
            tag.putInt("Tier", tier);
            tag.putInt("BroodLeft", broodLeft);
            tag.putLong("NextWave", nextWave);
            tag.putLong("WaveGap", waveGap);
            tag.putLong("NextGrowl", nextGrowl);
            return tag;
        }

        @Nullable
        static Tear load(CompoundTag tag) {
            Vec3 facing = getVec(tag, "Facing");
            if (facing.lengthSqr() < 1.0E-4D) {
                return null;
            }
            VariantAlignment alignment = tag.contains("Alignment", Tag.TAG_STRING) ? VariantAlignment.byKey(tag.getString("Alignment")) : null;
            Tear tear = new Tear(getVec(tag, "Base"), getVec(tag, "Ground"), facing, tag.getLong("Opened"), tag.getLong("Closes"),
                    tag.getBoolean("Nexus"), alignment);
            tear.expires = tag.getLong("Expires");
            tear.frozenAt = tag.getLong("FrozenAt");
            ListTag ids = tag.getList("Visuals", Tag.TAG_INT_ARRAY);
            for (int i = 0; i < Math.min(ids.size(), 4); i++) {
                tear.visuals.add(NbtUtils.loadUUID(ids.get(i)));
            }
            ListTag pending = tag.getList("Arrivals", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(pending.size(), 4); i++) {
                CompoundTag entry = pending.getCompound(i);
                VariantProfile profile = VariantProfile.load(entry.getCompound("Profile"));
                if (profile != null) {
                    tear.arrivals.add(new Arrival(profile, getVec(entry, "Spot"), entry.getLong("At"), null));
                }
            }
            ListTag later = tag.getList("Queued", Tag.TAG_COMPOUND);
            for (int i = 0; i < Math.min(later.size(), 4); i++) {
                CompoundTag entry = later.getCompound(i);
                tear.queued.add(new Visual(getVec(entry, "At"), getVec(entry, "Facing"), entry.getLong("Due"), entry.getInt("Lifetime")));
            }
            if (tag.hasUUID("Owner")) {
                tear.owner = tag.getUUID("Owner");
            }
            tear.tier = tag.getInt("Tier");
            tear.broodLeft = Mth.clamp(tag.getInt("BroodLeft"), 0, 10);
            tear.nextWave = tag.getLong("NextWave");
            tear.waveGap = Math.max(30L, tag.getLong("WaveGap"));
            tear.nextGrowl = tag.getLong("NextGrowl");
            return tear;
        }

        Vec3 center() {
            return base.add(0.0D, CENTER, 0.0D);
        }

        Vec3 right() {
            return new Vec3(-facing.z, 0.0D, facing.x);
        }

        boolean open(long now) {
            return now >= opened + OPEN_TICKS && now < closes - 12L;
        }
    }

    private static final class Arrival {
        final VariantProfile profile;
        final Vec3 spot;
        long at;
        @Nullable
        final Consumer<Mob> then;

        Arrival(VariantProfile profile, Vec3 spot, long at, @Nullable Consumer<Mob> then) {
            this.profile = profile;
            this.spot = spot;
            this.at = at;
            this.then = then;
        }
    }

    private static final class Visual {
        final Vec3 at;
        final Vec3 facing;
        long due;
        final int lifetime;

        Visual(Vec3 at, Vec3 facing, long due, int lifetime) {
            this.at = at;
            this.facing = facing;
            this.due = due;
            this.lifetime = lifetime;
        }
    }

    private static final class Store extends SavedData {
        final List<Tear> tears = new ArrayList<>();

        static Store load(CompoundTag tag, HolderLookup.Provider registries) {
            Store store = new Store();
            ListTag list = tag.getList("Tears", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && store.tears.size() < MAX_TEARS; i++) {
                Tear tear = Tear.load(list.getCompound(i));
                if (tear != null) {
                    store.tears.add(tear);
                }
            }
            return store;
        }

        @Override
        public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
            ListTag list = new ListTag();
            int from = Math.max(0, tears.size() - MAX_TEARS);
            for (int i = from; i < tears.size(); i++) {
                list.add(tears.get(i).save());
            }
            tag.put("Tears", list);
            return tag;
        }
    }

    private static void putVec(CompoundTag tag, String key, Vec3 vec) {
        ListTag list = new ListTag();
        list.add(DoubleTag.valueOf(vec.x));
        list.add(DoubleTag.valueOf(vec.y));
        list.add(DoubleTag.valueOf(vec.z));
        tag.put(key, list);
    }

    private static Vec3 getVec(CompoundTag tag, String key) {
        ListTag list = tag.getList(key, Tag.TAG_DOUBLE);
        return list.size() == 3 ? new Vec3(list.getDouble(0), list.getDouble(1), list.getDouble(2)) : Vec3.ZERO;
    }

    private static Store store(ServerLevel level) {
        Store store = STORES.get(level.dimension());
        if (store == null) {
            store = level.getDataStorage().computeIfAbsent(FACTORY, DATA_NAME);
            STORES.put(level.dimension(), store);
            restore(level, store);
        }
        return store;
    }

    private static List<Tear> tears(ServerLevel level) {
        return store(level).tears;
    }

    private static void dirty(ServerLevel level) {
        store(level).setDirty();
    }

    private static void restore(ServerLevel level, Store store) {
        long now = level.getGameTime();
        int before = store.tears.size();
        store.tears.removeIf(tear -> now >= tear.expires || (tear.frozenAt < 0L && now >= tear.closes));
        while (store.tears.size() > MAX_TEARS) {
            store.tears.remove(0);
        }
        for (Tear tear : store.tears) {
            LIVE.addAll(tear.visuals);
            tear.verify = VERIFY_TICKS;
        }
        if (store.tears.size() != before) {
            store.setDirty();
        }
    }

    private record Escape(Mob mob, Tear tear, ResourceKey<Level> dimension, long deadline) {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(VariantTears::onLevelTick);
        NeoForge.EVENT_BUS.addListener(VariantTears::onJoin);
        NeoForge.EVENT_BUS.addListener(VariantTears::onChangeTarget);
        NeoForge.EVENT_BUS.addListener(VariantTears::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(VariantTears::onServerStopped);
    }

    public static boolean arrive(ServerLevel level, Vec3 spot, int tier, @Nullable Vec3 faceToward) {
        return arrive(level, spot, tier, faceToward, null);
    }

    public static boolean arrive(ServerLevel level, Vec3 spot, int tier, @Nullable Vec3 faceToward,
                                 @Nullable Consumer<Mob> then) {
        VariantProfile profile = VariantPool.get(level.getServer()).roll(level.random, tier, null);
        return profile != null && arrive(level, spot, profile, faceToward, then);
    }

    public static boolean arrive(ServerLevel level, Vec3 spot, VariantProfile profile, @Nullable Vec3 faceToward, @Nullable Consumer<Mob> then) {
        if (!level.isPositionEntityTicking(BlockPos.containing(spot))) {
            return false;
        }
        Vec3 facing = facing(level, spot, faceToward);
        Vec3 base = spot.subtract(facing.scale(EXIT)).add(0.0D, LIFT, 0.0D);
        if (!clear(level, base)) {
            base = spot.add(0.0D, LIFT, 0.0D);
        }
        long now = level.getGameTime();
        Tear tear = new Tear(base, spot, facing, now, now + ARRIVAL_LIFETIME + 2L, false, profile.alignment());
        tear.arrivals.add(new Arrival(profile, spot, now + OPEN_TICKS, then));
        return open(level, tear, ARRIVAL_LIFETIME);
    }

    public static int pending(ServerLevel level) {
        int count = 0;
        for (Tear tear : tears(level)) {
            if (tear.frozenAt < 0L) {
                count += tear.arrivals.size();
            }
        }
        return count;
    }

    static boolean tryNexus(ServerLevel level, ServerPlayer player) {
        double chance = VariantConfig.NEXUS_CHANCE.get();
        if (chance <= 0.0D || level.random.nextDouble() >= chance) {
            return false;
        }
        int tier = VariantTiers.forPlayer(player);
        if (tier < VariantConfig.NEXUS_MIN_TIER.get()) {
            return false;
        }
        CompoundTag data = player.getPersistentData();
        long clock = level.getServer().overworld().getGameTime();
        if (data.getLong(NEXUS_READY_KEY) > clock || nexusNear(level, player.position(), 96.0D)
                || VoidVariants.loaded(level).size() >= broodCap() || !openNexus(level, player, tier)) {
            return false;
        }
        data.putLong(NEXUS_READY_KEY, clock + VariantConfig.NEXUS_COOLDOWN_MINUTES.get() * 1200L);
        return true;
    }

    public static boolean openNexus(ServerLevel level, ServerPlayer player, int tier) {
        Vec3 spot = VariantSpawner.surfaceSpot(level, player.position(), 10.0D, 18.0D);
        if (spot == null) {
            return false;
        }
        Vec3 facing = facing(level, spot, player.position());
        Vec3 base = spot.add(0.0D, LIFT + NEXUS_LIFT, 0.0D);
        int lifetime = VariantConfig.NEXUS_DURATION_SECONDS.get() * 20;
        long now = level.getGameTime();
        Tear tear = new Tear(base, spot, facing, now, now + lifetime + 2L, true, null);
        tear.tier = tier;
        tear.broodLeft = Mth.clamp(1 + tier * 3 / 2 + level.random.nextInt(3), 4, 10);
        int waves = Math.max(1, (tear.broodLeft + 1) / 2 + 1);
        tear.waveGap = Math.max(30L, (lifetime - OPEN_TICKS - 80L) / waves);
        tear.nextWave = now + OPEN_TICKS + 10L;
        tear.nextGrowl = now + 30L;
        return open(level, tear, lifetime);
    }

    private static int broodCap() {
        return VariantConfig.MAX_LOADED.get() + VariantConfig.BROOD_EXTRA.get();
    }

    private static boolean nexusNear(ServerLevel level, Vec3 at, double radius) {
        for (Tear tear : tears(level)) {
            if (tear.nexus && tear.base.distanceToSqr(at) <= radius * radius) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 facing(ServerLevel level, Vec3 from, @Nullable Vec3 toward) {
        if (toward != null) {
            Vec3 flat = new Vec3(toward.x - from.x, 0.0D, toward.z - from.z);
            if (flat.lengthSqr() > 1.0E-4D) {
                return flat.normalize();
            }
        }
        double angle = level.random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private static boolean clear(ServerLevel level, Vec3 base) {
        return level.hasChunkAt(BlockPos.containing(base)) && level.noCollision(AABB.ofSize(base.add(0.0D, CENTER, 0.0D), 0.8D, 1.8D, 0.8D));
    }

    private static boolean standable(ServerLevel level, Vec3 feet) {
        BlockPos pos = BlockPos.containing(feet);
        if (!level.isPositionEntityTicking(pos) || !level.noCollision(AABB.ofSize(feet.add(0.0D, 0.95D, 0.0D), 0.7D, 1.8D, 0.7D))) {
            return false;
        }
        for (int i = 0; i <= 3; i++) {
            BlockPos below = pos.below(i);
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static float yaw(Vec3 facing) {
        return (float) Math.toDegrees(Math.atan2(-facing.x, facing.z));
    }

    private static float pitch(@Nullable VariantAlignment alignment) {
        if (alignment == null) {
            return 0.55F;
        }
        return alignment == VariantAlignment.HERO ? 1.3F : 0.7F;
    }

    private static DustParticleOptions dust(@Nullable VariantAlignment alignment, boolean primary, float scale) {
        Vector3f color;
        if (alignment == null) {
            color = primary ? NEXUS_A : NEXUS_B;
        } else if (alignment == VariantAlignment.HERO) {
            color = primary ? HERO_A : HERO_B;
        } else {
            color = primary ? VILLAIN_A : VILLAIN_B;
        }
        return new DustParticleOptions(color, scale);
    }

    private static SoundEvent emergeSound() {
        return ModCompat.ANTARCHY ? AntarchyDimensionalTear.emergeSound() : SoundEvents.ENDERMAN_TELEPORT;
    }

    private static boolean open(ServerLevel level, Tear tear, int lifetime) {
        Store store = store(level);
        if (store.tears.size() >= MAX_TEARS) {
            return false;
        }
        store.tears.add(tear);
        store.setDirty();
        BlockPos at = BlockPos.containing(tear.center());
        if (ModCompat.ANTARCHY || MarvelPortal.enabled()) {
            visual(level, tear, tear.base, tear.facing, lifetime);
            if (tear.nexus) {
                tear.queued.add(side(tear, 1, tear.opened + 8L, lifetime - 8));
                tear.queued.add(side(tear, -1, tear.opened + 14L, lifetime - 14));
            }
        } else {
            level.playSound(null, at, SoundEvents.PORTAL_TRIGGER, SoundSource.HOSTILE, tear.nexus ? 1.4F : 0.45F, pitch(tear.alignment));
        }
        if (tear.nexus) {
            level.playSound(null, at, SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2.0F, pitch(null));
        }
        return true;
    }

    private static Visual side(Tear tear, int sign, long due, int lifetime) {
        Vec3 right = tear.right();
        return new Visual(tear.base.add(right.scale(1.7D * sign)).add(0.0D, sign > 0 ? -0.35D : 0.3D, 0.0D),
                tear.facing.yRot((float) Math.toRadians(22.0D * sign)), due, lifetime);
    }

    private static void verify(ServerLevel level, Tear tear, long now) {
        BlockPos at = BlockPos.containing(tear.base);
        if (!level.areEntitiesLoaded(ChunkPos.asLong(at))) {
            tear.verify = 20;
            return;
        }
        for (UUID id : tear.visuals) {
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()
                    && (!(entity instanceof MarvelPortal portal) || portal.style() != MarvelPortal.HIDDEN)) {
                return;
            }
        }
        boolean changed = !tear.visuals.isEmpty();
        for (UUID id : tear.visuals) {
            if (level.getEntity(id) instanceof MarvelPortal portal) {
                portal.discard();
            }
        }
        tear.visuals.forEach(LIVE::remove);
        tear.visuals.clear();
        int remaining = (int) (tear.closes - now - 2L);
        if ((ModCompat.ANTARCHY || MarvelPortal.enabled()) && remaining > 20) {
            visual(level, tear, tear.base, tear.facing, remaining);
            if (tear.nexus && tear.queued.isEmpty()) {
                Visual left = side(tear, 1, now, remaining);
                Visual right = side(tear, -1, now, remaining);
                visual(level, tear, left.at, left.facing, remaining);
                visual(level, tear, right.at, right.facing, remaining);
            }
            changed = true;
        }
        if (changed) {
            dirty(level);
        }
    }

    private static void visual(ServerLevel level, Tear tear, Vec3 at, Vec3 facing, int lifetime) {
        if (MarvelPortal.enabled()) {
            Vec3 center = at.add(0, 1, 0);
            MarvelPortal portal = MarvelPortal.create(level, center, facing, MarvelPortal.SPIDERVERSE, lifetime);
            Vec3 view = center.add(facing.scale(-6));
            if (trackVisual(level, tear, portal) && level.isPositionEntityTicking(BlockPos.containing(view))) {
                MarvelPortal target = MarvelPortal.create(level, view, facing.scale(-1), MarvelPortal.HIDDEN, lifetime);
                if (trackVisual(level, tear, target)) {
                    portal.linkTo(target);
                    target.linkTo(portal);
                }
            }
        } else if (ModCompat.ANTARCHY) {
            trackVisual(level, tear, AntarchyDimensionalTear.create(level, at, yaw(facing), lifetime));
        }
    }

    private static boolean trackVisual(ServerLevel level, Tear tear, Entity entity) {
        entity.addTag(TEAR_TAG);
        LIVE.add(entity.getUUID());
        if (level.addFreshEntity(entity)) {
            tear.visuals.add(entity.getUUID());
            return true;
        }
        LIVE.remove(entity.getUUID());
        entity.discard();
        return false;
    }

    private static void finish(ServerLevel level, Tear tear) {
        for (UUID id : tear.visuals) {
            LIVE.remove(id);
            Entity entity = level.getEntity(id);
            if (entity != null && entity.isAlive()) {
                entity.discard();
            }
        }
        tear.visuals.clear();
        tear.arrivals.clear();
        tear.queued.clear();
    }

    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !VariantConfig.ready()) {
            return;
        }
        long now = level.getGameTime();
        List<Tear> list = tears(level);
        if (!list.isEmpty()) {
            Iterator<Tear> it = list.iterator();
            List<Tear> live = new ArrayList<>(list.size());
            boolean removed = false;
            while (it.hasNext()) {
                Tear tear = it.next();
                if (now >= tear.expires || (tear.frozenAt < 0L && now >= tear.closes)) {
                    finish(level, tear);
                    it.remove();
                    removed = true;
                } else {
                    live.add(tear);
                }
            }
            if (removed) {
                dirty(level);
            }
            for (Tear tear : live) {
                tickTear(level, tear, now);
            }
        }
        if (!ESCAPES.isEmpty()) {
            tickEscapes(level, now);
        }
        if (now % 5L == 0L) {
            scanEscapes(level, now);
        }
    }

    private static void tickTear(ServerLevel level, Tear tear, long now) {
        if (!level.isPositionEntityTicking(BlockPos.containing(tear.ground))) {
            if (tear.frozenAt < 0L) {
                tear.frozenAt = now;
                dirty(level);
            }
            return;
        }
        if (tear.frozenAt >= 0L) {
            tear.delay(now - tear.frozenAt);
            tear.frozenAt = -1L;
            dirty(level);
            if (now >= tear.closes) {
                return;
            }
        }
        if (tear.verify > 0 && --tear.verify == 0) {
            verify(level, tear, now);
        }
        boolean changed = false;
        Iterator<Arrival> pending = tear.arrivals.iterator();
        while (pending.hasNext()) {
            Arrival arrival = pending.next();
            if (now < arrival.at || !level.isPositionEntityTicking(BlockPos.containing(arrival.spot))) {
                continue;
            }
            pending.remove();
            changed = true;
            Mob mob = emerge(level, tear, arrival.profile, arrival.spot, 0.0D);
            if (mob != null) {
                if (tear.owner == null) {
                    tear.owner = mob.getUUID();
                }
                if (arrival.then != null) {
                    arrival.then.accept(mob);
                }
            }
        }
        Iterator<Visual> queued = tear.queued.iterator();
        while (queued.hasNext()) {
            Visual next = queued.next();
            if (now >= next.due) {
                queued.remove();
                changed = true;
                visual(level, tear, next.at, next.facing, next.lifetime);
            }
        }
        if (changed) {
            dirty(level);
        }
        Vec3 center = tear.center();
        if (tear.nexus) {
            tickNexus(level, tear, now, center);
        }
        if (MarvelPortal.enabled() || now % 2L != 0L) {
            return;
        }
        double grow = Mth.clamp((now - tear.opened) / (double) OPEN_TICKS, 0.15D, 1.0D);
        double shrink = Mth.clamp((tear.closes - now) / 12.0D, 0.0D, 1.0D);
        double size = Math.min(grow, shrink) * (tear.nexus ? 2.0D : 1.0D);
        Vec3 right = tear.right();
        if (!ModCompat.ANTARCHY) {
            GauntletFx.farEllipse(level, ParticleTypes.REVERSE_PORTAL, center, right.scale(0.9D * size), 1.3D * size, tear.nexus ? 28 : 14, now * 0.2D, FAR_RANGE);
            GauntletFx.farEllipse(level, ParticleTypes.END_ROD, center, right.scale(0.55D * size), 0.85D * size, tear.nexus ? 12 : 6, -now * 0.3D, FAR_RANGE);
            GauntletFx.farParticles(level, ParticleTypes.PORTAL, center, 4, 0.3D * size, 0.6D * size, 0.3D * size, 0.4D, FAR_RANGE);
            if ((now - tear.opened) % 40L == 0L) {
                level.playSound(null, BlockPos.containing(center), SoundEvents.PORTAL_AMBIENT, SoundSource.HOSTILE, tear.nexus ? 1.2F : 0.35F, pitch(tear.alignment));
            }
        }
        ellipse(level, dust(tear.alignment, true, 1.1F), center, right.scale(1.05D * size), 1.45D * size, tear.nexus ? 20 : 10, now * 0.12D);
        if (now % 4L == 0L) {
            ellipse(level, dust(tear.alignment, false, 0.9F), center, right.scale(0.75D * size), 1.1D * size, tear.nexus ? 14 : 8, -now * 0.15D);
        }
    }

    private static void ellipse(ServerLevel level, ParticleOptions particle, Vec3 center, Vec3 horizontal, double vertical, int points, double phase) {
        if (ModCompat.ANTARCHY) {
            GauntletFx.ellipse(level, particle, center, horizontal, vertical, points, phase);
        } else {
            GauntletFx.farEllipse(level, particle, center, horizontal, vertical, points, phase, FAR_RANGE);
        }
    }

    private static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double dx, double dy, double dz, double speed) {
        if (ModCompat.ANTARCHY) {
            level.sendParticles(particle, at.x, at.y, at.z, count, dx, dy, dz, speed);
        } else {
            GauntletFx.farParticles(level, particle, at, count, dx, dy, dz, speed, FAR_RANGE);
        }
    }

    private static void tickNexus(ServerLevel level, Tear tear, long now, Vec3 center) {
        if (now % 2L == 0L) {
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 10, 2.6D, 2.0D, 2.6D, 0.06D);
            level.sendParticles(ParticleTypes.PORTAL, center.x, center.y, center.z, 16, 1.5D, 1.8D, 1.5D, 0.9D);
            if (now % 4L == 0L) {
                for (int i = 0; i < 4; i++) {
                    double angle = now * 0.18D + i * Math.PI * 0.5D;
                    double x = center.x + Math.cos(angle) * 3.2D;
                    double y = center.y + Math.sin(now * 0.1D + i) * 1.2D;
                    double z = center.z + Math.sin(angle) * 3.2D;
                    level.sendParticles(dust(null, true, 1.6F), x, y, z, 3, 0.15D, 0.15D, 0.15D, 0.0D);
                }
            }
            if (now % 6L == 0L) {
                level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 3, 1.2D, 1.4D, 1.2D, 0.01D);
            }
        }
        BlockPos at = BlockPos.containing(center);
        if ((now - tear.opened) % 40L == 0L) {
            level.playSound(null, at, SoundEvents.RESPAWN_ANCHOR_AMBIENT, SoundSource.HOSTILE, 2.5F, 0.5F);
            if (ModCompat.ANTARCHY) {
                level.playSound(null, at, AntarchyDimensionalTear.idleSound(), SoundSource.HOSTILE, 1.5F, 0.6F);
            }
        }
        if (now >= tear.nextGrowl) {
            tear.nextGrowl = now + 160L + level.random.nextInt(160);
            level.playSound(null, at, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.2F, 0.4F);
        }
        long last = tear.closes - 40L;
        if (tear.broodLeft <= 0 || now < tear.nextWave || now >= last) {
            return;
        }
        if (!playerNear(level, tear.ground, NEXUS_PLAYER_RANGE)) {
            return;
        }
        tear.nextWave = now + tear.waveGap;
        dirty(level);
        long wavesLeft = Math.max(1L, (last - now) / tear.waveGap + 1L);
        int wave = (int) Math.max(1L, (tear.broodLeft + wavesLeft - 1L) / wavesLeft);
        if (tear.tier >= 4 && level.random.nextBoolean()) {
            wave++;
        }
        wave = Math.min(wave, Math.min(tear.broodLeft, 3));
        VariantPool pool = VariantPool.get(level.getServer());
        for (int i = 0; i < wave; i++) {
            if (VoidVariants.loaded(level).size() >= broodCap()) {
                break;
            }
            tear.broodLeft--;
            VariantProfile profile = pool.roll(level.random, tear.tier, null);
            if (profile == null) {
                continue;
            }
            double lateral = wave == 1 ? 0.0D : (i - (wave - 1) * 0.5D) * 1.4D;
            Vec3 spot = tear.ground.add(tear.facing.scale(1.8D)).add(tear.right().scale(lateral));
            Mob mob = emerge(level, tear, profile, spot, lateral);
            if (mob != null) {
                mob.addTag(VoidVariants.BROOD_TAG);
            }
        }
    }

    private static boolean playerNear(ServerLevel level, Vec3 at, double range) {
        return level.getNearestPlayer(at.x, at.y, at.z, range, entity -> entity.isAlive() && !entity.isSpectator()) != null;
    }

    private static Mob emerge(ServerLevel level, Tear tear, VariantProfile profile, Vec3 spot, double lateral) {
        Vec3 start = new Vec3(tear.base.x, spot.y, tear.base.z).add(tear.facing.scale(0.35D)).add(tear.right().scale(lateral));
        if (!standable(level, start)) {
            start = standable(level, spot) ? spot : tear.ground;
        }
        if (!level.isPositionEntityTicking(BlockPos.containing(start))) {
            return null;
        }
        Mob mob = VoidVariants.spawnProfile(level, start, profile);
        if (mob == null) {
            return null;
        }
        float yaw = yaw(tear.facing);
        Vec3 placed = level.noCollision(mob) ? start : tear.ground;
        mob.moveTo(placed.x, placed.y, placed.z, yaw, 0.0F);
        mob.setYHeadRot(yaw);
        mob.setYBodyRot(yaw);
        mob.setDeltaMovement(tear.facing.scale(0.42D).add(0.0D, 0.36D, 0.0D));
        mob.hasImpulse = true;
        Vec3 center = tear.center();
        burst(level, dust(profile.alignment(), true, 1.4F), center, 14, 0.3D, 0.6D, 0.3D, 0.02D);
        burst(level, dust(profile.alignment(), false, 1.1F), center, 8, 0.3D, 0.6D, 0.3D, 0.02D);
        burst(level, ParticleTypes.REVERSE_PORTAL, placed.add(0.0D, 1.0D, 0.0D), 20, 0.3D, 0.5D, 0.3D, 0.08D);
        level.playSound(null, BlockPos.containing(center), emergeSound(), SoundSource.HOSTILE, tear.nexus ? 1.0F : 1.5F, pitch(profile.alignment()));
        return mob;
    }

    private static boolean boss(Mob mob) {
        return mob.getType().is(Tags.EntityTypes.BOSSES);
    }

    private static boolean hitByPlayer(Mob mob) {
        return mob.getLastHurtByMob() instanceof Player && mob.tickCount - mob.getLastHurtByMobTimestamp() <= RECENT_HIT_TICKS;
    }

    private static boolean rollsEscape(Mob mob, double chance) {
        UUID id = mob.getUUID();
        return Math.floorMod(id.getMostSignificantBits() ^ id.getLeastSignificantBits(), 10000L) < chance * 10000.0D;
    }

    private static void scanEscapes(ServerLevel level, long now) {
        double chance = VariantConfig.ESCAPE_CHANCE.get();
        if (chance <= 0.0D) {
            return;
        }
        for (Mob mob : VoidVariants.loaded(level)) {
            if (mob.getHealth() >= mob.getMaxHealth() * 0.2F || ESCAPES.containsKey(mob.getUUID()) || mob.getTags().contains(ESCAPE_TRIED_TAG)
                    || mob.isDeadOrDying() || boss(mob) || !hitByPlayer(mob)) {
                continue;
            }
            if (!rollsEscape(mob, chance)) {
                mob.addTag(ESCAPE_TRIED_TAG);
                continue;
            }
            Tear tear = ownTear(level, mob, now);
            if (tear == null) {
                tear = escapeTear(level, mob, now);
            }
            if (tear == null) {
                continue;
            }
            mob.addTag(ESCAPE_TRIED_TAG);
            if (mob.isPassenger()) {
                mob.stopRiding();
            }
            ESCAPES.put(mob.getUUID(), new Escape(mob, tear, level.dimension(), now + ESCAPE_TIMEOUT));
            setTarget(mob, null);
        }
    }

    @Nullable
    private static Tear ownTear(ServerLevel level, Mob mob, long now) {
        for (Tear tear : tears(level)) {
            if (!tear.nexus && tear.frozenAt < 0L && mob.getUUID().equals(tear.owner) && tear.closes - now > 30L && tear.base.distanceToSqr(mob.position()) < 24.0D * 24.0D) {
                return tear;
            }
        }
        return null;
    }

    @Nullable
    private static Tear escapeTear(ServerLevel level, Mob mob, long now) {
        if (ESCAPE_READY.getOrDefault(level.dimension(), Long.MIN_VALUE) > now) {
            return null;
        }
        Player threat = level.getNearestPlayer(mob, 32.0D);
        Vec3 away = threat == null ? Vec3.directionFromRotation(0.0F, mob.getYRot()) : mob.position().subtract(threat.position());
        away = new Vec3(away.x, 0.0D, away.z);
        away = away.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, 0.0D, 1.0D) : away.normalize();
        Vec3 base = null;
        for (double turn : new double[]{0.0D, 50.0D, -50.0D, 90.0D, -90.0D}) {
            Vec3 candidate = mob.position().add(away.yRot((float) Math.toRadians(turn)).scale(3.0D));
            if (standable(level, candidate)) {
                base = candidate.add(0.0D, LIFT, 0.0D);
                break;
            }
        }
        if (base == null) {
            return null;
        }
        Vec3 facing = facing(level, base, mob.position());
        Tear tear = new Tear(base, base.subtract(0.0D, LIFT, 0.0D), facing, now, now + ESCAPE_LIFETIME + 2L, false, VoidVariants.alignment(mob));
        tear.owner = mob.getUUID();
        if (!open(level, tear, ESCAPE_LIFETIME)) {
            return null;
        }
        ESCAPE_READY.put(level.dimension(), now + ESCAPE_COOLDOWN);
        return tear;
    }

    private static void setTarget(Mob mob, @Nullable LivingEntity target) {
        if (VoidVariants.thematicHost(mob)) {
            ThematicVariants.setTarget(mob, target);
        } else {
            mob.setTarget(target);
        }
    }

    private static void tickEscapes(ServerLevel level, long now) {
        List<Tear> list = tears(level);
        Iterator<Escape> it = ESCAPES.values().iterator();
        while (it.hasNext()) {
            Escape escape = it.next();
            if (escape.dimension() != level.dimension()) {
                continue;
            }
            Mob mob = escape.mob();
            Tear tear = escape.tear();
            if (!mob.isAlive() || mob.level() != level) {
                it.remove();
                continue;
            }
            if (now > escape.deadline() || now >= tear.closes - 12L || tear.frozenAt >= 0L || !list.contains(tear)) {
                it.remove();
                abort(level, mob);
                continue;
            }
            double ground = tear.base.y - LIFT;
            double dx = tear.base.x - mob.getX();
            double dz = tear.base.z - mob.getZ();
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (now % 10L == 0L) {
                setTarget(mob, null);
                mob.getNavigation().moveTo(tear.base.x, ground, tear.base.z, 1.45D);
            }
            if (distance < 4.0D && distance > 1.0D) {
                mob.setDeltaMovement(mob.getDeltaMovement().add(dx / distance * 0.1D, 0.0D, dz / distance * 0.1D));
                mob.hasImpulse = true;
            }
            if (distance < 1.4D && tear.open(now) && Math.abs(mob.getY() - ground) < 2.5D) {
                it.remove();
                dive(level, mob, tear);
            }
        }
    }

    private static void abort(ServerLevel level, Mob mob) {
        mob.getNavigation().stop();
        Player threat = level.getNearestPlayer(mob.getX(), mob.getY(), mob.getZ(), 24.0D,
                entity -> entity instanceof Player player && player.isAlive() && !player.isCreative() && !player.isSpectator());
        if (threat != null) {
            setTarget(mob, threat);
        }
    }

    private static void dive(ServerLevel level, Mob mob, Tear tear) {
        VariantAlignment alignment = VoidVariants.alignment(mob);
        Vec3 at = mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D);
        level.sendParticles(dust(alignment, true, 1.4F), at.x, at.y, at.z, 16, 0.3D, 0.5D, 0.3D, 0.02D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y, at.z, 24, 0.3D, 0.6D, 0.3D, 0.1D);
        level.playSound(null, BlockPos.containing(tear.center()), emergeSound(), SoundSource.HOSTILE, 1.0F, pitch(alignment) * 0.85F);
        mob.ejectPassengers();
        mob.stopRiding();
        mob.discard();
    }

    private static void onChangeTarget(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() != null && !ESCAPES.isEmpty() && !event.getEntity().level().isClientSide()
                && ESCAPES.containsKey(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !event.getEntity().getTags().contains(TEAR_TAG)) {
            return;
        }
        store(level);
        if (!LIVE.contains(event.getEntity().getUUID())) {
            event.setCanceled(true);
        }
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Store store = STORES.remove(level.dimension());
        if (store != null) {
            for (Tear tear : store.tears) {
                tear.visuals.forEach(LIVE::remove);
            }
        }
        ESCAPES.values().removeIf(escape -> escape.dimension() == level.dimension());
        ESCAPE_READY.remove(level.dimension());
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        STORES.clear();
        LIVE.clear();
        ESCAPES.clear();
        ESCAPE_READY.clear();
    }
}
