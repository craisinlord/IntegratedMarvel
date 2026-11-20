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

public record WingedFlightPayload(int entityId, int mode, int lift, boolean wings) implements CustomPacketPayload {
    public static final int NONE = 0;
    public static final int FOLDED = 1;
    public static final int POWERED = 2;
    public static final int GLIDE = 3;
    public static final int DIVE = 4;

    public static final Type<WingedFlightPayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "winged_flight"));
    public static final StreamCodec<ByteBuf, WingedFlightPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, WingedFlightPayload::entityId,
            ByteBufCodecs.VAR_INT, WingedFlightPayload::mode,
            ByteBufCodecs.VAR_INT, WingedFlightPayload::lift,
            ByteBufCodecs.BOOL, WingedFlightPayload::wings,
            WingedFlightPayload::new
    );

    private static final Map<Integer, WingedFlightPayload> CLIENT = new ConcurrentHashMap<>();

    @Override
    public Type<WingedFlightPayload> type() {
        return TYPE;
    }

    public static WingedFlightPayload client(int entityId) {
        return CLIENT.get(entityId);
    }

    public static void clearClient() {
        CLIENT.clear();
    }

    static void accept(WingedFlightPayload payload) {
        if (payload.mode() == NONE) {
            CLIENT.remove(payload.entityId());
        } else {
            CLIENT.put(payload.entityId(), payload);
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
