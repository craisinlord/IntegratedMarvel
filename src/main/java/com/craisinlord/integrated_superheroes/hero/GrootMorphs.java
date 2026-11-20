package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class GrootMorphs {
    public static final int CLEAR_ALL = 0;
    public static final int TAKE_ROOT = 1;
    public static final int LASH = 2;
    public static final int GRAPPLE = 3;
    public static final int SKEWER = 4;
    public static final int TRUNK_SLAM = 5;
    public static final int TRUNK_LIFT = 6;
    public static final int WALL = 7;
    public static final int SHELTER = 8;
    public static final int THORN = 9;
    public static final int BLOOM = 10;
    public static final int SPROUT = 11;
    public static final float FADE = 6.0F;
    private static final Set<Integer> HELD = Set.of(TAKE_ROOT, GRAPPLE, SHELTER, BLOOM, SPROUT, TRUNK_LIFT);
    private static final Map<Integer, Map<Integer, Morph>> ACTIVE = new HashMap<>();

    private GrootMorphs() {
    }

    public record Morph(int kind, long start, long end, Vec3 point, Vec3 aux, int[] targets) {
        public float age(long now, float partial) {
            return now - this.start + partial;
        }

        public float progress(long now, float partial) {
            return Mth.clamp(this.age(now, partial) / Math.max(1L, this.end - this.start), 0.0F, 1.0F);
        }

        public float held(long now, float partial) {
            float time = now + partial;
            float in = Mth.clamp((time - this.start) / FADE, 0.0F, 1.0F);
            float out = Mth.clamp((this.end - time) / FADE, 0.0F, 1.0F);
            return ease(Math.min(in, out));
        }
    }

    public static float ease(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    public static void send(ServerPlayer player, int kind, int ticks, Vec3 point, Vec3 aux, int... targets) {
        Vec3 p = point == null ? player.position() : point;
        Vec3 a = aux == null ? Vec3.ZERO : aux;
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new GrootMorphPayload(player.getId(), kind, ticks,
                (float) p.x, (float) p.y, (float) p.z, (float) a.x, (float) a.y, (float) a.z, targets == null ? new int[0] : targets));
    }

    public static void send(ServerPlayer player, int kind, int ticks, Vec3 point) {
        send(player, kind, ticks, point, null);
    }

    public static void end(ServerPlayer player, int kind) {
        send(player, kind, 0, null, null);
    }

    public static void endAll(ServerPlayer player) {
        send(player, CLEAR_ALL, 0, null, null);
    }

    public static int[] ids(Iterable<? extends Entity> entities) {
        int count = 0;
        for (Entity ignored : entities) {
            count++;
        }
        int[] ids = new int[Math.min(32, count)];
        int i = 0;
        for (Entity entity : entities) {
            if (i >= ids.length) {
                break;
            }
            ids[i++] = entity.getId();
        }
        return ids;
    }

    static void receive(GrootMorphPayload payload, long now) {
        Map<Integer, Morph> morphs = ACTIVE.computeIfAbsent(payload.entityId(), ignored -> new HashMap<>());
        if (payload.kind() == CLEAR_ALL) {
            morphs.replaceAll((kind, morph) -> fadeOut(morph, now));
            return;
        }
        Morph existing = morphs.get(payload.kind());
        if (payload.ticks() <= 0) {
            if (existing != null) {
                morphs.put(payload.kind(), fadeOut(existing, now));
            }
            return;
        }
        long start = existing != null && HELD.contains(payload.kind()) && existing.end() > now ? existing.start() : now;
        morphs.put(payload.kind(), new Morph(payload.kind(), start, now + payload.ticks(),
                new Vec3(payload.x(), payload.y(), payload.z()), new Vec3(payload.ax(), payload.ay(), payload.az()), payload.targets()));
    }

    private static Morph fadeOut(Morph morph, long now) {
        if (!HELD.contains(morph.kind())) {
            return new Morph(morph.kind(), morph.start(), Math.min(morph.end(), now), morph.point(), morph.aux(), morph.targets());
        }
        long end = Math.min(morph.end(), now + (long) FADE);
        return new Morph(morph.kind(), morph.start(), end, morph.point(), morph.aux(), morph.targets());
    }

    public static Morph get(Entity entity, int kind) {
        Map<Integer, Morph> morphs = ACTIVE.get(entity.getId());
        if (morphs == null) {
            return null;
        }
        Morph morph = morphs.get(kind);
        if (morph == null) {
            return null;
        }
        if (entity.level().getGameTime() >= morph.end()) {
            morphs.remove(kind);
            if (morphs.isEmpty()) {
                ACTIVE.remove(entity.getId());
            }
            return null;
        }
        return morph;
    }

    public static boolean any(Entity entity) {
        Map<Integer, Morph> morphs = ACTIVE.get(entity.getId());
        return morphs != null && !morphs.isEmpty();
    }

    public static void prune(long now) {
        Iterator<Map.Entry<Integer, Map<Integer, Morph>>> iterator = ACTIVE.entrySet().iterator();
        while (iterator.hasNext()) {
            Map<Integer, Morph> morphs = iterator.next().getValue();
            morphs.values().removeIf(morph -> now >= morph.end() || morph.start() > now + 200L);
            if (morphs.isEmpty()) {
                iterator.remove();
            }
        }
    }

    public static void clearClient() {
        ACTIVE.clear();
    }
}
