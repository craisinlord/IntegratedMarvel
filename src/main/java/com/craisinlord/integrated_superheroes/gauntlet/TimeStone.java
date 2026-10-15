package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.anthology.time.TimeDilationApi;
import com.craisinlord.anthology.time.TimeDilationEntityAccess;
import com.craisinlord.anthology.time.TimeDilationManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TimeStone {
    private static final double[] FIELD_RATES = {0.05D, 0.1D, 0.25D, 0.5D, 2.0D, 3.0D};
    private static final int DEFAULT_RATE = 2;
    private static final Map<UUID, Integer> FIELD_RATE_INDEX = new ConcurrentHashMap<>();
    private static final Set<UUID> INFINITY_FREEZERS = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Map<UUID, Float>> STRIKES = new ConcurrentHashMap<>();

    private TimeStone() {
    }

    public static void chronostasis(GauntletCast cast) {
        TimeFreezeManager.start(cast.player());
        if (cast.infinity()) INFINITY_FREEZERS.add(cast.player().getUUID());
        GauntletFx.sound(cast.level(), cast.player().position(), SoundEvents.BEACON_DEACTIVATE, 2.0F, 0.5F);
        GauntletFx.sphere(cast.level(), cast.player().position().add(0.0D, 1.0D, 0.0D), 3.0D, "time", 60);
    }

    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer attacker) || !INFINITY_FREEZERS.contains(attacker.getUUID())) return;
        LivingEntity victim = event.getEntity();
        if (!TimeFreezeManager.owners(victim.level().dimension()).contains(attacker.getUUID())
                || !TimeFreezeState.isFrozen(victim)) return;
        STRIKES.computeIfAbsent(attacker.getUUID(), key -> new ConcurrentHashMap<>())
                .merge(victim.getUUID(), event.getAmount(), Float::sum);
        event.setCanceled(true);
        if (victim.level() instanceof ServerLevel level) {
            GauntletFx.burst(level, victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D), "time", 6, 0.3D);
        }
    }

    public static void resolveStrikes(MinecraftServer server, UUID owner) {
        INFINITY_FREEZERS.remove(owner);
        Map<UUID, Float> strikes = STRIKES.remove(owner);
        if (strikes == null || strikes.isEmpty()) return;
        ServerPlayer attacker = server.getPlayerList().getPlayer(owner);
        for (Map.Entry<UUID, Float> strike : strikes.entrySet()) {
            for (ServerLevel level : server.getAllLevels()) {
                Entity entity = level.getEntity(strike.getKey());
                if (!(entity instanceof LivingEntity living) || !living.isAlive()) continue;
                living.invulnerableTime = 0;
                living.hurt(attacker != null ? living.damageSources().playerAttack(attacker) : living.damageSources().magic(),
                        strike.getValue() * 1.5F);
                level.sendParticles(ParticleTypes.CRIT, living.getX(), living.getY() + living.getBbHeight() * 0.5D, living.getZ(),
                        30, 0.4D, 0.4D, 0.4D, 0.4D);
                GauntletFx.sound(level, living.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.5F, 0.6F);
                break;
            }
        }
    }

    public static void dilate(GauntletCast cast, double delta) {
        ServerPlayer player = cast.player();
        LivingEntity target = GauntletAim.living(player, 32.0D);
        if (!(target instanceof TimeDilationEntityAccess access)) {
            cast.message("time_no_target");
            return;
        }
        double currentRate = access.anthology$getInheritedTimeDilationRate();
        double adjustedRate = Math.clamp(currentRate + delta * 0.05D, 0.05D, 3.0D);
        TimeDilationApi.setInheritedRate(target, adjustedRate);
        TimeDilationManager.trackPotentiallyAffected(target);
        TimeDilationApi.syncEntityRate(target, TimeDilationApi.getRate(target));
        TimeRateTuningManager.hold(player, target);
        GauntletFx.sound(cast.level(), player.position(), SoundEvents.NOTE_BLOCK_HAT.value(), 0.5F, (float) (0.5D + adjustedRate * 0.5D));
        cast.message("time_rate", target.getDisplayName(), Math.round(adjustedRate * 100.0D));
    }

    public static void age(GauntletCast cast) {
        if (!cast.crouching()) {
            cast.message("time_target_controls");
            return;
        }
        ServerPlayer player = cast.player();
        ServerLevel level = cast.level();
        LivingEntity target = GauntletAim.living(player, 32.0D);
        if (target != null) {
            Vec3 at = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
            if (target instanceof AgeableMob ageable && ageable.isBaby()) {
                ageable.setAge(0);
                cast.message("time_aged_up", target.getDisplayName());
            } else {
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, cast.infinity() ? 4 : 1), player);
                target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 2), player);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2), player);
                if (cast.infinity()) target.hurt(player.damageSources().indirectMagic(player, player), target.getMaxHealth() * 0.5F);
                level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, 80, target.getBbWidth(), target.getBbHeight() * 0.4D,
                        target.getBbWidth(), 0.02D);
                cast.message("time_aged_down", target.getDisplayName());
            }
            GauntletFx.burst(level, at, "time", 30, target.getBbWidth() * 0.6D);
            GauntletFx.sound(level, at, SoundEvents.BELL_RESONATE, 1.0F, 0.5F);
            cast.cooldown();
            return;
        }
        BlockHitResult hit = GauntletAim.block(player, 32.0D, ClipContext.Block.OUTLINE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            cast.message("time_no_target");
            return;
        }
        int radius = cast.infinity() ? 4 : 1;
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(hit.getBlockPos().offset(-radius, -1, -radius), hit.getBlockPos().offset(radius, 1, radius))) {
            if (grow(player, level, pos.immutable())) grown++;
        }
        GauntletFx.burst(level, hit.getLocation(), "time", 30, radius + 0.5D);
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, 30,
                radius, 0.5D, radius, 0.0D);
        GauntletFx.sound(level, hit.getLocation(), SoundEvents.BONE_MEAL_USE, 1.0F, 0.6F);
        cast.message("time_grown", grown);
        cast.cooldown();
    }

    private static boolean grow(ServerPlayer player, ServerLevel level, BlockPos pos) {
        BlockState start = level.getBlockState(pos);
        if (start.isAir() || !GauntletGrief.canEdit(player, level, pos)) return false;
        boolean changed = false;
        for (int attempt = 0; attempt < 48; attempt++) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof BonemealableBlock bonemealable && bonemealable.isValidBonemealTarget(level, pos, state)) {
                bonemealable.performBonemeal(level, level.random, pos, state);
            } else if (state.isRandomlyTicking()) {
                state.randomTick(level, pos, level.random);
            } else {
                break;
            }
            if (level.getBlockState(pos) != state) changed = true;
        }
        return changed;
    }

    public static double fieldRate(UUID player) {
        return FIELD_RATES[FIELD_RATE_INDEX.getOrDefault(player, DEFAULT_RATE)];
    }

    public static void fieldRate(GauntletCast cast, double delta) {
        int index = Math.clamp(FIELD_RATE_INDEX.getOrDefault(cast.player().getUUID(), DEFAULT_RATE) + (int) delta, 0,
                FIELD_RATES.length - 1);
        FIELD_RATE_INDEX.put(cast.player().getUUID(), index);
        cast.message("time_field_rate", Math.round(FIELD_RATES[index] * 100.0D));
    }

    public static void timeField(GauntletCast cast) {
        ServerPlayer player = cast.player();
        if (cast.crouching()) {
            if (!GauntletConfig.worldWide()) {
                cast.message("world_wide_disabled");
                return;
            }
            DimensionTimeOverrideManager.create(cast.level(), player.getUUID());
            GauntletFx.cast(player, "time", SoundEvents.BEACON_DEACTIVATE, 0.4F);
            cast.message("time_dimension_field");
            cast.cooldown(1200);
            return;
        }
        BlockHitResult hit = GauntletAim.block(player, 32.0D);
        Vec3 center = hit.getType() == HitResult.Type.BLOCK ? hit.getLocation()
                : player.position().add(player.getLookAngle().normalize().scale(16.0D));
        double radius = cast.infinity() ? 32.0D : 20.0D;
        double rate = fieldRate(player.getUUID());
        TimeDilationApi.createField(cast.level(), center, radius, rate, 400, player.getUUID(), true);
        TimeFieldManager.create(cast.level(), center, radius, rate, 400, player.getUUID());
        if (rate > 1.0D) TimeSurgeManager.activateArea(cast.level(), center, radius, 400, rate);
        GauntletFx.cast(player, "time", rate > 1.0D ? SoundEvents.BEACON_POWER_SELECT : SoundEvents.BEACON_DEACTIVATE, 0.6F);
        GauntletFx.ring(cast.level(), center, Math.min(radius, 24.0D), "time");
        cast.message("time_field", Math.round(radius), Math.round(rate * 100.0D));
        cast.cooldown();
    }

    public static void surge(GauntletCast cast) {
        ServerPlayer player = cast.player();
        if (GauntletConfig.worldWide()) {
            TimeSurgeManager.activate(cast.level());
        } else {
            TimeSurgeManager.activateArea(cast.level(), player.position(), GauntletConfig.TIME_SURGE_CHUNK_RADIUS.get() * 16.0D, 400,
                    3.0D);
        }
        GauntletFx.cast(player, "time", SoundEvents.BELL_BLOCK, 0.5F);
        GauntletFx.sphere(cast.level(), player.position().add(0.0D, 1.0D, 0.0D), 4.0D, "time", 80);
        GauntletFx.shake(cast.level(), player.position(), 0.6F, 20, 32.0F);
        cast.message("time_surge");
        cast.cooldown();
    }

    public static void rewind(GauntletCast cast) {
        TimeRewindManager.rewind(cast);
    }

    public static void clear() {
        FIELD_RATE_INDEX.clear();
        INFINITY_FREEZERS.clear();
        STRIKES.clear();
    }
}
