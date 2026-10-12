package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MindBeamStatePayload(boolean active, int beamKind, int entityId, double startX, double startY, double startZ,
        double endX, double endY, double endZ, int age) implements CustomPacketPayload {
    public static final Type<MindBeamStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "mind_beam_state"));
    public static final StreamCodec<ByteBuf, MindBeamStatePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.BOOL.encode(buffer, payload.active());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.beamKind());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.entityId());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.startX());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.startY());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.startZ());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.endX());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.endY());
                ByteBufCodecs.DOUBLE.encode(buffer, payload.endZ());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.age());
            },
            buffer -> new MindBeamStatePayload(ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer), ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer), ByteBufCodecs.DOUBLE.decode(buffer),
                    ByteBufCodecs.DOUBLE.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)));

    @Override
    public Type<MindBeamStatePayload> type() {
        return TYPE;
    }
}
