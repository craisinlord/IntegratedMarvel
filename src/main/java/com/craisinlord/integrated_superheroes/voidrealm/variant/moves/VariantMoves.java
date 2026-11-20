package com.craisinlord.integrated_superheroes.voidrealm.variant.moves;

import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.villain.VillainFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantKind;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class VariantMoves {
    public record Move(String id, double minRange, double maxRange, Runner runner) {
    }

    @FunctionalInterface
    public interface Runner {
        void run(ServerLevel level, Mob mob, LivingEntity target);
    }

    private static final Map<String, Move> MOVES = Map.of(
            "lunge", new Move("lunge", 3.0D, 10.0D, VariantMoves::lunge),
            "slam", new Move("slam", 0.0D, 5.0D, VariantMoves::slam),
            "smoke", new Move("smoke", 0.0D, 4.0D, VariantMoves::smoke),
            "flash", new Move("flash", 0.0D, 8.0D, VariantMoves::flash),
            "dive", new Move("dive", 6.0D, 22.0D, VariantMoves::dive),
            "flying_punch", new Move("flying_punch", 4.0D, 18.0D, VariantMoves::flyingPunch));

    private VariantMoves() {
    }

    public static boolean isFallback(String id) {
        return get(id) != null;
    }

    public static Move get(String id) {
        Move move = MOVES.get(id);
        return move != null ? move : HeroMoves.get(id);
    }

    public static List<String> fallbackFor(VariantKind kind, List<String> style, int count) {
        Set<String> order = new LinkedHashSet<>();
        if (kind == VariantKind.VILTRUMITE) {
            order.add("flying_punch");
            order.add("dive");
        }
        if (style.contains("flyer")) {
            order.add("dive");
        }
        if (style.contains("brawler")) {
            order.add("slam");
            order.add("lunge");
        }
        if (style.contains("duelist")) {
            order.add("lunge");
            order.add("smoke");
        }
        if (style.contains("utility")) {
            order.add("flash");
            order.add("smoke");
        }
        if (style.contains("magic")) {
            order.add("flash");
            order.add("slam");
        }
        order.add("lunge");
        order.add("slam");
        List<String> out = new ArrayList<>();
        for (String id : order) {
            if (out.size() < count) {
                out.add(id);
            }
        }
        return out;
    }

    public static boolean tryUse(ServerLevel level, Mob mob, LivingEntity target, List<String> ids, RandomSource random) {
        double distance = mob.distanceTo(target);
        List<Move> ready = new ArrayList<>();
        for (String id : ids) {
            Move move = get(id);
            if (move != null && distance >= move.minRange() && distance <= move.maxRange()) {
                ready.add(move);
            }
        }
        if (ready.isEmpty()) {
            return false;
        }
        Move move = ready.get(random.nextInt(ready.size()));
        move.runner().run(level, mob, target);
        return true;
    }

    private static float damage(Mob mob, double scale) {
        double base = mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? mob.getAttributeValue(Attributes.ATTACK_DAMAGE) : 4.0D;
        return (float) (base * scale);
    }

    private static List<LivingEntity> victims(ServerLevel level, Mob mob, Vec3 center, double radius) {
        return level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), entity -> entity != mob && entity.isAlive()
                && entity.position().distanceToSqr(center) <= radius * radius
                && !(entity instanceof Player player && (player.isCreative() || player.isSpectator()))
                && !VoidVariants.allied(mob, entity));
    }

    private static void hit(ServerLevel level, Mob mob, LivingEntity victim, float amount, Vec3 from, double push, double lift) {
        if (victim.hurt(level.damageSources().mobAttack(mob), amount)) {
            Vec3 away = victim.position().subtract(from).multiply(1.0D, 0.0D, 1.0D);
            away = away.lengthSqr() < 1.0E-4D ? Vec3.ZERO : away.normalize();
            victim.push(away.x * push, lift, away.z * push);
            victim.hurtMarked = true;
        }
    }

    private static Vec3 toward(Mob mob, LivingEntity target) {
        Vec3 flat = target.position().subtract(mob.position()).multiply(1.0D, 0.0D, 1.0D);
        return flat.lengthSqr() < 1.0E-4D ? mob.getLookAngle().multiply(1.0D, 0.0D, 1.0D).normalize() : flat.normalize();
    }

    private static void lunge(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = toward(mob, target);
        mob.getLookControl().setLookAt(target);
        mob.setDeltaMovement(dir.x * 1.25D, 0.35D, dir.z * 1.25D);
        mob.hurtMarked = true;
        level.playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.0F, 0.8F);
        VillainWorld.later(level, 7, () -> {
            if (!mob.isAlive() || !target.isAlive() || mob.distanceTo(target) > 3.0D || VoidVariants.allied(mob, target)) {
                return;
            }
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            hit(level, mob, target, damage(mob, 1.5D), mob.position(), 0.9D, 0.3D);
            HeroFx.vfx(level, target.position().add(0.0D, 1.0D, 0.0D), "punch_pressure_wave", 0xFFFFFFFF);
        });
    }

    private static void slam(ServerLevel level, Mob mob, LivingEntity target) {
        mob.setDeltaMovement(mob.getDeltaMovement().x * 0.2D, 0.55D, mob.getDeltaMovement().z * 0.2D);
        mob.hurtMarked = true;
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = mob.position();
            for (LivingEntity victim : victims(level, mob, center, 4.0D)) {
                hit(level, mob, victim, damage(mob, 1.25D), center, 1.1D, 0.45D);
            }
            HeroFx.vfx(level, center, "powergirl_slam_wave", 0xFFFFFFFF, () -> level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z, 3, 1.0D, 0.1D, 1.0D, 0.0D));
            level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 0.8F, 1.3F);
            HeroFx.shake(level, center, 12.0D, 0.4F, 8);
        });
    }

    private static void smoke(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = mob.position();
        VillainFx.smoke(level, at.add(0.0D, 1.0D, 0.0D), 2.5D);
        for (LivingEntity victim : victims(level, mob, at, 4.0D)) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 50, 0), mob);
        }
        Vec3 back = toward(mob, target).scale(-7.0D);
        Vec3 to = at.add(back);
        if (level.noCollision(mob, mob.getBoundingBox().move(back))) {
            mob.teleportTo(to.x, to.y, to.z);
            mob.getNavigation().stop();
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.HOSTILE, 1.0F, 0.7F);
    }

    private static void flash(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = mob.position().add(toward(mob, target).scale(Math.min(4.0D, mob.distanceTo(target)))).add(0.0D, 1.0D, 0.0D);
        HeroFx.vfx(level, center, "impact_starburst", 0xFFFFFFFF, () -> VillainFx.sparks(level, center, 20));
        for (LivingEntity victim : victims(level, mob, center, 5.0D)) {
            victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), mob);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), mob);
            hit(level, mob, victim, damage(mob, 0.5D), center, 0.3D, 0.1D);
        }
        level.playSound(null, mob.blockPosition(), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.HOSTILE, 1.2F, 1.4F);
    }

    private static void dive(ServerLevel level, Mob mob, LivingEntity target) {
        mob.setDeltaMovement(0.0D, 1.3D, 0.0D);
        mob.hurtMarked = true;
        level.playSound(null, mob.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.HOSTILE, 1.0F, 0.6F);
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive() || !target.isAlive()) {
                return;
            }
            Vec3 path = target.position().subtract(mob.position());
            Vec3 dir = path.lengthSqr() < 1.0E-4D ? new Vec3(0.0D, -1.0D, 0.0D) : path.normalize();
            mob.setDeltaMovement(dir.scale(1.8D));
            mob.hurtMarked = true;
        });
        VillainWorld.later(level, 24, () -> {
            if (!mob.isAlive()) {
                return;
            }
            mob.fallDistance = 0.0F;
            Vec3 center = mob.position();
            for (LivingEntity victim : victims(level, mob, center, 3.5D)) {
                hit(level, mob, victim, damage(mob, 1.75D), center, 1.3D, 0.5D);
            }
            HeroFx.vfx(level, center, "impact_starburst", 0xFFFFFFFF, () -> level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z, 2, 0.6D, 0.1D, 0.6D, 0.0D));
            level.playSound(null, mob.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 1.0F, 1.0F);
            HeroFx.shake(level, center, 14.0D, 0.5F, 10);
        });
    }

    private static void flyingPunch(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 path = target.getEyePosition().subtract(mob.getEyePosition());
        Vec3 dir = path.lengthSqr() < 1.0E-4D ? mob.getLookAngle() : path.normalize();
        mob.getLookControl().setLookAt(target);
        mob.setDeltaMovement(dir.scale(2.2D).add(0.0D, 0.15D, 0.0D));
        mob.hurtMarked = true;
        HeroFx.cue(mob, "thematic:sonic_punch", 1.0F, 1.0F);
        for (int delay = 3; delay <= 9; delay += 3) {
            VillainWorld.later(level, delay, () -> {
                if (!mob.isAlive() || !target.isAlive() || mob.distanceTo(target) > 3.0D || VoidVariants.allied(mob, target)
                        || target.invulnerableTime > 10) {
                    return;
                }
                mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
                hit(level, mob, target, damage(mob, 2.0D), mob.position(), 2.4D, 0.6D);
                HeroFx.vfx(level, target.position().add(0.0D, 1.0D, 0.0D), "punch_pressure_wave", 0xFFFFFFFF);
                mob.setDeltaMovement(mob.getDeltaMovement().scale(0.2D));
            });
        }
    }
}
