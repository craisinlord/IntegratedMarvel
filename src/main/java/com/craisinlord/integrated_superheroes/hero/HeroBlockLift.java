package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.AlexsCavesBlockLift;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class HeroBlockLift {
    private static final TagKey<Block> UNMOVABLE = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("alexscaves", "unmovable"));

    private HeroBlockLift() {
    }

    public record Profile(double range, int maxBlocks, int holdTicks, double throwSpeed, double damageMultiplier,
                          Predicate<BlockState> filter, BiConsumer<ServerLevel, Vec3> aura,
                          Supplier<SoundEvent> liftSound, Supplier<SoundEvent> loopSound, Supplier<SoundEvent> throwSound) {
    }

    public static boolean isLifting(ServerPlayer player) {
        return ModCompat.ALEXS_CAVES ? AlexsCavesBlockLift.isLifting(player) : VanillaBlockLift.isLifting(player);
    }

    public static boolean lift(ServerPlayer player, Profile profile) {
        return ModCompat.ALEXS_CAVES ? AlexsCavesBlockLift.lift(player, profile) : VanillaBlockLift.lift(player, profile);
    }

    public static void tick(ServerPlayer player) {
        if (ModCompat.ALEXS_CAVES) {
            AlexsCavesBlockLift.tick(player);
        } else {
            VanillaBlockLift.tick(player);
        }
    }

    public static boolean throwHeld(ServerPlayer player) {
        return ModCompat.ALEXS_CAVES ? AlexsCavesBlockLift.throwHeld(player) : VanillaBlockLift.throwHeld(player);
    }

    public static void tickThrown(ServerLevel level) {
        if (ModCompat.ALEXS_CAVES) {
            AlexsCavesBlockLift.tickThrown(level);
        } else {
            VanillaBlockLift.tickThrown(level);
        }
    }

    public static void drop(ServerPlayer player) {
        if (ModCompat.ALEXS_CAVES) {
            AlexsCavesBlockLift.drop(player);
        } else {
            VanillaBlockLift.drop(player);
        }
    }

    public static BlockPos lowerHalf(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.UPPER) {
            return pos.below();
        }
        return pos;
    }

    public static boolean movable(ServerLevel level, ServerPlayer player, BlockPos pos, Profile profile) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && profile.filter().test(state) && !state.is(UNMOVABLE) && state.getDestroySpeed(level, pos) >= 0.0F
                && level.mayInteract(player, pos);
    }

    public static List<BlockPos> gather(ServerLevel level, ServerPlayer player, BlockPos start, Profile profile, boolean simpleOnly) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && result.size() < profile.maxBlocks()) {
            BlockPos pos = queue.poll();
            result.add(pos);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (seen.add(next) && movable(level, player, next, profile) && (!simpleOnly || simple(level, next))) {
                    queue.add(next);
                }
            }
        }
        if (simpleOnly) {
            return result;
        }
        List<BlockPos> partners = new ArrayList<>();
        for (BlockPos pos : result) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) && state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER) {
                BlockPos upper = pos.above();
                if (!result.contains(upper) && level.getBlockState(upper).is(state.getBlock())) {
                    partners.add(upper);
                }
            }
        }
        result.addAll(partners);
        return result;
    }

    public static boolean simple(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return level.getBlockEntity(pos) == null && !state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF);
    }
}
