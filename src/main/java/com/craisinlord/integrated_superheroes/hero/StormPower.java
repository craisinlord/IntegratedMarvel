package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

public final class StormPower {
    public static final String ID = "weather_manipulation_ororo_munroe";
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath("integrated_superheroes", "storm_flight");
    public static final HeroPower.Toggle STORM_AURA = new HeroPower.Toggle("integrated_superheroes_off_storm_aura", "Storm Aura",
            () -> HeroFx.item("antarchy:eye_of_the_storm", Items.WIND_CHARGE));
    private static final Set<UUID> WAS_FLYING = new HashSet<>();

    private StormPower() {
    }

    private static final Map<UUID, Set<Integer>> LAST_STRIKE = new java.util.HashMap<>();
    private static final Map<UUID, Gust> GUSTS = new java.util.HashMap<>();

    private record Gust(Map<LivingEntity, Vec3> starts, long until) {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        HeroWheels.register(StormWeather.wheel());
        return new HeroPower(
                ID,
                "Weather Manipulation (Ororo Munroe)",
                syringe,
                MarvelBalanceConfig.STORM,
                List.of(),
                List.of(
                        new HeroPower.Ability("lightning_strike", "Lightning Strike", StormPower::lightningStrike),
                        new HeroPower.Ability("gale", "Gale", StormPower::gale),
                        new HeroPower.Ability(StormWeather.ABILITY, "Weather Control", (player, slot) -> 0.0D),
                        HeroToggles.WHEEL
                ),
                List.of(HeroToggles.FLIGHT, STORM_AURA),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickGust(player);
                        HeroFlight.tick(player, FLIGHT_ID, HeroToggles.isOn(player, HeroToggles.FLIGHT), MarvelBalanceConfig.STORM_FLY_SPEED.get().floatValue());
                        boolean flying = player.getAbilities().flying;
                        boolean was = WAS_FLYING.contains(player.getUUID());
                        if (flying && !was) {
                            WAS_FLYING.add(player.getUUID());
                            takeoff(player);
                        } else if (!flying && was) {
                            WAS_FLYING.remove(player.getUUID());
                        }
                        if (player.tickCount % 10 == 0) {
                            StormCharge.update(player, StormWeather.isUnleashing(player));
                        }
                        if (flying && player.tickCount % 8 == 0) {
                            player.serverLevel().sendParticles(ParticleTypes.SMALL_GUST, player.getX(), player.getY(), player.getZ(), 1, 0.3D, 0.1D, 0.3D, 0.0D);
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        WAS_FLYING.remove(player.getUUID());
                        StormWeather.forget(player);
                        StormCharge.update(player, false);
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (HeroToggles.isOn(player, STORM_AURA) && event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
                            event.setCanceled(true);
                        }
                    }

                    @Override
                    public void outgoingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (HeroToggles.isOn(player, STORM_AURA) && StormWeather.isStormy(player)) {
                            event.setAmount(event.getAmount() * (1.0F + MarvelBalanceConfig.STORM_RAIN_DAMAGE_BONUS.get().floatValue()));
                        }
                    }
                }
        );
    }

    private static void takeoff(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        BlockPos ground = player.blockPosition();
        boolean nearGround = false;
        for (int i = 0; i < 4; i++) {
            if (!level.getBlockState(ground.below(i + 1)).isAir()) {
                nearGround = true;
                break;
            }
        }
        if (!nearGround) {
            return;
        }
        StormWind.takeoff(player);
        HeroFx.play(player, SoundEvents.BREEZE_WIND_CHARGE_BURST.value(), 0.9F, 1.1F);
        HeroAnimations.play(player, "celestisynth:cs_breezebreaker_jump");
    }

    private static double lightningStrike(ServerPlayer player, int slot) {
        ServerLevel level = player.serverLevel();
        double range = MarvelBalanceConfig.LIGHTNING_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        Vec3 point;
        if (!beam.targets().isEmpty()) {
            point = beam.targets().get(0).position();
        } else {
            HitResult hit = player.pick(range, 0.0F, false);
            point = hit.getLocation();
        }
        boolean stormy = StormWeather.isStormy(player);
        float damage = MarvelBalanceConfig.LIGHTNING_DAMAGE.get().floatValue();
        double radius = MarvelBalanceConfig.LIGHTNING_RADIUS.get();
        Set<Integer> hit = new HashSet<>();
        int bolts = stormy ? 3 : 1;
        for (int i = 0; i < bolts; i++) {
            Vec3 at = i == 0 ? point : point.add((level.random.nextDouble() - 0.5D) * 6.0D, 0.0D, (level.random.nextDouble() - 0.5D) * 6.0D);
            strike(level, player, at, radius, damage, hit);
        }
        List<LivingEntity> chain = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(8.0D),
                entity -> entity instanceof Enemy && entity.isAlive() && !hit.contains(entity.getId())));
        chain.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(point)));
        for (int i = 0; i < Math.min(chain.size(), MarvelBalanceConfig.LIGHTNING_CHAIN_TARGETS.get()); i++) {
            strike(level, player, chain.get(i).position(), 1.0D, damage * 0.6F, hit);
        }
        Set<Integer> previous = LAST_STRIKE.put(player.getUUID(), hit);
        if (previous != null && previous.stream().anyMatch(id -> hit.contains(id) && level.getEntity(id) instanceof LivingEntity living && living.isAlive())) {
            HeroAdvancements.award(player, "x_men/storm/lightning_strikes_twice");
        }
        HeroAnimations.play(player, "celestisynth:cs_rainfall_aim_right");
        return stormy ? MarvelBalanceConfig.LIGHTNING_STORM_COOLDOWN.get() : MarvelBalanceConfig.LIGHTNING_COOLDOWN.get();
    }

    private static void strike(ServerLevel level, ServerPlayer player, Vec3 at, double radius, float damage, Set<Integer> hit) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, radius + 1.0D, radius),
                entity -> entity != player && entity.isAlive() && !hit.contains(entity.getId()))) {
            hit.add(target.getId());
            target.hurt(player.damageSources().mobAttack(player), damage);
        }
    }

    private static void tickGust(ServerPlayer player) {
        Gust gust = GUSTS.get(player.getUUID());
        if (gust == null) {
            return;
        }
        if (player.level().getGameTime() > gust.until()) {
            GUSTS.remove(player.getUUID());
            return;
        }
        for (Map.Entry<LivingEntity, Vec3> entry : gust.starts().entrySet()) {
            if (entry.getKey().isAlive() && entry.getKey().level() == player.level() && entry.getKey().position().distanceTo(entry.getValue()) >= 20.0D) {
                HeroAdvancements.award(player, "x_men/storm/gone_with_the_wind");
                GUSTS.remove(player.getUUID());
                return;
            }
        }
    }

    private static double gale(ServerPlayer player, int slot) {
        Vec3 look = player.getLookAngle();
        Map<LivingEntity, Vec3> starts = new java.util.HashMap<>();
        for (LivingEntity target : player.level().getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(24.0D),
                entity -> entity != player && entity.isAlive() && !(entity instanceof net.minecraft.world.entity.player.Player))) {
            starts.put(target, target.position());
        }
        GUSTS.put(player.getUUID(), new Gust(starts, player.level().getGameTime() + 120L));
        StormWind.gale(player, player.getEyePosition().add(look), look);
        HeroAnimations.play(player, "celestisynth:cs_solaris_spin");
        return MarvelBalanceConfig.GALE_COOLDOWN.get();
    }
}
