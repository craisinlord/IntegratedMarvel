package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteAnimations;
import com.stxllarplanes.marvellegacy.init.EffectRegistry;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class PantherPower {
    public static final String ID = "heart_shaped_herb_tchalla";
    private static final String ANCESTRAL_PLANE = "ancestral_plane";
    private static final Map<Holder<MobEffect>, Integer> PERMANENT_EFFECTS = Map.of(
            MobEffects.NIGHT_VISION, 0, MobEffects.MOVEMENT_SPEED, 1, MobEffects.JUMP, 1);
    private static final Map<Holder<MobEffect>, HeroPower.Toggle> EFFECT_TOGGLES = Map.of(
            MobEffects.NIGHT_VISION, HeroToggles.SENSES, MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED, MobEffects.JUMP, HeroToggles.JUMP);

    private PantherPower() {
    }

    private static final Map<java.util.UUID, long[]> FIGHTS = new java.util.HashMap<>();

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Heart-Shaped Herb (T'Challa)",
                syringe,
                MarvelBalanceConfig.PANTHER,
                List.of("agility"),
                List.of(
                        new HeroPower.Ability("panther_pounce", "Panther Pounce", PantherPower::pounce),
                        new HeroPower.Ability("claw_swipe", "Claw Swipe", PantherPower::clawSwipe),
                        new HeroPower.Ability(ANCESTRAL_PLANE, "Ancestral Plane", PantherPower::ancestralPlaneInfo),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.SPEED, HeroToggles.JUMP, HeroToggles.SENSES),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 20 == 0) {
                            HeroToggles.applyEffects(player, PERMANENT_EFFECTS, EFFECT_TOGGLES);
                            if (!HeroToggles.isOn(player, HeroToggles.SENSES)) {
                                return;
                            }
                            double radius = MarvelBalanceConfig.SENSES_RADIUS.get();
                            for (LivingEntity hostile : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                                    entity -> entity instanceof Enemy && entity.isAlive())) {
                                hostile.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, true, false));
                            }
                            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.craisinlord.integrated_superheroes.api.HeroSensesEvent(player, radius));
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroAbilities.clearDash(player);
                        PERMANENT_EFFECTS.keySet().forEach(player::removeEffect);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().getDirectEntity() != null
                                && event.getSource().getDirectEntity().getType().is(EntityTypeTags.ARROWS)
                                && player.getRandom().nextDouble() < MarvelBalanceConfig.ARROW_DEFLECT_CHANCE.get()) {
                            event.setCanceled(true);
                            return;
                        }
                        long now = player.level().getGameTime();
                        long[] fight = FIGHTS.computeIfAbsent(player.getUUID(), ignored -> new long[2]);
                        fight[0] = now - fight[1] > 200L ? 1L : fight[0] + 1L;
                        fight[1] = now;
                        if (fight[0] >= 50L) {
                            HeroAdvancements.award(player, "avengers/black_panther/vibranium_kinetics");
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        Power power = HeroAbilities.slotPower(player, ANCESTRAL_PLANE);
                        if (power != null && power.getCooldown() > 0.0D) {
                            return;
                        }
                        event.setCanceled(true);
                        player.setHealth(player.getMaxHealth() * 0.5F);
                        player.removeAllEffects();
                        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
                        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 3));
                        if (player.level() instanceof ServerLevel level) {
                            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, player.getX(), player.getY() + 1.0D, player.getZ(), 60, 0.6D, 1.0D, 0.6D, 0.05D);
                        }
                        player.level().playSound(null, player.blockPosition(), SoundEvents.TOTEM_USE, SoundSource.PLAYERS, 1.0F, 0.6F);
                        player.displayClientMessage(Component.literal("§5The Ancestral Plane sends you back."), true);
                        HeroAdvancements.award(player, "avengers/black_panther/ancestral_plane");
                        HeroAbilities.setCooldown(player, ANCESTRAL_PLANE, MarvelBalanceConfig.ANCESTRAL_PLANE_COOLDOWN.get());
                    }
                }
        );
    }

    private static double pounce(ServerPlayer player, int slot) {
        Vec3 look = player.getLookAngle();
        Vec3 velocity = new Vec3(look.x * 1.4D, Math.max(0.6D, look.y + 0.4D), look.z * 1.4D);
        double damage = MarvelBalanceConfig.POUNCE_DAMAGE.get();
        HeroAbilities.startDash(player, velocity, 20, damage, 1.0D);
        player.fallDistance = 0.0F;
        HeroAnimations.play(player, "wolverine_pounce");
        player.level().playSound(null, player.blockPosition(), SoundEvents.CAT_HISS, SoundSource.PLAYERS, 1.0F, 0.7F);
        return MarvelBalanceConfig.POUNCE_COOLDOWN.get();
    }

    private static double clawSwipe(ServerPlayer player, int slot) {
        double damage = MarvelBalanceConfig.CLAW_SWIPE_DAMAGE.get();
        for (LivingEntity target : HeroAbilities.cone(player, 4.0D, 60.0D)) {
            target.hurt(player.damageSources().mobAttack(player), (float) damage);
            target.knockback(0.6D, player.getX() - target.getX(), player.getZ() - target.getZ());
            target.addEffect(new MobEffectInstance(EffectRegistry.BLEED_EFFECT, MarvelBalanceConfig.CLAW_BLEED_TICKS.get(), 1));
        }
        if (player.level() instanceof ServerLevel level) {
            Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(2.0D));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y - 0.4D, front.z, 3, 0.6D, 0.2D, 0.6D, 0.0D);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.2F);
        ViltrumiteAnimations.play(player, ViltrumiteAnimations.CHOP);
        return MarvelBalanceConfig.CLAW_SWIPE_COOLDOWN.get();
    }

    private static double ancestralPlaneInfo(ServerPlayer player, int slot) {
        player.displayClientMessage(Component.literal("§5Ancestral Plane triggers on its own when you would die."), true);
        return 0.0D;
    }
}
