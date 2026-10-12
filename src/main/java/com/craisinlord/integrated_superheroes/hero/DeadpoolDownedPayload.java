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

public record DeadpoolDownedPayload(int state) implements CustomPacketPayload {
    public static final int UP = 0;
    public static final int DOWNED = 1;
    public static final int READY = 2;
    public static final Type<DeadpoolDownedPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "deadpool_downed"));
    public static final StreamCodec<ByteBuf, DeadpoolDownedPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, DeadpoolDownedPayload::state,
            DeadpoolDownedPayload::new
    );

    @Override
    public Type<DeadpoolDownedPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> DeadpoolClient.downed(payload.state())));
        }
    }
}
