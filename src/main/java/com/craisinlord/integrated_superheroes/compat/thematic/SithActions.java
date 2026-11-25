package com.craisinlord.integrated_superheroes.compat.thematic;

import com.craisinlord.integrated_superheroes.hero.ServerState;
import bond.thematic.api.content.ability.state.NumberSpec;
import bond.thematic.api.content.action.ActionContext;
import bond.thematic.api.content.action.Actions;
import bond.thematic.api.content.action.ContentAction;
import bond.thematic.api.util.AbilityUtils;
import bond.thematic.api.vfx.VfxAPI;
import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.joml.Vector3f;

public final class SithActions {
    private static final DustParticleOptions RED = new DustParticleOptions(new Vector3f(0.75F, 0.02F, 0.02F), 0.9F);
    private static final Map<UUID, List<Grip>> GRIPS = ServerState.map(new HashMap<>());
    private static final List<Slam> SLAMS = ServerState.all(new ArrayList<>());
    private static boolean registered;

    private SithActions() {
    }

    private static final class Grip {
        ServerLevel level;
        UUID owner;
        LivingEntity victim;
        String group;
        String flag;
        Vec3 anchor;
        boolean boss;
        boolean player;
        int age;
        int ticks;
        int every;
        float damage;
        double lift;
        float breakAt;
        float taken;
        int slownessTicks;
        int slownessAmplifier;
    }

    private static final class Slam {
        ServerLevel level;
        UUID owner;
        LivingEntity victim;
        int ticks;
        float damage;
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        try {
            action("sith_grip", SithActions::grip);
            action("sith_release", SithActions::release);
            action("force_wall_slam", SithActions::wallSlam);
            action("force_redirect", SithActions::redirect);
        } catch (RuntimeException | LinkageError error) {
            IntegratedSuperheroes.LOGGER.warn("Couldn't register the Sith ability actions with Thematic", error);
            return;
        }
        NeoForge.EVENT_BUS.addListener(SithActions::onServerTick);
        NeoForge.EVENT_BUS.addListener(SithActions::onDamage);
        NeoForge.EVENT_BUS.addListener(SithActions::onLogout);
    }

    private static void action(String name, Function<JsonObject, ContentAction> factory) {
        ResourceLocation id = IntegratedSuperheroes.id(name);
        if (!Actions.TYPES.has(id)) {
            Actions.TYPES.register(id, factory::apply);
        }
    }

    private static double number(ActionContext context, JsonObject json, String field, double fallback) {
        return NumberSpec.parse(json, field, NumberSpec.constant(fallback)).resolve(context.inputs());
    }

    private static DamageSource source(ServerLevel level, LivingEntity owner) {
        return owner instanceof Player player ? level.damageSources().playerAttack(player) : level.damageSources().mobAttack(owner);
    }

    private static ContentAction grip(JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "grip");
        String flag = GsonHelper.getAsString(json, "flag", "");
        return context -> {
            LivingEntity owner = context.owner();
            if (context.isClient() || owner == null || !(owner.level() instanceof ServerLevel level)
                    || !(context.hitEntity() instanceof LivingEntity victim) || ThematicTargets.spared(owner, victim)) {
                return;
            }
            List<Grip> grips = GRIPS.computeIfAbsent(owner.getUUID(), id -> new ArrayList<>());
            int max = Math.max(1, (int) Math.round(number(context, json, "max", 1.0D)));
            long held = grips.stream().filter(grip -> grip.group.equals(group)).count();
            if (held >= max || held(victim)) {
                return;
            }
            boolean player = victim instanceof Player;
            double seconds = number(context, json, "seconds", 4.0D);
            if (player) {
                seconds = Math.min(seconds, number(context, json, "player_seconds", 2.0D));
            }
            Grip grip = new Grip();
            grip.level = level;
            grip.owner = owner.getUUID();
            grip.victim = victim;
            grip.group = group;
            grip.flag = flag;
            grip.anchor = victim.position();
            grip.boss = ThematicTargets.boss(victim);
            grip.player = player;
            grip.ticks = Math.max(1, (int) Math.round(seconds * 20.0D));
            grip.every = Math.max(1, (int) Math.round(number(context, json, "every", 10.0D)));
            grip.damage = (float) number(context, json, "damage", 10.0D);
            if (grip.boss) {
                grip.damage *= (float) number(context, json, "boss_share", 0.5D);
            }
            double lift = number(context, json, "lift", 2.0D);
            grip.lift = player ? Math.min(lift, number(context, json, "player_lift", 1.5D)) : lift;
            double share = number(context, json, "break_share", 0.0D);
            grip.breakAt = share > 0.0D ? (float) (owner.getMaxHealth() * share) : Float.MAX_VALUE;
            grip.slownessTicks = (int) Math.round(number(context, json, "slowness_seconds", 3.0D) * 20.0D);
            grip.slownessAmplifier = (int) Math.round(number(context, json, "slowness_amplifier", 1.0D));
            victim.invulnerableTime = 0;
            boolean landed = victim.hurt(source(level, owner), grip.damage);
            if (player && !landed) {
                return;
            }
            grips.add(grip);
            if (!flag.isEmpty()) {
                AbilityUtils.storeBoolean(owner, flag, true);
            }
        };
    }

    private static boolean held(LivingEntity victim) {
        for (List<Grip> grips : GRIPS.values()) {
            for (Grip grip : grips) {
                if (grip.victim == victim) {
                    return true;
                }
            }
        }
        return false;
    }

    private static ContentAction release(JsonObject json) {
        String group = GsonHelper.getAsString(json, "group", "");
        return context -> {
            LivingEntity owner = context.owner();
            if (context.isClient() || owner == null) {
                return;
            }
            List<Grip> grips = GRIPS.get(owner.getUUID());
            if (grips == null) {
                return;
            }
            Iterator<Grip> iterator = grips.iterator();
            while (iterator.hasNext()) {
                Grip grip = iterator.next();
                if (group.isEmpty() || grip.group.equals(group)) {
                    iterator.remove();
                    end(grip, owner);
                }
            }
            if (grips.isEmpty()) {
                GRIPS.remove(owner.getUUID());
            }
        };
    }

    private static void end(Grip grip, LivingEntity owner) {
        LivingEntity victim = grip.victim;
        if (victim.isAlive()) {
            victim.fallDistance = 0.0F;
            if (grip.slownessTicks > 0) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, grip.slownessTicks, grip.slownessAmplifier), owner);
            }
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.0D, 1.0D, 0.0D));
            victim.hurtMarked = true;
        }
        if (owner != null && !grip.flag.isEmpty() && !holding(owner.getUUID(), grip.group)) {
            AbilityUtils.storeBoolean(owner, grip.flag, false);
        }
    }

    private static boolean holding(UUID owner, String group) {
        List<Grip> grips = GRIPS.get(owner);
        return grips != null && grips.stream().anyMatch(grip -> grip.group.equals(group));
    }

    private static void onServerTick(ServerTickEvent.Post event) {
        tickGrips();
        tickSlams();
    }

    private static void tickGrips() {
        if (GRIPS.isEmpty()) {
            return;
        }
        Iterator<Map.Entry<UUID, List<Grip>>> owners = GRIPS.entrySet().iterator();
        List<Runnable> endings = new ArrayList<>();
        while (owners.hasNext()) {
            Map.Entry<UUID, List<Grip>> entry = owners.next();
            Iterator<Grip> iterator = entry.getValue().iterator();
            while (iterator.hasNext()) {
                Grip grip = iterator.next();
                Entity found = grip.level.getEntity(grip.owner);
                LivingEntity owner = found instanceof LivingEntity living && living.isAlive() ? living : null;
                LivingEntity victim = grip.victim;
                if (owner == null || !victim.isAlive() || victim.isRemoved() || victim.level() != grip.level
                        || victim.distanceToSqr(owner) > 48.0D * 48.0D || grip.age >= grip.ticks || grip.taken >= grip.breakAt) {
                    iterator.remove();
                    endings.add(() -> end(grip, owner));
                    continue;
                }
                hold(grip, owner);
            }
            if (entry.getValue().isEmpty()) {
                owners.remove();
            }
        }
        endings.forEach(Runnable::run);
    }

    private static void hold(Grip grip, LivingEntity owner) {
        LivingEntity victim = grip.victim;
        ServerLevel level = grip.level;
        grip.age++;
        victim.fallDistance = 0.0F;
        if (victim instanceof Mob mob) {
            mob.getNavigation().stop();
        }
        if (grip.boss) {
            if (grip.age % 20 == 1) {
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, grip.slownessAmplifier + 1), owner);
            }
        } else {
            double rise = grip.lift * Math.min(1.0D, grip.age / 12.0D);
            Vec3 target = grip.anchor.add(0.0D, rise, 0.0D);
            Vec3 shift = target.subtract(victim.position());
            if (shift.lengthSqr() > 1.0E-4D && level.noCollision(victim, victim.getBoundingBox().move(shift))) {
                if (victim instanceof ServerPlayer player) {
                    player.teleportTo(target.x, target.y, target.z);
                } else {
                    victim.setPos(target.x, target.y, target.z);
                }
            }
            victim.setDeltaMovement(Vec3.ZERO);
            victim.hurtMarked = true;
        }
        if (grip.age % 4 == 0) {
            Vec3 neck = victim.position().add(0.0D, victim.getBbHeight() * 0.85D, 0.0D);
            level.sendParticles(RED, neck.x, neck.y, neck.z, 3, victim.getBbWidth() * 0.3D, 0.1D, victim.getBbWidth() * 0.3D, 0.0D);
        }
        if (grip.age % grip.every == 0) {
            victim.invulnerableTime = 0;
            victim.hurt(source(level, owner), grip.damage);
        }
    }

    private static ContentAction wallSlam(JsonObject json) {
        return context -> {
            LivingEntity owner = context.owner();
            if (context.isClient() || owner == null || !(owner.level() instanceof ServerLevel level)
                    || !(context.hitEntity() instanceof LivingEntity victim) || ThematicTargets.spared(owner, victim)) {
                return;
            }
            SLAMS.removeIf(slam -> slam.victim == victim);
            Slam slam = new Slam();
            slam.level = level;
            slam.owner = owner.getUUID();
            slam.victim = victim;
            slam.ticks = Math.max(1, (int) Math.round(number(context, json, "ticks", 10.0D)));
            slam.damage = (float) number(context, json, "damage", 20.0D);
            if (ThematicTargets.boss(victim)) {
                slam.damage *= (float) number(context, json, "boss_share", 0.5D);
            }
            SLAMS.add(slam);
        };
    }

    private static void tickSlams() {
        if (SLAMS.isEmpty()) {
            return;
        }
        Iterator<Slam> iterator = SLAMS.iterator();
        List<Slam> hits = new ArrayList<>();
        while (iterator.hasNext()) {
            Slam slam = iterator.next();
            slam.ticks--;
            LivingEntity victim = slam.victim;
            if (!victim.isAlive() || victim.isRemoved() || victim.level() != slam.level) {
                iterator.remove();
                continue;
            }
            if (victim.horizontalCollision) {
                iterator.remove();
                hits.add(slam);
            } else if (slam.ticks <= 0) {
                iterator.remove();
            }
        }
        for (Slam slam : hits) {
            LivingEntity victim = slam.victim;
            ServerLevel level = slam.level;
            Entity found = level.getEntity(slam.owner);
            LivingEntity owner = found instanceof LivingEntity living ? living : null;
            Vec3 at = victim.position().add(0.0D, victim.getBbHeight() * 0.5D, 0.0D);
            VfxAPI.triggerVFX(level, at, "thematic:impact_dust");
            level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 12, 0.3D, 0.3D, 0.3D, 0.2D);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.8F, 0.5F);
            victim.invulnerableTime = 0;
            victim.hurt(owner == null ? level.damageSources().flyIntoWall() : source(level, owner), slam.damage);
        }
    }

    private static ContentAction redirect(JsonObject json) {
        return context -> {
            LivingEntity owner = context.owner();
            if (context.isClient() || owner == null || !(owner.level() instanceof ServerLevel level)) {
                return;
            }
            double range = number(context, json, "range", 8.0D);
            double cone = Math.cos(Math.toRadians(number(context, json, "angle", 50.0D)));
            double speed = number(context, json, "speed", 1.2D);
            Vec3 eyes = owner.getEyePosition();
            Vec3 look = owner.getLookAngle();
            AABB box = owner.getBoundingBox().inflate(range);
            for (Projectile projectile : level.getEntitiesOfClass(Projectile.class, box, entity -> entity.getOwner() != owner)) {
                Vec3 offset = projectile.position().subtract(eyes);
                double distance = offset.length();
                if (distance > range || distance < 1.0E-3D || offset.scale(1.0D / distance).dot(look) < cone) {
                    continue;
                }
                Vec3 motion = projectile.getDeltaMovement();
                Vec3 away = motion.lengthSqr() < 1.0E-4D ? look : motion.scale(-1.0D);
                Vec3 flung = away.normalize().scale(Math.max(motion.length(), 1.0D) * speed);
                projectile.setOwner(owner);
                projectile.setDeltaMovement(flung);
                projectile.hasImpulse = true;
                projectile.hurtMarked = true;
                level.sendParticles(ParticleTypes.CLOUD, projectile.getX(), projectile.getY(), projectile.getZ(), 4, 0.1D, 0.1D, 0.1D, 0.02D);
            }
        };
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        LivingEntity hurt = event.getEntity();
        float amount = event.getNewDamage();
        if (amount <= 0.0F || hurt.level().isClientSide()) {
            return;
        }
        List<Grip> grips = GRIPS.get(hurt.getUUID());
        if (grips != null) {
            for (Grip grip : grips) {
                grip.taken += amount;
            }
        }
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        UUID id = event.getEntity().getUUID();
        List<Grip> grips = GRIPS.remove(id);
        if (grips != null) {
            for (Grip grip : grips) {
                end(grip, event.getEntity());
            }
        }
        for (List<Grip> others : GRIPS.values()) {
            others.removeIf(grip -> grip.victim == event.getEntity());
        }
        SLAMS.removeIf(slam -> slam.victim == event.getEntity());
    }
}
