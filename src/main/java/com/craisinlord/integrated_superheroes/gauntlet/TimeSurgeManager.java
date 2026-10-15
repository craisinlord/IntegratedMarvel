package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class TimeSurgeManager {
    private static final long DURATION = 400L;
    private static final Map<ResourceKey<Level>, Long> ACTIVE = new ConcurrentHashMap<>();
    private static final List<Area> AREAS = new CopyOnWriteArrayList<>();

    private TimeSurgeManager() {
    }

    public static void activate(ServerLevel level) {
        long now = level.getServer().overworld().getGameTime();
        ACTIVE.put(level.dimension(), now + DURATION);
    }

    public static void activateArea(ServerLevel level, Vec3 center, double radius, int duration, double rate) {
        long now = level.getServer().overworld().getGameTime();
        double scale = Math.clamp((rate - 1.0D) / 2.0D, 0.25D, 1.0D);
        AREAS.add(new Area(level.dimension(), center, radius, now + duration, scale));
    }

    public static boolean isSurging(ResourceKey<Level> dimension) {
        return ACTIVE.containsKey(dimension);
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        long now = level.getServer().overworld().getGameTime();
        if (!TimeFreezeManager.owners(level.dimension()).isEmpty()) return;
        tickAreas(level, now);
        Long expires = ACTIVE.get(level.dimension());
        if (expires == null) return;
        if (now >= expires) {
            ACTIVE.remove(level.dimension(), expires);
            return;
        }
        advanceDay(level);
        int radius = GauntletConfig.TIME_SURGE_CHUNK_RADIUS.get();
        Set<Long> visited = new HashSet<>();
        Pass pass = new Pass(level, 1.0D, now);
        for (ServerPlayer player : level.players()) {
            ChunkPos center = player.chunkPosition();
            pass.begin();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    accelerateChunk(level, center.x + dx, center.z + dz, pass, visited);
                }
            }
            pass.ageMobs(level, center.x - radius, center.z - radius, center.x + radius, center.z + radius);
            if (now % 10L == 0L) {
                level.sendParticles(GauntletFx.dust("time", 1.0F), player.getX(), player.getY() + 1.0D, player.getZ(), 6,
                        6.0D, 3.0D, 6.0D, 0.0D);
            }
        }
    }

    private static void tickAreas(ServerLevel level, long now) {
        if (AREAS.isEmpty()) return;
        Set<Long> visited = new HashSet<>();
        for (Area area : AREAS) {
            if (now >= area.expiry()) {
                AREAS.remove(area);
                continue;
            }
            if (!area.dimension().equals(level.dimension())) continue;
            int minX = SectionPos.blockToSectionCoord(area.center().x - area.radius());
            int maxX = SectionPos.blockToSectionCoord(area.center().x + area.radius());
            int minZ = SectionPos.blockToSectionCoord(area.center().z - area.radius());
            int maxZ = SectionPos.blockToSectionCoord(area.center().z + area.radius());
            Pass pass = new Pass(level, area.scale(), now);
            pass.begin();
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) accelerateChunk(level, x, z, pass, visited);
            }
            pass.ageMobs(level, minX, minZ, maxX, maxZ);
            if (now % 5L == 0L) {
                Vec3 center = area.center();
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, center.x, center.y + 1.0D, center.z, 4,
                        area.radius() * 0.4D, 1.0D, area.radius() * 0.4D, 0.0D);
            }
        }
    }

    private static void accelerateChunk(ServerLevel level, int chunkX, int chunkZ, Pass pass, Set<Long> visited) {
        long key = ChunkPos.asLong(chunkX, chunkZ);
        if (!visited.add(key) || !level.shouldTickBlocksAt(key)) return;
        LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
        if (chunk == null) return;
        if (pass.randomTicks > 0) randomTicks(level, chunk, pass.randomTicks);
        if (pass.blockEntityTicks > 0) {
            for (BlockEntity blockEntity : new ArrayList<>(chunk.getBlockEntities().values())) {
                tickBlockEntity(level, blockEntity, pass.blockEntityTicks);
            }
        }
        if (pass.ages) pass.chunks.add(key);
    }

    private static final class Pass {
        private final int multiplier;
        private final int randomTicks;
        private final int blockEntityTicks;
        private final boolean ages;
        private final Set<Long> chunks = new HashSet<>();

        private Pass(ServerLevel level, double scale, long now) {
            int speed = level.getGameRules().getInt(GameRules.RULE_RANDOMTICKING);
            multiplier = Math.max(1, (int) Math.round(GauntletConfig.TIME_SURGE_TICK_MULTIPLIER.get() * scale));
            randomTicks = speed > 0 ? speed * multiplier : 0;
            blockEntityTicks = (int) Math.round(GauntletConfig.TIME_SURGE_BLOCK_ENTITY_TICKS.get() * scale);
            ages = now % 20L == 0L;
        }

        private void begin() {
            chunks.clear();
        }

        private void ageMobs(ServerLevel level, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
            if (!ages || chunks.isEmpty()) return;
            AABB box = new AABB(SectionPos.sectionToBlockCoord(minChunkX), level.getMinBuildHeight(), SectionPos.sectionToBlockCoord(minChunkZ),
                    SectionPos.sectionToBlockCoord(maxChunkX) + 16, level.getMaxBuildHeight(), SectionPos.sectionToBlockCoord(maxChunkZ) + 16);
            for (AgeableMob mob : level.getEntitiesOfClass(AgeableMob.class, box,
                    mob -> chunks.contains(ChunkPos.asLong(SectionPos.blockToSectionCoord(mob.getBlockX()),
                            SectionPos.blockToSectionCoord(mob.getBlockZ()))))) {
                if (mob.getAge() < 0) {
                    mob.ageUp(multiplier);
                } else if (mob.getAge() > 0) {
                    mob.setAge(Math.max(0, mob.getAge() - 20 * multiplier));
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity> void tickBlockEntity(ServerLevel level, BlockEntity blockEntity, int times) {
        if (blockEntity.isRemoved()) return;
        BlockState state = blockEntity.getBlockState();
        BlockEntityTicker<T> ticker = state.getTicker(level, (BlockEntityType<T>) blockEntity.getType());
        if (ticker == null) return;
        BlockPos pos = blockEntity.getBlockPos();
        for (int index = 0; index < times && !blockEntity.isRemoved(); index++) {
            ticker.tick(level, pos, state, (T) blockEntity);
        }
    }

    private static void advanceDay(ServerLevel level) {
        int speed = GauntletConfig.TIME_SURGE_DAY_SPEED.get();
        if (speed <= 0) return;
        long dayTime = level.getDayTime() + speed;
        level.setDayTime(dayTime);
        ClientboundSetTimePacket packet = new ClientboundSetTimePacket(level.getGameTime(), dayTime,
                level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
        for (ServerPlayer player : level.players()) player.connection.send(packet);
    }

    private static void randomTicks(ServerLevel level, LevelChunk chunk, int ticksPerSection) {
        RandomSource random = level.getRandom();
        int minX = chunk.getPos().getMinBlockX();
        int minZ = chunk.getPos().getMinBlockZ();
        LevelChunkSection[] sections = chunk.getSections();
        for (int index = 0; index < sections.length; index++) {
            LevelChunkSection section = sections[index];
            if (!section.isRandomlyTicking()) continue;
            int minY = SectionPos.sectionToBlockCoord(chunk.getSectionYFromSectionIndex(index));
            for (int i = 0; i < ticksPerSection; i++) {
                BlockPos pos = level.getBlockRandomPos(minX, minY, minZ, 15);
                BlockState state = section.getBlockState(pos.getX() - minX, pos.getY() - minY, pos.getZ() - minZ);
                if (state.isRandomlyTicking()) state.randomTick(level, pos, random);
                FluidState fluid = state.getFluidState();
                if (fluid.isRandomlyTicking()) fluid.randomTick(level, pos, random);
            }
        }
    }

    public static void clear(MinecraftServer server) {
        ACTIVE.clear();
        AREAS.clear();
    }

    public static void clearExpired(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        ACTIVE.entrySet().removeIf(entry -> now >= entry.getValue());
    }

    private record Area(ResourceKey<Level> dimension, Vec3 center, double radius, long expiry, double scale) {
    }
}
