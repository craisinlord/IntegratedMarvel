package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GauntletSelectionToastPayload(String stone, int ability) implements CustomPacketPayload {
    public static final Type<GauntletSelectionToastPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "gauntlet_selection_toast"));
    public static final StreamCodec<ByteBuf, GauntletSelectionToastPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GauntletSelectionToastPayload::stone,
            ByteBufCodecs.VAR_INT, GauntletSelectionToastPayload::ability,
            GauntletSelectionToastPayload::new
    );

    @Override
    public Type<GauntletSelectionToastPayload> type() {
        return TYPE;
    }
}
