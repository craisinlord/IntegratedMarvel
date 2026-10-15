package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record GauntletHudPayload(String clipLabel, int clipCount, List<Integer> clipSize, int clipFacing, int clipRotation,
                                 boolean clipEntity, float rewindFill, int realityRadius, int timeRate)
        implements CustomPacketPayload {
    public static final Type<GauntletHudPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_hud"));
    public static final StreamCodec<ByteBuf, GauntletHudPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.STRING_UTF8.encode(buffer, payload.clipLabel());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.clipCount());
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(3)).encode(buffer, payload.clipSize());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.clipFacing());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.clipRotation());
                ByteBufCodecs.BOOL.encode(buffer, payload.clipEntity());
                ByteBufCodecs.FLOAT.encode(buffer, payload.rewindFill());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.realityRadius());
                ByteBufCodecs.VAR_INT.encode(buffer, payload.timeRate());
            },
            buffer -> new GauntletHudPayload(ByteBufCodecs.STRING_UTF8.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(3)).decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.VAR_INT.decode(buffer), ByteBufCodecs.VAR_INT.decode(buffer)));

    public static final GauntletHudPayload EMPTY = new GauntletHudPayload("", 0, List.of(0, 0, 0), 0, 0, false, 0.0F, 0, 25);

    @Override
    public Type<GauntletHudPayload> type() {
        return TYPE;
    }
}
