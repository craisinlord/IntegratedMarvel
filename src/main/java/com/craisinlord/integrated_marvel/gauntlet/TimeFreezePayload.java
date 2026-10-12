package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record TimeFreezePayload(boolean active) implements CustomPacketPayload {
    public static final Type<TimeFreezePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "time_freeze"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimeFreezePayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, TimeFreezePayload::active, TimeFreezePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
