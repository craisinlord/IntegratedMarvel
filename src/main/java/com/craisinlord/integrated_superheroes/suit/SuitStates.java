package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SuitStates {
    private static final Map<UUID, State> STATES = new HashMap<>();

    private SuitStates() {
    }

    static final class State {
        boolean landingArmed;
        int landingTicks;
        double landingPeakY;
        float landingDamage;
        boolean noFall;
        int noFallGrace;
        long stealthUntil;
        boolean overchargeArmed;
        long braceUntil;
        UUID carried;
        long carryUntil;
        float carryDamage;
        final List<Thrown> thrown = new ArrayList<>();
        int starboostTicks;
        int starboostTotal;
        long irradiatedImmuneUntil;
        int protonTicks;
        float protonDamage;
    }

    static final class Thrown {
        final UUID entity;
        final float damage;
        int ticks;

        Thrown(UUID entity, float damage) {
            this.entity = entity;
            this.damage = damage;
        }
    }

    static State get(ServerPlayer player) {
        return STATES.computeIfAbsent(player.getUUID(), uuid -> new State());
    }

    static boolean isMidAbility(ServerPlayer player, String ability) {
        State state = STATES.get(player.getUUID());
        return state != null && SuitAbilities.HEAVY_THROW.equals(ability) && state.carried != null;
    }

    static void noFall(ServerPlayer player) {
        State state = get(player);
        state.noFall = true;
        state.noFallGrace = 5;
        player.resetFallDistance();
    }

    public static void registerEvents() {
        NeoForge.EVENT_BUS.addListener(SuitStates::onFall);
        NeoForge.EVENT_BUS.addListener(SuitStates::onKnockBack);
        NeoForge.EVENT_BUS.addListener(SuitStates::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SuitStates::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(SuitStates::onLogout);
    }

    public static void tick(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null) {
            return;
        }
        long now = player.level().getGameTime();
        tickLanding(player, state);
        tickStarboost(player, state);
        if (state.noFall) {
            player.resetFallDistance();
            if (state.noFallGrace > 0) {
                state.noFallGrace--;
            } else if ((player.onGround() || player.isInWater()) && !state.landingArmed && state.starboostTicks <= 0) {
                state.noFall = false;
            }
        }
        if (state.stealthUntil > now && player.tickCount % 10 == 0) {
            SuitCombatAbilities.loseTrackOf(player, 32.0D);
        }
        tickCarry(player, state, now);
        tickThrown(player, state);
        if (state.protonTicks > 0) {
            SuitCombatAbilities.protonPulse(player, state.protonTicks, state.protonDamage);
            state.protonTicks--;
        }
    }

    private static void tickLanding(ServerPlayer player, State state) {
        if (!state.landingArmed) {
            return;
        }
        state.landingTicks++;
        state.landingPeakY = Math.max(state.landingPeakY, player.getY());
        if (state.landingTicks > 2 && (player.onGround() || player.isInWater())) {
            state.landingArmed = false;
            SuitMovementAbilities.landingImpact(player, state.landingPeakY - player.getY(), state.landingDamage);
        } else if (state.landingTicks > 200) {
            state.landingArmed = false;
        }
    }

    private static void tickStarboost(ServerPlayer player, State state) {
        if (state.starboostTicks <= 0) {
            return;
        }
        int elapsed = state.starboostTotal - state.starboostTicks;
        Vec3 motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.max(motion.y, 0.8D + 0.04D * elapsed), motion.z);
        player.hurtMarked = true;
        player.serverLevel().sendParticles(ParticleTypes.FLAME, player.getX(), player.getY(), player.getZ(), 4, 0.15D, 0.05D, 0.15D, 0.02D);
        state.starboostTicks--;
    }

    private static void tickCarry(ServerPlayer player, State state, long now) {
        if (state.carried == null) {
            return;
        }
        Entity entity = player.serverLevel().getEntity(state.carried);
        if (!(entity instanceof LivingEntity living) || !living.isAlive() || living.level() != player.level()) {
            state.carried = null;
            HeroAbilities.setCooldown(player, SuitAbilities.HEAVY_THROW, SuitAbilityConfig.HEAVY_THROW.cooldownSeconds().get());
            return;
        }
        if (now >= state.carryUntil) {
            throwCarried(player);
            HeroAbilities.setCooldown(player, SuitAbilities.HEAVY_THROW, SuitAbilityConfig.HEAVY_THROW.cooldownSeconds().get());
            return;
        }
        Vec3 hold = player.getEyePosition().add(player.getLookAngle().scale(1.5D + living.getBbWidth() * 0.5D)).subtract(0.0D, living.getBbHeight() * 0.5D, 0.0D);
        living.teleportTo(hold.x, hold.y, hold.z);
        living.setDeltaMovement(Vec3.ZERO);
        living.resetFallDistance();
        if (living instanceof Mob mob) {
            mob.getNavigation().stop();
        }
    }

    static void grab(ServerPlayer player, LivingEntity target, float damage) {
        State state = get(player);
        state.carried = target.getUUID();
        state.carryUntil = player.level().getGameTime() + SuitAbilityConfig.HEAVY_THROW_HOLD_TICKS.get();
        state.carryDamage = damage;
    }

    static boolean isCarrying(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.carried != null;
    }

    static void throwCarried(ServerPlayer player) {
        State state = get(player);
        Entity entity = state.carried == null ? null : player.serverLevel().getEntity(state.carried);
        state.carried = null;
        if (entity instanceof LivingEntity living && living.isAlive()) {
            living.setDeltaMovement(player.getLookAngle().scale(2.2D).add(0.0D, 0.3D, 0.0D));
            living.hurtMarked = true;
            state.thrown.add(new Thrown(living.getUUID(), state.carryDamage));
        }
    }

    private static void tickThrown(ServerPlayer player, State state) {
        Iterator<Thrown> iterator = state.thrown.iterator();
        while (iterator.hasNext()) {
            Thrown thrown = iterator.next();
            thrown.ticks++;
            Entity entity = player.serverLevel().getEntity(thrown.entity);
            if (!(entity instanceof LivingEntity living) || !living.isAlive() || thrown.ticks > 60) {
                iterator.remove();
                continue;
            }
            if (thrown.ticks > 2 && (living.horizontalCollision || living.verticalCollision || living.onGround())) {
                living.invulnerableTime = 0;
                HeroAbilities.runAsAbilityDamage(() -> living.hurt(player.damageSources().playerAttack(player), thrown.damage));
                player.serverLevel().sendParticles(ParticleTypes.EXPLOSION, living.getX(), living.getY(), living.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                iterator.remove();
            }
        }
    }

    public static double consumeOvercharge(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        if (state == null || !state.overchargeArmed) {
            return 1.0D;
        }
        state.overchargeArmed = false;
        double energy = SuitEnergy.drainAll(player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST));
        player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.2D, player.getZ(), 40, 0.4D, 0.4D, 0.4D, 0.2D);
        return 1.0D + SuitAbilityConfig.OVERCHARGE_MAX_BONUS.get() * (energy / 100.0D);
    }

    private static void onFall(LivingFallEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player.getUUID());
            if (state != null && state.noFall) {
                event.setCanceled(true);
            }
        }
    }

    private static void onKnockBack(LivingKnockBackEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            State state = STATES.get(player.getUUID());
            if (state != null && state.braceUntil > player.level().getGameTime()) {
                event.setCanceled(true);
            }
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().getEntity() instanceof ServerPlayer attacker) {
            State state = STATES.get(attacker.getUUID());
            if (state != null && state.stealthUntil > attacker.level().getGameTime()) {
                state.stealthUntil = 0L;
                attacker.removeEffect(MobEffects.INVISIBILITY);
            }
        }
        if (SuitGrenadeAbilities.blocksFriendlyFire(event)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ServerPlayer target && SuitPassives.blocksDamage(target, event.getSource())) {
            event.setCanceled(true);
            return;
        }
        SuitIntegrationAbilities.onChargeDamage(event);
    }

    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        if (event.getEntity() instanceof ServerPlayer player && SuitPassives.blocksEffect(player, event.getEffectInstance().getEffect())) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    static boolean irradiatedImmune(ServerPlayer player) {
        State state = STATES.get(player.getUUID());
        return state != null && state.irradiatedImmuneUntil > player.level().getGameTime();
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        STATES.remove(event.getEntity().getUUID());
    }
}
