package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record SpaceDestinationPayload(ResourceLocation dimension, int x, int y, int z, boolean generate)
        implements CustomPacketPayload {
    public static final Type<SpaceDestinationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "space_destination"));
    public static final StreamCodec<ByteBuf, SpaceDestinationPayload> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, SpaceDestinationPayload::dimension,
            ByteBufCodecs.INT, SpaceDestinationPayload::x,
            ByteBufCodecs.INT, SpaceDestinationPayload::y,
            ByteBufCodecs.INT, SpaceDestinationPayload::z,
            ByteBufCodecs.BOOL, SpaceDestinationPayload::generate,
            SpaceDestinationPayload::new);

    @Override
    public Type<SpaceDestinationPayload> type() {
        return TYPE;
    }
}
