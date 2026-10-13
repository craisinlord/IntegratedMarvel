package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.compat.InfiniteDimensionsCompat;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SpaceDestinationManager {
    private static final Map<UUID, Destination> DESTINATIONS = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingTravel> PENDING = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingPortal> PENDING_PORTALS = new ConcurrentHashMap<>();
    private static final long FORCE_LOAD_AFTER_TICKS = 100L;
    private static final long DIMENSION_TIMEOUT_TICKS = 1200L;

    private SpaceDestinationManager() {}

    public static Destination selected(ServerPlayer player) {
        return DESTINATIONS.get(player.getUUID());
    }

    public static void select(ServerPlayer player, SpaceDestinationPayload payload) {
        if (!validGauntlet(player)) {
            player.displayClientMessage(Component.literal("Select the Space Stone on your gauntlet first."), true);
            return;
        }
        if (Math.abs((long) payload.x()) > 29_999_984L || Math.abs((long) payload.z()) > 29_999_984L) {
            player.displayClientMessage(Component.literal("Those coordinates are outside the world."), true);
            return;
        }
        ResourceLocation id = payload.dimension();
        boolean generated = false;
        if (payload.generate() && ModCompat.INFINITE_DIMENSIONS) {
            id = InfiniteDimensionsCompat.randomDimensionId(player.getRandom());
            if (id == null || !id.getNamespace().equals(InfiniteDimensionsCompat.NAMESPACE)) {
                player.displayClientMessage(Component.literal("Infinite Dimensions could not create a destination."), true);
                return;
            }
            InfiniteDimensionsCompat.Generation generation = InfiniteDimensionsCompat.requestCreation(player.server, id);
            if (generation != InfiniteDimensionsCompat.Generation.READY && generation != InfiniteDimensionsCompat.Generation.PENDING) {
                String message = generation == InfiniteDimensionsCompat.Generation.UNAVAILABLE
                        ? "This Infinite Dimensions version does not support generating destinations."
                        : "Infinite Dimensions rejected the generation request.";
                player.displayClientMessage(Component.literal(message), true);
                return;
            }
            generated = true;
            player.displayClientMessage(Component.literal("Generating " + id + "; destination saved."), true);
        } else if (payload.generate()) {
            id = randomVanillaDimension(player);
            if (id == null) {
                player.displayClientMessage(Component.literal("No other dimension is available."), true);
                return;
            }
        }
        ResourceKey<Level> key = ResourceKey.create(Registries.DIMENSION, id);
        if (!generated && player.server.getLevel(key) == null) {
            player.displayClientMessage(Component.literal("Dimension " + id + " is not available on this server."), true);
            return;
        }
        DESTINATIONS.put(player.getUUID(), new Destination(key, new BlockPos(payload.x(), payload.y(), payload.z())));
        player.displayClientMessage(Component.literal("Space destination set: " + id + " @ "
                + payload.x() + ", " + payload.y() + ", " + payload.z()), true);
    }

    public static boolean travel(ServerPlayer player) {
        Destination destination = selected(player);
        if (destination == null) {
            player.displayClientMessage(Component.literal("Choose a Space destination first (O)."), true);
            return false;
        }
        if (!validGauntlet(player)) return false;
        if (PENDING.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.literal("Space Fold is already resolving."), true);
            return false;
        }
        PENDING.put(player.getUUID(), new PendingTravel(destination, player.server.overworld().getGameTime()));
        player.displayClientMessage(Component.literal("Space Fold: resolving destination…"), true);
        return true;
    }

    public static boolean openPortal(ServerPlayer player, BlockPos sourceBlock, Vec3 sourcePosition, Vec3 normal) {
        Destination destination = selected(player);
        if (destination == null) {
            player.displayClientMessage(Component.literal("Choose a Space destination first (O)."), true);
            return false;
        }
        PendingPortal request = new PendingPortal(player.serverLevel().dimension(), sourcePosition, normal,
                player.position(), destination, player.server.overworld().getGameTime());
        if (PENDING_PORTALS.putIfAbsent(player.getUUID(), request) != null) {
            player.displayClientMessage(Component.literal("A Space rift is already resolving."), true);
            return false;
        }
        player.displayClientMessage(Component.literal("Rift destination is resolving…"), true);
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
            Resolution resolution = resolve(server, pending.destination(), pending.startedAt(), now);
            if (resolution.failure() != null) {
                finish(player, pending, false, resolution.failure());
            } else if (resolution.level() != null) {
                Vec3 arrival = SpaceTravel.arrivalPosition(player, resolution.level(), resolution.target());
                boolean moved = SpaceTravel.teleport(player, resolution.level(), arrival, player.getYRot());
                finish(player, pending, moved, moved ? "Space Fold complete." : "Space Fold could not move you.");
            }
        }
        for (Map.Entry<UUID, PendingPortal> entry : PENDING_PORTALS.entrySet()) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            PendingPortal pending = entry.getValue();
            if (player == null) {
                PENDING_PORTALS.remove(entry.getKey(), pending);
                continue;
            }
            Resolution resolution = resolve(server, pending.destination(), pending.startedAt(), now);
            if (resolution.failure() != null) {
                finishPortal(player, pending, false, resolution.failure());
                continue;
            }
            if (resolution.level() == null) continue;
            ServerLevel source = server.getLevel(pending.sourceDimension());
            if (source == null) {
                finishPortal(player, pending, false, "The rift's origin dimension is no longer loaded.");
                continue;
            }
            boolean created = SpacePortalManager.create(source, pending.sourcePosition(), pending.normal(),
                    pending.openerPosition(), resolution.level(), resolution.target(), player, now);
            finishPortal(player, pending, created, created ? "Temporary dimensional rift opened."
                    : "The rift collapsed before it could open.");
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

    private static Resolution resolve(MinecraftServer server, Destination destination, long startedAt, long now) {
        long waited = now - startedAt;
        ServerLevel level = server.getLevel(destination.dimension());
        if (level == null) {
            return waited >= DIMENSION_TIMEOUT_TICKS
                    ? Resolution.failed("Dimension " + destination.dimension().location() + " is not available.")
                    : Resolution.WAITING;
        }
        BlockPos target = SpaceTravel.clamp(destination.position(), level);
        if (!level.getWorldBorder().isWithinBounds(target)) return Resolution.failed("Destination is outside the world border.");
        SpaceTravel.keepLoaded(level, target);
        if (SpaceTravel.isLoaded(level, target)) return Resolution.ready(level, target);
        if (waited < FORCE_LOAD_AFTER_TICKS) return Resolution.WAITING;
        return SpaceTravel.forceLoad(level, target)
                ? Resolution.ready(level, target)
                : Resolution.failed("Destination chunks could not be loaded.");
    }

    private static void finish(ServerPlayer player, PendingTravel pending, boolean success, String message) {
        PENDING.remove(player.getUUID(), pending);
        if (success) GauntletAbilityController.addAbilityCooldown(player, 100);
        player.displayClientMessage(Component.literal(message), true);
    }

    private static void finishPortal(ServerPlayer player, PendingPortal pending, boolean success, String message) {
        if (!PENDING_PORTALS.remove(player.getUUID(), pending)) return;
        if (success) GauntletAbilityController.addAbilityCooldown(player, 200);
        player.displayClientMessage(Component.literal(message), true);
    }

    private static boolean validGauntlet(ServerPlayer player) {
        var stack = InfinityGauntletStones.equippedGauntlet(player);
        return !stack.isEmpty() && InfinityGauntletStones.hasStone(stack, "space")
                && InfinityGauntletStones.selectedStone(stack).equals("space");
    }

    private static ResourceLocation randomVanillaDimension(ServerPlayer player) {
        List<ResourceLocation> options = new ArrayList<>();
        for (ResourceKey<Level> key : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            if (!key.equals(player.level().dimension()) && player.server.getLevel(key) != null) {
                options.add(key.location());
            }
        }
        return options.isEmpty() ? null : options.get(player.getRandom().nextInt(options.size()));
    }

    public record Destination(ResourceKey<Level> dimension, BlockPos position) {}
    private record PendingTravel(Destination destination, long startedAt) {}
    private record PendingPortal(ResourceKey<Level> sourceDimension, Vec3 sourcePosition, Vec3 normal,
                                 Vec3 openerPosition, Destination destination, long startedAt) {}

    private record Resolution(ServerLevel level, BlockPos target, String failure) {
        private static final Resolution WAITING = new Resolution(null, null, null);

        private static Resolution ready(ServerLevel level, BlockPos target) {
            return new Resolution(level, target, null);
        }

        private static Resolution failed(String failure) {
            return new Resolution(null, null, failure);
        }
    }
}
