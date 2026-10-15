package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MindControllerPayload(boolean active) implements CustomPacketPayload {
    public static final Type<MindControllerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_mind_controller"));
    public static final StreamCodec<ByteBuf, MindControllerPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, MindControllerPayload::active,
            MindControllerPayload::new);

    @Override
    public Type<MindControllerPayload> type() {
        return TYPE;
    }
}
