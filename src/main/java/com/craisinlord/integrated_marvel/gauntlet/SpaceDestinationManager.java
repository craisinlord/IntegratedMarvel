package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.antarchy.compat.infinity.InfinityCompat;
import com.craisinlord.integrated_marvel.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_marvel.gauntlet.GauntletAbilityController;
import com.craisinlord.antarchy.content.portal.CrossDimensionTravelApi;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceDestinationManager {
    private static final Map<UUID, Destination> DESTINATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingTravel> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingPortal> PENDING_PORTALS = new ConcurrentHashMap<>();

    private SpaceDestinationManager() {}

    public static Destination selected(ServerPlayer player) {
        return DESTINATIONS.get(player.getUUID());
    }

    public static void select(ServerPlayer player, SpaceDestinationPayload payload) {
        if (!validGauntlet(player) || Math.abs((long) payload.x()) > 29_999_984L
                || Math.abs((long) payload.z()) > 29_999_984L) return;
        ResourceLocation id = payload.dimension();
        if (payload.generate()) {
            id = InfinityCompat.get().getRandomDimensionId(player.getRandom());
            if (id == null || !id.getNamespace().equals("infinity")) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Infinite Dimensions could not create a destination."), true);
                return;
            }
            String generation = requestDimensionCreation(player.server, id);
            if (!"READY".equals(generation) && !"PENDING".equals(generation)) {
                String message = "UNAVAILABLE".equals(generation)
                        ? "This Antarchy build lacks the generation-only Infinity hook; rebuild/update Antarchy first."
                        : "Infinite Dimensions rejected the generation request.";
                player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
                return;
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Generating " + id + "; destination saved."), true);
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!payload.generate() && player.server.registryAccess().lookupOrThrow(Registries.DIMENSION).get(key).isEmpty()) return;
        Destination destination = new Destination(key, new BlockPos(payload.x(), payload.y(), payload.z()));
        DESTINATIONS.put(player.getUUID(), destination);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Space destination set: " + id + " @ "
                + payload.x() + ", " + payload.y() + ", " + payload.z()), true);
    }

    public static boolean travel(ServerPlayer player) {
        Destination destination = selected(player);
        if (destination == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Choose a Space destination first (O)."), true);
            return false;
        }
        if (!validGauntlet(player) || PENDING.containsKey(player.getUUID())) return false;
        PENDING.put(player.getUUID(), new PendingTravel(destination, player.server.overworld().getGameTime() + 600L));
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Space Fold: resolving destination…"), true);
        return true;
    }

    public static boolean openPortal(ServerPlayer player, BlockPos sourceBlock, net.minecraft.world.phys.Vec3 sourcePosition,
                                     net.minecraft.world.phys.Vec3 normal) {
        Destination destination = selected(player);
        if (destination == null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("Choose a Space destination first (O)."), true);
            return false;
        }
        PendingPortal request = new PendingPortal(player.serverLevel().dimension(), sourceBlock,
                sourcePosition, normal, destination, player.server.overworld().getGameTime() + 600L);
        if (PENDING_PORTALS.putIfAbsent(player.getUUID(), request) != null) {
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("A Space rift is already resolving."), true);
            return false;
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal("Rift destination is resolving…"), true);
        return true;
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, PendingTravel> entry : PENDING.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PendingTravel pending = entry.getValue();
            if (player == null) {
                PENDING.remove(entry.getKey(), pending);
                continue;
            }
            ServerLevel destination = server.getLevel(pending.destination().dimension());
            if (destination == null) {
                if (now >= pending.expiresAt()) finish(player, pending, false, "Dimension generation timed out.");
                continue;
            }
            BlockPos target = clamp(pending.destination().position(), destination);
            ChunkPos center = new ChunkPos(target);
            boolean ready = true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                var future = destination.getChunkSource().getChunkFuture(center.x + dx, center.z + dz, ChunkStatus.FULL, true);
                if (!future.isDone() || future.isCompletedExceptionally()) ready = false;
            }
            if (!ready) {
                if (now >= pending.expiresAt()) finish(player, pending, false, "Destination chunks did not become ready.");
                continue;
            }
            boolean moved = CrossDimensionTravelApi.transferToSafePosition(player, destination, target);
            finish(player, pending, moved, moved ? "Space Fold complete." : "No safe arrival position was found.");
        }
        for (Map.Entry<UUID, PendingPortal> entry : PENDING_PORTALS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PendingPortal pending = entry.getValue();
            if (player == null) {
                PENDING_PORTALS.remove(entry.getKey(), pending);
                continue;
            }
            ServerLevel destination = server.getLevel(pending.destination().dimension());
            if (destination == null) {
                if (now >= pending.expiresAt()) finishPortal(player, pending, false);
                continue;
            }
            BlockPos target = clamp(pending.destination().position(), destination);
            ChunkPos center = new ChunkPos(target);
            boolean ready = true;
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                var future = destination.getChunkSource().getChunkFuture(center.x + dx, center.z + dz, ChunkStatus.FULL, true);
                if (!future.isDone() || future.isCompletedExceptionally()) ready = false;
            }
            if (ready) {
                ServerLevel source = server.getLevel(pending.sourceDimension());
                boolean created = source != null && SpacePortalManager.create(source, pending.sourceBlock(), pending.sourcePosition(),
                        pending.normal(), destination, target, player.getUUID(), now);
                finishPortal(player, pending, created);
            } else if (now >= pending.expiresAt()) {
                finishPortal(player, pending, false);
            }
        }
    }

    public static void clear() {
        DESTINATIONS.clear();
        PENDING.clear();
        PENDING_PORTALS.clear();
    }

    public static void clearPlayer(UUID id) {
        DESTINATIONS.remove(id);
        PENDING.remove(id);
        PENDING_PORTALS.remove(id);
    }

    private static void finish(ServerPlayer player, PendingTravel pending, boolean success, String message) {
        PENDING.remove(player.getUUID(), pending);
        if (success) {
            GauntletAbilityController.addAbilityCooldown(player, 100);
            player.serverLevel().playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
                    player.getSoundSource(), 1.0F, 1.0F);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(message), true);
    }

    private static void finishPortal(ServerPlayer player, PendingPortal pending, boolean success) {
        if (!PENDING_PORTALS.remove(player.getUUID(), pending)) return;
        if (success) GauntletAbilityController.addAbilityCooldown(player, 200);
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(success ? "Temporary dimensional rift opened." : "Rift destination failed to become ready."), true);
    }

    private static BlockPos clamp(BlockPos pos, ServerLevel level) {
        return new BlockPos(pos.getX(), Math.clamp(pos.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 2), pos.getZ());
    }

    private static boolean validGauntlet(ServerPlayer player) {
        var stack = InfinityGauntletStones.equippedGauntlet(player);
        return !stack.isEmpty() && InfinityGauntletStones.hasStone(stack, "space")
                && InfinityGauntletStones.selectedStone(stack).equals("space");
    }

    private static String requestDimensionCreation(MinecraftServer server, ResourceLocation id) {
        try {
            var method = InfinityCompat.get().getClass().getMethod("requestDimensionCreation", MinecraftServer.class,
                    ResourceLocation.class);
            Object result = method.invoke(InfinityCompat.get(), server, id);
            return result instanceof Enum<?> status ? status.name() : "REJECTED";
        } catch (ReflectiveOperationException exception) {
            return "UNAVAILABLE";
        }
    }

    public record Destination(ResourceKey<Level> dimension, BlockPos position) {}
    private record PendingTravel(Destination destination, long expiresAt) {}
    private record PendingPortal(ResourceKey<Level> sourceDimension, BlockPos sourceBlock,
                                 net.minecraft.world.phys.Vec3 sourcePosition, net.minecraft.world.phys.Vec3 normal,
                                 Destination destination, long expiresAt) {}
}
