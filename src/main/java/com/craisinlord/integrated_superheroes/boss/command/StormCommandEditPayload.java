package com.craisinlord.integrated_superheroes.boss.command;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record StormCommandEditPayload(BlockPos pos, String command, boolean trackOutput, boolean conditional, boolean automatic) implements CustomPacketPayload {
    public static final int MAX_COMMAND_LENGTH = 32500;
    public static final Type<StormCommandEditPayload> TYPE = new Type<>(IntegratedSuperheroes.id("storm_command_edit"));
    public static final StreamCodec<RegistryFriendlyByteBuf, StormCommandEditPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, StormCommandEditPayload::pos,
            ByteBufCodecs.stringUtf8(MAX_COMMAND_LENGTH), StormCommandEditPayload::command,
            ByteBufCodecs.BOOL, StormCommandEditPayload::trackOutput,
            ByteBufCodecs.BOOL, StormCommandEditPayload::conditional,
            ByteBufCodecs.BOOL, StormCommandEditPayload::automatic,
            StormCommandEditPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
