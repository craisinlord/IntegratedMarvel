package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.anthology.travel.CrossDimensionTravelApi;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.Comparator;

public final class SpaceTravel {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final TicketType<ChunkPos> TICKET = TicketType.create("integrated_superheroes_space",
            Comparator.comparingLong(ChunkPos::toLong), 200);

    private SpaceTravel() {
    }

    public static BlockPos clamp(BlockPos pos, ServerLevel level) {
        return new BlockPos(pos.getX(), Math.clamp(pos.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 3), pos.getZ());
    }

    public static void keepLoaded(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        level.getChunkSource().addRegionTicket(TICKET, chunk, 2, chunk);
    }

    public static boolean isLoaded(ServerLevel level, BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (level.getChunkSource().getChunkNow(center.x + dx, center.z + dz) == null) return false;
            }
        }
        return true;
    }

    public static boolean forceLoad(ServerLevel level, BlockPos pos) {
        ChunkPos center = new ChunkPos(pos);
        try {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    level.getChunk(center.x + dx, center.z + dz);
                }
            }
            return true;
        } catch (RuntimeException exception) {
            LOGGER.warn("Space Stone could not load destination chunks at {} in {}", pos, level.dimension().location(), exception);
            return false;
        }
    }

    public static Vec3 arrivalPosition(Entity entity, ServerLevel level, BlockPos preferred) {
        BlockPos target = clamp(preferred, level);
        try {
            Vec3 safe = CrossDimensionTravelApi.findSafePosition(entity, level, target);
            if (safe != null) return safe;
        } catch (RuntimeException exception) {
            LOGGER.warn("Space Stone safe-position search failed at {} in {}", target, level.dimension().location(), exception);
        }
        int min = level.getMinBuildHeight() + 1;
        int max = level.getMaxBuildHeight() - 3;
        for (int offset = 0; offset <= max - min; offset++) {
            for (int sign = 1; sign >= -1; sign -= 2) {
                if (offset == 0 && sign < 0) continue;
                int y = target.getY() + offset * sign;
                if (y < min || y > max) continue;
                BlockPos feet = new BlockPos(target.getX(), y, target.getZ());
                if (isStandable(level, feet)) return Vec3.atBottomCenterOf(feet);
            }
        }
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target);
        if (surface.getY() > level.getMinBuildHeight()) return Vec3.atBottomCenterOf(surface);
        return Vec3.atBottomCenterOf(target);
    }

    public static Vec3 confirmArrival(Entity entity, ServerLevel level, Vec3 arrival) {
        BlockPos feet = BlockPos.containing(arrival);
        if (level.hasChunkAt(feet) && isOpen(level, feet) && isOpen(level, feet.above())) return arrival;
        return arrivalPosition(entity, level, feet);
    }

    public static boolean isStandable(ServerLevel level, BlockPos feet) {
        if (!isOpen(level, feet) || !isOpen(level, feet.above())) return false;
        BlockPos below = feet.below();
        BlockState floor = level.getBlockState(below);
        return floor.isFaceSturdy(level, below, Direction.UP) && !floor.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean isOpen(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.getCollisionShape(level, pos).isEmpty() && !state.getFluidState().is(FluidTags.LAVA);
    }

    public static boolean teleport(Entity entity, ServerLevel destination, Vec3 arrival, float yRot) {
        if (!entity.isAlive() || !(entity.level() instanceof ServerLevel source)) return false;
        Vec3 from = entity.position();
        if (entity instanceof ServerPlayer player) {
            if (player.isPassenger()) player.stopRiding();
            player.teleportTo(destination, arrival.x, arrival.y, arrival.z, yRot, player.getXRot());
            player.resetFallDistance();
            player.setPortalCooldown();
        } else if (source == destination) {
            entity.teleportTo(arrival.x, arrival.y, arrival.z);
            entity.setYRot(yRot);
            entity.resetFallDistance();
            entity.setPortalCooldown();
        } else {
            if (!entity.canChangeDimensions(source, destination)) return false;
            Entity moved = entity.changeDimension(new DimensionTransition(destination, arrival, Vec3.ZERO, yRot,
                    entity.getXRot(), DimensionTransition.DO_NOTHING));
            if (moved == null) return false;
            moved.resetFallDistance();
            moved.setPortalCooldown();
        }
        source.playSound(null, BlockPos.containing(from), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
        destination.playSound(null, BlockPos.containing(arrival), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }
}
