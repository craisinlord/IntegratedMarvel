package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MindControlInputPayload(boolean up, boolean down, boolean left, boolean right,
        boolean jump, boolean sprint, boolean attack, float yaw, float pitch) implements CustomPacketPayload {
    public static final Type<MindControlInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "mind_control_input"));
    public static final StreamCodec<ByteBuf, MindControlInputPayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> {
                ByteBufCodecs.BOOL.encode(buffer, payload.up());
                ByteBufCodecs.BOOL.encode(buffer, payload.down());
                ByteBufCodecs.BOOL.encode(buffer, payload.left());
                ByteBufCodecs.BOOL.encode(buffer, payload.right());
                ByteBufCodecs.BOOL.encode(buffer, payload.jump());
                ByteBufCodecs.BOOL.encode(buffer, payload.sprint());
                ByteBufCodecs.BOOL.encode(buffer, payload.attack());
                ByteBufCodecs.FLOAT.encode(buffer, payload.yaw());
                ByteBufCodecs.FLOAT.encode(buffer, payload.pitch());
            },
            buffer -> new MindControlInputPayload(
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.BOOL.decode(buffer),
                    ByteBufCodecs.BOOL.decode(buffer), ByteBufCodecs.FLOAT.decode(buffer),
                    ByteBufCodecs.FLOAT.decode(buffer)));

    @Override
    public Type<MindControlInputPayload> type() {
        return TYPE;
    }
}
