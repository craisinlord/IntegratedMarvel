package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record MindLinkPayload(int entityId, boolean active) implements CustomPacketPayload {
    public static final Type<MindLinkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_mind_link"));
    public static final StreamCodec<ByteBuf, MindLinkPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MindLinkPayload::entityId,
            ByteBufCodecs.BOOL, MindLinkPayload::active,
            MindLinkPayload::new);

    @Override
    public Type<MindLinkPayload> type() {
        return TYPE;
    }
}
