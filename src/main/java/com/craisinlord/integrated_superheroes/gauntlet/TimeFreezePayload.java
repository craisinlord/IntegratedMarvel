package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public record TimeFreezePayload(List<UUID> owners) implements CustomPacketPayload {
    public static final Type<TimeFreezePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "time_freeze"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TimeFreezePayload> STREAM_CODEC =
            StreamCodec.composite(UUIDUtil.STREAM_CODEC.apply(ByteBufCodecs.list()), TimeFreezePayload::owners,
                    TimeFreezePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
