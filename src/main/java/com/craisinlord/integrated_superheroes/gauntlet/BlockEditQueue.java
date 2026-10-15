package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Clearable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public final class BlockEditQueue {
    private static final List<Job> JOBS = new ArrayList<>();

    private BlockEditQueue() {
    }

    public record Edit(BlockPos pos, BlockState state, CompoundTag data, BlockState expected) {
        public Edit(BlockPos pos, BlockState state, CompoundTag data) {
            this(pos, state, data, null);
        }
    }

    public static final class Job {
        private final ResourceKey<Level> dimension;
        private final Deque<Edit> edits;
        private final String stone;
        private final int flags;
        private final List<Edit> undo = new ArrayList<>();
        private Consumer<Job> onComplete;
        private EditListener listener;
        private UUID guard;
        private int applied;
        private int denied;
        private boolean keepUndo = true;

        private Job(ResourceKey<Level> dimension, Collection<Edit> edits, String stone, int flags) {
            this.dimension = dimension;
            this.edits = new ArrayDeque<>(edits);
            this.stone = stone;
            this.flags = flags;
        }

        public Job onComplete(Consumer<Job> callback) {
            onComplete = callback;
            return this;
        }

        public Job onEdit(EditListener callback) {
            listener = callback;
            return this;
        }

        public Job withoutUndo() {
            keepUndo = false;
            return this;
        }

        public Job guardedBy(ServerPlayer player) {
            guard = player.getUUID();
            return this;
        }

        public List<Edit> undo() {
            return undo;
        }

        public ResourceKey<Level> dimension() {
            return dimension;
        }

        public int applied() {
            return applied;
        }

        public int denied() {
            return denied;
        }
    }

    public interface EditListener {
        void edited(ServerLevel level, BlockPos pos, BlockState previous, BlockState replacement);
    }

    public static Job submit(ServerLevel level, Collection<Edit> edits, String stone, int flags) {
        Job job = new Job(level.dimension(), edits, stone, flags);
        JOBS.add(job);
        return job;
    }

    public static void tick(MinecraftServer server) {
        if (JOBS.isEmpty()) return;
        int budget = GauntletConfig.BLOCK_EDITS_PER_TICK.get();
        for (var iterator = JOBS.iterator(); iterator.hasNext() && budget > 0;) {
            Job job = iterator.next();
            ServerLevel level = server.getLevel(job.dimension);
            if (level == null) {
                iterator.remove();
                continue;
            }
            ServerPlayer guard = job.guard == null ? null : server.getPlayerList().getPlayer(job.guard);
            while (budget > 0 && !job.edits.isEmpty()) {
                Edit edit = job.edits.poll();
                budget--;
                if (!level.isLoaded(edit.pos())) continue;
                if (job.guard != null && (guard == null || !GauntletGrief.canEdit(guard, level, edit.pos()))) {
                    job.denied++;
                    continue;
                }
                apply(level, job, edit);
            }
            if (job.edits.isEmpty()) {
                iterator.remove();
                if (job.onComplete != null) job.onComplete.accept(job);
            }
        }
    }

    private static void apply(ServerLevel level, Job job, Edit edit) {
        BlockPos pos = edit.pos();
        BlockState previous = level.getBlockState(pos);
        if (edit.expected() != null && previous != edit.expected()) return;
        if (previous == edit.state() && edit.data() == null) return;
        BlockEntity previousEntity = level.getBlockEntity(pos);
        CompoundTag previousData = previousEntity == null ? null : previousEntity.saveWithFullMetadata(level.registryAccess());
        if (previousEntity != null) Clearable.tryClear(previousEntity);
        if (!level.setBlock(pos, edit.state(), job.flags) && previous != edit.state()) {
            if (previousEntity != null && previousData != null) previousEntity.loadWithComponents(previousData, level.registryAccess());
            return;
        }
        if (job.keepUndo) job.undo.add(new Edit(pos.immutable(), previous, previousData));
        if (edit.data() != null) {
            BlockEntity destination = level.getBlockEntity(pos);
            if (destination != null) {
                CompoundTag data = edit.data().copy();
                data.putInt("x", pos.getX());
                data.putInt("y", pos.getY());
                data.putInt("z", pos.getZ());
                destination.loadWithComponents(data, level.registryAccess());
                destination.setChanged();
                level.sendBlockUpdated(pos, edit.state(), edit.state(), 3);
            }
        }
        job.applied++;
        if (job.stone != null && job.applied % 12 == 0) {
            GauntletFx.burst(level, Vec3.atCenterOf(pos), job.stone, 3, 0.4D);
        }
        if (job.listener != null) job.listener.edited(level, pos, previous, edit.state());
    }

    public static void clear() {
        JOBS.clear();
    }
}
