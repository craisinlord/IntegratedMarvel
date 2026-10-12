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

public record DeadpoolFxPayload(int kind, int ticks) implements CustomPacketPayload {
    public static final int LOADING = 1;
    public static final int PAUSE = 2;
    public static final int BREAK = 3;
    public static final int RETCON = 4;
    public static final Type<DeadpoolFxPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "deadpool_fx"));
    public static final StreamCodec<ByteBuf, DeadpoolFxPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeadpoolFxPayload::kind,
            ByteBufCodecs.VAR_INT, DeadpoolFxPayload::ticks,
            DeadpoolFxPayload::new
    );

    @Override
    public Type<DeadpoolFxPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> DeadpoolClient.fx(payload.kind(), payload.ticks())));
        }
    }
}
