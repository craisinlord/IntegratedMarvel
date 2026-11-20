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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public record SurferBoardPayload(int entityId, int flags, float maxSpeed, float acceleration, float turn) implements CustomPacketPayload {
    public static final int RIDING = 1;
    public static final int PRECISION = 2;
    public static final int TRAIL = 4;

    public static final Type<SurferBoardPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "silver_surfer_board"));
    public static final StreamCodec<ByteBuf, SurferBoardPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SurferBoardPayload::entityId,
            ByteBufCodecs.VAR_INT, SurferBoardPayload::flags,
            ByteBufCodecs.FLOAT, SurferBoardPayload::maxSpeed,
            ByteBufCodecs.FLOAT, SurferBoardPayload::acceleration,
            ByteBufCodecs.FLOAT, SurferBoardPayload::turn,
            SurferBoardPayload::new
    );

    private static final Map<Integer, SurferBoardPayload> CLIENT = new ConcurrentHashMap<>();

    @Override
    public Type<SurferBoardPayload> type() {
        return TYPE;
    }

    public boolean riding() {
        return (flags & RIDING) != 0;
    }

    public boolean precision() {
        return (flags & PRECISION) != 0;
    }

    public boolean trail() {
        return (flags & TRAIL) != 0;
    }

    public static SurferBoardPayload client(int entityId) {
        return CLIENT.get(entityId);
    }

    public static Iterable<SurferBoardPayload> clientAll() {
        return CLIENT.values();
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    static void accept(SurferBoardPayload payload) {
        if (payload.riding()) {
            CLIENT.put(payload.entityId(), payload);
        } else {
            CLIENT.remove(payload.entityId());
        }
    }

    @EventBusSubscriber(modid = IntegratedSuperheroes.MODID, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        private Registration() {
        }

        @SubscribeEvent
        public static void register(RegisterPayloadHandlersEvent event) {
            event.registrar("1").playToClient(TYPE, STREAM_CODEC, (payload, context) -> context.enqueueWork(() -> accept(payload)));
        }
    }
}
