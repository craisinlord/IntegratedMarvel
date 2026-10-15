package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = com.craisinlord.integrated_superheroes.IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class GauntletInputNetwork {
    private GauntletInputNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(
                GauntletInputPayload.TYPE,
                GauntletInputPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        GauntletAbilityController.handleInput(player, payload.action(), payload.delta());
                    }
                })
        ).playToServer(MindControlInputPayload.TYPE, MindControlInputPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        MindControlManager.acceptInput(player, payload);
                    }
                }))
                .playToClient(MindControlStatePayload.TYPE, MindControlStatePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> MindControlClientState.set(payload)))
                .playToClient(MindBeamStatePayload.TYPE, MindBeamStatePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> MindBeamClientState.set(payload)))
                .playToClient(GauntletSelectionToastPayload.TYPE, GauntletSelectionToastPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() ->
                                GauntletSelectionToast.show(payload.stone(), payload.ability())))
                .playToClient(GauntletCooldownPayload.TYPE, GauntletCooldownPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> GauntletCooldownClientState.set(payload)))
                .playToClient(GauntletHudPayload.TYPE, GauntletHudPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> GauntletClientState.setHud(payload)))
                .playToClient(GauntletConfigSyncPayload.TYPE, GauntletConfigSyncPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> GauntletConfig.applySync(payload)))
                .playToClient(MindLinkPayload.TYPE, MindLinkPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> MindLinkClient.set(payload)))
                .playToClient(MindSightPayload.TYPE, MindSightPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> GauntletClientState.setMindSight(payload)))
                .playToClient(GauntletShapePayload.TYPE, GauntletShapePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> GauntletFxClient.spawn(payload)))
                .playToClient(SnapDustPayload.TYPE, SnapDustPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> GauntletFxClient.snapDust(payload)))
                .playToClient(MindControllerPayload.TYPE, MindControllerPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> MindControlClientState.setControlling(payload.active())))
                .playToClient(TimeFreezePayload.TYPE, TimeFreezePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> TimeFreezeClientState.set(payload.owners())))
                .playToServer(SpaceDestinationPayload.TYPE, SpaceDestinationPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) SpaceDestinationManager.select(player, payload);
                        }));
    }
}
