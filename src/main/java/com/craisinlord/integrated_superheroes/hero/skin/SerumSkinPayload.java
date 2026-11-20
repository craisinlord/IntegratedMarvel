package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record SerumSkinPayload(int entityId, String skinId, String costume) implements CustomPacketPayload {
    public static final Type<SerumSkinPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "serum_skin"));
    public static final StreamCodec<ByteBuf, SerumSkinPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SerumSkinPayload::entityId,
            ByteBufCodecs.STRING_UTF8, SerumSkinPayload::skinId,
            ByteBufCodecs.STRING_UTF8, SerumSkinPayload::costume,
            SerumSkinPayload::new
    );

    @Override
    public Type<SerumSkinPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("2").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> {
                Entity entity = context.player().level().getEntity(payload.entityId());
                if (entity instanceof Player player) {
                    SkinData.applyClient(player, payload.skinId(), payload.costume());
                }
            }));
        }
    }
}
