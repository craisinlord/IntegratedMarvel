package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record HellchainPayload(int ownerId, int targetId, int ticks) implements CustomPacketPayload {
    public static final Type<HellchainPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "hellchain"));
    public static final StreamCodec<ByteBuf, HellchainPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, HellchainPayload::ownerId,
            ByteBufCodecs.VAR_INT, HellchainPayload::targetId,
            ByteBufCodecs.VAR_INT, HellchainPayload::ticks,
            HellchainPayload::new
    );

    @Override
    public Type<HellchainPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> HellchainController.receive(payload)));
        }
    }
}
