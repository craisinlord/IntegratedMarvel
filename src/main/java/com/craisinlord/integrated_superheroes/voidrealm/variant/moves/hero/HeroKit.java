package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public final class HeroKit {
    private HeroKit() {
    }

    public interface Trail {
        void draw(ServerLevel level, Vec3 from, Vec3 to);
    }

    public static float damage(Mob mob, double scale) {
        double base = mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? mob.getAttributeValue(Attributes.ATTACK_DAMAGE) : 4.0D;
        return (float) (base * scale);
    }

    public static boolean foe(Mob mob, LivingEntity entity) {
        return entity != null && entity != mob && entity.isAlive() && entity.level() == mob.level()
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                && !VoidVariants.allied(mob, entity);
    }

    public static boolean gone(Mob mob, LivingEntity target) {
        return !mob.isAlive() || target == null || !target.isAlive() || target.level() != mob.level();
    }

    public static List<LivingEntity> victims(ServerLevel level, Mob mob, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                entity -> foe(mob, entity) && entity.position().distanceToSqr(center) <= radius * radius);
    }

    public static List<LivingEntity> cone(ServerLevel level, Mob mob, Vec3 from, Vec3 dir, double range, double halfAngle) {
        double cos = Math.cos(Math.toRadians(halfAngle));
        Vec3 unit = dir.normalize();
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(from, from).inflate(range), entity -> {
            if (!foe(mob, entity)) {
                return false;
            }
            Vec3 to = chest(entity).subtract(from);
            double length = to.length();
            return length <= range && (length < 1.0D || to.scale(1.0D / length).dot(unit) >= cos);
        });
    }

    public static Vec3 rotate(Vec3 dir, double degrees) {
        double rad = Math.toRadians(degrees);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new Vec3(dir.x * cos - dir.z * sin, dir.y, dir.x * sin + dir.z * cos);
    }

    public static boolean hit(ServerLevel level, Mob mob, LivingEntity victim, float amount, Vec3 from, double push, double lift) {
        if (!victim.hurt(level.damageSources().mobAttack(mob), amount)) {
            return false;
        }
        Vec3 away = victim.position().subtract(from).multiply(1.0D, 0.0D, 1.0D);
        away = away.lengthSqr() < 1.0E-4D ? Vec3.ZERO : away.normalize();
        victim.push(away.x * push, lift, away.z * push);
        victim.hurtMarked = true;
        return true;
    }

    public static Vec3 toward(Mob mob, LivingEntity target) {
        Vec3 flat = target.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        return flat.lengthSqr() < 1.0E-4D ? mob.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize() : flat.normalize();
    }

    public static Vec3 chest(LivingEntity entity) {
        return entity.getBoundingBox().getCenter();
    }

    public static Vec3 muzzle(Mob mob) {
        return mob.getEyePosition().subtract(0.0D, 0.15D * mob.getScale(), 0.0D);
    }

    public static Vec3 hand(Mob mob) {
        return mob.position().add(0.0D, mob.getBbHeight() * 0.62D, 0.0D);
    }

    public static void face(Mob mob, LivingEntity target) {
        mob.getLookControl().setLookAt(target, 90.0F, 90.0F);
    }

    public static void sound(ServerLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playSound(null, at.x, at.y, at.z, sound, SoundSource.HOSTILE, volume, pitch);
    }

    public static void cue(ServerLevel level, Vec3 at, String id, SoundEvent fallback, float volume, float pitch) {
        sound(level, at, HeroFx.sound(id, fallback), volume, pitch);
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
        double length = from.distanceTo(to);
        if (length < 0.05D) {
            return;
        }
        Vec3 step = to.subtract(from).scale(1.0D / length);
        int points = 0;
        for (double d = 0.0D; d < length && points < 96; d += spacing, points++) {
            Vec3 point = from.add(step.scale(d));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }

    public static Vec3 clip(ServerLevel level, Mob mob, Vec3 from, Vec3 to) {
        BlockHitResult result = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, mob));
        return result.getType() == HitResult.Type.MISS ? to : result.getLocation();
    }

    public static List<LivingEntity> along(ServerLevel level, Mob mob, Vec3 from, Vec3 to, double radius) {
        AABB box = new AABB(from, to).inflate(radius + 1.0D);
        List<LivingEntity> out = new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class, box,
                entity -> foe(mob, entity) && entity.getBoundingBox().inflate(radius).clip(from, to).isPresent()));
        out.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(from)));
        return out;
    }

    public static void ray(ServerLevel level, Mob mob, LivingEntity target, int windup, double range, double radius, boolean pierce,
            Trail trail, BiConsumer<LivingEntity, Vec3> onHit, Consumer<Vec3> onEnd) {
        face(mob, target);
        Vec3 marked = chest(target);
        VillainWorld.later(level, windup, () -> {
            if (gone(mob, target)) {
                return;
            }
            face(mob, target);
            Vec3 from = muzzle(mob);
            Vec3 point = marked.add(chest(target).subtract(marked).scale(0.5D));
            Vec3 path = point.subtract(from);
            Vec3 dir = path.lengthSqr() < 1.0E-4D ? mob.getLookAngle() : path.normalize();
            Vec3 end = clip(level, mob, from, from.add(dir.scale(range)));
            List<LivingEntity> struck = along(level, mob, from, end, radius);
            if (!pierce && !struck.isEmpty()) {
                LivingEntity first = struck.get(0);
                struck = List.of(first);
                Optional<Vec3> at = first.getBoundingBox().inflate(radius).clip(from, end);
                end = at.orElse(chest(first));
            }
            trail.draw(level, from, end);
            for (LivingEntity victim : struck) {
                onHit.accept(victim, from);
            }
            if (onEnd != null) {
                onEnd.accept(end);
            }
        });
    }

    public static void bolt(ServerLevel level, Mob mob, Vec3 from, Vec3 velocity, int life, double radius, LivingEntity homing, double turn,
            Trail trail, BiConsumer<LivingEntity, Vec3> onHit, Consumer<Vec3> onBurst) {
        step(level, mob, from, velocity, life, radius, homing, turn, trail, onHit, onBurst);
    }

    private static void step(ServerLevel level, Mob mob, Vec3 at, Vec3 velocity, int life, double radius, LivingEntity homing, double turn,
            Trail trail, BiConsumer<LivingEntity, Vec3> onHit, Consumer<Vec3> onBurst) {
        Vec3 vel = velocity;
        if (homing != null && homing.isAlive() && turn > 0.0D) {
            Vec3 want = chest(homing).subtract(at);
            if (want.lengthSqr() > 1.0E-4D) {
                double speed = vel.length();
                vel = vel.normalize().scale(1.0D - turn).add(want.normalize().scale(turn)).normalize().scale(speed);
            }
        }
        Vec3 next = at.add(vel);
        Vec3 stop = clip(level, mob, at, next);
        List<LivingEntity> struck = along(level, mob, at, stop, radius);
        trail.draw(level, at, struck.isEmpty() ? stop : chest(struck.get(0)));
        if (!struck.isEmpty()) {
            LivingEntity victim = struck.get(0);
            onHit.accept(victim, at);
            if (onBurst != null) {
                onBurst.accept(chest(victim));
            }
            return;
        }
        if (stop != next || life <= 1 || !mob.isAlive()) {
            if (onBurst != null && (stop != next || life <= 1)) {
                onBurst.accept(stop);
            }
            return;
        }
        Vec3 carry = vel;
        VillainWorld.later(level, 1, () -> step(level, mob, next, carry, life - 1, radius, homing, turn, trail, onHit, onBurst));
    }

    public static void hurl(ServerLevel level, Mob mob, LivingEntity target, ItemStack stack, double speed, int bounces, float spin,
            Trail trail, BiConsumer<LivingEntity, Vec3> onHit, Runnable onCatch) {
        Display.ItemDisplay display = new Display.ItemDisplay(EntityType.ITEM_DISPLAY, level) {
            @Override
            public boolean shouldBeSaved() {
                return false;
            }
        };
        display.getSlot(0).set(stack.copy());
        Vec3 start = hand(mob);
        display.moveTo(start.x, start.y, start.z, mob.getYRot(), 0.0F);
        level.addFreshEntity(display);
        Thrown state = new Thrown(display, target, bounces);
        fly(level, mob, state, speed, spin, trail, onHit, onCatch);
    }

    private static final class Thrown {
        final Display.ItemDisplay display;
        final Set<Integer> hit = new HashSet<>();
        LivingEntity target;
        int bounces;
        boolean back;
        int ticks;

        Thrown(Display.ItemDisplay display, LivingEntity target, int bounces) {
            this.display = display;
            this.target = target;
            this.bounces = bounces;
        }
    }

    private static void fly(ServerLevel level, Mob mob, Thrown state, double speed, float spin, Trail trail,
            BiConsumer<LivingEntity, Vec3> onHit, Runnable onCatch) {
        Display.ItemDisplay display = state.display;
        if (display.isRemoved()) {
            return;
        }
        state.ticks++;
        if (!mob.isAlive() || state.ticks > 100) {
            display.discard();
            return;
        }
        Vec3 at = display.position();
        Vec3 goal;
        if (state.back || state.target == null || !state.target.isAlive()) {
            state.back = true;
            goal = hand(mob);
        } else {
            goal = chest(state.target);
        }
        Vec3 path = goal.subtract(at);
        double distance = path.length();
        Vec3 next = distance <= speed ? goal : at.add(path.scale(speed / distance));
        if (!state.back) {
            Vec3 stop = clip(level, mob, at, next);
            for (LivingEntity victim : along(level, mob, at, stop, 0.6D)) {
                if (state.hit.add(victim.getId())) {
                    onHit.accept(victim, at);
                    LivingEntity bounce = null;
                    if (state.bounces > 0) {
                        double best = 64.0D;
                        for (LivingEntity other : victims(level, mob, chest(victim), 8.0D)) {
                            double d = other.distanceToSqr(victim);
                            if (!state.hit.contains(other.getId()) && d < best) {
                                best = d;
                                bounce = other;
                            }
                        }
                    }
                    if (bounce != null) {
                        state.bounces--;
                        state.target = bounce;
                    } else {
                        state.back = true;
                    }
                    break;
                }
            }
            if (stop != next || state.ticks > 40) {
                state.back = true;
                next = stop;
            }
        } else if (distance <= Math.max(1.0D, speed)) {
            display.discard();
            if (onCatch != null) {
                onCatch.run();
            }
            return;
        }
        trail.draw(level, at, next);
        display.moveTo(next.x, next.y, next.z, display.getYRot() + spin, 0.0F);
        VillainWorld.later(level, 1, () -> fly(level, mob, state, speed, spin, trail, onHit, onCatch));
    }

    public static void lightning(ServerLevel level, Mob mob, Vec3 at, double radius, float amount) {
        BlockPos ground = BlockPos.containing(at);
        for (int i = 0; i < 6 && level.getBlockState(ground.below()).isAir(); i++) {
            ground = ground.below();
        }
        Vec3 strike = Vec3.atBottomCenterOf(ground);
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt != null) {
            bolt.moveTo(strike.x, strike.y, strike.z);
            bolt.setVisualOnly(true);
            level.addFreshEntity(bolt);
        }
        if (HeroFx.vfx(level, strike, "fork_strike", 0)) {
            HeroFx.vfx(level, strike, "fork_burst", 0, (float) Math.max(2.0D, radius * 2.0D));
        } else {
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, strike.x, strike.y + 0.5D, strike.z, 30, radius * 0.4D, 0.5D, radius * 0.4D, 0.2D);
        }
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(strike, strike).inflate(radius, radius + 2.0D, radius),
                entity -> foe(mob, entity))) {
            hit(level, mob, victim, amount, strike, 0.4D, 0.2D);
        }
    }

    public static boolean blink(ServerLevel level, Mob mob, Vec3 to) {
        Vec3 shift = to.subtract(mob.position());
        for (int up = 0; up <= 2; up++) {
            AABB box = mob.getBoundingBox().move(shift.add(0.0D, up, 0.0D));
            if (level.noCollision(mob, box) && !level.getBlockState(BlockPos.containing(to.x, to.y + up - 0.5D, to.z)).isAir()) {
                mob.teleportTo(to.x, to.y + up, to.z);
                mob.getNavigation().stop();
                return true;
            }
        }
        return false;
    }

    public static Vec3 behind(LivingEntity target, double distance) {
        Vec3 look = target.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        look = look.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : look.normalize();
        return target.position().subtract(look.scale(distance));
    }
}
