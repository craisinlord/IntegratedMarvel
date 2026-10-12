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

public record HulkRagePayload(int rage) implements CustomPacketPayload {
    public static final Type<HulkRagePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "hulk_rage"));
    public static final StreamCodec<ByteBuf, HulkRagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HulkRagePayload::rage,
            HulkRagePayload::new
    );

    @Override
    public Type<HulkRagePayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> HulkClient.setRage(payload.rage())));
        }
    }
}
