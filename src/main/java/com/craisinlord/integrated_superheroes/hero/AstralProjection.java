package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AstralProjection {
    private static final String TAG = "integrated_superheroes_astral";
    private static final int MIND_COLOR = 0xB57CFF;
    private static final Map<UUID, Trip> TRIPS = ServerState.map(new HashMap<>());
    private static boolean registered;

    private record Trip(ResourceKey<Level> dimension, Vec3 origin, long start, long until) {
    }

    private AstralProjection() {
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        NeoForge.EVENT_BUS.addListener(AstralProjection::onServerTick);
        NeoForge.EVENT_BUS.addListener(AstralProjection::onLogin);
        NeoForge.EVENT_BUS.addListener(AstralProjection::onLogout);
        NeoForge.EVENT_BUS.addListener(AstralProjection::onClone);
        NeoForge.EVENT_BUS.addListener(AstralProjection::onRespawn);
        NeoForge.EVENT_BUS.addListener(AstralProjection::onStopping);
    }

    public static boolean active(Player player) {
        return player != null && TRIPS.containsKey(player.getUUID());
    }

    static boolean start(ServerPlayer player, int ticks) {
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (mode != GameType.SURVIVAL && mode != GameType.ADVENTURE) {
            player.displayClientMessage(Component.literal("§7Your mind won't leave a body that isn't at risk."), true);
            return false;
        }
        if (player.isPassenger() || player.isSleeping()) {
            player.displayClientMessage(Component.literal("§7You can't let go of your body right now."), true);
            return false;
        }
        CompoundTag tag = new CompoundTag();
        tag.putString("dimension", player.level().dimension().location().toString());
        tag.putDouble("x", player.getX());
        tag.putDouble("y", player.getY());
        tag.putDouble("z", player.getZ());
        tag.putFloat("yaw", player.getYRot());
        tag.putFloat("pitch", player.getXRot());
        tag.putString("mode", mode.getName());
        player.getPersistentData().put(TAG, tag);
        long now = player.serverLevel().getGameTime();
        TRIPS.put(player.getUUID(), new Trip(player.level().dimension(), player.position(), now, now + ticks));
        ServerLevel level = player.serverLevel();
        SerumKit.ring(level, player.position().add(0.0D, 0.1D, 0.0D), SerumKit.glow(player, MIND_COLOR, 0.9F), 1.2D, 24);
        level.sendParticles(SerumKit.glow(player, MIND_COLOR, 0.8F), player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.3D, 0.6D, 0.3D, 0.02D);
        HeroFx.play(player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.8F);
        player.setGameMode(GameType.SPECTATOR);
        player.displayClientMessage(Component.literal("§dYour mind slips out of your body. Press again to go back."), true);
        return true;
    }

    static void end(ServerPlayer player) {
        TRIPS.remove(player.getUUID());
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        if (tag.isEmpty()) {
            return;
        }
        player.getPersistentData().remove(TAG);
        MinecraftServer server = player.server;
        ResourceLocation id = ResourceLocation.tryParse(tag.getString("dimension"));
        ServerLevel level = id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) {
            level = server.overworld();
        }
        player.stopRiding();
        player.teleportTo(level, tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z"), tag.getFloat("yaw"), tag.getFloat("pitch"));
        player.setGameMode(GameType.byName(tag.getString("mode"), GameType.SURVIVAL));
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0.0F;
        SerumKit.meter(player, -1.0F);
        SerumKit.clearMeter(player);
        level.sendParticles(SerumKit.glow(player, MIND_COLOR, 0.8F), player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.3D, 0.6D, 0.3D, 0.0D);
        HeroFx.play(player, SoundEvents.BEACON_DEACTIVATE, 0.8F, 1.8F);
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        if (TRIPS.isEmpty()) {
            return;
        }
        MinecraftServer server = event.getServer();
        double range = ProfessorXConfig.ASTRAL_RANGE.get();
        for (Map.Entry<UUID, Trip> entry : new ArrayList<>(TRIPS.entrySet())) {
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            Trip trip = entry.getValue();
            if (player == null) {
                TRIPS.remove(entry.getKey());
                continue;
            }
            long now = player.serverLevel().getGameTime();
            boolean away = !player.level().dimension().equals(trip.dimension())
                    || player.position().distanceToSqr(trip.origin()) > range * range;
            if (now >= trip.until() || away || !player.isSpectator() || !ProfessorXPower.isXavier(player)) {
                end(player);
                HeroAbilities.setCooldown(player, ProfessorXPower.ASTRAL, ProfessorXConfig.ASTRAL_COOLDOWN.get());
                player.displayClientMessage(Component.literal(away ? "§7You drift too far and snap back into your body."
                        : "§7You snap back into your body."), true);
                continue;
            }
            SerumKit.meter(player, (float) (trip.until() - now) / Math.max(1L, trip.until() - trip.start()));
            if (now % 10L == 0L) {
                ServerLevel level = player.serverLevel();
                Vec3 body = trip.origin();
                level.sendParticles(SerumKit.glow(player, MIND_COLOR, 0.6F), body.x, body.y + 1.0D, body.z, 4, 0.25D, 0.5D, 0.25D, 0.0D);
            }
        }
    }

    private static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().contains(TAG)) {
            end(player);
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && player.getPersistentData().contains(TAG)) {
            end(player);
        }
    }

    private static void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = event.getOriginal().getPersistentData().getCompound(TAG);
        TRIPS.remove(event.getOriginal().getUUID());
        if (!tag.isEmpty()) {
            player.getPersistentData().put(TAG, tag.copy());
        }
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        CompoundTag tag = player.getPersistentData().getCompound(TAG);
        if (tag.isEmpty()) {
            return;
        }
        player.getPersistentData().remove(TAG);
        TRIPS.remove(player.getUUID());
        player.setGameMode(GameType.byName(tag.getString("mode"), GameType.SURVIVAL));
    }

    private static void onStopping(ServerStoppingEvent event) {
        for (UUID id : new ArrayList<>(TRIPS.keySet())) {
            ServerPlayer player = event.getServer().getPlayerList().getPlayer(id);
            if (player != null) {
                end(player);
            }
        }
        TRIPS.clear();
    }
}
