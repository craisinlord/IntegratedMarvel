package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class GauntletHudSync {
    private static final Map<UUID, GauntletHudPayload> LAST = new ConcurrentHashMap<>();

    private GauntletHudSync() {
    }

    public static void tick(MinecraftServer server) {
        if (server.overworld().getGameTime() % 5L != 0L) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (InfinityGauntletStones.equippedGauntlet(player).isEmpty()) continue;
            GauntletHudPayload payload = build(player);
            if (!payload.equals(LAST.get(player.getUUID()))) {
                LAST.put(player.getUUID(), payload);
                PacketDistributor.sendToPlayer(player, payload);
            }
        }
    }

    private static GauntletHudPayload build(ServerPlayer player) {
        UUID id = player.getUUID();
        RealityClipboard.Clip clip = RealityClipboard.clip(id);
        float fill = Math.round(TimeRewindManager.fill(player) * 20.0F) / 20.0F;
        int rate = (int) Math.round(TimeStone.fieldRate(id) * 100.0D);
        if (clip == null) {
            return new GauntletHudPayload("", 0, List.of(0, 0, 0), 0, 0, false, fill, RealityStone.radius(id), rate);
        }
        return new GauntletHudPayload(clip.label(), clip.count(),
                List.of(clip.size().getX(), clip.size().getY(), clip.size().getZ()), clip.facing().get2DDataValue(),
                RealityClipboard.rotation(id), clip.entity() != null, fill, RealityStone.radius(id), rate);
    }

    public static void clearPlayer(UUID player) {
        LAST.remove(player);
    }

    public static void clear() {
        LAST.clear();
    }
}
