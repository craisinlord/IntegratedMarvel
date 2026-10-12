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

public record StretchPayload(int entityId, int mode, float x, float y, float z, int ticks) implements CustomPacketPayload {
    public static final Type<StretchPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "stretch"));
    public static final StreamCodec<ByteBuf, StretchPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StretchPayload::entityId,
            ByteBufCodecs.VAR_INT, StretchPayload::mode,
            ByteBufCodecs.FLOAT, StretchPayload::x,
            ByteBufCodecs.FLOAT, StretchPayload::y,
            ByteBufCodecs.FLOAT, StretchPayload::z,
            ByteBufCodecs.VAR_INT, StretchPayload::ticks,
            StretchPayload::new
    );

    @Override
    public Type<StretchPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> MrFantasticPower.receive(payload, context.player().level().getGameTime())));
        }
    }
}
