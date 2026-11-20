package com.craisinlord.integrated_superheroes.voidworld;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

public record VoidPrunePayload(double x, double y, double z, int tier, long seed, int warnTicks) implements CustomPacketPayload {
    public static final Type<VoidPrunePayload> TYPE = new Type<>(IntegratedSuperheroes.id("void_prune"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VoidPrunePayload> STREAM_CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeDouble(p.x);
                buf.writeDouble(p.y);
                buf.writeDouble(p.z);
                ByteBufCodecs.VAR_INT.encode(buf, p.tier);
                buf.writeLong(p.seed);
                ByteBufCodecs.VAR_INT.encode(buf, p.warnTicks);
            },
            buf -> new VoidPrunePayload(buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    ByteBufCodecs.VAR_INT.decode(buf), buf.readLong(), ByteBufCodecs.VAR_INT.decode(buf)));

    public Vec3 pos() {
        return new Vec3(this.x, this.y, this.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void broadcast(ServerLevel level, Vec3 pos, int tier, long seed, int warnTicks) {
        VoidPrunePayload payload = new VoidPrunePayload(pos.x, pos.y, pos.z, tier, seed, warnTicks);
        for (ServerPlayer player : level.players()) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }
}
