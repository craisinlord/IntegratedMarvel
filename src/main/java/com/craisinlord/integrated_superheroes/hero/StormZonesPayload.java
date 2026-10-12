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

import java.util.List;

public record StormZonesPayload(List<StormZones.Zone> zones) implements CustomPacketPayload {
    public static final Type<StormZonesPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "storm_zones"));
    public static final StreamCodec<ByteBuf, StormZonesPayload> STREAM_CODEC = StreamCodec.composite(
            StormZones.Zone.STREAM_CODEC.apply(ByteBufCodecs.list()), StormZonesPayload::zones,
            StormZonesPayload::new
    );

    @Override
    public Type<StormZonesPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> StormZones.setClient(payload.zones())));
        }
    }
}
