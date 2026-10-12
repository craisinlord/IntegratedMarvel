package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record DeadpoolStandUpPayload() implements CustomPacketPayload {
    public static final DeadpoolStandUpPayload INSTANCE = new DeadpoolStandUpPayload();
    public static final Type<DeadpoolStandUpPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "deadpool_stand_up"));
    public static final StreamCodec<ByteBuf, DeadpoolStandUpPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<DeadpoolStandUpPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    HealingFactor.requestStand(player);
                }
            }));
        }
    }
}
