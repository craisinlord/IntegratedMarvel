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

public record EyeBeamPayload(int entityId, int channel, boolean active, double endX, double endY, double endZ,
                             int core, int glow, float scale, int life) implements CustomPacketPayload {
    public static final Type<EyeBeamPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "eye_beam"));
    public static final StreamCodec<ByteBuf, EyeBeamPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.VAR_INT.encode(buffer, payload.entityId());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.channel());
                ByteBufCodecs.BOOL.encode(buffer, payload.active());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.endX());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.endY());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.endZ());
                ByteBufCodecs.INT.encode(buffer, payload.core());
                ByteBufCodecs.INT.encode(buffer, payload.glow());
                ByteBufCodecs.FLOAT.encode(buffer, payload.scale());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.life());
            },
            buffer -> new EyeBeamPayload(ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.DOUBLE.decode(buffer), ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer), ByteBufCodecs.INT.decode(buffer), ByteBufCodecs.INT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer))
    );

    @Override
    public Type<EyeBeamPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> EyeBeamClient.accept(payload)));
        }
    }
}
