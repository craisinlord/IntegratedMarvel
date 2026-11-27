package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.api.HeroSensesEvent;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteAnimations;
import com.craisinlord.integrated_superheroes.hero.skin.SerumCostumes;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.EffectCures;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.tintankgames.marvel.world.level.MarvelGameRules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class HomelanderPower {
    public static final String ID = "compound_v_homelander";
    public static final HeroPower.Toggle SUPER_HEARING = new HeroPower.Toggle("integrated_superheroes_on_super_hearing", "Super Hearing",
            () -> Items.SCULK_SENSOR, false);
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "homelander_flight");
    private static final int LASER_COLOR = 0xE01818;
    private static final int THEMATIC_LASER = 0xFF0000;
    private static final int INTIMIDATE_WAVE = 0x80E01818;
    private static final Map<UUID, Map<Integer, Long>> FLEEING = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> MILK = ServerState.map(new HashMap<>());
    private static boolean registered;

    private HomelanderPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            NeoForge.EVENT_BUS.addListener(HomelanderPower::onEffectRemoved);
            NeoForge.EVENT_BUS.addListener(HomelanderPower::onFinishUsing);
            HeroCaster.onTick(HomelanderPower::tickFleeing);
            HeroCaster.onCleanup(caster -> FLEEING.remove(caster.getUUID()));
            SerumCostumes.register(ID, "thematic:homelander");
        }
        return new HeroPower(
                ID,
                "Compound V (Homelander)",
                syringe,
                NewSerumConfig.HOMELANDER,
                List.of(),
                List.of(
                        HeroPower.Ability.of("laser_sweep", "Laser Sweep", HomelanderPower::laserSweep),
                        HeroPower.Ability.of("intimidate", "Intimidate", HomelanderPower::intimidate),
                        HeroPower.Ability.of("grab_and_throw", "Grab and Throw", HomelanderPower::grabAndThrow),
                        HeroToggles.WHEEL
                ),
                HeroToggles.flying(SUPER_HEARING),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        HeroFlight.tick(player, FLIGHT_ID, HeroToggles.isOn(player, HeroToggles.FLIGHT),
                                NewSerumConfig.HOMELANDER_FLY_SPEED.get().floatValue());
                        HeroGrip.tick(player);
                        if (player.tickCount % 5 == 0) {
                            tickFleeing(player);
                        }
                        if (player.tickCount % 20 == 0 && HeroToggles.isOn(player, SUPER_HEARING)) {
                            hear(player);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        HeroBeams.stopAll(player);
                        HeroAbilities.clearDash(player);
                        HeroGrip.clear(player);
                        FLEEING.remove(player.getUUID());
                        MILK.remove(player.getUUID());
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL) || event.getSource().is(DamageTypes.FLY_INTO_WALL)) {
                            event.setCanceled(true);
                            return;
                        }
                        if (!event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY) && !event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                            event.setAmount(event.getAmount() * (float) (1.0D - NewSerumConfig.HOMELANDER_DAMAGE_REDUCTION.get() / 100.0D));
                        }
                    }
                }
        );
    }

    public static boolean isHomelander(Player player) {
        return SerumKit.is(player, ID);
    }

    private static double laserSweep(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        boolean griefing = HeroCaster.griefs(player) && level.getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING);
        double terrain = griefing ? NewSerumConfig.LASER_SWEEP_TERRAIN_RADIUS.get() : 0.0D;
        BeamSettings settings = HeroBeams.settings(NewSerumConfig.LASER_SWEEP_RANGE.get(), NewSerumConfig.LASER_SWEEP_DURATION_TICKS.get(),
                0.8F, 1.6F, NewSerumConfig.LASER_SWEEP_DAMAGE.get(), 0.3F, 2, terrain, 0.0D,
                NewSerumConfig.LASER_SWEEP_MAX_RESISTANCE.get());
        float arc = NewSerumConfig.LASER_SWEEP_ARC.get().floatValue();
        BeamTerrainMode cut = terrain > 0.0D ? BeamTerrainMode.DESTROY : BeamTerrainMode.NONE;
        HeroBeams.Eye look = HeroBeams.thematicEye(0xFFFFFF, THEMATIC_LASER, 1.0F, "thematic:heat_vision", 20, "powergirl_heat_lock");
        HeroAdvancements.cast(player).hits(5, "justice_league/homelander/clean_sweep").run(() -> {
            HeroBeams.fire(player, 0, new HeroBeams.Spec(HeroBeams.QUEEN_RED, settings, cut, -3.0F, 0.6D, 0.0D, 2, 0.0F, arc), look);
            HeroBeams.fire(player, 1, new HeroBeams.Spec(HeroBeams.QUEEN_RED, settings, BeamTerrainMode.NONE, 3.0F, 0.6D, 0.0D, 2, 0.0F, arc), look);
        });
        if (look != null) {
            HeroFx.cue(player, "thematic:laser_open", 0.8F, 1.0F);
        } else {
            Vec3 eye = player.getEyePosition();
            level.sendParticles(HeroAbilities.emissive(LASER_COLOR, 1.0F), eye.x, eye.y, eye.z, 8, 0.2D, 0.1D, 0.2D, 0.0D);
            HeroFx.play(player, SoundEvents.BEACON_ACTIVATE, 0.8F, 1.8F);
            HeroFx.play(player, SoundEvents.FIRECHARGE_USE, 0.6F, 1.2F);
        }
        return NewSerumConfig.LASER_SWEEP_COOLDOWN.get();
    }

    private static void hear(ServerPlayer player) {
        double radius = NewSerumConfig.SUPER_HEARING_RADIUS.get();
        if (radius <= 0.0D) {
            return;
        }
        NeoForge.EVENT_BUS.post(new HeroSensesEvent(player, radius));
    }

    private static double intimidate(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        int ticks = NewSerumConfig.INTIMIDATE_TICKS.get();
        long until = level.getGameTime() + ticks;
        Map<Integer, Long> fleeing = FLEEING.computeIfAbsent(player.getUUID(), ignored -> new HashMap<>());
        for (LivingEntity target : HeroAbilities.sphere(player, NewSerumConfig.INTIMIDATE_RADIUS.get())) {
            if (!HeroAbilities.canControl(target)) {
                continue;
            }
            if (target instanceof Player) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, NewSerumConfig.INTIMIDATE_WEAKNESS_LEVEL.get() - 1));
            } else if (target instanceof IronGolem golem && !golem.isPlayerCreated()) {
                golem.setPersistentAngerTarget(player.getUUID());
                golem.startPersistentAngerTimer();
                golem.setTarget(player);
            } else if (target instanceof Mob mob && !(mob instanceof OwnableEntity ownable && player.getUUID().equals(ownable.getOwnerUUID()))) {
                fleeing.put(mob.getId(), until);
                flee(mob, player);
            }
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, target.getX(), target.getEyeY() + 0.4D, target.getZ(), 1, 0.2D, 0.1D, 0.2D, 0.0D);
        }
        if (fleeing.isEmpty()) {
            FLEEING.remove(player.getUUID());
        }
        Vec3 feet = player.position().add(0.0D, 0.2D, 0.0D);
        if (HeroFx.vfx(level, player.position().add(0.0D, 1.0D, 0.0D), "air_burst_ring", 0)) {
            HeroFx.vfx(level, feet, "powergirl_slam_wave", INTIMIDATE_WAVE);
            HeroFx.cue(player, "thematic:deafening_clap", 0.6F, 1.0F);
            HeroFx.cue(player, "thematic:sonic_panic", 0.8F, 0.5F);
        } else {
            SerumKit.ring(level, feet, HeroAbilities.emissive(LASER_COLOR, 1.0F), 1.5D, 24);
            SerumKit.ring(level, feet, HeroAbilities.emissive(LASER_COLOR, 0.8F), 3.0D, 36);
            HeroFx.play(player, SoundEvents.WARDEN_ANGRY, 0.8F, 1.3F);
        }
        ViltrumiteAnimations.play(player, ViltrumiteAnimations.THUNDERCLAP);
        return NewSerumConfig.INTIMIDATE_COOLDOWN.get();
    }

    private static void flee(Mob mob, LivingEntity player) {
        mob.setTarget(null);
        mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        if (mob instanceof PathfinderMob pathfinder) {
            Vec3 away = DefaultRandomPos.getPosAway(pathfinder, 16, 7, player.position());
            if (away != null) {
                pathfinder.getNavigation().moveTo(away.x, away.y, away.z, 1.4D);
            }
        }
    }

    private static void tickFleeing(LivingEntity player) {
        Map<Integer, Long> fleeing = FLEEING.get(player.getUUID());
        if (fleeing == null) {
            return;
        }
        ServerLevel level = HeroCaster.level(player);
        long now = level.getGameTime();
        Iterator<Map.Entry<Integer, Long>> iterator = fleeing.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, Long> entry = iterator.next();
            Entity entity = level.getEntity(entry.getKey());
            if (!(entity instanceof Mob mob) || !mob.isAlive() || now > entry.getValue()) {
                iterator.remove();
                continue;
            }
            mob.setTarget(null);
            mob.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            if (mob.getNavigation().isDone()) {
                flee(mob, player);
            }
        }
        if (fleeing.isEmpty()) {
            FLEEING.remove(player.getUUID());
        }
    }

    private static double grabAndThrow(LivingEntity player, int slot) {
        ServerLevel level = HeroCaster.level(player);
        if (HeroGrip.isHolding(player)) {
            Vec3 look = player.getLookAngle();
            Vec3 chest = player.position().add(0.0D, 1.0D, 0.0D);
            HeroGrip.release(player, NewSerumConfig.GRAB_THROW_SPEED.get(), NewSerumConfig.GRAB_THROW_DAMAGE.get().floatValue());
            if (HeroFx.vfx(level, chest.add(look), "punch_pressure_wave", 0)) {
                HeroFx.cue(player, "thematic:sonic_punch", 1.0F, 0.8F);
            } else {
                SerumKit.ring(level, chest, ParticleTypes.CLOUD, 1.2D, 20);
            }
            HeroFx.play(player, SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
            HeroAnimations.play(player, "celestisynth:cs_crescentia_throw");
            return NewSerumConfig.GRAB_COOLDOWN.get();
        }
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), NewSerumConfig.GRAB_RANGE.get(), 1);
        if (beam.targets().isEmpty() || !HeroGrip.grab(player, beam.targets().get(0), NewSerumConfig.GRAB_HOLD_TICKS.get(), null,
                held -> held.fallDistance = 0.0F)) {
            HeroCaster.message(player, Component.literal("§7Get closer and look at something to grab it."));
            return 0.0D;
        }
        HeroFx.play(player, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.9F);
        if (!ViltrumiteAnimations.play(player, ViltrumiteAnimations.PUNCH)) {
            HeroAnimations.play(player, "uppercut");
        }
        return 0.5D;
    }

    private static void onEffectRemoved(MobEffectEvent.Remove event) {
        if (event.getCure() == EffectCures.MILK && event.getEntity() instanceof ServerPlayer player && isHomelander(player)) {
            event.setCanceled(true);
        }
    }

    private static void onFinishUsing(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !isHomelander(player)
                || !event.getItem().is(Items.MILK_BUCKET) && !event.getItem().is(Tags.Items.DRINKS_MILK)) {
            return;
        }
        long now = player.serverLevel().getGameTime();
        Long ready = MILK.get(player.getUUID());
        if (ready != null && now < ready) {
            return;
        }
        MILK.put(player.getUUID(), now + Math.round(NewSerumConfig.MILK_COOLDOWN.get() * 20.0D));
        player.heal(NewSerumConfig.MILK_HEAL.get().floatValue());
        int strength = NewSerumConfig.MILK_STRENGTH_TICKS.get();
        if (strength > 0) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, strength, NewSerumConfig.MILK_STRENGTH_LEVEL.get() - 1));
        }
        player.serverLevel().sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.6D, player.getZ(), 4, 0.4D, 0.3D, 0.4D, 0.0D);
        HeroFx.play(player, SoundEvents.PLAYER_BURP, 0.8F, 0.9F);
        HeroAdvancements.award(player, "justice_league/homelander/got_milk");
    }
}
