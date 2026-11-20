package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

public record GrootMorphPayload(int entityId, int kind, int ticks, float x, float y, float z, float ax, float ay, float az,
                                int[] targets) implements CustomPacketPayload {
    public static final Type<GrootMorphPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "groot_morph"));
    public static final StreamCodec<ByteBuf, GrootMorphPayload> STREAM_CODEC = StreamCodec.of(GrootMorphPayload::write, GrootMorphPayload::read);

    private static void write(ByteBuf buf, GrootMorphPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buf, payload.entityId);
        ByteBufCodecs.VAR_INT.encode(buf, payload.kind);
        ByteBufCodecs.VAR_INT.encode(buf, payload.ticks);
        buf.writeFloat(payload.x);
        buf.writeFloat(payload.y);
        buf.writeFloat(payload.z);
        buf.writeFloat(payload.ax);
        buf.writeFloat(payload.ay);
        buf.writeFloat(payload.az);
        ByteBufCodecs.VAR_INT.encode(buf, payload.targets.length);
        for (int target : payload.targets) {
            ByteBufCodecs.VAR_INT.encode(buf, target);
        }
    }

    private static GrootMorphPayload read(ByteBuf buf) {
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        int kind = ByteBufCodecs.VAR_INT.decode(buf);
        int ticks = ByteBufCodecs.VAR_INT.decode(buf);
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        float ax = buf.readFloat();
        float ay = buf.readFloat();
        float az = buf.readFloat();
        int count = Math.min(32, ByteBufCodecs.VAR_INT.decode(buf));
        int[] targets = new int[count];
        for (int i = 0; i < count; i++) {
            targets[i] = ByteBufCodecs.VAR_INT.decode(buf);
        }
        return new GrootMorphPayload(entityId, kind, ticks, x, y, z, ax, ay, az, targets);
    }

    @Override
    public Type<GrootMorphPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> GrootMorphs.receive(payload, context.player().level().getGameTime())));
        }
    }
}
