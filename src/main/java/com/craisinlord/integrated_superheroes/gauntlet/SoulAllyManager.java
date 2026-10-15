package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulAllyManager {
    private static final String OWNER_TAG = "integrated_superheroes_soul_owner";
    private static final Map<UUID, Ally> ALLIES = new ConcurrentHashMap<>();

    private SoulAllyManager() {
    }

    public static void bind(ServerPlayer owner, Mob mob) {
        int cap = GauntletConfig.SOUL_ALLY_CAP.get();
        if (cap <= 0) return;
        long owned = ALLIES.values().stream().filter(ally -> ally.owner().equals(owner.getUUID())).count();
        while (owned >= cap) {
            var oldest = ALLIES.entrySet().stream().filter(entry -> entry.getValue().owner().equals(owner.getUUID()))
                    .min(Comparator.comparingLong(entry -> entry.getValue().boundAt()));
            if (oldest.isEmpty()) break;
            release(owner.server, oldest.get().getKey());
            owned--;
        }
        mob.getPersistentData().putUUID(OWNER_TAG, owner.getUUID());
        mob.setPersistenceRequired();
        mob.setTarget(null);
        ALLIES.put(mob.getUUID(), new Ally(owner.getUUID(), mob.level().dimension(), owner.server.overworld().getGameTime()));
        if (mob.level() instanceof ServerLevel level) {
            GauntletFx.burst(level, mob.position().add(0.0D, mob.getBbHeight() * 0.5D, 0.0D), "soul", 40, mob.getBbWidth());
            level.sendParticles(ParticleTypes.SOUL, mob.getX(), mob.getY() + 1.0D, mob.getZ(), 20, 0.4D, 0.6D, 0.4D, 0.02D);
        }
        owner.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.soul_bound",
                mob.getDisplayName()), true);
    }

    private static void release(MinecraftServer server, UUID mobId) {
        Ally ally = ALLIES.remove(mobId);
        if (ally == null) return;
        ServerLevel level = server.getLevel(ally.dimension());
        Entity entity = level == null ? null : level.getEntity(mobId);
        if (entity != null) entity.getPersistentData().remove(OWNER_TAG);
    }

    public static void forget(UUID mob) {
        ALLIES.remove(mob);
    }

    public static boolean isAllyOf(UUID owner, Entity entity) {
        Ally ally = entity == null ? null : ALLIES.get(entity.getUUID());
        return ally != null && ally.owner().equals(owner);
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof Mob mob)) return;
        if (!mob.getPersistentData().hasUUID(OWNER_TAG)) return;
        UUID owner = mob.getPersistentData().getUUID(OWNER_TAG);
        Ally previous = ALLIES.get(mob.getUUID());
        ALLIES.put(mob.getUUID(), new Ally(owner, event.getLevel().dimension(), previous == null ? 0L : previous.boundAt()));
    }

    public static void onChangeTarget(LivingChangeTargetEvent event) {
        Ally ally = ALLIES.get(event.getEntity().getUUID());
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (ally == null || target == null) return;
        if (target.getUUID().equals(ally.owner()) || isAllyOf(ally.owner(), target)) event.setCanceled(true);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        LivingEntity victim = event.getEntity();
        Ally attackerAlly = ALLIES.get(attacker.getUUID());
        if (attackerAlly != null && (victim.getUUID().equals(attackerAlly.owner()) || isAllyOf(attackerAlly.owner(), victim))) {
            event.setCanceled(true);
            return;
        }
        if (isAllyOf(attacker.getUUID(), victim)) event.setCanceled(true);
    }

    public static void tick(MinecraftServer server) {
        long now = server.overworld().getGameTime();
        if (ALLIES.isEmpty() || now % 5L != 0L) return;
        for (Map.Entry<UUID, Ally> entry : Map.copyOf(ALLIES).entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue().dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob)) continue;
            if (!mob.isAlive()) {
                ALLIES.remove(entry.getKey());
                continue;
            }
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getValue().owner());
            if (owner == null || owner.level() != mob.level()) continue;
            LivingEntity current = mob.getTarget();
            if (current != null && (current == owner || isAllyOf(owner.getUUID(), current) || !current.isAlive())) {
                mob.setTarget(null);
            }
            LivingEntity desired = pick(owner, mob, owner.getLastHurtMob());
            if (desired == null) desired = pick(owner, mob, owner.getLastHurtByMob());
            if (desired != null) {
                mob.setTarget(desired);
            } else if (mob.getTarget() == null) {
                double distance = mob.distanceToSqr(owner);
                if (distance > 48.0D * 48.0D) {
                    mob.teleportTo(owner.getX() + 1.5D, owner.getY(), owner.getZ() + 1.5D);
                } else if (distance > 36.0D) {
                    mob.getNavigation().moveTo(owner, 1.25D);
                }
            }
            if (now % 20L == 0L) {
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(),
                        2, 0.2D, 0.1D, 0.2D, 0.01D);
            }
        }
    }

    private static LivingEntity pick(ServerPlayer owner, Mob mob, LivingEntity candidate) {
        if (candidate == null || !candidate.isAlive() || candidate == mob || candidate == owner
                || isAllyOf(owner.getUUID(), candidate) || candidate.distanceToSqr(owner) > 32.0D * 32.0D) {
            return null;
        }
        return candidate;
    }

    public static void clear() {
        ALLIES.clear();
    }

    private record Ally(UUID owner, ResourceKey<Level> dimension, long boundAt) {
    }
}
