package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.tintankgames.marvel.sounds.MarvelSoundEvents;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class BinaryPower {
    public static final String ID = "binary_carol_danvers";
    public static final String FLYING_STATE = "integrated_superheroes_flying";
    private static final Map<Holder<MobEffect>, Integer> PERMANENT_EFFECTS = Map.of(
            MobEffects.DAMAGE_RESISTANCE, 0, MobEffects.FIRE_RESISTANCE, 0);
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "binary_flight");
    private static final float DEFAULT_FLY_SPEED = 0.05F;
    private static final int PHOTON_COLOR = 16760130;
    private static final Map<UUID, Integer> BINARY_MODE = ServerState.map(new HashMap<>());
    private static boolean registered;

    public static void clearStale(ServerPlayer player) {
        if (!BINARY_MODE.containsKey(player.getUUID())) {
            HeroAbilities.clearActive(player, "binary_mode");
        }
    }

    private BinaryPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        HeroFlight.register(ID, FLIGHT_ID);
        if (!registered) {
            registered = true;
            HeroCaster.onTick(BinaryPower::tickBinaryMode);
            HeroCaster.onCleanup(caster -> BINARY_MODE.remove(caster.getUUID()));
        }
        return new HeroPower(
                ID,
                "Binary (Carol Danvers)",
                syringe,
                MarvelBalanceConfig.BINARY,
                List.of("agility"),
                List.of(
                        HeroPower.Ability.of("photon_blast", "Photon Blast", BinaryPower::photonBlast),
                        HeroPower.Ability.of("binary_mode", "Binary Mode", BinaryPower::binaryMode),
                        HeroPower.Ability.of("cosmic_dash", "Cosmic Dash", BinaryPower::cosmicDash),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.FLIGHT, HeroToggles.SUPERSONIC),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
                        if (HeroToggles.isOn(player, HeroToggles.FLIGHT) && (!viltrumiteFlight() || ViltrumiteBridge.inFlight(player))) {
                            grantFlight(player);
                            float flySpeed = MarvelBalanceConfig.BINARY_FLY_SPEED.get().floatValue();
                            if (player.getAbilities().getFlyingSpeed() != flySpeed && !(ModCompat.THEMATIC && ThematicFixes.ownsAbilities(player))) {
                                player.getAbilities().setFlyingSpeed(flySpeed);
                                player.onUpdateAbilities();
                            }
                        } else if (flight != null && flight.hasModifier(FLIGHT_ID)) {
                            removeFlight(player);
                        }
                        if (player.tickCount % 20 == 0) {
                            PERMANENT_EFFECTS.forEach((effect, amplifier) -> {
                                MobEffectInstance current = player.getEffect(effect);
                                if (current == null || current.getAmplifier() < amplifier) {
                                    player.addEffect(new MobEffectInstance(effect, MobEffectInstance.INFINITE_DURATION, amplifier, true, false, true));
                                }
                            });
                        }
                        tickBinaryMode(player);
                        boolean flying = player.getAbilities().flying;
                        PlayerData data = HeroPowers.data(player);
                        if (flying != data.hasState(FLYING_STATE)) {
                            if (flying) {
                                data.addState(FLYING_STATE);
                            } else {
                                data.removeState(FLYING_STATE);
                            }
                            PlayerData.sync(player);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        removeFlight(player);
                        BINARY_MODE.remove(player.getUUID());
                        PERMANENT_EFFECTS.keySet().forEach(player::removeEffect);
                        HeroAbilities.clearDash(player);
                        HeroBeams.stopAll(player);
                        HeroPowers.data(player).removeState(FLYING_STATE);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FIRE) || event.getSource().is(DamageTypeTags.IS_FALL)
                                || event.getSource().is(DamageTypeTags.IS_EXPLOSION) && event.getSource().getEntity() == player) {
                            event.setCanceled(true);
                        }
                    }
                }
        );
    }

    private static boolean viltrumiteFlight() {
        return ModCompat.VILTRUMITE && MarvelBalanceConfig.VILTRUMITE_FLIGHT_HEROES.get().contains(ID);
    }

    public static void grantFlight(ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null && !flight.hasModifier(FLIGHT_ID)) {
            flight.addTransientModifier(new AttributeModifier(FLIGHT_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static void removeFlight(ServerPlayer player) {
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (flight != null) {
            flight.removeModifier(FLIGHT_ID);
        }
        player.getAbilities().setFlyingSpeed(DEFAULT_FLY_SPEED);
        if (!player.isCreative() && !player.isSpectator()) {
            player.getAbilities().flying = false;
        }
        player.onUpdateAbilities();
    }

    private static double photonBlast(LivingEntity player, int slot) {
        boolean griefs = HeroCaster.griefs(player);
        boolean binary = BINARY_MODE.containsKey(player.getUUID());
        double multiplier = binary ? 2.0D : 1.0D;
        double range = MarvelBalanceConfig.PHOTON_BLAST_RANGE.get();
        BeamSettings settings = HeroBeams.settings(range, MarvelBalanceConfig.PHOTON_BLAST_DURATION_TICKS.get(),
                binary ? 1.6F : 1.1F, binary ? 3.5F : 2.5F, MarvelBalanceConfig.PHOTON_BLAST_DAMAGE.get() * multiplier, 1.5F, 1,
                griefs ? MarvelBalanceConfig.PHOTON_BLAST_TERRAIN_RADIUS.get() * (binary ? 1.5D : 1.0D) : 0.0D, 0.1D, 1200.0D);
        HeroBeams.fire(player, 0, new HeroBeams.Spec(HeroBeams.KING_FIRE, settings, griefs ? BeamTerrainMode.DESTROY : BeamTerrainMode.NONE, 0.0F, 0.8D, 0.45D,
                MarvelBalanceConfig.PHOTON_FIRE_SECONDS.get(), 0.0F));
        player.level().playSound(null, player.blockPosition(), MarvelSoundEvents.CAPTAIN_MARVEL_PHOTON_BLAST.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        return MarvelBalanceConfig.PHOTON_BLAST_COOLDOWN.get();
    }

    private static double binaryMode(LivingEntity player, int slot) {
        int duration = MarvelBalanceConfig.BINARY_MODE_DURATION_TICKS.get();
        BINARY_MODE.put(player.getUUID(), duration);
        if (player instanceof ServerPlayer user) {
            HeroAdvancements.window(user, duration, 5, "avengers/binary/binary_star");
        }
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 2, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, duration, 0, false, false, false));
        player.level().playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 1.5F, 0.6F);
        if (player instanceof ServerPlayer user) {
            HeroAbilities.setActive(user, "binary_mode", true);
        }
        return MarvelBalanceConfig.BINARY_MODE_COOLDOWN.get();
    }

    private static double cosmicDash(LivingEntity player, int slot) {
        Vec3 velocity = player.getLookAngle().scale(MarvelBalanceConfig.COSMIC_DASH_SPEED.get());
        HeroAdvancements.cast(player).kills(1, "avengers/binary/photon_finish")
                .run(() -> HeroAbilities.startDash(player, velocity, 12, MarvelBalanceConfig.COSMIC_DASH_DAMAGE.get(), 2.0D));
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.FLASH, player.getX(), player.getY() + 1.0D, player.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        player.level().playSound(null, player.blockPosition(), SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, SoundSource.PLAYERS, 1.2F, 0.8F);
        return MarvelBalanceConfig.COSMIC_DASH_COOLDOWN.get();
    }

    private static void tickBinaryMode(LivingEntity player) {
        Integer ticksLeft = BINARY_MODE.get(player.getUUID());
        if (ticksLeft == null) {
            return;
        }
        ServerLevel level = (ServerLevel) player.level();
        level.sendParticles(HeroAbilities.emissive(SerumKit.primary(player, PHOTON_COLOR), 1.0F), player.getX(), player.getY() + 1.0D, player.getZ(), 6, 0.5D, 0.9D, 0.5D, 0.02D);
        if (ticksLeft % 10 == 0) {
            float damage = MarvelBalanceConfig.BINARY_MODE_AURA_DAMAGE.get().floatValue();
            for (LivingEntity target : HeroAbilities.sphere(player, MarvelBalanceConfig.BINARY_MODE_AURA_RADIUS.get())) {
                if (target instanceof Enemy || !(player instanceof ServerPlayer)) {
                    HeroHits.hurt(player, target, player.damageSources().indirectMagic(player, player), damage);
                }
            }
        }
        if (ticksLeft <= 1) {
            BINARY_MODE.remove(player.getUUID());
            if (player instanceof ServerPlayer user) {
                HeroAbilities.setActive(user, "binary_mode", false);
            }
        } else {
            BINARY_MODE.put(player.getUUID(), ticksLeft - 1);
        }
    }
}
