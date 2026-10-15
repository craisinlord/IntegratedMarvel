package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.mixin.ServerGamePacketListenerAccessor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeFreezeManager {
    private static final Map<UUID, ResourceKey<Level>> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, Anchor> ANCHORS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> TIMED = new ConcurrentHashMap<>();
    private static volatile Map<ResourceKey<Level>, Set<UUID>> byDimension = Map.of();

    private TimeFreezeManager() {
    }

    public static Set<UUID> owners(ResourceKey<Level> dimension) {
        return byDimension.getOrDefault(dimension, Set.of());
    }

    public static void start(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty() || !isTimeFreezeSelected(gauntlet) || ACTIVE.containsKey(player.getUUID())) return;
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        ACTIVE.put(player.getUUID(), dimension);
        rebuild();
        sync(player.server, dimension);
    }

    public static boolean startTimed(ServerPlayer player, int ticks) {
        if (ACTIVE.containsKey(player.getUUID()) || ticks <= 0) return false;
        ResourceKey<Level> dimension = player.serverLevel().dimension();
        TIMED.put(player.getUUID(), player.server.getTickCount() + ticks);
        ACTIVE.put(player.getUUID(), dimension);
        rebuild();
        sync(player.server, dimension);
        return true;
    }

    public static boolean isTimed(UUID owner) {
        return TIMED.containsKey(owner);
    }

    public static void stopHeld(ServerPlayer player) {
        if (!TIMED.containsKey(player.getUUID())) stop(player);
    }

    public static void stop(ServerPlayer player) {
        stop(player.getUUID(), player);
    }

    public static void stop(UUID owner, ServerPlayer player) {
        TIMED.remove(owner);
        ResourceKey<Level> dimension = ACTIVE.remove(owner);
        if (dimension == null) return;
        rebuild();
        ANCHORS.values().removeIf(anchor -> !byDimension.containsKey(anchor.dimension()));
        MinecraftServer server = player != null ? player.server : ServerLifecycleHooks.getCurrentServer();
        if (server != null) sync(server, dimension);
        if (server != null) TimeStone.resolveStrikes(server, owner);
        if (player != null && !player.serverLevel().dimension().equals(dimension)) syncPlayer(player);
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (Map.Entry<UUID, ResourceKey<Level>> entry : Set.copyOf(ACTIVE.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Integer expires = TIMED.get(entry.getKey());
            boolean held = expires != null ? server.getTickCount() < expires : player != null && isTimeFreezeHeld(player);
            if (player == null || !held || !player.serverLevel().dimension().equals(entry.getValue())) {
                stop(entry.getKey(), player);
            }
        }
        for (ResourceKey<Level> dimension : byDimension.keySet()) {
            ServerLevel level = server.getLevel(dimension);
            if (level == null) continue;
            for (LivingEntity living : level.getEntities(EntityTypeTest.forClass(LivingEntity.class),
                    living -> living.invulnerableTime > 0)) {
                if (TimeFreezeState.isFrozen(living)) living.invulnerableTime--;
            }
            for (ServerPlayer player : level.players()) {
                if (!TimeFreezeState.isFrozenPlayer(player)) {
                    ANCHORS.remove(player.getUUID());
                    continue;
                }
                Anchor anchor = ANCHORS.get(player.getUUID());
                if (anchor == null || !anchor.dimension().equals(dimension)) {
                    anchor = new Anchor(dimension, player.position(), player.getYRot(), player.getXRot());
                    ANCHORS.put(player.getUUID(), anchor);
                }
                player.setDeltaMovement(Vec3.ZERO);
                if (!player.isPassenger() && player.position().distanceToSqr(anchor.position()) > 0.01D) {
                    player.connection.teleport(anchor.position().x, anchor.position().y, anchor.position().z,
                            anchor.yRot(), anchor.xRot());
                }
                ((ServerGamePacketListenerAccessor) player.connection).integratedSuperheroes$setAboveGroundTickCount(0);
            }
        }
    }

    public static void syncPlayer(ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new TimeFreezePayload(List.copyOf(owners(player.serverLevel().dimension()))));
    }

    public static void clear(MinecraftServer server) {
        Set<ResourceKey<Level>> dimensions = Set.copyOf(byDimension.keySet());
        ACTIVE.clear();
        ANCHORS.clear();
        TIMED.clear();
        rebuild();
        for (ResourceKey<Level> dimension : dimensions) sync(server, dimension);
    }

    private static void rebuild() {
        Map<ResourceKey<Level>, Set<UUID>> next = new HashMap<>();
        ACTIVE.forEach((owner, dimension) -> next.computeIfAbsent(dimension, key -> new HashSet<>()).add(owner));
        Map<ResourceKey<Level>, Set<UUID>> frozen = new HashMap<>();
        next.forEach((dimension, owners) -> frozen.put(dimension, Set.copyOf(owners)));
        byDimension = Map.copyOf(frozen);
    }

    private static void sync(MinecraftServer server, ResourceKey<Level> dimension) {
        ServerLevel level = server.getLevel(dimension);
        if (level == null) return;
        PacketDistributor.sendToPlayersInDimension(level, new TimeFreezePayload(List.copyOf(owners(dimension))));
    }

    private static boolean isTimeFreezeHeld(ServerPlayer player) {
        if (!player.isUsingItem()) return false;
        ItemStack held = player.getItemInHand(player.getUsedItemHand());
        return held == InfinityGauntletStones.equippedGauntlet(player) && isTimeFreezeSelected(held);
    }

    private static boolean isTimeFreezeSelected(ItemStack gauntlet) {
        return InfinityGauntletStones.isGauntlet(gauntlet)
                && InfinityGauntletStones.hasStone(gauntlet, "time")
                && InfinityGauntletStones.selectedStone(gauntlet).equals("time")
                && InfinityGauntletStones.selectedAbility(gauntlet, "time") == 0
                && GauntletConfig.isEnabled("time", 0);
    }

    private record Anchor(ResourceKey<Level> dimension, Vec3 position, float yRot, float xRot) {
    }
}
