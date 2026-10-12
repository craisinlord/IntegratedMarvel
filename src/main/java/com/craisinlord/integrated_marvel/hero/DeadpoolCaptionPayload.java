package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record DeadpoolCaptionPayload(int entityId, String key) implements CustomPacketPayload {
    public static final Type<DeadpoolCaptionPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "deadpool_caption"));
    public static final StreamCodec<ByteBuf, DeadpoolCaptionPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeadpoolCaptionPayload::entityId,
            ByteBufCodecs.STRING_UTF8, DeadpoolCaptionPayload::key,
            DeadpoolCaptionPayload::new
    );

    @Override
    public Type<DeadpoolCaptionPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> DeadpoolClient.caption(payload.entityId(), payload.key())));
        }
    }
}
