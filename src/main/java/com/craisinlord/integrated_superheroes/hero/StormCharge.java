package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = IntegratedSuperheroes.MODID)
public final class StormCharge {
    private static final Set<UUID> CHARGED = ConcurrentHashMap.newKeySet();
    private static final Set<Integer> CLIENT_CHARGED = ConcurrentHashMap.newKeySet();

    private StormCharge() {
    }

    public static void update(ServerPlayer player, boolean charged) {
        boolean changed = charged ? CHARGED.add(player.getUUID()) : CHARGED.remove(player.getUUID());
        if (changed) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new StormChargePayload(player.getId(), charged));
        }
    }

    public static void setClient(int entityId, boolean charged) {
        if (charged) {
            CLIENT_CHARGED.add(entityId);
        } else {
            CLIENT_CHARGED.remove(entityId);
        }
    }

    public static Set<Integer> clientCharged() {
        return CLIENT_CHARGED;
    }

    public static void clearClient() {
        CLIENT_CHARGED.clear();
    }

    @SubscribeEvent
    public static void onStartTracking(PlayerEvent.StartTracking event) {
        if (event.getEntity() instanceof ServerPlayer tracker && event.getTarget() instanceof ServerPlayer target
                && CHARGED.contains(target.getUUID())) {
            PacketDistributor.sendToPlayer(tracker, new StormChargePayload(target.getId(), true));
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && CHARGED.remove(player.getUUID())) {
            PacketDistributor.sendToPlayer(player, new StormChargePayload(player.getId(), false));
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        CHARGED.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        CHARGED.remove(event.getEntity().getUUID());
    }
}
