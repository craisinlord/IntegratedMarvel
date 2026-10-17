package com.craisinlord.integrated_superheroes.compat;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record ViltrumiteAnimationPayload(int entityId, int move, int variant) implements CustomPacketPayload {
    public static final Type<ViltrumiteAnimationPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "viltrumite_animation"));
    public static final StreamCodec<ByteBuf, ViltrumiteAnimationPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, ViltrumiteAnimationPayload::entityId,
            ByteBufCodecs.VAR_INT, ViltrumiteAnimationPayload::move,
            ByteBufCodecs.VAR_INT, ViltrumiteAnimationPayload::variant,
            ViltrumiteAnimationPayload::new
    );

    @Override
    public Type<ViltrumiteAnimationPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").optional().playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (ModCompat.VILTRUMITE) {
                    ViltrumiteAnimationsClient.apply(payload.entityId(), payload.move(), payload.variant());
                }
            }));
        }
    }
}
