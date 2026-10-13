package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

public final class HeroGnaw {
    public enum Step {
        CHEWING,
        BROKE,
        DONE
    }

    private final Block block;
    private final Deque<BlockPos> queue = new ArrayDeque<>();
    private final Set<BlockPos> seen = new HashSet<>();
    private final int crackId;
    private BlockPos current;
    private float progress;
    private int broken;

    private HeroGnaw(Block block, BlockPos start, int crackId) {
        this.block = block;
        this.crackId = crackId;
        this.queue.add(start);
        this.seen.add(start);
    }

    public static HeroGnaw aim(ServerPlayer player, double range, Predicate<BlockState> allowed, int crackId) {
        Vec3 eye = player.getEyePosition();
        BlockHitResult hit = player.level().clip(new ClipContext(eye, eye.add(player.getLookAngle().scale(range)),
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        BlockPos pos = hit.getBlockPos();
        BlockState state = player.level().getBlockState(pos);
        if (!gnawable(player.serverLevel(), pos, state) || !allowed.test(state)) {
            return null;
        }
        player.serverLevel().sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D, 6, 0.3D, 0.1D, 0.3D, 0.0D);
        return new HeroGnaw(state.getBlock(), pos.immutable(), crackId);
    }

    public static boolean gnawable(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty() && state.getDestroySpeed(level, pos) >= 0.0F
                && !state.is(BlockTags.WITHER_IMMUNE);
    }

    public BlockPos target() {
        return this.current != null ? this.current : this.queue.peek();
    }

    public void stop(ServerLevel level) {
        if (this.current != null) {
            level.destroyBlockProgress(this.crackId, this.current, -1);
        }
    }

    public Step tick(ServerPlayer player, ToLongFunction<Vec3> chewers, double ticksPerHardness, boolean drops, int max) {
        ServerLevel level = player.serverLevel();
        while (this.current == null) {
            BlockPos next = this.queue.poll();
            if (next == null) {
                return Step.DONE;
            }
            BlockState state = level.getBlockState(next);
            if (state.is(this.block) && gnawable(level, next, state)) {
                this.current = next;
                this.progress = 0.0F;
            }
        }
        BlockPos pos = this.current;
        BlockState state = level.getBlockState(pos);
        if (!state.is(this.block)) {
            level.destroyBlockProgress(this.crackId, pos, -1);
            this.current = null;
            return Step.CHEWING;
        }
        Vec3 center = Vec3.atCenterOf(pos);
        long chewing = chewers.applyAsLong(center);
        if (chewing == 0) {
            return Step.CHEWING;
        }
        float hardness = Math.max(0.3F, state.getDestroySpeed(level, pos));
        this.progress += chewing / (float) (hardness * ticksPerHardness);
        if (player.tickCount % 4 == 0) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), center.x, center.y, center.z, 4, 0.3D, 0.3D, 0.3D, 0.05D);
            level.playSound(null, pos, state.getSoundType().getHitSound(), player.getSoundSource(), 0.4F, 1.3F);
        }
        if (this.progress < 1.0F) {
            level.destroyBlockProgress(this.crackId, pos, (int) (this.progress * 10.0F));
            return Step.CHEWING;
        }
        level.destroyBlockProgress(this.crackId, pos, -1);
        this.current = null;
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, player);
        NeoForge.EVENT_BUS.post(event);
        if (event.isCanceled()) {
            return Step.CHEWING;
        }
        level.destroyBlock(pos, drops, player);
        this.broken++;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos neighbor = pos.offset(dx, dy, dz);
                    if (this.broken + this.queue.size() < max && this.seen.add(neighbor) && level.getBlockState(neighbor).is(this.block)) {
                        this.queue.add(neighbor);
                    }
                }
            }
        }
        return Step.BROKE;
    }
}
