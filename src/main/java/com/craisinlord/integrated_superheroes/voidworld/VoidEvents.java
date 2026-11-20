package com.craisinlord.integrated_superheroes.voidworld;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class VoidEvents {
    private static final Map<UUID, Long> LAST_CATCH = new ConcurrentHashMap<>();
    private static final Set<UUID> SETTLED = ConcurrentHashMap.newKeySet();

    private VoidEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || !VoidWorld.isVoid(player.level())) {
            return;
        }
        int catchY = VoidConfig.SPEC.isLoaded() ? VoidConfig.CATCH_Y.get() : 16;
        if (player.getY() >= catchY) {
            SETTLED.add(player.getUUID());
            return;
        }
        if (!SETTLED.contains(player.getUUID())) {
            VoidTravel.sendTo(player, player.getBlockX(), player.getBlockZ());
            SETTLED.add(player.getUUID());
            return;
        }
        long now = player.level().getGameTime();
        Long last = LAST_CATCH.get(player.getUUID());
        if (last != null && now - last < 20L) {
            return;
        }
        LAST_CATCH.put(player.getUUID(), now);
        VoidFallEvent fall = NeoForge.EVENT_BUS.post(new VoidFallEvent(player));
        if (!fall.isHandled()) {
            VoidTravel.sendToRespawn(player);
        }
    }

    @SubscribeEvent
    public static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL && event.getSpawnType() != MobSpawnType.CHUNK_GENERATION) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevelAccessor accessor) || !VoidWorld.isVoid(accessor.getLevel())) {
            return;
        }
        double chance = VoidConfig.SPEC.isLoaded() ? VoidConfig.NATURAL_SPAWN_CHANCE.get() : 0.1D;
        if (accessor.getRandom().nextDouble() >= chance) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public static void onChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        SETTLED.remove(event.getEntity().getUUID());
        CompoundTag data = event.getEntity().getPersistentData();
        if (event.getTo().equals(VoidWorld.LEVEL)) {
            data.putLong(VoidTier.VISIT_START_KEY, event.getEntity().level().getGameTime());
        } else if (event.getFrom().equals(VoidWorld.LEVEL)) {
            data.remove(VoidTier.VISIT_START_KEY);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        CompoundTag data = event.getEntity().getPersistentData();
        if (VoidWorld.isVoid(event.getEntity().level())) {
            if (!data.contains(VoidTier.VISIT_START_KEY)) {
                data.putLong(VoidTier.VISIT_START_KEY, event.getEntity().level().getGameTime());
            }
        } else {
            data.remove(VoidTier.VISIT_START_KEY);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        LAST_CATCH.remove(event.getEntity().getUUID());
        SETTLED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!VoidWorld.isVoid(event.getEntity().level())) {
            event.getEntity().getPersistentData().remove(VoidTier.VISIT_START_KEY);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level && VoidWorld.isVoid(level)) {
            VoidStorm.tick(level);
            com.craisinlord.integrated_superheroes.voidworld.pruning.VoidPruning.tick(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        VoidStorm.clear();
        com.craisinlord.integrated_superheroes.voidworld.pruning.VoidPruning.clear();
    }

    @SubscribeEvent
    public static void onReload(AddReloadListenerEvent event) {
        event.addListener(PrunedDebrisPool.LISTENER);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        VoidCommand.register(event.getDispatcher());
    }
}
