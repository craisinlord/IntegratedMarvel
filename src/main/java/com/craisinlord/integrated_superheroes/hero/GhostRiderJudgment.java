package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class GhostRiderJudgment {
    private static final Map<UUID, Map<UUID, Mark>> MARKS = new HashMap<>();

    private GhostRiderJudgment() {
    }

    private static final class Mark {
        private int stacks;
        private long lastAggression = -1_000_000L;
        private long touched;
    }

    public static int stacks(ServerPlayer owner, LivingEntity target) {
        Mark mark = mark(owner, target, false);
        return mark == null ? 0 : mark.stacks;
    }

    public static int add(ServerPlayer owner, LivingEntity target) {
        Mark mark = mark(owner, target, true);
        mark.stacks = Math.min(GhostRiderConfig.JUDGMENT_MAX.get(), mark.stacks + 1);
        mark.touched = owner.serverLevel().getGameTime();
        return mark.stacks;
    }

    public static int consume(ServerPlayer owner, LivingEntity target) {
        Map<UUID, Mark> marks = MARKS.get(owner.getUUID());
        if (marks == null) {
            return 0;
        }
        Mark mark = marks.remove(target.getUUID());
        return mark == null || expired(owner, mark) ? 0 : mark.stacks;
    }

    private static boolean expired(ServerPlayer owner, Mark mark) {
        return owner.serverLevel().getGameTime() - mark.touched > GhostRiderConfig.JUDGMENT_EXPIRE_TICKS.get();
    }

    private static Mark mark(ServerPlayer owner, LivingEntity target, boolean create) {
        Map<UUID, Mark> marks = create ? MARKS.computeIfAbsent(owner.getUUID(), ignored -> new HashMap<>()) : MARKS.get(owner.getUUID());
        if (marks == null) {
            return null;
        }
        Mark mark = marks.get(target.getUUID());
        if (mark != null && expired(owner, mark)) {
            marks.remove(target.getUUID());
            mark = null;
        }
        if (mark == null && create) {
            mark = new Mark();
            marks.put(target.getUUID(), mark);
        }
        return mark;
    }

    public static void tick(ServerPlayer owner) {
        Map<UUID, Mark> marks = MARKS.get(owner.getUUID());
        if (marks == null) {
            return;
        }
        ServerLevel level = owner.serverLevel();
        Iterator<Map.Entry<UUID, Mark>> iterator = marks.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Mark> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || expired(owner, entry.getValue())
                    || living.distanceToSqr(owner) > 64.0D * 64.0D) {
                iterator.remove();
                continue;
            }
            int stacks = entry.getValue().stacks;
            level.sendParticles(ParticleTypes.SOUL, living.getX(), living.getY() + living.getBbHeight() + 0.3D, living.getZ(),
                    stacks, 0.2D, 0.1D, 0.2D, 0.0D);
        }
        if (marks.isEmpty()) {
            MARKS.remove(owner.getUUID());
        }
    }

    public static void onDamage(LivingDamageEvent.Post event) {
        if (event.getNewDamage() <= 0.0F || !(event.getSource().getEntity() instanceof LivingEntity attacker)
                || !(event.getEntity().level() instanceof ServerLevel level)) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (attacker == victim) {
            return;
        }
        double radius = GhostRiderConfig.JUDGMENT_RADIUS.get();
        for (ServerPlayer rider : level.players()) {
            if (!SerumKit.is(rider, GhostRiderPower.ID) || rider == attacker || !rider.isAlive()) {
                continue;
            }
            if (victim != rider && (victim.distanceToSqr(rider) > radius * radius || !ally(rider, victim, attacker))) {
                continue;
            }
            if (!HellfireZones.harmable(rider, attacker)) {
                continue;
            }
            Mark mark = mark(rider, attacker, true);
            long now = level.getGameTime();
            if (now - mark.lastAggression < GhostRiderConfig.JUDGMENT_AGGRESSION_TICKS.get()) {
                mark.touched = now;
                continue;
            }
            mark.lastAggression = now;
            mark.stacks = Math.min(GhostRiderConfig.JUDGMENT_MAX.get(), mark.stacks + 1);
            mark.touched = now;
        }
    }

    private static boolean ally(ServerPlayer rider, LivingEntity victim, LivingEntity attacker) {
        if (victim instanceof OwnableEntity ownable && rider.getUUID().equals(ownable.getOwnerUUID())) {
            return true;
        }
        if (victim instanceof Player other) {
            if (other.isAlliedTo(rider)) {
                return true;
            }
            return !(attacker instanceof Player);
        }
        return victim.isAlliedTo(rider);
    }

    public static void clear(ServerPlayer owner) {
        MARKS.remove(owner.getUUID());
    }

    public static void clearAll() {
        MARKS.clear();
    }
}
