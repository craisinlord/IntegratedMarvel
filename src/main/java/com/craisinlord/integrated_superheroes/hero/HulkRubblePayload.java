package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

import java.util.List;

public record HulkRubblePayload(List<BlockPos> blocks, int tier) implements CustomPacketPayload {
    public static final Type<HulkRubblePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "hulk_rubble"));
    public static final StreamCodec<ByteBuf, HulkRubblePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()), HulkRubblePayload::blocks,
            ByteBufCodecs.VAR_INT, HulkRubblePayload::tier,
            HulkRubblePayload::new
    );

    @Override
    public Type<HulkRubblePayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> HulkRubbleRenderer.add(payload.blocks(), payload.tier())));
        }
    }
}
