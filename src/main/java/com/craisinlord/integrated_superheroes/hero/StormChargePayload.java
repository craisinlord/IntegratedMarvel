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

public record StormChargePayload(int entityId, boolean charged) implements CustomPacketPayload {
    public static final Type<StormChargePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "storm_charge"));
    public static final StreamCodec<ByteBuf, StormChargePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, StormChargePayload::entityId,
            ByteBufCodecs.BOOL, StormChargePayload::charged,
            StormChargePayload::new
    );

    @Override
    public Type<StormChargePayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> StormCharge.setClient(payload.entityId(), payload.charged())));
        }
    }
}
