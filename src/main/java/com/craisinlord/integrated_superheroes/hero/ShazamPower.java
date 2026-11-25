package com.craisinlord.integrated_superheroes.hero;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.AbstractGolem;
import net.minecraft.world.entity.npc.Npc;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Supplier;

public final class ShazamPower {
    public static final String ID = "shazam_billy_batson";
    public static final String POWERED_STATE = "integrated_superheroes_shazam_powered";
    private static final ResourceLocation FLIGHT_ID = ResourceLocation.fromNamespaceAndPath(IntegratedSuperheroes.MODID, "shazam_flight");
    private static final String HEAL_KEY = "integrated_superheroes_shazam_heal";
    private static final int BOLT_COLOR = 0xFFE14A;
    private static final Map<UUID, Chain> CHAINS = ServerState.map(new HashMap<>());
    private static final Map<UUID, Long> CAUGHT = ServerState.map(new HashMap<>());

    private record Chain(int hopsLeft, long nextHop, int targetId, Set<Integer> hit) {
    }

    private static final Set<UUID> MOB_POWERED = ServerState.all(new HashSet<>());
    private static boolean registered;

    private ShazamPower() {
    }

    public static HeroPower create(Supplier<Item> syringe) {
        if (!registered) {
            registered = true;
            HeroCaster.onTick(ShazamPower::tickMob);
            HeroCaster.onCleanup(caster -> {
                CHAINS.remove(caster.getUUID());
                MOB_POWERED.remove(caster.getUUID());
            });
        }
        return new HeroPower(
                ID,
                "Shazam (Billy Batson)",
                syringe,
                NewSerumConfig.SHAZAM,
                List.of(),
                List.of(
                        HeroPower.Ability.of("say_the_word", "Shazam!", ShazamPower::sayTheWord),
                        HeroPower.Ability.of("lightning_call", "Lightning Call", ShazamPower::lightningCall),
                        HeroPower.Ability.of("thunder_dash", "Thunder Dash", ShazamPower::thunderDash),
                        HeroToggles.WHEEL
                ),
                HeroToggles.flying(HeroToggles.SPEED),
                new HeroPower.Hooks() {
                    @Override
                    public void tick(ServerPlayer player) {
                        tickChain(player);
                        boolean powered = isPowered(player);
                        HeroFlight.tick(player, FLIGHT_ID, powered && HeroToggles.isOn(player, HeroToggles.FLIGHT),
                                NewSerumConfig.SHAZAM_FLY_SPEED.get().floatValue());
                        if (player.tickCount % 10 == 0) {
                            StormCharge.update(player, powered);
                        }
                        if (player.tickCount % 20 == 0) {
                            if (powered) {
                                Map<Holder<MobEffect>, Integer> effects = Map.of(MobEffects.MOVEMENT_SPEED, NewSerumConfig.SHAZAM_SPEED_LEVEL.get() - 1,
                                        MobEffects.FIRE_RESISTANCE, 0);
                                Map<Holder<MobEffect>, HeroPower.Toggle> toggles = Map.of(MobEffects.MOVEMENT_SPEED, HeroToggles.SPEED);
                                HeroToggles.applyEffects(player, effects, toggles);
                            } else {
                                SerumKit.dropEffect(player, MobEffects.MOVEMENT_SPEED);
                                SerumKit.dropEffect(player, MobEffects.FIRE_RESISTANCE);
                            }
                        }
                    }

                    @Override
                    public void extract(ServerPlayer player) {
                        HeroFlight.remove(player, FLIGHT_ID);
                        HeroAbilities.clearDash(player);
                        CHAINS.remove(player.getUUID());
                        CAUGHT.remove(player.getUUID());
                        HeroPowers.data(player).removeState(POWERED_STATE);
                        StormCharge.update(player, false);
                        SerumKit.dropEffect(player, MobEffects.MOVEMENT_SPEED);
                        SerumKit.dropEffect(player, MobEffects.FIRE_RESISTANCE);
                    }

                    @Override
                    public MarvelBalanceConfig.HeroStats stats(Player player) {
                        return isPowered(player) ? NewSerumConfig.SHAZAM : NewSerumConfig.SHAZAM_MORTAL;
                    }

                    @Override
                    public void incomingDamage(ServerPlayer player, LivingIncomingDamageEvent event) {
                        if (isPowered(player) && (event.getSource().is(DamageTypeTags.IS_LIGHTNING) || event.getSource().is(DamageTypeTags.IS_FALL))) {
                            event.setCanceled(true);
                            if (event.getSource().is(DamageTypeTags.IS_LIGHTNING)) {
                                catchLightning(player);
                            }
                        }
                    }

                    @Override
                    public void death(ServerPlayer player, LivingDeathEvent event) {
                        if (isPowered(player)) {
                            HeroPowers.data(player).removeState(POWERED_STATE);
                            StormCharge.update(player, false);
                            PlayerData.sync(player);
                        }
                    }
                }
        );
    }

    public static boolean isPowered(Player player) {
        return SerumKit.is(player, ID) && HeroPowers.data(player).hasState(POWERED_STATE);
    }

    private static boolean powered(LivingEntity caster) {
        return caster instanceof Player player ? isPowered(player) : MOB_POWERED.contains(caster.getUUID());
    }

    private static void tickMob(LivingEntity caster) {
        tickChain(caster);
        if (caster.tickCount % 20 == 0 && MOB_POWERED.contains(caster.getUUID())) {
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40,
                    NewSerumConfig.SHAZAM_SPEED_LEVEL.get() - 1, true, false));
            caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, true, false));
        }
    }

    private static boolean fair(LivingEntity caster, LivingEntity entity) {
        return caster instanceof Player ? fair(entity) : HeroCaster.foe(caster, entity);
    }

    private static boolean enemy(LivingEntity caster, LivingEntity entity) {
        return caster instanceof Player ? entity instanceof Enemy : HeroCaster.foe(caster, entity);
    }

    private static int bolt(LivingEntity player) {
        return 0xFF000000 | SerumKit.primary(player, BOLT_COLOR) & 0xFFFFFF;
    }

    private static void bolt(ServerLevel level, Vec3 at) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(at.x, at.y, at.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
    }

    private static boolean fair(LivingEntity entity) {
        return !(entity instanceof Player) && !(entity instanceof Npc) && !(entity instanceof AbstractGolem)
                && !(entity instanceof OwnableEntity ownable && ownable.getOwnerUUID() != null);
    }

    private static double sayTheWord(LivingEntity caster, int slot) {
        if (!(caster instanceof ServerPlayer player)) {
            return mobWord(caster);
        }
        ServerLevel level = player.serverLevel();
        PlayerData data = HeroPowers.data(player);
        boolean powering = !data.hasState(POWERED_STATE);
        bolt(level, player.position());
        if (HeroFx.vfx(level, player.position(), "fork_strike", bolt(player))) {
            HeroFx.vfx(level, player.position(), "fork_burst", bolt(player));
            HeroFx.cue(player, "thematic:thunder_clap", 1.0F, 1.0F);
        }
        float damage = NewSerumConfig.SHAZAM_WORD_DAMAGE.get().floatValue();
        if (damage > 0.0F) {
            for (LivingEntity target : HeroAbilities.sphere(player, 3.0D).stream().filter(ShazamPower::fair).toList()) {
                HeroAbilities.runAsAbilityDamage(() -> HeroHits.hurt(player, target, player.damageSources().lightningBolt(), damage));
            }
        }
        if (powering) {
            data.addState(POWERED_STATE);
            player.displayClientMessage(Component.literal("§eSHAZAM!"), true);
            level.sendParticles(SerumKit.glow(player, BOLT_COLOR, 1.4F), player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.5D, 1.0D, 0.5D, 0.0D);
            HeroAdvancements.award(player, "justice_league/shazam/say_the_word");
            if (level.isThundering()) {
                HeroAdvancements.award(player, "justice_league/shazam/storm_chaser");
            }
        } else {
            data.removeState(POWERED_STATE);
            player.displayClientMessage(Component.literal("§7Shazam! You're yourself again."), true);
            level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 1.0D, player.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.02D);
        }
        PlayerData.sync(player);
        StormCharge.update(player, powering);
        HeroPowers.syncStats(player);
        if (powering) {
            transformHeal(player);
        }
        return NewSerumConfig.SHAZAM_WORD_COOLDOWN.get();
    }

    private static double mobWord(LivingEntity caster) {
        ServerLevel level = HeroCaster.level(caster);
        boolean powering = MOB_POWERED.add(caster.getUUID());
        bolt(level, caster.position());
        if (HeroFx.vfx(level, caster.position(), "fork_strike", bolt(caster))) {
            HeroFx.vfx(level, caster.position(), "fork_burst", bolt(caster));
            HeroFx.cue(caster, "thematic:thunder_clap", 1.0F, 1.0F);
        }
        float damage = NewSerumConfig.SHAZAM_WORD_DAMAGE.get().floatValue();
        if (damage > 0.0F) {
            for (LivingEntity target : HeroAbilities.sphere(caster, 3.0D)) {
                HeroAbilities.runAsAbilityDamage(() -> target.hurt(caster.damageSources().lightningBolt(), damage));
            }
        }
        if (powering) {
            level.sendParticles(SerumKit.glow(caster, BOLT_COLOR, 1.4F), caster.getX(), caster.getY() + 1.0D, caster.getZ(), 40, 0.5D, 1.0D, 0.5D, 0.0D);
            transformHeal(caster);
        } else {
            MOB_POWERED.remove(caster.getUUID());
            level.sendParticles(ParticleTypes.SMOKE, caster.getX(), caster.getY() + 1.0D, caster.getZ(), 30, 0.4D, 0.8D, 0.4D, 0.02D);
        }
        return NewSerumConfig.SHAZAM_WORD_COOLDOWN.get();
    }

    private static void transformHeal(LivingEntity player) {
        double cooldown = NewSerumConfig.SHAZAM_HEAL_COOLDOWN.get();
        if (cooldown <= 0.0D) {
            return;
        }
        long now = HeroCaster.level(player).getServer().overworld().getGameTime();
        long last = player.getPersistentData().getLong(HEAL_KEY);
        if (last > 0L && last <= now && now - last < (long) (cooldown * 20.0D)) {
            return;
        }
        player.getPersistentData().putLong(HEAL_KEY, now);
        player.setHealth(player.getMaxHealth());
    }

    private static boolean human(LivingEntity player) {
        if (powered(player)) {
            return false;
        }
        HeroCaster.message(player, Component.literal("§7Nothing happens. You have to say the word first."));
        return true;
    }

    private static double lightningCall(LivingEntity player, int slot) {
        if (human(player)) {
            return 0.0D;
        }
        ServerLevel level = HeroCaster.level(player);
        double range = NewSerumConfig.SHAZAM_LIGHTNING_RANGE.get();
        HeroAbilities.Beam beam = HeroAbilities.beam(player, player.getLookAngle(), range, 1);
        Vec3 point;
        LivingEntity aimed = HeroCaster.target(player);
        if (!beam.targets().isEmpty()) {
            point = beam.targets().get(0).position();
        } else if (aimed != null && aimed.distanceToSqr(player) <= range * range) {
            point = aimed.position();
        } else {
            HitResult hit = player.pick(range, 0.0F, false);
            point = hit.getLocation();
        }
        float damage = NewSerumConfig.SHAZAM_LIGHTNING_DAMAGE.get().floatValue();
        double radius = NewSerumConfig.SHAZAM_LIGHTNING_RADIUS.get();
        if (player instanceof ServerPlayer caught && point.distanceToSqr(player.position()) <= radius * radius) {
            bolt(level, player.position());
            if (!catchLightning(caught)) {
                caught.displayClientMessage(Component.literal("§7The lightning has nothing more to give you yet."), true);
            }
            return NewSerumConfig.SHAZAM_LIGHTNING_COOLDOWN.get();
        }
        Set<Integer> hit = new HashSet<>();
        strike(level, player, point, radius, damage, hit);
        List<LivingEntity> chain = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, new AABB(point, point).inflate(8.0D),
                entity -> enemy(player, entity) && entity.isAlive() && !hit.contains(entity.getId())));
        chain.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(point)));
        for (int i = 0; i < Math.min(chain.size(), NewSerumConfig.SHAZAM_LIGHTNING_CHAIN.get()); i++) {
            strike(level, player, chain.get(i).position(), 1.0D, damage * 0.6F, hit);
        }
        if (HeroFx.vfx(level, point, "fork_burst_large", bolt(player))) {
            HeroFx.cue(player, "thematic:thunder_clap", 1.2F, 1.2F);
        } else {
            HeroAbilities.particleLine(level, player.getEyePosition().add(player.getLookAngle()), point.add(0.0D, 1.0D, 0.0D),
                    SerumKit.glow(player, BOLT_COLOR, 0.8F), 0.6D);
        }
        HeroAnimations.play(player, "celestisynth:cs_rainfall_aim_right");
        return NewSerumConfig.SHAZAM_LIGHTNING_COOLDOWN.get();
    }

    private static void strike(ServerLevel level, LivingEntity player, Vec3 at, double radius, float damage, Set<Integer> hit) {
        bolt(level, at);
        HeroFx.vfx(level, at, "fork_strike", bolt(player));
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius, radius + 1.0D, radius),
                entity -> entity != player && entity.isAlive() && !hit.contains(entity.getId()) && fair(player, entity))) {
            hit.add(target.getId());
            HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
        }
    }

    private static boolean catchLightning(ServerPlayer player) {
        long now = player.level().getGameTime();
        Long ready = CAUGHT.get(player.getUUID());
        if (ready != null && now < ready) {
            return false;
        }
        CAUGHT.put(player.getUUID(), now + Math.round(NewSerumConfig.SHAZAM_CATCH_COOLDOWN.get() * 20.0D));
        player.setHealth(player.getMaxHealth());
        HeroPower hero = HeroPowers.get(ID);
        if (hero != null) {
            for (HeroPower.Ability ability : hero.abilities()) {
                HeroAbilities.setCooldown(player, ability.name(), 0.0D);
            }
        }
        player.serverLevel().sendParticles(SerumKit.glow(player, BOLT_COLOR, 1.4F), player.getX(), player.getY() + 1.0D, player.getZ(),
                40, 0.5D, 1.0D, 0.5D, 0.05D);
        HeroFx.play(player, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.6F);
        player.displayClientMessage(Component.literal("§eThe lightning fills you right back up."), true);
        return true;
    }

    private static LivingEntity nextHop(LivingEntity player, Vec3 from, Set<Integer> hit) {
        double range = NewSerumConfig.SHAZAM_DASH_HOP_RANGE.get();
        LivingEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (LivingEntity candidate : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range),
                entity -> enemy(player, entity) && entity.isAlive() && !hit.contains(entity.getId()))) {
            double distance = candidate.distanceToSqr(from);
            if (distance <= range * range && distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    private static void hop(LivingEntity player, LivingEntity target, Set<Integer> hit) {
        ServerLevel level = HeroCaster.level(player);
        Vec3 from = player.position().add(0.0D, 1.0D, 0.0D);
        Vec3 offset = player.position().subtract(target.position());
        Vec3 flat = new Vec3(offset.x, 0.0D, offset.z);
        flat = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize().scale(target.getBbWidth() * 0.5D + 0.8D);
        Vec3 landing = target.position().add(flat);
        player.teleportTo(landing.x, landing.y, landing.z);
        player.fallDistance = 0.0F;
        hit.add(target.getId());
        float damage = NewSerumConfig.SHAZAM_DASH_DAMAGE.get().floatValue();
        HeroAbilities.runAsAbilityDamage(() -> target.hurt(player.damageSources().mobAttack(player), damage));
        SerumKit.push(target, landing, 1.2D, 0.4D);
        Vec3 to = target.getBoundingBox().getCenter();
        if (!HeroFx.beam(level, from, to, "sonar_beam", bolt(player))) {
            HeroAbilities.particleLine(level, from, to, SerumKit.glow(player, BOLT_COLOR, 0.9F), 0.4D);
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 16, 0.3D, 0.4D, 0.3D, 0.2D);
        HeroFx.play(player, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.8F, 1.6F);
        HeroAnimations.play(player, "uppercut");
    }

    private static void tickChain(LivingEntity player) {
        Chain chain = CHAINS.get(player.getUUID());
        if (chain == null || player.level().getGameTime() < chain.nextHop()) {
            return;
        }
        CHAINS.remove(player.getUUID());
        if (chain.hopsLeft() <= 0 || !powered(player)) {
            return;
        }
        Entity last = player.level().getEntity(chain.targetId());
        Vec3 from = last != null ? last.position() : player.position();
        LivingEntity next = nextHop(player, from, chain.hit());
        if (next == null) {
            return;
        }
        hop(player, next, chain.hit());
        CHAINS.put(player.getUUID(), new Chain(chain.hopsLeft() - 1, player.level().getGameTime() + 4L, next.getId(), chain.hit()));
    }

    private static double thunderDash(LivingEntity player, int slot) {
        if (human(player)) {
            return 0.0D;
        }
        LivingEntity first = null;
        double best = Double.MAX_VALUE;
        for (LivingEntity candidate : HeroAbilities.cone(player, NewSerumConfig.SHAZAM_DASH_HOP_RANGE.get(), 25.0D)) {
            if (enemy(player, candidate) && candidate.distanceToSqr(player) < best) {
                best = candidate.distanceToSqr(player);
                first = candidate;
            }
        }
        if (first != null) {
            Set<Integer> hit = new HashSet<>();
            hop(player, first, hit);
            CHAINS.put(player.getUUID(), new Chain(NewSerumConfig.SHAZAM_DASH_HOPS.get() - 1, player.level().getGameTime() + 4L, first.getId(), hit));
            HeroFx.play(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4F, 1.8F);
            return NewSerumConfig.SHAZAM_DASH_COOLDOWN.get();
        }
        Vec3 velocity = player.getLookAngle().scale(NewSerumConfig.SHAZAM_DASH_SPEED.get());
        HeroAbilities.startDash(player, velocity, 10, NewSerumConfig.SHAZAM_DASH_DAMAGE.get(), 2.5D);
        ServerLevel level = HeroCaster.level(player);
        if (HeroFx.vfx(level, player.position(), "fork_ground_line", bolt(player))) {
            HeroFx.vfx(level, player.position().add(0.0D, 1.0D, 0.0D), "air_burst_ring", 0);
            HeroFx.cue(player, "thematic:thunder_clap", 0.6F, 1.6F);
        } else {
            level.sendParticles(SerumKit.glow(player, BOLT_COLOR, 1.2F), player.getX(), player.getY() + 1.0D, player.getZ(),
                    20, 0.4D, 0.8D, 0.4D, 0.0D);
            HeroFx.play(player, SoundEvents.LIGHTNING_BOLT_THUNDER, 0.4F, 1.8F);
        }
        HeroAnimations.play(player, "uppercut");
        return NewSerumConfig.SHAZAM_DASH_COOLDOWN.get();
    }
}
