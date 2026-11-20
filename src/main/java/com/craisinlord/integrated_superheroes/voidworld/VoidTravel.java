package com.craisinlord.integrated_superheroes.voidworld;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.portal.DimensionTransition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Blocks;

public final class VoidTravel {
    private static final int MIN_GROUND = 40;
    private static final int SEARCH_RADIUS = 32;
    private static final int SEARCH_STEP = 4;

    private VoidTravel() {
    }

    public static ResourceKey<Level> dimension() {
        return VoidWorld.LEVEL;
    }

    public static ServerLevel level(ServerPlayer player) {
        return player.server.getLevel(VoidWorld.LEVEL);
    }

    public static boolean send(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        RandomSource random = player.getRandom();
        int min = VoidConfig.SPEC.isLoaded() ? VoidConfig.ARRIVAL_MIN_RADIUS.get() : 64;
        int max = VoidConfig.SPEC.isLoaded() ? VoidConfig.ARRIVAL_MAX_RADIUS.get() : 256;
        if (max < min) {
            max = min;
        }
        double angle = random.nextDouble() * Math.PI * 2.0;
        double radius = min + random.nextDouble() * (max - min);
        return sendTo(player, Mth.floor(Math.cos(angle) * radius), Mth.floor(Math.sin(angle) * radius));
    }

    public static boolean sendTo(ServerPlayer player, int x, int z) {
        if (player == null) {
            return false;
        }
        ServerLevel level = level(player);
        if (level == null) {
            return false;
        }
        BlockPos ground = findGround(level, x, z);
        if (ground == null) {
            ground = buildLanding(level, x, z);
        }
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        player.teleportTo(level, ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, player.getYRot(), player.getXRot());
        player.resetFallDistance();
        int slowFall = VoidConfig.SPEC.isLoaded() ? VoidConfig.ARRIVAL_SLOW_FALL_TICKS.get() : 100;
        if (slowFall > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, slowFall, 0, false, false, true));
        }
        return true;
    }

    public static void sendToRespawn(ServerPlayer player) {
        DimensionTransition transition = player.findRespawnPositionAndUseSpawnBlock(true, DimensionTransition.DO_NOTHING);
        player.resetFallDistance();
        player.setDeltaMovement(Vec3.ZERO);
        if (transition.newLevel().dimension().equals(VoidWorld.LEVEL)) {
            ServerLevel overworld = player.server.overworld();
            BlockPos spawn = overworld.getSharedSpawnPos();
            player.teleportTo(overworld, spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, player.getYRot(), player.getXRot());
        } else {
            player.changeDimension(transition);
        }
        player.resetFallDistance();
    }

    public static BlockPos findGround(ServerLevel level, int x, int z) {
        BlockPos found = loadedGroundAt(level, x, z);
        if (found != null) {
            return found;
        }
        for (int r = SEARCH_STEP; r <= SEARCH_RADIUS; r += SEARCH_STEP) {
            for (int dx = -r; dx <= r; dx += SEARCH_STEP) {
                for (int dz = -r; dz <= r; dz += SEARCH_STEP) {
                    if (Math.abs(dx) != r && Math.abs(dz) != r) {
                        continue;
                    }
                    found = loadedGroundAt(level, x + dx, z + dz);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        level.getChunk(x >> 4, z >> 4);
        return loadedGroundAt(level, x, z);
    }

    private static BlockPos loadedGroundAt(ServerLevel level, int x, int z) {
        if (level.getChunkSource().getChunkNow(x >> 4, z >> 4) == null) return null;
        int height = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        if (height < MIN_GROUND) {
            return null;
        }
        BlockState below = level.getBlockState(new BlockPos(x, height - 1, z));
        BlockPos feet = new BlockPos(x, height, z);
        if (!below.isFaceSturdy(level, feet.below(), Direction.UP)
                || !below.getFluidState().isEmpty() || !level.isEmptyBlock(feet) || !level.isEmptyBlock(feet.above())) {
            return null;
        }
        return new BlockPos(x, height, z);
    }

    private static BlockPos buildLanding(ServerLevel level, int x, int z) {
        BlockState block = Blocks.SMOOTH_SANDSTONE.defaultBlockState();
        if (level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator) {
            block = generator.generatorSettings().value().defaultBlock();
        }
        x = (x & ~15) + Math.clamp(x & 15, 2, 13);
        z = (z & ~15) + Math.clamp(z & 15, 2, 13);
        int y = 72;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                y = Math.max(y, level.getHeight(Heightmap.Types.WORLD_SURFACE, x + dx, z + dz) + 1);
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                level.setBlock(new BlockPos(x + dx, y - 1, z + dz), block, Block.UPDATE_ALL);
            }
        }
        return new BlockPos(x, y, z);
    }
}
