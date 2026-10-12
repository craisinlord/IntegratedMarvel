package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.antarchy.content.time.TimeDilationApi;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.core.particles.MarvelParticleTypes;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;

import java.util.List;
import java.util.function.Supplier;

public final class QuicksilverPower {
    public static final String ID = "super_speed_pietro_maximoff";
    public static final String SPEED_FORCE = "speed_force";
    private static final String RATE_SOURCE = "integrated_superheroes:quicksilver";
    private static final String RATE_KEY = "integrated_superheroes.speed_force_rate";

    private QuicksilverPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Super Speed (Pietro Maximoff)",
                syringe,
                MarvelBalanceConfig.QUICKSILVER,
                List.of(),
                List.of(
                        new HeroPower.Ability(SPEED_FORCE, "Speed Force", QuicksilverPower::toggleSpeedForce),
                        new HeroPower.Ability("bullet_time", "Bullet Time", QuicksilverPower::bulletTime),
                        new HeroPower.Ability("sonic_boom", "Sonic Boom", QuicksilverPower::sonicBoom)
                ),
                List.of(),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        PlayerData data = HeroPowers.data(player);
                        if (data.hasPassivePower("agility")) {
                            data.removePassivePower("agility");
                            PlayerData.sync(player);
                        }
                        if (!isSpeedForceActive(player)) {
                            return;
                        }
                        MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
                        if (speed == null || speed.getAmplifier() < 1) {
                            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, MobEffectInstance.INFINITE_DURATION, 1, true, false, true));
                        }
                        double rate = storedRate(player);
                        if (TimeDilationApi.getPersonalRate(player, RATE_SOURCE) != rate
                                || TimeDilationApi.getRateCeiling(player) != MarvelBalanceConfig.SPEED_FORCE_MAX_RATE.get()) {
                            applyRate(player, rate);
                        }
                        if (player.getFoodData().getFoodLevel() <= 0 && !player.getAbilities().instabuild) {
                            setSpeedForce(player, false);
                            player.displayClientMessage(Component.literal("§cToo hungry to keep up the Speed Force."), true);
                            HeroAdvancements.award(player, "avengers/quicksilver/running_on_empty");
                            return;
                        }
                        player.causeFoodExhaustion((float) (MarvelBalanceConfig.SPEED_FORCE_EXHAUSTION_PER_RATE.get() * (rate - 1.0D)));
                        if (player.getDeltaMovement().horizontalDistanceSqr() > 0.01D && player.level() instanceof ServerLevel level) {
                            Vec3 behind = player.position().subtract(player.getViewVector(0.0F));
                            level.sendParticles(MarvelParticleTypes.QUICKSILVER.get(), behind.x, behind.y + 1.0D, behind.z, 20, 0.0D, 0.0D, 0.0D, 0.3D);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        clearRate(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                            event.setAmount(event.getAmount() * 0.25F);
                        }
                    }
                }
        );
    }

    public static void adjustRate(ServerPlayer player, double scrollDelta) {
        if (!ID.equals(HeroPowers.data(player).getPrimaryPower()) || !isSpeedForceActive(player)) {
            return;
        }
        double max = MarvelBalanceConfig.SPEED_FORCE_MAX_RATE.get();
        double rate = Math.max(1.0D, Math.min(max, storedRate(player) + Math.signum(scrollDelta) * MarvelBalanceConfig.SPEED_FORCE_STEP.get()));
        player.getPersistentData().putDouble(RATE_KEY, rate);
        if (rate >= max) {
            HeroAdvancements.award(player, "avengers/quicksilver/ludicrous_speed");
        }
        applyRate(player, rate);
        player.displayClientMessage(Component.literal(String.format("§bSpeed Force: %.1fx", rate)), true);
    }

    public static boolean isSpeedForceActive(ServerPlayer player) {
        Power power = HeroAbilities.slotPower(player, SPEED_FORCE);
        return power != null && power.isActive();
    }

    private static double toggleSpeedForce(ServerPlayer player, int slot) {
        setSpeedForce(player, !isSpeedForceActive(player));
        return 0.5D;
    }

    private static void setSpeedForce(ServerPlayer player, boolean active) {
        HeroAbilities.setActive(player, SPEED_FORCE, active);
        if (active) {
            applyRate(player, storedRate(player));
            player.displayClientMessage(Component.literal(String.format("§bSpeed Force: %.1fx §7(Shift + scroll to change)", storedRate(player))), true);
        } else {
            clearRate(player);
        }
    }

    private static double bulletTime(ServerPlayer player, int slot) {
        TimeDilationApi.createAttachedField((ServerLevel) player.level(), player, MarvelBalanceConfig.BULLET_TIME_RADIUS.get(),
                MarvelBalanceConfig.BULLET_TIME_RATE.get(), MarvelBalanceConfig.BULLET_TIME_DURATION_TICKS.get());
        HeroAdvancements.window(player, MarvelBalanceConfig.BULLET_TIME_DURATION_TICKS.get(), 5, "avengers/quicksilver/slow_mo_montage");
        return MarvelBalanceConfig.BULLET_TIME_COOLDOWN.get();
    }

    private static double sonicBoom(ServerPlayer player, int slot) {
        double minRate = MarvelBalanceConfig.SONIC_BOOM_MIN_RATE.get();
        double rate = isSpeedForceActive(player) ? storedRate(player) : 1.0D;
        if (rate < minRate) {
            player.displayClientMessage(Component.literal(String.format("§cRun at %.0fx or faster to break the sound barrier.", minRate)), true);
            return 0.0D;
        }
        double scale = rate / minRate;
        double radius = MarvelBalanceConfig.SONIC_BOOM_RADIUS.get() * Math.sqrt(scale);
        float damage = (float) (MarvelBalanceConfig.SONIC_BOOM_DAMAGE.get() * scale);
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            target.hurt(player.damageSources().sonicBoom(player), damage);
            Vec3 away = target.position().subtract(player.position()).normalize();
            target.knockback(1.5D * scale, -away.x, -away.z);
        }
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(ParticleTypes.SONIC_BOOM, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.EXPLOSION, player.getX(), player.getY() + 1.0D, player.getZ(), 6, radius / 3.0D, 0.5D, radius / 3.0D, 0.0D);
        level.playSound(null, player.blockPosition(), MarvelSoundEvents.QUICKSILVER_SONIC_BOOM.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
        HeroAdvancements.award(player, "avengers/quicksilver/sound_barrier");
        return MarvelBalanceConfig.SONIC_BOOM_COOLDOWN.get();
    }

    private static double storedRate(ServerPlayer player) {
        double max = MarvelBalanceConfig.SPEED_FORCE_MAX_RATE.get();
        double stored = player.getPersistentData().contains(RATE_KEY)
                ? player.getPersistentData().getDouble(RATE_KEY)
                : MarvelBalanceConfig.SPEED_FORCE_START_RATE.get();
        return Math.max(1.0D, Math.min(max, stored));
    }

    private static void applyRate(ServerPlayer player, double rate) {
        TimeDilationApi.setRateCeiling(player, MarvelBalanceConfig.SPEED_FORCE_MAX_RATE.get());
        TimeDilationApi.setPersonalRate(player, RATE_SOURCE, rate);
    }

    private static void clearRate(ServerPlayer player) {
        TimeDilationApi.clearPersonalRate(player, RATE_SOURCE);
        TimeDilationApi.clearRateCeiling(player);
        MobEffectInstance speed = player.getEffect(MobEffects.MOVEMENT_SPEED);
        if (speed != null && speed.isInfiniteDuration()) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
        }
    }
}
