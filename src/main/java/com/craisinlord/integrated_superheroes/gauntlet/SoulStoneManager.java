package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import com.craisinlord.integrated_superheroes.registry.ModEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SoulStoneManager {
    private static final Map<UUID, SeverState> SEVERED = new ConcurrentHashMap<>();
    private static final Map<UUID, PendingDeath> PENDING_DEATHS = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> FINALIZING = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> REAPING = new ConcurrentHashMap<>();
    private static final Map<UUID, StoredDamage> STORED_DAMAGE = new ConcurrentHashMap<>();

    private SoulStoneManager() {
    }

    public static void clearFinalizing(UUID id) {
        FINALIZING.remove(id);
    }

    public static boolean isPending(LivingEntity entity) {
        return PENDING_DEATHS.containsKey(entity.getUUID());
    }

    public static void exchange(ServerPlayer wielder, LivingEntity target) {
        if (target == wielder || !target.isAlive()) return;
        float amount = Math.min(10.0F, target.getHealth());
        if (wielder.isCrouching()) {
            float available = Math.max(0.0F, wielder.getHealth() - 1.0F);
            float requested = Math.min(available, Math.min(10.0F, target.getMaxHealth() - target.getHealth()));
            if (requested <= 0.0F) return;
            float before = wielder.getHealth();
            wielder.hurt(wielder.damageSources().magic(), requested);
            float paid = Math.max(0.0F, before - wielder.getHealth());
            target.heal(Math.min(paid, target.getMaxHealth() - target.getHealth()));
            wielder.displayClientMessage(Component.literal("You trade " + paid + " life to " + target.getDisplayName().getString()), true);
            return;
        }
        float before = target.getHealth();
        target.hurt(wielder.damageSources().playerAttack(wielder), amount);
        float drained = Math.max(0.0F, before - target.getHealth());
        wielder.heal(Math.min(drained, wielder.getMaxHealth() - wielder.getHealth()));
        wielder.displayClientMessage(Component.literal("You draw " + drained + " life from " + target.getDisplayName().getString()), true);
    }

    public static void sever(ServerPlayer wielder, LivingEntity target) {
        if (target == wielder || !target.isAlive() || isPending(target)) return;
        long expiry = target.level().getGameTime() + 160L;
        SeverState old = SEVERED.get(target.getUUID());
        boolean priorNoAi = target instanceof Mob mob && mob.isNoAi();
        boolean priorInvulnerable = target.isInvulnerable();
        if (old != null) {
            priorNoAi = old.noAi();
            priorInvulnerable = old.invulnerable();
        }
        GauntletEntityLocks.remember(target);
        if (target instanceof Mob mob) mob.setNoAi(true);
        target.setDeltaMovement(Vec3.ZERO);
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.soulSevered(), 160, 0, false, false, true));
        SEVERED.put(target.getUUID(), new SeverState(target.level().dimension(), expiry, priorNoAi, priorInvulnerable));
        STORED_DAMAGE.putIfAbsent(target.getUUID(), new StoredDamage(wielder.getUUID(), new float[1]));
        wielder.displayClientMessage(Component.translatable("message.integrated_superheroes.gauntlet.soul_severed",
                target.getDisplayName()), true);
    }

    public static void fate(ServerPlayer wielder, LivingEntity target) {
        if (isPending(target)) {
            restore(target);
            if (target instanceof Mob mob && target != wielder) SoulAllyManager.bind(wielder, mob);
            wielder.displayClientMessage(Component.literal(target.getDisplayName().getString() + " is restored"), true);
            return;
        }
        if (target == wielder) return;
        if (wielder.isCrouching()) {
            REAPING.put(target.getUUID(), true);
            target.hurt(wielder.damageSources().playerAttack(wielder), target.getMaxHealth() * 100.0F);
            REAPING.remove(target.getUUID());
            wielder.displayClientMessage(Component.literal("The Soul Stone reaps " + target.getDisplayName().getString()), true);
            return;
        }
        wielder.displayClientMessage(Component.literal("Select a fresh soul remnant to restore it; crouch-activate to reap a living target"), true);
    }

    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity entity = event.getEntity();
        UUID id = entity.getUUID();
        if (FINALIZING.remove(id) != null || REAPING.remove(id) != null) return;
        ServerPlayer catcher = null;
        double nearest = 32.0D * 32.0D;
        if (entity.level() instanceof ServerLevel level) {
            for (ServerPlayer candidate : level.players()) {
                var gauntlet = InfinityGauntletStones.equippedGauntlet(candidate);
                if (gauntlet.isEmpty() || !InfinityGauntletStones.hasStone(gauntlet, "soul")
                        || !InfinityGauntletStones.selectedStone(gauntlet).equals("soul")
                        || GauntletAbilityController.isAbilityOnCooldown(candidate)) continue;
                double distance = candidate.distanceToSqr(entity);
                if (GauntletAbilities.isSelected(gauntlet, "soul", "reapRestore") && distance <= nearest) {
                    nearest = distance;
                    catcher = candidate;
                }
            }
        }
        if (catcher == null) return;
        event.setCanceled(true);
        ItemStack gauntlet = InfinityGauntletStones.equippedGauntlet(catcher);
        GauntletAbilityController.addAbilityCooldown(catcher, 100);
        long expiry = entity.level().getGameTime() + 100L;
        boolean priorNoAi = entity instanceof Mob mob && mob.isNoAi();
        boolean priorInvulnerable = entity.isInvulnerable();
        GauntletEntityLocks.remember(entity);
        entity.setHealth(1.0F);
        entity.setInvulnerable(true);
        entity.setDeltaMovement(Vec3.ZERO);
        if (entity instanceof Mob mob) mob.setNoAi(true);
        entity.addEffect(new net.minecraft.world.effect.MobEffectInstance(ModEffects.soulSevered(), 100, 0, false, false, true));
        SEVERED.put(id, new SeverState(entity.level().dimension(), expiry, priorNoAi, priorInvulnerable));
        PENDING_DEATHS.put(id, new PendingDeath(entity.level().dimension(), expiry));
    }

    public static void tick(MinecraftServer server) {
        for (Map.Entry<UUID, SeverState> entry : SEVERED.entrySet()) {
            UUID id = entry.getKey();
            SeverState state = entry.getValue();
            ServerLevel level = server.getLevel(state.dimension());
            Entity entity = level == null ? null : level.getEntity(id);
            if (entity == null) {
                ServerLevel currentLevel = findEntityLevel(server, id);
                if (currentLevel != null) {
                    level = currentLevel;
                    entity = currentLevel.getEntity(id);
                }
            }
            if (!(entity instanceof LivingEntity living)) {
                if (level != null && level.getGameTime() >= state.expiry()) {
                    SEVERED.remove(id);
                    PENDING_DEATHS.remove(id);
                    STORED_DAMAGE.remove(id);
                }
                continue;
            }
            if (!level.dimension().equals(state.dimension())) {
                if (PENDING_DEATHS.containsKey(id)) finishDeath(living);
                else release(living, state);
                continue;
            }
            if (living instanceof Player) living.setDeltaMovement(Vec3.ZERO);
            if (level.getGameTime() >= state.expiry()) {
                if (PENDING_DEATHS.containsKey(id)) {
                    finishDeath(living);
                } else {
                    release(living, state);
                }
            }
        }
    }

    public static void clear(MinecraftServer server) {
        for (Map.Entry<UUID, SeverState> entry : SEVERED.entrySet()) {
            ServerLevel level = server.getLevel(entry.getValue().dimension());
            Entity entity = level == null ? null : level.getEntity(entry.getKey());
            if (entity == null) {
                level = findEntityLevel(server, entry.getKey());
                entity = level == null ? null : level.getEntity(entry.getKey());
            }
            if (entity instanceof LivingEntity living) {
                if (PENDING_DEATHS.containsKey(entry.getKey())) finishDeath(living);
                else release(living, entry.getValue());
            }
        }
        SEVERED.clear();
        PENDING_DEATHS.clear();
        STORED_DAMAGE.clear();
        FINALIZING.clear();
        REAPING.clear();
    }

    public static void onPlayerLogout(ServerPlayer player) {
        UUID id = player.getUUID();
        SeverState state = SEVERED.get(id);
        if (PENDING_DEATHS.containsKey(id)) finishDeath(player);
        else if (state != null) release(player, state);
    }

    private static void restore(LivingEntity entity) {
        UUID id = entity.getUUID();
        SeverState state = SEVERED.remove(id);
        PENDING_DEATHS.remove(id);
        entity.removeEffect(ModEffects.soulSevered());
        GauntletEntityLocks.forget(entity);
        entity.setInvulnerable(state != null && state.invulnerable());
        if (entity instanceof Mob mob && state != null) mob.setNoAi(state.noAi());
        entity.setHealth(Math.max(1.0F, entity.getMaxHealth() * 0.3F));
        entity.setDeltaMovement(Vec3.ZERO);
    }

    private static void release(LivingEntity entity, SeverState state) {
        SEVERED.remove(entity.getUUID());
        entity.removeEffect(ModEffects.soulSevered());
        GauntletEntityLocks.forget(entity);
        entity.setInvulnerable(state.invulnerable());
        if (entity instanceof Mob mob) mob.setNoAi(state.noAi());
        StoredDamage stored = STORED_DAMAGE.remove(entity.getUUID());
        if (stored != null && stored.amount()[0] > 0.0F && entity.isAlive()
                && entity.level() instanceof ServerLevel level) {
            ServerPlayer wielder = level.getServer().getPlayerList().getPlayer(stored.wielder());
            entity.invulnerableTime = 0;
            entity.hurt(wielder != null ? entity.damageSources().playerAttack(wielder) : entity.damageSources().magic(),
                    stored.amount()[0] * 2.0F);
            GauntletFx.burst(level, entity.position().add(0.0D, entity.getBbHeight() * 0.5D, 0.0D), "soul", 60,
                    entity.getBbWidth());
            GauntletFx.sound(level, entity.position(), net.minecraft.sounds.SoundEvents.SOUL_ESCAPE.value(), 2.0F, 0.5F);
        }
    }

    public static void onIncomingDamage(net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent event) {
        UUID id = event.getEntity().getUUID();
        StoredDamage stored = STORED_DAMAGE.get(id);
        if (stored == null || !SEVERED.containsKey(id) || PENDING_DEATHS.containsKey(id)) return;
        if (event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        stored.amount()[0] += event.getAmount();
        event.setCanceled(true);
        if (event.getEntity().level() instanceof ServerLevel level) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL, event.getEntity().getX(),
                    event.getEntity().getY() + event.getEntity().getBbHeight() * 0.5D, event.getEntity().getZ(), 6,
                    0.3D, 0.3D, 0.3D, 0.02D);
        }
    }

    private static void finishDeath(LivingEntity entity) {
        UUID id = entity.getUUID();
        SeverState state = SEVERED.remove(id);
        PENDING_DEATHS.remove(id);
        entity.removeEffect(ModEffects.soulSevered());
        entity.setInvulnerable(false);
        if (entity instanceof Mob mob) mob.setNoAi(false);
        STORED_DAMAGE.remove(id);
        GauntletEntityLocks.forget(entity);
        FINALIZING.put(id, true);
        entity.hurt(entity.damageSources().genericKill(), Float.MAX_VALUE);
    }

    private static ServerLevel findEntityLevel(MinecraftServer server, UUID id) {
        for (ServerLevel level : server.getAllLevels()) if (level.getEntity(id) != null) return level;
        return null;
    }

    private record SeverState(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,
                              long expiry, boolean noAi, boolean invulnerable) {}
    private record StoredDamage(UUID wielder, float[] amount) {}
    private record PendingDeath(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, long expiry) {}
}
