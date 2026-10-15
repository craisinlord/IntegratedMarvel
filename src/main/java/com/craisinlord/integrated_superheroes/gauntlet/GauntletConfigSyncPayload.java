package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record GauntletConfigSyncPayload(List<Boolean> enabled, boolean infinityTier) implements CustomPacketPayload {
    public static final Type<GauntletConfigSyncPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_config_sync"));
    public static final StreamCodec<ByteBuf, GauntletConfigSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL.apply(ByteBufCodecs.list()), GauntletConfigSyncPayload::enabled,
            ByteBufCodecs.BOOL, GauntletConfigSyncPayload::infinityTier,
            GauntletConfigSyncPayload::new);

    @Override
    public Type<GauntletConfigSyncPayload> type() {
        return TYPE;
    }
}
