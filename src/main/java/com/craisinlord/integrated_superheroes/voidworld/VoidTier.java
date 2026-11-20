package com.craisinlord.integrated_superheroes.voidworld;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.List;

public final class VoidTier {
    public static final int MAX = 5;
    public static final String ENTITY_TIER_KEY = "integrated_superheroes_void_tier";
    public static final String VISIT_START_KEY = "integrated_superheroes_void_visit_start";
    private static final int[] DEFAULT_EDGES = {0, 600, 1800, 3600, 6000, 9000};
    private static volatile int[] edges = DEFAULT_EDGES;

    private VoidTier() {
    }

    public static int tierAt(int x, int z) {
        double distance = Math.sqrt((double) x * x + (double) z * z);
        int[] current = edges;
        int tier = 0;
        for (int i = 1; i < current.length && i <= MAX; i++) {
            if (distance >= current[i]) {
                tier = i;
            }
        }
        return tier;
    }

    public static int at(Level level, BlockPos pos) {
        if (level == null || pos == null || !VoidWorld.isVoid(level)) {
            return 0;
        }
        return tierAt(pos.getX(), pos.getZ());
    }

    public static int forPlayer(Player player) {
        if (player == null || !VoidWorld.isVoid(player.level())) {
            return 0;
        }
        return Math.min(MAX, tierAt(player.getBlockX(), player.getBlockZ()) + timeTiers(player));
    }

    public static int tierOf(Entity entity) {
        if (entity == null) {
            return 0;
        }
        if (entity instanceof Player player) {
            return forPlayer(player);
        }
        CompoundTag data = entity.getPersistentData();
        if (data.contains(ENTITY_TIER_KEY)) {
            return clamp(data.getInt(ENTITY_TIER_KEY));
        }
        return at(entity.level(), entity.blockPosition());
    }

    public static void stamp(Entity entity, int tier) {
        if (entity != null) {
            entity.getPersistentData().putInt(ENTITY_TIER_KEY, clamp(tier));
        }
    }

    public static boolean isStamped(Entity entity) {
        return entity != null && entity.getPersistentData().contains(ENTITY_TIER_KEY);
    }

    public static int timeTiers(Player player) {
        int minutes = VoidConfig.SPEC.isLoaded() ? VoidConfig.MINUTES_PER_TIME_TIER.get() : 10;
        int cap = VoidConfig.SPEC.isLoaded() ? VoidConfig.MAX_TIME_TIERS.get() : MAX;
        if (minutes <= 0) {
            return 0;
        }
        long ticks = visitTicks(player);
        return (int) Math.min(cap, ticks / (minutes * 1200L));
    }

    public static long visitTicks(Player player) {
        if (player == null || !VoidWorld.isVoid(player.level())) {
            return 0L;
        }
        CompoundTag data = player.getPersistentData();
        if (!data.contains(VISIT_START_KEY)) {
            return 0L;
        }
        return Math.max(0L, player.level().getGameTime() - data.getLong(VISIT_START_KEY));
    }

    public static int clamp(int tier) {
        return Math.max(0, Math.min(MAX, tier));
    }

    static void reloadEdges() {
        if (!VoidConfig.SPEC.isLoaded()) {
            edges = DEFAULT_EDGES;
            return;
        }
        List<? extends Integer> list = VoidConfig.RING_EDGES.get();
        int[] parsed = new int[list.size()];
        for (int i = 0; i < parsed.length; i++) {
            parsed[i] = list.get(i);
        }
        edges = parsed.length == 0 ? DEFAULT_EDGES : parsed;
    }
}
