package com.craisinlord.integrated_superheroes.boss.command;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StormCommandOpenPayload(BlockPos pos, String command, Component lastOutput, boolean trackOutput, boolean conditional, boolean automatic) implements CustomPacketPayload {
    public static final Type<StormCommandOpenPayload> TYPE = new Type<>(IntegratedSuperheroes.id("storm_command_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StormCommandOpenPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StormCommandOpenPayload::pos,
            ByteBufCodecs.stringUtf8(StormCommandEditPayload.MAX_COMMAND_LENGTH), StormCommandOpenPayload::command,
            ComponentSerialization.STREAM_CODEC, StormCommandOpenPayload::lastOutput,
            ByteBufCodecs.BOOL, StormCommandOpenPayload::trackOutput,
            ByteBufCodecs.BOOL, StormCommandOpenPayload::conditional,
            ByteBufCodecs.BOOL, StormCommandOpenPayload::automatic,
            StormCommandOpenPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
