package com.craisinlord.integrated_marvel.gauntlet;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = com.craisinlord.integrated_marvel.IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD)
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
                .playToClient(TimeFreezePayload.TYPE, TimeFreezePayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> TimeFreezeClientState.set(payload.active())))
                .playToServer(SpaceDestinationPayload.TYPE, SpaceDestinationPayload.STREAM_CODEC,
                        (payload, context) -> context.enqueueWork(() -> {
                            if (context.player() instanceof ServerPlayer player) SpaceDestinationManager.select(player, payload);
                        }));
    }
}
