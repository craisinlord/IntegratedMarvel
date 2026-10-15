package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record GauntletShapePayload(ParticleOptions particle, int shape, double ax, double ay, double az,
                                   double bx, double by, double bz, float radius, int points, float spread,
                                   float phase) implements CustomPacketPayload {
    public static final int RING = 0;
    public static final int SPHERE = 1;
    public static final int BOX = 2;
    public static final int LINE = 3;
    public static final int ELLIPSE = 4;
    public static final Type<GauntletShapePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_shape"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GauntletShapePayload> STREAM_CODEC =
            StreamCodec.of(GauntletShapePayload::write, GauntletShapePayload::read);

    private static void write(RegistryFriendlyByteBuf buffer, GauntletShapePayload payload) {
        ParticleTypes.STREAM_CODEC.encode(buffer, payload.particle);
        buffer.writeByte(payload.shape);
        buffer.writeDouble(payload.ax);
        buffer.writeDouble(payload.ay);
        buffer.writeDouble(payload.az);
        buffer.writeDouble(payload.bx);
        buffer.writeDouble(payload.by);
        buffer.writeDouble(payload.bz);
        buffer.writeFloat(payload.radius);
        buffer.writeVarInt(payload.points);
        buffer.writeFloat(payload.spread);
        buffer.writeFloat(payload.phase);
    }

    private static GauntletShapePayload read(RegistryFriendlyByteBuf buffer) {
        return new GauntletShapePayload(ParticleTypes.STREAM_CODEC.decode(buffer), buffer.readByte(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readFloat(), Math.min(buffer.readVarInt(), 4096), buffer.readFloat(), buffer.readFloat());
    }

    @Override
    public Type<GauntletShapePayload> type() {
        return TYPE;
    }
}
