package com.craisinlord.integrated_marvel.gauntlet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MindBeamClientState {
    private static final Map<Integer, Beam> BEAMS = new ConcurrentHashMap<>();

    private MindBeamClientState() {
    }

    public static void set(MindBeamStatePayload payload) {
        if (!payload.active()) {
            BEAMS.remove(payload.entityId());
            return;
        }
        BEAMS.put(payload.entityId(), new Beam(payload.beamKind(), payload.startX(), payload.startY(), payload.startZ(),
                payload.endX(), payload.endY(), payload.endZ(), payload.age(), System.currentTimeMillis() + 500L));
    }

    public static Map<Integer, Beam> beams() {
        long now = System.currentTimeMillis();
        BEAMS.entrySet().removeIf(entry -> entry.getValue().expiresAt() < now);
        return BEAMS;
    }

    public record Beam(int kind, double startX, double startY, double startZ, double endX, double endY, double endZ,
                       int age, long expiresAt) {
    }
}
