package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record MindSightPayload(List<Integer> entities, List<Long> ores, int durationTicks) implements CustomPacketPayload {
    public static final Type<MindSightPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_mind_sight"));
    public static final StreamCodec<ByteBuf, MindSightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list()), MindSightPayload::entities,
            ByteBufCodecs.VAR_LONG.apply(ByteBufCodecs.list()), MindSightPayload::ores,
            ByteBufCodecs.VAR_INT, MindSightPayload::durationTicks,
            MindSightPayload::new);

    @Override
    public Type<MindSightPayload> type() {
        return TYPE;
    }
}
