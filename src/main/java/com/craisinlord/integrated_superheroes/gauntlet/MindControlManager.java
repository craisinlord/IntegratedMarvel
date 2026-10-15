package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MindControlManager {
    private static final Map<UUID, Session> BY_TARGET = new ConcurrentHashMap<>();
    private static final Map<UUID, java.util.Set<UUID>> BY_CONTROLLER = new ConcurrentHashMap<>();
    private static final Map<UUID, MindControlInputPayload> INPUTS = new ConcurrentHashMap<>();
    private static final Map<UUID, MindControlInputPayload> LAST_SENT_STATES = new ConcurrentHashMap<>();
    private static final Map<UUID, TargetCache> TARGET_CACHES = new ConcurrentHashMap<>();
    private static final java.util.Set<UUID> SYNCED_CONTROLLERS = ConcurrentHashMap.newKeySet();

    private MindControlManager() {
    }

    public static boolean control(ServerPlayer controller, LivingEntity target) {
        return control(controller, target, 300L, true);
    }

    public static boolean control(ServerPlayer controller, LivingEntity target, long duration, boolean exclusive) {
        if (target == controller || target.level() != controller.level() || !target.isAlive()) {
            return false;
        }
        if (exclusive) {
            java.util.Set<UUID> priorTargets = BY_CONTROLLER.remove(controller.getUUID());
            if (priorTargets != null) {
                for (UUID prior : java.util.List.copyOf(priorTargets)) release(prior);
            }
        }
        release(target.getUUID());
        UUID originalMobTarget = target instanceof Mob mob && mob.getTarget() != null
                ? mob.getTarget().getUUID() : null;
        long expiry = controller.server.overworld().getGameTime() + duration;
        BY_TARGET.put(target.getUUID(), new Session(controller.getUUID(), target.level().dimension(), expiry, originalMobTarget));
        BY_CONTROLLER.computeIfAbsent(controller.getUUID(), key -> ConcurrentHashMap.newKeySet()).add(target.getUUID());
        INPUTS.putIfAbsent(controller.getUUID(), neutralInput(controller));
        return true;
    }

    public static void acceptInput(ServerPlayer controller, MindControlInputPayload input) {
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(controller);
        if ((BY_CONTROLLER.containsKey(controller.getUUID()) || MindLinkManager.isLinked(controller.getUUID())) && !gauntlet.isEmpty()
                && InfinityGauntletStones.hasStone(gauntlet, "mind")
                && Float.isFinite(input.yaw()) && Float.isFinite(input.pitch())) {
            INPUTS.put(controller.getUUID(), new MindControlInputPayload(input.up(), input.down(), input.left(), input.right(),
                    input.jump(), input.sprint(), input.attack(), net.minecraft.util.Mth.wrapDegrees(input.yaw()),
                    net.minecraft.util.Mth.clamp(input.pitch(), -90.0F, 90.0F)));
        }
    }

    public static void seedInput(ServerPlayer controller) {
        INPUTS.putIfAbsent(controller.getUUID(), neutralInput(controller));
    }

    public static MindControlInputPayload input(UUID controller) {
        return INPUTS.get(controller);
    }

    public static boolean isControlled(UUID entityId) {
        return BY_TARGET.containsKey(entityId);
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        for (Map.Entry<UUID, Session> entry : BY_TARGET.entrySet()) {
            UUID targetId = entry.getKey();
            Session session = entry.getValue();
            ServerLevel level = server.getLevel(session.dimension());
            Entity targetEntity = level == null ? null : level.getEntity(targetId);
            ServerPlayer controller = server.getPlayerList().getPlayer(session.controller());
            if (!(targetEntity instanceof LivingEntity target) || controller == null || !target.isAlive()
                    || !controller.isAlive() || controller.level() != target.level()
                    || !hasEquippedMindGauntlet(controller)
                    || now >= session.expiry()) {
                release(targetId);
                continue;
            }
            MindControlInputPayload input = INPUTS.getOrDefault(controller.getUUID(), neutralInput(controller));
            LivingEntity commandTarget = findCommandTarget(controller, target, now);
            if (target instanceof Mob mob) {
                if (commandTarget != null && commandTarget != mob && !mob.isAlliedTo(commandTarget)) {
                    mob.setTarget(commandTarget);
                }
            } else if (target instanceof ServerPlayer controlledPlayer) {
                MindControlInputPayload sent = new MindControlInputPayload(
                        input.up(), input.down(), input.left(), input.right(), input.jump(), input.sprint(),
                        input.attack(), input.yaw(), input.pitch());
                if (!sent.equals(LAST_SENT_STATES.get(targetId))) {
                    LAST_SENT_STATES.put(targetId, sent);
                    PacketDistributor.sendToPlayer(controlledPlayer, new MindControlStatePayload(true,
                            sent.up(), sent.down(), sent.left(), sent.right(), sent.jump(), sent.sprint(),
                            sent.yaw(), sent.pitch()));
                }
                if (commandTarget != null && input.attack() && now % 10L == 0L
                        && controlledPlayer.distanceToSqr(commandTarget) <= 25.0D) {
                    controlledPlayer.attack(commandTarget);
                    controlledPlayer.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                }
            }
        }
        INPUTS.keySet().removeIf(controller -> !BY_CONTROLLER.containsKey(controller) && !MindLinkManager.isLinked(controller));
        syncControllers(server);
    }

    private static void syncControllers(MinecraftServer server) {
        java.util.Set<UUID> active = new java.util.HashSet<>(BY_CONTROLLER.keySet());
        active.addAll(INPUTS.keySet());
        for (UUID id : active) {
            if (!BY_CONTROLLER.containsKey(id) && !MindLinkManager.isLinked(id)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null && SYNCED_CONTROLLERS.add(id)) PacketDistributor.sendToPlayer(player, new MindControllerPayload(true));
        }
        for (var iterator = SYNCED_CONTROLLERS.iterator(); iterator.hasNext();) {
            UUID id = iterator.next();
            if (BY_CONTROLLER.containsKey(id) || MindLinkManager.isLinked(id)) continue;
            iterator.remove();
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            if (player != null) PacketDistributor.sendToPlayer(player, new MindControllerPayload(false));
        }
    }

    public static void release(UUID targetId) {
        Session session = BY_TARGET.remove(targetId);
        if (session == null) {
            return;
        }
        BY_CONTROLLER.computeIfPresent(session.controller(), (key, targets) -> {
            targets.remove(targetId);
            return targets.isEmpty() ? null : targets;
        });
        LAST_SENT_STATES.remove(targetId);
        TARGET_CACHES.remove(targetId);
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        ServerPlayer controlledPlayer = server == null ? null : server.getPlayerList().getPlayer(targetId);
        if (controlledPlayer != null) {
            PacketDistributor.sendToPlayer(controlledPlayer, new MindControlStatePayload(false, false, false,
                    false, false, false, false, controlledPlayer.getYRot(), controlledPlayer.getXRot()));
        }
        ServerLevel level = server == null ? null : server.getLevel(session.dimension());
        Entity entity = level == null ? null : level.getEntity(targetId);
        if (entity instanceof Mob mob) {
            Entity previousTarget = session.originalMobTarget() == null ? null : level.getEntity(session.originalMobTarget());
            mob.setTarget(previousTarget instanceof LivingEntity living && living.isAlive() ? living : null);
        }
    }

    public static void releaseController(UUID controllerId) {
        java.util.Set<UUID> targets = BY_CONTROLLER.remove(controllerId);
        if (targets != null) {
            for (UUID target : java.util.List.copyOf(targets)) release(target);
        }
        INPUTS.remove(controllerId);
    }

    public static void clear(MinecraftServer server) {
        for (UUID target : BY_TARGET.keySet().toArray(UUID[]::new)) {
            release(target);
        }
        BY_TARGET.clear();
        BY_CONTROLLER.clear();
        INPUTS.clear();
        LAST_SENT_STATES.clear();
        TARGET_CACHES.clear();
        SYNCED_CONTROLLERS.clear();
    }

    private static boolean hasEquippedMindGauntlet(ServerPlayer controller) {
        var gauntlet = InfinityGauntletStones.equippedGauntlet(controller);
        return !gauntlet.isEmpty() && InfinityGauntletStones.hasStone(gauntlet, "mind");
    }

    private static LivingEntity findCommandTarget(ServerPlayer controller, LivingEntity controlled, long now) {
        LivingEntity candidate = controller.getLastHurtMob();
        if (validTarget(controller, controlled, candidate)) {
            TARGET_CACHES.put(controlled.getUUID(), new TargetCache(candidate.getUUID(), now + 10L));
            return candidate;
        }
        candidate = controller.getLastHurtByMob();
        if (validTarget(controller, controlled, candidate)) {
            TARGET_CACHES.put(controlled.getUUID(), new TargetCache(candidate.getUUID(), now + 10L));
            return candidate;
        }
        if (controlled instanceof Mob mob && validTarget(controller, controlled, mob.getLastHurtByMob())) {
            candidate = mob.getLastHurtByMob();
            TARGET_CACHES.put(controlled.getUUID(), new TargetCache(candidate.getUUID(), now + 10L));
            return mob.getLastHurtByMob();
        }
        TargetCache cache = TARGET_CACHES.get(controlled.getUUID());
        Entity cached = cache == null || cache.target() == null || !(controlled.level() instanceof ServerLevel level)
                ? null : level.getEntity(cache.target());
        if (cached instanceof LivingEntity living && validTarget(controller, controlled, living)) return living;
        if (cache != null && now < cache.nextSearchTick()) return null;
        List<LivingEntity> nearby = controlled.level().getEntitiesOfClass(LivingEntity.class,
                controlled.getBoundingBox().inflate(24.0D), entity -> validTarget(controller, controlled, entity));
        LivingEntity closest = null;
        double closestDistance = Double.MAX_VALUE;
        for (LivingEntity entity : nearby) {
            double distance = controlled.distanceToSqr(entity);
            if (distance < closestDistance) {
                closest = entity;
                closestDistance = distance;
            }
        }
        TARGET_CACHES.put(controlled.getUUID(), new TargetCache(closest == null ? null : closest.getUUID(), now + 10L));
        return closest;
    }

    private static boolean validTarget(ServerPlayer controller, LivingEntity controlled, LivingEntity candidate) {
        return candidate != null && candidate.isAlive() && candidate != controller && candidate != controlled
                && !candidate.isAlliedTo(controller) && !candidate.isAlliedTo(controlled);
    }

    private static MindControlInputPayload neutralInput(ServerPlayer player) {
        return new MindControlInputPayload(false, false, false, false, false, false, false,
                player.getYRot(), player.getXRot());
    }

    private record Session(UUID controller, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                           long expiry, UUID originalMobTarget) {
    }

    private record TargetCache(UUID target, long nextSearchTick) {
    }
}
