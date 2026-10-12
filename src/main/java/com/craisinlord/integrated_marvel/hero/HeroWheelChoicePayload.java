package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record HeroWheelChoicePayload(String ability, String option) implements CustomPacketPayload {
    public static final Type<HeroWheelChoicePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "hero_wheel_choice"));
    public static final StreamCodec<ByteBuf, HeroWheelChoicePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, HeroWheelChoicePayload::ability,
            ByteBufCodecs.STRING_UTF8, HeroWheelChoicePayload::option,
            HeroWheelChoicePayload::new
    );

    @Override
    public Type<HeroWheelChoicePayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedMarvel.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToServer(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                if (context.player() instanceof ServerPlayer player) {
                    HeroWheels.choose(player, payload.ability(), payload.option());
                }
            }));
        }
    }
}
