package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.Set;

public final class GauntletClientState {
    private static GauntletHudPayload hud = GauntletHudPayload.EMPTY;
    private static Set<Integer> sightEntities = Set.of();
    private static List<BlockPos> sightOres = List.of();
    private static long sightExpiresAt;

    private GauntletClientState() {
    }

    public static void setHud(GauntletHudPayload payload) {
        hud = payload;
    }

    public static GauntletHudPayload hud() {
        return hud;
    }

    public static void setMindSight(MindSightPayload payload) {
        sightEntities = Set.copyOf(payload.entities());
        sightOres = payload.ores().stream().map(BlockPos::of).toList();
        sightExpiresAt = System.currentTimeMillis() + payload.durationTicks() * 50L;
    }

    public static boolean mindSightActive() {
        return System.currentTimeMillis() < sightExpiresAt;
    }

    public static Set<Integer> sightEntities() {
        return sightEntities;
    }

    public static List<BlockPos> sightOres() {
        return sightOres;
    }

    public static void reset() {
        hud = GauntletHudPayload.EMPTY;
        sightEntities = Set.of();
        sightOres = List.of();
        sightExpiresAt = 0L;
    }
}
