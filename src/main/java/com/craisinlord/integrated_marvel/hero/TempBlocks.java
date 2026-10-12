package com.craisinlord.integrated_marvel.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.event.level.BlockEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class TempBlocks {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Entry>> PLACED = new HashMap<>();

    private TempBlocks() {
    }

    private record Entry(BlockState placed, BlockState original, long expires) {
    }

    public static boolean place(ServerLevel level, ServerPlayer player, BlockPos pos, int ticks) {
        return place(level, player, pos, null, ticks);
    }

    public static boolean place(ServerLevel level, ServerPlayer player, BlockPos pos, BlockState block, int ticks) {
        BlockState current = level.getBlockState(pos);
        Map<BlockPos, Entry> placed = PLACED.computeIfAbsent(level.dimension(), ignored -> new HashMap<>());
        Entry existing = placed.get(pos);
        if (existing != null && current == existing.placed()) {
            placed.put(pos.immutable(), new Entry(existing.placed(), existing.original(), Math.max(existing.expires(), level.getGameTime() + ticks)));
            return true;
        }
        if (player != null && !level.mayInteract(player, pos)) {
            return false;
        }
        BlockState state;
        if (block != null) {
            if (!current.canBeReplaced() || !current.getFluidState().isEmpty()) {
                return false;
            }
            state = block;
        } else if (current.isAir()) {
            state = Blocks.PACKED_ICE.defaultBlockState();
        } else if (current.getFluidState().is(Fluids.WATER) && current.getFluidState().isSource() && current.is(Blocks.WATER)) {
            state = Blocks.FROSTED_ICE.defaultBlockState();
        } else {
            return false;
        }
        level.setBlockAndUpdate(pos, state);
        placed.put(pos.immutable(), new Entry(state, current, level.getGameTime() + ticks));
        return true;
    }

    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        if (placed == null) {
            return;
        }
        Entry entry = placed.get(event.getPos());
        if (entry == null || event.getState() != entry.placed()) {
            return;
        }
        event.setCanceled(true);
        placed.remove(event.getPos());
        level.setBlockAndUpdate(event.getPos(), entry.original());
    }

    public static void tick(ServerLevel level) {
        Map<BlockPos, Entry> placed = PLACED.get(level.dimension());
        if (placed == null || placed.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Entry>> iterator = placed.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Entry> entry = iterator.next();
            Entry value = entry.getValue();
            if (now < value.expires()) {
                continue;
            }
            BlockPos pos = entry.getKey();
            if (!level.isLoaded(pos)) {
                continue;
            }
            if (level.getBlockState(pos).is(value.placed().getBlock())) {
                level.setBlockAndUpdate(pos, value.original());
            }
            iterator.remove();
        }
    }
}
