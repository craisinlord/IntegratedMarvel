package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = IntegratedMarvel.MODID)
public final class GauntletBeamEvents {
    private GauntletBeamEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            GauntletAbilityController.tickBeam(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        SpacePortalManager.tick(event.getServer());
        SpaceDestinationManager.tick(event.getServer());
        GauntletAbilityController.tickReality(event.getServer());
        MindControlManager.tick(event.getServer());
        SoulStoneManager.tick(event.getServer());
        DimensionTimeOverrideManager.tick(event.getServer());
        TimeFreezeManager.tick(event.getServer());
        TimeRateTuningManager.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        SpacePortalManager.clear(event.getServer());
        SpaceDestinationManager.clear();
        GauntletAbilityController.clearReality(event.getServer());
        MindControlManager.clear(event.getServer());
        SoulStoneManager.clear(event.getServer());
        GauntletAbilityController.clearSessionState();
        DimensionTimeOverrideManager.clear(event.getServer());
        TimeFreezeManager.clear(event.getServer());
        TimeRateTuningManager.clear(event.getServer());
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        SoulStoneManager.onLivingDeath(event);
        MindControlManager.release(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            TimeRateTuningManager.clearPlayer(player.getUUID());
            GauntletAbilityController.clearPlayerSession(player.getUUID());
            GauntletAbilityController.cancelBeam(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            TimeRateTuningManager.clearPlayer(player.getUUID());
            SoulStoneManager.onPlayerLogout(player);
            SpaceDestinationManager.clearPlayer(player.getUUID());
            MindControlManager.releaseController(player.getUUID());
            MindControlManager.release(player.getUUID());
            GauntletAbilityController.cancelBeam(player);
        }
    }
}
