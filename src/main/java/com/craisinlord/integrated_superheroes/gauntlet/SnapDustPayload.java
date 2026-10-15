package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public record SnapDustPayload(List<Entry> entries) implements CustomPacketPayload {
    public static final Type<SnapDustPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "gauntlet_snap_dust"));
    public static final StreamCodec<FriendlyByteBuf, SnapDustPayload> STREAM_CODEC =
            StreamCodec.of(SnapDustPayload::write, SnapDustPayload::read);

    public record Entry(double x, double y, double z, float width, float height, int sound) {
    }

    private static void write(FriendlyByteBuf buffer, SnapDustPayload payload) {
        buffer.writeVarInt(payload.entries.size());
        for (Entry entry : payload.entries) {
            buffer.writeDouble(entry.x());
            buffer.writeDouble(entry.y());
            buffer.writeDouble(entry.z());
            buffer.writeFloat(entry.width());
            buffer.writeFloat(entry.height());
            buffer.writeVarInt(entry.sound() + 1);
        }
    }

    private static SnapDustPayload read(FriendlyByteBuf buffer) {
        int size = Math.min(buffer.readVarInt(), 8192);
        List<Entry> entries = new ArrayList<>(size);
        for (int index = 0; index < size; index++) {
            entries.add(new Entry(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(), buffer.readFloat(),
                    buffer.readFloat(), buffer.readVarInt() - 1));
        }
        return new SnapDustPayload(entries);
    }

    @Override
    public Type<SnapDustPayload> type() {
        return TYPE;
    }
}
