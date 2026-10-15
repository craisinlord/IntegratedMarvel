package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.hero.HealingFactor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class SnapStone {
    public static final String SCORCHED_TAG = "integrated_superheroes_scorched_until";
    private static final long CONFIRM_TICKS = 100L;
    private static final int SCAN_PER_TICK = 65536;
    private static final Map<String, Long> CONFIRMATIONS = new ConcurrentHashMap<>();
    private static final List<BlockErase> ERASES = new ArrayList<>();
    private static final List<SnapWave> WAVES = new ArrayList<>();
    private static final ThreadLocal<CompoundTag> REWIND_SNAPSHOT = new ThreadLocal<>();
    private static final int WAVE_TICKS = 40;
    private static final int WAVE_MIN_PER_TICK = 64;
    private static final int DUST_PER_PACKET = 1024;

    private SnapStone() {
    }

    private static boolean confirm(GauntletCast cast, String key) {
        long now = cast.player().server.overworld().getGameTime();
        String id = cast.player().getUUID() + ":" + key;
        Long previous = CONFIRMATIONS.get(id);
        if (previous == null || now - previous > CONFIRM_TICKS) {
            CONFIRMATIONS.put(id, now);
            GauntletFx.sound(cast.level(), cast.player().position(), SoundEvents.BEACON_AMBIENT, 1.5F, 0.5F);
            cast.message("snap_confirm_" + key);
            return false;
        }
        CONFIRMATIONS.remove(id);
        return true;
    }

    public static void halve(GauntletCast cast) {
        if (!confirm(cast, "halve")) return;
        ServerPlayer player = cast.player();
        List<LivingEntity> loaded = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && living.isAlive() && living != player
                        && !living.hasIndirectPassenger(player)) {
                    loaded.add(living);
                }
            }
        }
        java.util.Collections.shuffle(loaded, new java.util.Random(player.getRandom().nextLong()));
        int victims = loaded.size() / 2;
        SnapSavedData.get(player.server).replace(List.of());
        WAVES.add(new SnapWave(nearestFirst(player, loaded.subList(0, victims)), true));
        snapEffects(cast);
        cast.message("snap_result", victims);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.craisinlord.integrated_superheroes.api.GauntletSnapEvent(player, victims));
        toll(cast);
        cast.cooldown();
    }

    private static List<LivingEntity> nearestFirst(ServerPlayer player, List<LivingEntity> victims) {
        Level home = player.level();
        Vec3 origin = player.position();
        List<LivingEntity> sorted = new ArrayList<>(victims);
        sorted.sort(java.util.Comparator.comparingDouble(entity -> entity.level() == home ? entity.distanceToSqr(origin) : Double.MAX_VALUE));
        return sorted;
    }

    public static CompoundTag rewindSnapshot() {
        return REWIND_SNAPSHOT.get();
    }

    public static void restore(GauntletCast cast) {
        SnapSavedData data = SnapSavedData.get(cast.player().server);
        if (data.size() == 0) {
            cast.message("snap_restore_empty");
            return;
        }
        if (!confirm(cast, "restore")) return;
        MinecraftServer server = cast.player().server;
        int restored = 0;
        for (CompoundTag tag : data.drain()) {
            ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString("integrated_superheroes_dimension"));
            ServerLevel level = dimensionId == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, dimensionId));
            if (level == null) continue;
            CompoundTag copy = tag.copy();
            copy.remove("integrated_superheroes_dimension");
            copy.remove("DeathTime");
            if (copy.hasUUID("UUID") && level.getEntity(copy.getUUID("UUID")) != null) copy.remove("UUID");
            Entity entity = EntityType.loadEntityRecursive(copy, level, loaded -> loaded);
            if (entity == null) continue;
            if (entity instanceof LivingEntity living) living.setHealth(living.getMaxHealth());
            if (level.addFreshEntity(entity)) {
                restored++;
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, entity.getX(), entity.getY() + 1.0D, entity.getZ(), 12,
                        0.3D, 0.6D, 0.3D, 0.2D);
            }
        }
        snapEffects(cast);
        cast.message("snap_restored", restored);
        toll(cast);
        cast.cooldown();
    }

    public static void selective(GauntletCast cast) {
        ServerPlayer player = cast.player();
        BlockState block = cast.crouching() ? RealityClipboard.singleBlock(player.getUUID()) : null;
        if (cast.crouching() && block == null) {
            cast.message("snap_selective_no_block");
            return;
        }
        LivingEntity aimed = block == null ? GauntletAim.living(player, 64.0D) : null;
        if (!confirm(cast, "selective")) return;
        if (block != null) {
            if (!GauntletConfig.griefing()) {
                cast.message("griefing_disabled");
                return;
            }
            int radius = cast.infinity() ? 96 : 48;
            ERASES.add(new BlockErase(cast.level().dimension(), player.getUUID(), block.getBlock(), player.blockPosition(), radius,
                    chunksAround(player.blockPosition(), radius)));
            snapEffects(cast);
            cast.message("snap_selective_blocks", block.getBlock().getName());
            toll(cast);
            cast.cooldown();
            return;
        }
        Predicate<Entity> filter;
        Component label;
        if (aimed != null && !(aimed instanceof Player)) {
            EntityType<?> type = aimed.getType();
            filter = entity -> entity.getType() == type;
            label = type.getDescription();
        } else {
            filter = entity -> entity instanceof Enemy;
            label = Component.translatable("message.integrated_superheroes.gauntlet.snap_hostiles");
        }
        List<LivingEntity> matches = new ArrayList<>();
        for (ServerLevel level : player.server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity living && !(living instanceof Player) && living.isAlive() && filter.test(living)) {
                    matches.add(living);
                }
            }
        }
        int erased = matches.size();
        WAVES.add(new SnapWave(nearestFirst(player, matches), false));
        snapEffects(cast);
        cast.message("snap_selective_result", erased, label);
        toll(cast);
        cast.cooldown();
    }

    private static Deque<ChunkPos> chunksAround(BlockPos center, int radius) {
        Deque<ChunkPos> chunks = new ArrayDeque<>();
        int chunkRadius = (radius >> 4) + 1;
        ChunkPos origin = new ChunkPos(center);
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) chunks.add(new ChunkPos(origin.x + dx, origin.z + dz));
        }
        return chunks;
    }

    private static void snapEffects(GauntletCast cast) {
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, true);
        for (String stone : List.of("power", "space", "reality", "soul", "time", "mind")) {
            GauntletFx.burst(level, player.getEyePosition().add(player.getLookAngle()), stone, 20, 0.3D);
        }
        GauntletFx.sound(level, player.position(), SoundEvents.LIGHTNING_BOLT_THUNDER, 2.0F, 1.6F);
        GauntletFx.sound(level, player.position(), SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.4F);
        GauntletFx.shake(level, player.position(), 2.5F, 40, 128.0F);
    }

    private static void toll(GauntletCast cast) {
        ServerPlayer player = cast.player();
        player.setHealth(Math.max(1.0F, player.getHealth() * 0.5F));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 3600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 3600, 1));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 0));
        ItemStack gauntlet = cast.gauntlet();
        CompoundTag data = gauntlet.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        data.putLong(SCORCHED_TAG, player.server.overworld().getGameTime() + 6000L);
        gauntlet.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        cast.level().sendParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getY() + 1.2D, player.getZ(), 30,
                0.3D, 0.3D, 0.3D, 0.02D);
    }

    public static boolean isScorched(ItemStack gauntlet, long now) {
        return scorchedUntil(gauntlet) > now;
    }

    public static long scorchedUntil(ItemStack gauntlet) {
        return InfinityGauntletStones.data(gauntlet).getLong(SCORCHED_TAG);
    }

    private static SnapDustPayload.Entry dust(LivingEntity target) {
        return new SnapDustPayload.Entry(target.getX(), target.getY(), target.getZ(), target.getBbWidth(), target.getBbHeight(),
                target.getSoundSource().ordinal());
    }

    private static void sendDust(ServerLevel level, List<SnapDustPayload.Entry> entries) {
        for (ServerPlayer viewer : level.players()) {
            List<SnapDustPayload.Entry> visible = new ArrayList<>();
            for (SnapDustPayload.Entry entry : entries) {
                double centerY = entry.y() + entry.height() * 0.5D;
                if (!viewer.blockPosition().closerToCenterThan(new Vec3(entry.x(), centerY, entry.z()), 32.0D)) continue;
                double dx = Math.floor(entry.x()) + 0.5D - viewer.getX();
                double dy = Math.floor(entry.y()) + 0.5D - viewer.getY();
                double dz = Math.floor(entry.z()) + 0.5D - viewer.getZ();
                boolean audible = dx * dx + dy * dy + dz * dz < 256.0D;
                visible.add(audible ? entry : new SnapDustPayload.Entry(entry.x(), entry.y(), entry.z(), entry.width(),
                        entry.height(), -1));
                if (visible.size() >= DUST_PER_PACKET) {
                    PacketDistributor.sendToPlayer(viewer, new SnapDustPayload(visible));
                    visible = new ArrayList<>();
                }
            }
            if (!visible.isEmpty()) PacketDistributor.sendToPlayer(viewer, new SnapDustPayload(visible));
        }
    }

    private static boolean tickWave(MinecraftServer server, SnapWave wave) {
        int budget = Math.max(WAVE_MIN_PER_TICK, Mth.ceil(wave.victims.size() / (double) WAVE_TICKS));
        SnapSavedData data = wave.saves ? SnapSavedData.get(server) : null;
        Map<ServerLevel, List<SnapDustPayload.Entry>> dust = new java.util.IdentityHashMap<>();
        while (budget-- > 0 && wave.cursor < wave.victims.size()) {
            LivingEntity target = wave.victims.get(wave.cursor++);
            if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) continue;
            CompoundTag snapshot = null;
            if (data != null && !(target instanceof Player)) {
                CompoundTag tag = new CompoundTag();
                if (target.saveAsPassenger(tag)) {
                    tag.remove("Passengers");
                    TimeRewindManager.stripLoot(tag);
                    tag.putString("integrated_superheroes_dimension", level.dimension().location().toString());
                    data.add(tag);
                    snapshot = tag;
                }
            }
            dust.computeIfAbsent(level, key -> new ArrayList<>()).add(dust(target));
            REWIND_SNAPSHOT.set(snapshot);
            try {
                HealingFactor.snap(() -> target.hurt(target.damageSources().genericKill(), 1.0E9F));
            } finally {
                REWIND_SNAPSHOT.remove();
            }
        }
        dust.forEach(SnapStone::sendDust);
        return wave.cursor >= wave.victims.size();
    }

    public static void beamImpact(ServerPlayer player, Vec3 end, Set<UUID> affected) {
        ServerLevel level = player.serverLevel();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(end, end).inflate(4.0D),
                entity -> entity != player && entity.isAlive() && !GauntletAim.isProtectedEntity(entity))) {
            if (!affected.add(target.getUUID())) continue;
            Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 6), player);
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 2), player);
            RealityStone.invert(target, 200, player);
            float drained = Math.min(8.0F, target.getHealth());
            target.hurt(player.damageSources().indirectMagic(player, player), drained);
            player.heal(drained);
            if (target instanceof Mob mob) mob.setTarget(null);
            for (String stone : List.of("power", "space", "reality", "soul", "time", "mind")) {
                GauntletFx.burst(level, at, stone, 6, target.getBbWidth() * 0.6D);
            }
            if (level.random.nextFloat() < 0.35F) {
                double angle = level.random.nextDouble() * Math.PI * 2.0D;
                Vec3 arrival = SpaceTravel.arrivalPosition(target, level,
                        BlockPos.containing(target.getX() + Math.cos(angle) * 32.0D, target.getY(), target.getZ() + Math.sin(angle) * 32.0D));
                SpaceTravel.teleport(target, level, arrival, target.getYRot());
            }
        }
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (now % 20L == 0L) CONFIRMATIONS.entrySet().removeIf(entry -> now - entry.getValue() > CONFIRM_TICKS);
        if (now % 10L == 0L) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
                if (!gauntlet.isEmpty() && isScorched(gauntlet, now)) {
                    player.serverLevel().sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 3,
                            0.25D, 0.25D, 0.25D, 0.01D);
                }
            }
        }
        WAVES.removeIf(wave -> tickWave(server, wave));
        for (var iterator = ERASES.iterator(); iterator.hasNext();) {
            BlockErase erase = iterator.next();
            ServerLevel level = server.getLevel(erase.dimension);
            if (level == null || erase.finished()) {
                iterator.remove();
                continue;
            }
            tickErase(level, server.getPlayerList().getPlayer(erase.owner), erase);
        }
    }

    private static void tickErase(ServerLevel level, ServerPlayer owner, BlockErase erase) {
        int scanBudget = SCAN_PER_TICK;
        int editBudget = GauntletConfig.BLOCK_EDITS_PER_TICK.get();
        int radiusSqr = erase.radius * erase.radius;
        int centerX = erase.center.getX();
        int centerY = erase.center.getY();
        int centerZ = erase.center.getZ();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        while (scanBudget > 0 && editBudget > 0) {
            if (erase.chunk == null) {
                erase.chunk = erase.chunks.poll();
                if (erase.chunk == null) return;
                erase.section = 0;
                erase.cell = 0;
                erase.allowed = null;
                int dx = outside(centerX, erase.chunk.getMinBlockX(), erase.chunk.getMaxBlockX());
                int dz = outside(centerZ, erase.chunk.getMinBlockZ(), erase.chunk.getMaxBlockZ());
                erase.horizontalSqr = dx * dx + dz * dz;
                if (erase.horizontalSqr > radiusSqr) {
                    erase.chunk = null;
                    scanBudget -= 16;
                    continue;
                }
            }
            LevelChunk chunk = level.getChunkSource().getChunkNow(erase.chunk.x, erase.chunk.z);
            LevelChunkSection[] sections = chunk == null ? null : chunk.getSections();
            if (sections == null || erase.section >= sections.length || Boolean.FALSE.equals(erase.allowed)) {
                erase.chunk = null;
                continue;
            }
            LevelChunkSection section = sections[erase.section];
            int minY = net.minecraft.core.SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(erase.section));
            if (erase.cell == 0) {
                int dy = outside(centerY, minY, minY + 15);
                if (erase.horizontalSqr + dy * dy > radiusSqr || section.hasOnlyAir()
                        || !section.maybeHas(state -> state.is(erase.block))) {
                    erase.section++;
                    scanBudget -= 64;
                    continue;
                }
            }
            int baseX = erase.chunk.getMinBlockX();
            int baseZ = erase.chunk.getMinBlockZ();
            while (erase.cell < 4096 && scanBudget > 0 && editBudget > 0) {
                int cell = erase.cell++;
                scanBudget--;
                int x = cell & 15;
                int z = (cell >> 4) & 15;
                int y = cell >> 8;
                int offsetX = baseX + x - centerX;
                int offsetY = minY + y - centerY;
                int offsetZ = baseZ + z - centerZ;
                if (offsetX * offsetX + offsetY * offsetY + offsetZ * offsetZ > radiusSqr) continue;
                BlockState state = section.getBlockState(x, y, z);
                if (!state.is(erase.block)) continue;
                cursor.set(baseX + x, minY + y, baseZ + z);
                if (erase.allowed == null) {
                    erase.allowed = GauntletGrief.canEdit(owner, level, cursor.immutable());
                    if (!erase.allowed) break;
                }
                if (!GauntletGrief.canEditQuietly(owner, level, cursor, state)) continue;
                BlockPos pos = cursor.immutable();
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                editBudget--;
                if (level.random.nextInt(48) == 0) GauntletFx.burst(level, Vec3.atCenterOf(pos), "snap", 3, 0.4D);
            }
            if (erase.cell >= 4096) {
                erase.section++;
                erase.cell = 0;
            }
        }
    }

    private static int outside(int value, int min, int max) {
        return value < min ? min - value : value > max ? value - max : 0;
    }

    public static void clear() {
        CONFIRMATIONS.clear();
        ERASES.clear();
        WAVES.clear();
    }

    private static final class SnapWave {
        private final List<LivingEntity> victims;
        private final boolean saves;
        private int cursor;

        private SnapWave(List<LivingEntity> victims, boolean saves) {
            this.victims = victims;
            this.saves = saves;
        }
    }

    private static final class BlockErase {
        private final ResourceKey<Level> dimension;
        private final UUID owner;
        private final Block block;
        private final BlockPos center;
        private final int radius;
        private final Deque<ChunkPos> chunks;
        private ChunkPos chunk;
        private int section;
        private int cell;
        private Boolean allowed;
        private int horizontalSqr;

        private BlockErase(ResourceKey<Level> dimension, UUID owner, Block block, BlockPos center, int radius,
                           Deque<ChunkPos> chunks) {
            this.dimension = dimension;
            this.owner = owner;
            this.block = block;
            this.center = center;
            this.radius = radius;
            this.chunks = chunks;
        }

        private boolean finished() {
            return chunk == null && chunks.isEmpty();
        }
    }
}
