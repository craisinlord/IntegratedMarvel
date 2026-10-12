package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public final class TimeRateTuningManager {
    private static final long HOLD_TICKS = 30L;
    private static final Map<UUID, TuningState> TUNING = new HashMap<>();

    private TimeRateTuningManager() {}

    public static void hold(ServerPlayer player, LivingEntity target) {
        UUID playerId = player.getUUID();
        TuningState state = TUNING.get(playerId);
        if (state != null && state.entity != target) {
            restore(state);
            TUNING.remove(playerId);
            state = null;
        }
        if (state == null) {
            state = new TuningState(target, target.level().dimension(), target.isNoGravity(),
                    target instanceof Mob mob && mob.isNoAi());
            TUNING.put(playerId, state);
            target.setNoGravity(true);
            if (target instanceof Mob mob) mob.setNoAi(true);
        }
        state.expiresAt = player.getServer().overworld().getGameTime() + HOLD_TICKS;
        target.setDeltaMovement(Vec3.ZERO);
        target.hasImpulse = true;
    }

    public static void tick(MinecraftServer server) {
        Iterator<Map.Entry<UUID, TuningState>> iterator = TUNING.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, TuningState> entry = iterator.next();
            UUID playerId = entry.getKey();
            TuningState state = entry.getValue();
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            Entity entity = state.entity;
            if (player == null || entity.isRemoved() || entity.level().isClientSide
                    || !entity.level().dimension().equals(state.dimension)
                    || server.overworld().getGameTime() >= state.expiresAt
                    || !GauntletAbilityController.canAdjustTargetTime(player)) {
                restore(state);
                iterator.remove();
                continue;
            }
            entity.setDeltaMovement(Vec3.ZERO);
            entity.hasImpulse = true;
        }
    }

    public static void clear(MinecraftServer server) {
        for (TuningState state : TUNING.values()) restore(state);
        TUNING.clear();
    }

    public static void clearPlayer(UUID playerId) {
        TuningState state = TUNING.remove(playerId);
        if (state != null) restore(state);
    }

    private static void restore(TuningState state) {
        Entity entity = state.entity;
        if (entity.isRemoved() || !entity.level().dimension().equals(state.dimension)) return;
        entity.setNoGravity(state.noGravity);
        if (entity instanceof Mob mob) mob.setNoAi(state.noAi);
        entity.setDeltaMovement(Vec3.ZERO);
        entity.hasImpulse = true;
    }

    private static final class TuningState {
        private final Entity entity;
        private final net.minecraft.resources.ResourceKey<Level> dimension;
        private final boolean noGravity;
        private final boolean noAi;
        private long expiresAt;

        private TuningState(Entity entity, net.minecraft.resources.ResourceKey<Level> dimension,
                            boolean noGravity, boolean noAi) {
            this.entity = entity;
            this.dimension = dimension;
            this.noGravity = noGravity;
            this.noAi = noAi;
        }
    }
}
