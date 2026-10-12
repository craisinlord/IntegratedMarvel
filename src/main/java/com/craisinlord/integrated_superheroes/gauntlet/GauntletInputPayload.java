package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GauntletInputPayload(int action, double delta) implements CustomPacketPayload {
    public static final Type<GauntletInputPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_input"));
    public static final StreamCodec<ByteBuf, GauntletInputPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GauntletInputPayload::action,
            ByteBufCodecs.DOUBLE, GauntletInputPayload::delta,
            GauntletInputPayload::new
    );

    @Override
    public Type<GauntletInputPayload> type() {
        return TYPE;
    }
}
