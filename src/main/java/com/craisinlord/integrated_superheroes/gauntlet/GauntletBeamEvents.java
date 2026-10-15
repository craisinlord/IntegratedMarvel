package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
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
        MinecraftServer server = event.getServer();
        SpacePortalManager.tick(server);
        SpaceDestinationManager.tick(server);
        RealityStone.tick(server);
        RealityClipboard.tick(server.overworld().getGameTime());
        MindControlManager.tick(server);
        MindStone.tick(server);
        MindLinkManager.tick(server);
        SoulStoneManager.tick(server);
        SoulAllyManager.tick(server);
        DimensionTimeOverrideManager.tick(server);
        TimeSurgeManager.clearExpired(server);
        TimeFreezeManager.tick(server);
        TimeRateTuningManager.tick(server);
        TimeRewindManager.tick(server);
        TimeFieldManager.tick(server);
        PowerStone.tick(server);
        SpaceStone.tick(server);
        SnapStone.tick(server);
        BlockEditQueue.tick(server);
        GauntletHudSync.tick(server);
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MinecraftServer server = event.getServer();
        SpacePortalManager.clear(server);
        SpaceDestinationManager.clear();
        RealityStone.clear(server);
        RealityClipboard.clear();
        MindControlManager.clear(server);
        MindStone.clear();
        MindLinkManager.clear(server);
        SoulStoneManager.clear(server);
        SoulAllyManager.clear();
        GauntletAbilityController.clearSessionState();
        DimensionTimeOverrideManager.clear(server);
        TimeSurgeManager.clear(server);
        TimeFreezeManager.clear(server);
        TimeRateTuningManager.clear(server);
        TimeRewindManager.clear();
        TimeFieldManager.clear(server);
        TimeStone.clear();
        PowerStone.clear();
        SpaceStone.clear();
        SnapStone.clear();
        BlockEditQueue.clear();
        GauntletHudSync.clear();
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLivingDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && TimeRewindManager.tryTimeLoop(player)) {
            event.setCanceled(true);
            SoulStoneManager.clearFinalizing(player.getUUID());
            return;
        }
        SoulStoneManager.onLivingDeath(event);
        if (event.isCanceled()) return;
        TimeRewindManager.onDeath(event);
        MindControlManager.release(event.getEntity().getUUID());
        if (event.getEntity() instanceof ServerPlayer player) {
            TimeRateTuningManager.clearPlayer(player.getUUID());
            GauntletAbilityController.cancelBeam(player);
            GauntletAbilityController.clearPlayerSession(player.getUUID());
        }
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        SoulAllyManager.onIncomingDamage(event);
        if (event.isCanceled()) return;
        if (event.getEntity() instanceof ServerPlayer hurt && event.getAmount() > 0.0F) MindLinkManager.onControllerHurt(hurt);
        PowerStone.onIncomingDamage(event);
        if (event.isCanceled()) return;
        TimeStone.onIncomingDamage(event);
        if (event.isCanceled()) return;
        SoulStoneManager.onIncomingDamage(event);
    }

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        SoulAllyManager.onChangeTarget(event);
        if (!event.isCanceled()) MindStone.onChangeTarget(event);
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        GauntletEntityLocks.onJoin(event);
        SoulAllyManager.onJoin(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!event.isCanceled()) TimeRewindManager.onBreak(event);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        if (!event.isCanceled()) TimeRewindManager.onPlace(event);
    }

    @SubscribeEvent
    public static void onLeave(net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent event) {
        Entity.RemovalReason reason = event.getEntity().getRemovalReason();
        if (!event.getLevel().isClientSide() && reason != null && reason.shouldDestroy()) {
            SoulAllyManager.forget(event.getEntity().getUUID());
        }
    }

    @SubscribeEvent
    public static void onExplosion(ExplosionEvent.Detonate event) {
        TimeRewindManager.onExplosion(event);
    }

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && !player.server.isSingleplayerOwner(player.getGameProfile())) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, GauntletConfig.syncPayload());
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
            GauntletAbilityController.clearPlayerLogout(player.getUUID());
            MindLinkManager.end(player.server, player.getUUID(), "mind_link_end");
            RealityClipboard.clearPlayer(player.getUUID());
            TimeRewindManager.clearPlayer(player.getUUID());
            GauntletHudSync.clearPlayer(player.getUUID());
        }
    }
}
