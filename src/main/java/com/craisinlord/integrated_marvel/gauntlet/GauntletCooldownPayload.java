package com.craisinlord.integrated_marvel.gauntlet;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GauntletCooldownPayload(String stone, int ability, int duration) implements CustomPacketPayload {
    public static final Type<GauntletCooldownPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "gauntlet_ability_cooldown"));
    public static final StreamCodec<ByteBuf, GauntletCooldownPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, GauntletCooldownPayload::stone,
            ByteBufCodecs.VAR_INT, GauntletCooldownPayload::ability,
            ByteBufCodecs.VAR_INT, GauntletCooldownPayload::duration,
            GauntletCooldownPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
