package com.craisinlord.integrated_superheroes.hero;

import net.tintankgames.marvel.world.level.MarvelGameRules;
import com.craisinlord.anthology.beam.BeamSettings;
import com.craisinlord.anthology.beam.BeamTerrainMode;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteAnimations;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class KryptonianPower {
    public static final String ID = "kryptonian_clark_kent";
    public static final TagKey<Item> KRYPTONITE = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "kryptonite"));
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "kryptonian_flight");
    private static final int FIRE_BEAM = 2;
    private static final int HEAT_COLOR = 0xFF5A1E;
    private static final int FROST_COLOR = 0x9FE6FF;
    private static final int THEMATIC_HEAT = 0xFF0000;
    private static final int THEMATIC_FROST = 0xB1FCF5;
    private static final int FROST_CONE = 0xB3D8F4FF;
    private static final Map<UUID, Float> SOLAR = ServerState.map(new HashMap<>());
    private static final Map<UUID, Float> SUNLIGHT = ServerState.map(new HashMap<>());
    private static final Set<UUID> WEAKENED = ServerState.all(new HashSet<>());
    private static final TagKey<Block> KRYPTONITE_BLOCKS = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "kryptonite"));
    private static final ResourceKey<Biome> TOXIC_CAVES = ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("alexscaves", "toxic_caves"));
    private static final Map<UUID, Float> CHARGE = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> DRAINED = ServerState.map(new HashMap<>());

    private KryptonianPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        return new HeroPower(
                ID,
                "Kryptonian (Clark Kent)",
                syringe,
                NewSerumConfig.KRYPTONIAN,
                List.of(),
                List.of(
                        HeroPower.Ability.of("heat_vision", "Heat Vision", KryptonianPower::heatVision),
                        HeroPower.Ability.of("super_breath", "Super Breath", KryptonianPower::superBreath),
                        HeroPower.Ability.of("flying_punch", "Flying Punch", KryptonianPower::flyingPunch),
                        HeroToggles.WHEEL,
                        HeroPower.Ability.of("solar_flare", "Solar Flare", KryptonianPower::solarFlare)
                ),
                HeroToggles.flying(HeroToggles.SPEED, HeroToggles.SENSES),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        if (player.tickCount % 10 == 0) {
                            updateKryptonite(player);
                        }
                        tickDrained(player);
                        boolean weak = WEAKENED.contains(player.getUUID()) || drained(player);
                        HeroFlight.tick(player, FLIGHT_ID, !weak && HeroToggles.isOn(player, HeroToggles.FLIGHT),
                                NewSerumConfig.KRYPTONIAN_FLY_SPEED.get().floatValue());
                        if (player.tickCount % 20 == 0) {
                            tickSolar(player, weak);
                            Map<Holder<MobEffect>, Integer> effects = Map.of(MobEffects.MOVEMENT_SPEED, NewSerumConfig.KRYPTONIAN_SPEED_LEVEL.get() - 1,
                                    MobEffects.NIGHT_VISION, 0);
                            Map<Holder<MobEffect>, HeroPower.Toggle> toggles = Map.of(MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED,
                                    MobEffects.NIGHT_VISION, HeroToggles.SENSES);
                            if (weak) {
                                SerumKit.dropEffect(player, MobEffects.MOVEMENT_SPEED);
                            } else {
                                HeroToggles.applyEffects(player, effects, toggles);
                            }
                            if (HeroToggles.isOn(player, HeroToggles.SENSES)) {
                                xRay(player);
                            } else {
                                SerumKit.dropEffect(player, MobEffects.NIGHT_VISION);
                            }
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        HeroBeams.stopAll(player);
                        HeroAbilities.clearDash(player);
                        SerumKit.dropEffect(player, MobEffects.MOVEMENT_SPEED);
                        SerumKit.dropEffect(player, MobEffects.NIGHT_VISION);
                        SOLAR.remove(player.getUUID());
                        SUNLIGHT.remove(player.getUUID());
                        WEAKENED.remove(player.getUUID());
                        CHARGE.remove(player.getUUID());
                        DRAINED.remove(player.getUUID());
                        SerumKit.clearMeter(player);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (event.getSource().is(DamageTypeTags.IS_FALL)) {
                            event.setCanceled(true);
                            return;
                        }
                        if (WEAKENED.contains(player.getUUID())) {
                            event.setAmount(event.getAmount() * NewSerumConfig.KRYPTONITE_DAMAGE_TAKEN_MULTIPLIER.get().floatValue());
                        } else if (!drained(player) && event.getSource().is(DamageTypeTags.IS_FIRE) || event.getSource().is(DamageTypeTags.IS_FREEZING)
                                || event.getSource().is(DamageTypeTags.IS_DROWNING)) {
                            event.setCanceled(true);
                        } else if (!drained(player) && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)
                                && !event.getSource().is(DamageTypeTags.BYPASSES_RESISTANCE)) {
                            event.setAmount(event.getAmount() * damageTaken(player));
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        event.setAmount(event.getAmount() * solar(player));
                    }
                }
        );
    }

    public static boolean isKryptonian(Player player) {
        return SerumKit.is(player, ID);
    }

    public static boolean weakened(Player player) {
        return WEAKENED.contains(player.getUUID());
    }

    private static boolean drained(LivingEntity player) {
        return DRAINED.containsKey(player.getUUID());
    }

    private static boolean trait(LivingEntity caster, String trait) {
        return caster instanceof Player player && SerumKit.trait(player, trait);
    }

    private static void tickDrained(ServerPlayer player) {
        Long until = DRAINED.get(player.getUUID());
        if (until != null && player.level().getGameTime() >= until) {
            DRAINED.remove(player.getUUID());
            player.displayClientMessage(Component.literal("§eThe sun is getting back into you."), true);
        }
    }

    private static float solar(LivingEntity player) {
        if (WEAKENED.contains(player.getUUID()) || drained(player)) {
            return NewSerumConfig.SOLAR_MIN_MULTIPLIER.get().floatValue();
        }
        return SOLAR.getOrDefault(player.getUUID(), 1.0F);
    }

    private static float damageTaken(ServerPlayer player) {
        double shade = NewSerumConfig.SHADE_DAMAGE_REDUCTION.get();
        double sun = NewSerumConfig.SUN_DAMAGE_REDUCTION.get();
        double reduction = shade + (sun - shade) * SUNLIGHT.getOrDefault(player.getUUID(), 0.0F);
        return (float) (1.0D - reduction / 100.0D);
    }

    private static void tickSolar(ServerPlayer player, boolean weak) {
        ServerLevel level = player.serverLevel();
        BlockPos eye = BlockPos.containing(player.getEyePosition());
        double sky = level.dimensionType().hasSkyLight() ? level.getBrightness(LightLayer.SKY, eye) / 15.0D : 0.0D;
        double day = level.isDay() ? 1.0D : 0.0D;
        double sun = sky * day;
        double min = NewSerumConfig.SOLAR_MIN_MULTIPLIER.get();
        double max = NewSerumConfig.SOLAR_MAX_MULTIPLIER.get();
        SOLAR.put(player.getUUID(), (float) (min + (max - min) * sun));
        SUNLIGHT.put(player.getUUID(), (float) sun);
        float charge = CHARGE.getOrDefault(player.getUUID(), 0.0F);
        if (!weak && sun > 0.0D) {
            charge = (float) Math.min(1.0D, charge + sun / NewSerumConfig.SOLAR_FLARE_CHARGE_SECONDS.get());
            CHARGE.put(player.getUUID(), charge);
        }
        SerumKit.meter(player, charge);
        if (!weak && sun > 0.6D && player.getHealth() < player.getMaxHealth()) {
            player.heal(NewSerumConfig.SOLAR_REGEN_PER_SECOND.get().floatValue());
            if (player.tickCount % 60 == 0) {
                level.sendParticles(HeroAbilities.emissive(0xFFE27A, 0.8F), player.getX(), player.getY() + 1.0D, player.getZ(),
                        6, 0.4D, 0.8D, 0.4D, 0.0D);
            }
        }
        if (sun > 0.9D && player.getAbilities().flying && player.getY() > level.getMaxBuildHeight()) {
            HeroAdvancements.award(player, "justice_league/kryptonian/above_the_clouds");
        }
    }

    private static void updateKryptonite(ServerPlayer player) {
        boolean immune = SerumKit.trait(player, SerumKit.NO_KRYPTONITE);
        boolean near = !immune && nearKryptonite(player);
        boolean was = WEAKENED.contains(player.getUUID());
        if (near) {
            WEAKENED.add(player.getUUID());
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 1, true, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, true, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, false, false));
            HeroBeams.stopAll(player);
            if (player.tickCount % 20 == 0) {
                float damage = NewSerumConfig.KRYPTONITE_DAMAGE_PER_SECOND.get().floatValue();
                if (damage > 0.0F) {
                    player.hurt(player.damageSources().magic(), damage);
                }
                player.serverLevel().sendParticles(HeroAbilities.emissive(0x52FF3A, 0.9F), player.getX(), player.getY() + 1.0D, player.getZ(),
                        8, 0.4D, 0.8D, 0.4D, 0.0D);
            }
            if (!was) {
                player.displayClientMessage(Component.literal("§aYour strength drains away. Something nearby is making you sick."), true);
                HeroAdvancements.award(player, "justice_league/kryptonian/kryptonite");
            }
        } else if (was) {
            WEAKENED.remove(player.getUUID());
            player.removeEffect(MobEffects.CONFUSION);
        }
    }

    private static boolean nearKryptonite(ServerPlayer player) {
        if (holding(player)) {
            return true;
        }
        BlockPos origin = player.blockPosition();
        if (NewSerumConfig.KRYPTONITE_TOXIC_CAVES.get() && player.level().getBiome(origin).is(TOXIC_CAVES)) {
            return true;
        }
        int blocks = (int) Math.round(NewSerumConfig.KRYPTONITE_BLOCK_RADIUS.get());
        if (blocks > 0) {
            for (BlockPos pos : BlockPos.betweenClosed(origin.offset(-blocks, -blocks, -blocks), origin.offset(blocks, blocks + 1, blocks))) {
                if (player.level().getBlockState(pos).is(KRYPTONITE_BLOCKS)) {
                    return true;
                }
            }
        }
        double radius = NewSerumConfig.KRYPTONITE_RADIUS.get();
        if (radius <= 0.0D) {
            return false;
        }
        return !player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(radius),
                entity -> entity != player && entity.isAlive() && entity.distanceToSqr(player) <= radius * radius && holding(entity)).isEmpty();
    }

    private static boolean holding(LivingEntity entity) {
        return entity.getMainHandItem().is(KRYPTONITE) || entity.getOffhandItem().is(KRYPTONITE);
    }

    private static void xRay(ServerPlayer player) {
        double radius = NewSerumConfig.XRAY_RADIUS.get();
        if (radius <= 0.0D) {
            return;
        }
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new com.craisinlord.integrated_superheroes.api.HeroSensesEvent(player, radius));
    }

    private static boolean blocked(LivingEntity player) {
        if (WEAKENED.contains(player.getUUID())) {
            HeroCaster.message(player, Component.literal("§aYou're too weak to do that right now."));
            return true;
        }
        if (drained(player)) {
            HeroCaster.message(player, Component.literal("§eYou've got nothing left. Wait for the sun to fill you back up."));
            return true;
        }
        return false;
    }

    private static double heatVision(LivingEntity player, int slot) {
        if (blocked(player)) {
            return 0.0D;
        }
        if (trait(player, SerumKit.INVERTED)) {
            return freezeVision(player);
        }
        double damage = NewSerumConfig.HEAT_VISION_DAMAGE.get() * solar(player);
        double range = NewSerumConfig.HEAT_VISION_RANGE.get();
        double terrain = HeroCaster.griefs(player) && HeroCaster.level(player).getGameRules().getBoolean(MarvelGameRules.RULE_SUPERPOWERGRIEFING)
                ? NewSerumConfig.HEAT_VISION_TERRAIN_RADIUS.get() : 0.0D;
        BeamSettings settings = HeroBeams.settings(range, NewSerumConfig.HEAT_VISION_DURATION_TICKS.get(), 0.6F, 1.4F, damage, 0.4F, 2,
                terrain, 0.0D, 50.0D);
        int primary = SerumKit.primary(player, HEAT_COLOR);
        int kind = SerumKit.reddish(primary) ? HeroBeams.QUEEN_RED : FIRE_BEAM;
        HeroBeams.Eye eye = HeroBeams.thematicEye(0xFFFFFF, SerumKit.reddish(primary) ? THEMATIC_HEAT : primary, 1.0F,
                "thematic:heat_vision", 20, "powergirl_heat_lock");
        HeroBeams.fire(player, 0, new HeroBeams.Spec(kind, settings, terrain > 0.0D ? BeamTerrainMode.DESTROY : BeamTerrainMode.NONE,
                -4.0F, 0.6D, 0.0D, 3, 0.0F), eye);
        HeroBeams.fire(player, 1, new HeroBeams.Spec(kind, settings, BeamTerrainMode.NONE, 4.0F, 0.6D, 0.0D, 3, 0.0F), eye);
        if (eye == null) {
            HeroFx.play(player, SoundEvents.BLAZE_SHOOT, 0.8F, 0.6F);
            HeroFx.play(player, SoundEvents.FIRECHARGE_USE, 0.6F, 1.4F);
        }
        return NewSerumConfig.HEAT_VISION_COOLDOWN.get();
    }

    private static double freezeVision(LivingEntity player) {
        ServerLevel level = HeroCaster.level(player);
        double range = NewSerumConfig.HEAT_VISION_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 3);
        Vec3 from = beam.start().add(player.getLookAngle().scale(0.6D)).subtract(0.0D, 0.1D, 0.0D);
        HeroBeams.Eye eye = HeroBeams.thematicEye(0xFFFFFF, THEMATIC_FROST, 1.0F, null, 0, null);
        if (eye != null) {
            HeroBeams.flash(player, 0, beam.end(), eye, 8);
            HeroBeams.flash(player, 1, beam.end(), eye, 8);
            HeroFx.vfx(level, beam.end(), "powergirl_ice_shatter", 0);
        } else {
            HeroAbilities.particleLine(level, from, beam.end(), SerumKit.glow(player, FROST_COLOR, 1.2F), 0.4D);
            HeroAbilities.particleLine(level, from, beam.end(), ParticleTypes.SNOWFLAKE, 0.8D);
        }
        float damage = (float) (NewSerumConfig.HEAT_VISION_DAMAGE.get() * solar(player) * 1.5D);
        for (LivingEntity target : beam.targets()) {
            HeroAbilities.runAsAbilityDamage(() -> HeroHits.hurt(player, target, player.damageSources().freeze(), damage));
            chill(target, NewSerumConfig.SUPER_BREATH_FREEZE_TICKS.get());
        }
        if (eye != null) {
            HeroFx.cue(player, "thematic:ice_vision", 1.0F, 1.0F);
        } else {
            HeroFx.play(player, SoundEvents.PLAYER_HURT_FREEZE, 1.0F, 0.6F);
            HeroFx.play(player, SoundEvents.GLASS_BREAK, 0.6F, 1.6F);
        }
        return NewSerumConfig.HEAT_VISION_COOLDOWN.get();
    }

    private static void chill(LivingEntity target, int ticks) {
        target.clearFire();
        target.setTicksFrozen(Math.max(target.getTicksFrozen(), target.getTicksRequiredToFreeze() + ticks));
        if (!HeroAbilities.canControl(target)) {
            return;
        }
        Optional<Holder.Reference<MobEffect>> frozen = HeroFx.effect("iceandfire:frozen");
        frozen.ifPresent(effect -> target.addEffect(new MobEffectInstance(effect, ticks)));
        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
    }

    private static double superBreath(LivingEntity player, int slot) {
        if (blocked(player)) {
            return 0.0D;
        }
        boolean fire = trait(player, SerumKit.INVERTED);
        ServerLevel level = HeroCaster.level(player);
        double range = NewSerumConfig.SUPER_BREATH_RANGE.get();
        float damage = (float) (NewSerumConfig.SUPER_BREATH_DAMAGE.get() * solar(player));
        Vec3 look = player.getLookAngle();
        for (LivingEntity target : HeroAbilities.cone(player, range, 25.0D)) {
            if (fire) {
                HeroAbilities.runAsAbilityDamage(() -> HeroHits.hurt(player, target, player.damageSources().onFire(), damage));
                target.igniteForSeconds(6.0F);
            } else {
                HeroAbilities.runAsAbilityDamage(() -> HeroHits.hurt(player, target, player.damageSources().freeze(), damage));
                chill(target, NewSerumConfig.SUPER_BREATH_FREEZE_TICKS.get());
            }
            target.setDeltaMovement(target.getDeltaMovement().add(look.x * 1.2D, 0.25D, look.z * 1.2D));
            target.hurtMarked = true;
        }
        Vec3 mouth = player.getEyePosition().subtract(0.0D, 0.2D, 0.0D);
        if (!fire && player instanceof ServerPlayer serverPlayer) {
            frostTerrain(serverPlayer, level, mouth, range);
        }
        if (HeroFx.thematic() && HeroFx.beam(level, mouth.add(look), mouth.add(look.scale(range)), "pyre_cone", fire ? 0 : FROST_CONE)) {
            Vec3 reach = mouth.add(look.scale(range * 0.75D));
            if (!fire) {
                HeroFx.vfx(level, reach, "powergirl_frost_mist", 0);
            }
            HeroFx.cue(player, fire ? "thematic:ignite" : "thematic:frost_breath", 1.0F, 1.0F);
            HeroFx.cue(player, "thematic:super_breath", 0.8F, 1.0F);
            return NewSerumConfig.SUPER_BREATH_COOLDOWN.get();
        }
        for (int i = -2; i <= 2; i++) {
            Vec3 direction = Vec3.directionFromRotation(player.getXRot(), player.getYRot() + i * 8.0F);
            HeroAbilities.particleLine(level, mouth.add(direction), mouth.add(direction.scale(range)),
                    fire ? ParticleTypes.FLAME : ParticleTypes.SNOWFLAKE, 0.7D);
            if (!fire) {
                HeroAbilities.particleLine(level, mouth.add(direction), mouth.add(direction.scale(range)), ParticleTypes.CLOUD, 1.6D);
            }
        }
        if (!fire) {
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.SNOWBALL)),
                    mouth.x + look.x * 3.0D, mouth.y + look.y * 3.0D, mouth.z + look.z * 3.0D, 20, 1.0D, 0.5D, 1.0D, 0.1D);
        }
        HeroFx.play(player, fire ? SoundEvents.BLAZE_SHOOT : SoundEvents.POWDER_SNOW_BREAK, 1.2F, 0.6F);
        HeroFx.play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.8F, 0.7F);
        return NewSerumConfig.SUPER_BREATH_COOLDOWN.get();
    }

    private static void frostTerrain(ServerPlayer player, ServerLevel level, Vec3 mouth, double range) {
        int ticks = NewSerumConfig.SUPER_BREATH_ICE_TICKS.get();
        Set<BlockPos> seen = new HashSet<>();
        for (int yaw = -2; yaw <= 2; yaw++) {
            for (int pitch = -1; pitch <= 1; pitch++) {
                Vec3 direction = Vec3.directionFromRotation(player.getXRot() + pitch * 10.0F, player.getYRot() + yaw * 10.0F);
                for (double d = 1.0D; d < range; d += 0.7D) {
                    BlockPos pos = BlockPos.containing(mouth.add(direction.scale(d)));
                    if (!seen.add(pos)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                        level.removeBlock(pos, false);
                        continue;
                    }
                    if (ticks > 0 && state.is(Blocks.WATER) && state.getFluidState().isSource() && level.getFluidState(pos.above()).isEmpty()
                            && com.craisinlord.integrated_superheroes.gauntlet.GauntletGrief.canEdit(player, level, pos)) {
                        TempBlocks.place(level, player, pos, ticks + level.random.nextInt(60));
                        break;
                    }
                    if (state.blocksMotion() || !state.getFluidState().isEmpty()) {
                        break;
                    }
                }
            }
        }
    }

    private static double solarFlare(LivingEntity player, int slot) {
        if (blocked(player)) {
            return 0.0D;
        }
        ServerPlayer serverPlayer = player instanceof ServerPlayer sp ? sp : null;
        float charge = CHARGE.getOrDefault(player.getUUID(), 0.0F);
        if (serverPlayer != null && charge < 1.0F) {
            HeroCaster.message(player, Component.literal("§eNot enough sun in you yet (" + Math.round(charge * 100.0F) + "%)."));
            return 0.0D;
        }
        ServerLevel level = HeroCaster.level(player);
        double radius = NewSerumConfig.SOLAR_FLARE_RADIUS.get();
        float damage = NewSerumConfig.SOLAR_FLARE_DAMAGE.get().floatValue();
        for (LivingEntity target : HeroAbilities.sphere(player, radius)) {
            HeroAbilities.runAsAbilityDamage(() -> HeroHits.hurt(player, target, player.damageSources().onFire(), damage));
            target.igniteForSeconds(5.0F);
            SerumKit.push(target, player.position(), 2.0D, 0.6D);
        }
        Vec3 center = player.position().add(0.0D, 1.0D, 0.0D);
        if (HeroFx.vfx(level, center, "air_burst_ring", 0xFFFFE27A, (float) radius)) {
            HeroFx.vfx(level, player.position(), "powergirl_slam_wave", 0xFFFFC040, (float) radius);
            HeroFx.cue(player, "thematic:sonic_punch", 1.2F, 0.6F);
        } else {
            for (int i = 1; i <= 4; i++) {
                SerumKit.ring(level, center, HeroAbilities.emissive(0xFFE27A, 1.2F), radius * i / 4.0D, 16 * i);
            }
        }
        level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 80, radius * 0.3D, 1.0D, radius * 0.3D, 0.3D);
        HeroFx.play(player, SoundEvents.GENERIC_EXPLODE.value(), 1.4F, 0.7F);
        HeroFx.play(player, SoundEvents.BEACON_DEACTIVATE, 1.2F, 0.6F);
        HeroFx.shake(level, player.position(), radius * 2.0D, 0.7F, 12);
        if (serverPlayer == null) {
            return NewSerumConfig.SOLAR_FLARE_DRAINED_TICKS.get() / 20.0D;
        }
        CHARGE.put(player.getUUID(), 0.0F);
        SerumKit.meter(serverPlayer, 0.0F);
        int drained = NewSerumConfig.SOLAR_FLARE_DRAINED_TICKS.get();
        if (drained > 0) {
            DRAINED.put(player.getUUID(), level.getGameTime() + drained);
            HeroBeams.stopAll(player);
            HeroCaster.message(player, Component.literal("§eThat was everything. You feel human."));
        }
        return 1.0D;
    }

    private static double flyingPunch(LivingEntity player, int slot) {
        if (blocked(player)) {
            return 0.0D;
        }
        Vec3 velocity = player.getLookAngle().scale(NewSerumConfig.FLYING_PUNCH_SPEED.get());
        double damage = NewSerumConfig.FLYING_PUNCH_DAMAGE.get() * solar(player);
        HeroAbilities.startDash(player, velocity, 10, damage, 3.0D);
        ServerLevel level = HeroCaster.level(player);
        Vec3 chest = player.position().add(0.0D, 1.0D, 0.0D);
        if (HeroFx.vfx(level, chest, "punch_pressure_wave", 0)) {
            HeroFx.vfx(level, chest, "air_burst_ring", 0);
            HeroFx.cue(player, "thematic:sonic_punch", 1.0F, 1.0F);
        } else {
            level.sendParticles(ParticleTypes.EXPLOSION, chest.x, chest.y, chest.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            HeroFx.play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 1.2F, 0.6F);
        }
        if (!ViltrumiteAnimations.play(player, ViltrumiteAnimations.PUNCH)) {
            HeroAnimations.play(player, "uppercut");
        }
        return NewSerumConfig.FLYING_PUNCH_COOLDOWN.get();
    }
}
