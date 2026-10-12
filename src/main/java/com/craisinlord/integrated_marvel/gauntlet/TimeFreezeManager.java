package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.gauntlet.InfinityGauntletStones;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeFreezeManager {
    private static final Map<UUID, ActiveFreeze> ACTIVE = new ConcurrentHashMap<>();
    private static final Map<UUID, FrozenEntity> FROZEN = new ConcurrentHashMap<>();

    private TimeFreezeManager() {
    }

    public static void start(ServerPlayer player) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty() || !isTimeFreezeSelected(gauntlet) || ACTIVE.containsKey(player.getUUID())) return;
        ACTIVE.put(player.getUUID(), new ActiveFreeze(player.serverLevel().dimension()));
        freezeLoadedEntities(player.serverLevel(), player.getUUID());
        PacketDistributor.sendToPlayer(player, new TimeFreezePayload(true));
    }

    public static void stop(ServerPlayer player) {
        stop(player.getUUID(), player);
    }

    public static void stop(UUID owner, ServerPlayer player) {
        ActiveFreeze removed = ACTIVE.remove(owner);
        if (removed == null) return;
        for (Map.Entry<UUID, FrozenEntity> entry : FROZEN.entrySet()) {
            FrozenEntity frozen = entry.getValue();
            if (!frozen.owners().remove(owner) || !frozen.owners().isEmpty()) continue;
            if (!frozen.entity().isRemoved()) {
                frozen.entity().setNoGravity(frozen.noGravity());
                if (frozen.entity() instanceof Mob mob) mob.setNoAi(frozen.noAi());
            }
            FROZEN.remove(entry.getKey(), frozen);
        }
        if (player != null) PacketDistributor.sendToPlayer(player, new TimeFreezePayload(false));
    }

    public static void tick(MinecraftServer server) {
        if (ACTIVE.isEmpty()) return;
        for (UUID owner : Set.copyOf(ACTIVE.keySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(owner);
            ActiveFreeze freeze = ACTIVE.get(owner);
            if (player == null || freeze == null || !isTimeFreezeHeld(player)
                    || !player.serverLevel().dimension().equals(freeze.dimension())) {
                stop(owner, player);
                continue;
            }
            if (player.serverLevel().getGameTime() % 5L == 0L) {
                freezeLoadedEntities(player.serverLevel(), owner);
            }
        }
    }

    public static void clear(MinecraftServer server) {
        for (UUID owner : Set.copyOf(ACTIVE.keySet())) {
            stop(owner, server.getPlayerList().getPlayer(owner));
        }
        for (FrozenEntity frozen : FROZEN.values()) {
            if (!frozen.entity().isRemoved()) {
                frozen.entity().setNoGravity(frozen.noGravity());
                if (frozen.entity() instanceof Mob mob) mob.setNoAi(frozen.noAi());
            }
        }
        ACTIVE.clear();
        FROZEN.clear();
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
                && InfinityGauntletStones.selectedAbility(gauntlet, "time") == 0;
    }

    private static void freezeLoadedEntities(ServerLevel level, UUID owner) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof net.minecraft.world.entity.player.Player || entity.isRemoved()) continue;
            FrozenEntity frozen = FROZEN.computeIfAbsent(entity.getUUID(), id -> new FrozenEntity(
                    entity, entity.isNoGravity(), entity instanceof Mob mob && mob.isNoAi()));
            if (frozen.owners().add(owner)) {
                entity.setNoGravity(true);
                if (entity instanceof Mob mob) mob.setNoAi(true);
            }
        }
    }

    private record ActiveFreeze(net.minecraft.resources.ResourceKey<Level> dimension) {
    }

    private record FrozenEntity(Entity entity, boolean noGravity, boolean noAi, Set<UUID> owners) {
        private FrozenEntity(Entity entity, boolean noGravity, boolean noAi) {
            this(entity, noGravity, noAi, new HashSet<>());
        }
    }
}
