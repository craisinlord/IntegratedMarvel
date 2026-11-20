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

public record GrootPayload(int entityId, int targetId, int mode, float x, float y, float z, int ticks) implements CustomPacketPayload {
    public static final Type<GrootPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "groot_branch"));
    public static final StreamCodec<ByteBuf, GrootPayload> STREAM_CODEC = StreamCodec.of(GrootPayload::write, GrootPayload::read);

    private static void write(ByteBuf buf, GrootPayload payload) {
        ByteBufCodecs.VAR_INT.encode(buf, payload.entityId);
        ByteBufCodecs.VAR_INT.encode(buf, payload.targetId + 1);
        ByteBufCodecs.VAR_INT.encode(buf, payload.mode);
        buf.writeFloat(payload.x);
        buf.writeFloat(payload.y);
        buf.writeFloat(payload.z);
        ByteBufCodecs.VAR_INT.encode(buf, payload.ticks);
    }

    private static GrootPayload read(ByteBuf buf) {
        int entityId = ByteBufCodecs.VAR_INT.decode(buf);
        int targetId = ByteBufCodecs.VAR_INT.decode(buf) - 1;
        int mode = ByteBufCodecs.VAR_INT.decode(buf);
        float x = buf.readFloat();
        float y = buf.readFloat();
        float z = buf.readFloat();
        int ticks = ByteBufCodecs.VAR_INT.decode(buf);
        return new GrootPayload(entityId, targetId, mode, x, y, z, ticks);
    }

    @Override
    public Type<GrootPayload> type() {
        return TYPE;
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(
                    () -> GrootBranches.receive(payload, context.player().level().getGameTime())));
        }
    }
}
