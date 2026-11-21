package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

final class DcKitFx {
    private DcKitFx() {
    }

    static ParticleOptions glow(int color, float size) {
        return HeroAbilities.emissive(color & 0xFFFFFF, size);
    }

    static DustParticleOptions dust(int color, float size) {
        return new DustParticleOptions(new Vector3f(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F), size);
    }

    static void effect(Mob mob, LivingEntity victim, Holder<MobEffect> effect, int ticks, int amplifier) {
        boolean control = effect == MobEffects.MOVEMENT_SLOWDOWN || effect == MobEffects.LEVITATION || effect == MobEffects.CONFUSION;
        if (control && !HeroAbilities.canControl(victim)) {
            return;
        }
        victim.addEffect(new MobEffectInstance(effect, ticks, amplifier), mob);
    }

    static void self(Mob mob, Holder<MobEffect> effect, int ticks, int amplifier) {
        mob.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false));
    }

    static void pull(Mob mob, LivingEntity victim, double strength) {
        if (!HeroAbilities.canControl(victim)) {
            return;
        }
        Vec3 pull = mob.position().subtract(victim.position());
        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
        victim.setDeltaMovement(dir.x * strength, 0.35D, dir.z * strength);
        victim.hurtMarked = true;
    }

    static void freeze(LivingEntity victim, int ticks) {
        victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 60, victim.getTicksFrozen() + ticks));
        if (HeroAbilities.canControl(victim)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
            HeroFx.effect("iceandfire:frozen").ifPresent(effect -> victim.addEffect(new MobEffectInstance(effect, ticks / 2)));
        }
    }

    static void energy(ServerLevel level, Mob mob, LivingEntity target, int color, float size, double speed, double scale, double turn,
            SoundEvent sound, float pitch, ParticleOptions extra, BiConsumer<LivingEntity, Vec3> after) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        shot(level, mob, target, from, dir, color, size, speed, scale, turn, extra, after);
        HeroKit.sound(level, from, sound, 1.0F, pitch);
    }

    static void shot(ServerLevel level, Mob mob, LivingEntity target, Vec3 from, Vec3 dir, int color, float size, double speed, double scale,
            double turn, ParticleOptions extra, BiConsumer<LivingEntity, Vec3> after) {
        ParticleOptions trail = glow(color, size);
        HeroKit.bolt(level, mob, from.add(dir), dir.scale(speed), 40, 0.6D, turn > 0.0D ? target : null, turn,
                (lvl, a, b) -> {
                    HeroKit.line(lvl, a, b, trail, 0.3D);
                    if (extra != null) {
                        lvl.sendParticles(extra, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                    }
                },
                (victim, at) -> {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, 1.0D, 0.25D);
                    if (after != null) {
                        after.accept(victim, at);
                    }
                },
                end -> HeroFx.vfx(level, end, "impact_starburst", color, () -> level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D)));
    }

    static void beam(ServerLevel level, Mob mob, LivingEntity target, int windup, double range, boolean pierce, int color, float size, double scale,
            double push, SoundEvent sound, float pitch, BiConsumer<LivingEntity, Vec3> after, Consumer<Vec3> onEnd) {
        Vec3 eye = HeroKit.muzzle(mob);
        level.sendParticles(glow(color, size * 0.8F), eye.x, eye.y, eye.z, 8, 0.2D, 0.1D, 0.2D, 0.0D);
        HeroKit.ray(level, mob, target, windup, range, 0.7D, pierce,
                (lvl, from, to) -> {
                    HeroFx.beam(lvl, from, to, "pyre_ray", color, () -> HeroKit.line(lvl, from, to, glow(color, size), 0.3D));
                    HeroKit.sound(lvl, from, sound, 1.1F, pitch);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), from, push, 0.2D);
                    if (after != null) {
                        after.accept(victim, from);
                    }
                },
                end -> {
                    HeroFx.vfx(level, end, "impact_starburst", color, () -> level.sendParticles(glow(color, size), end.x, end.y, end.z, 12, 0.3D, 0.3D, 0.3D, 0.0D));
                    if (onEnd != null) {
                        onEnd.accept(end);
                    }
                });
    }

    static void blinkStrike(ServerLevel level, Mob mob, LivingEntity target, ParticleOptions puff, SoundEvent sound, double scale,
            BiConsumer<LivingEntity, Vec3> after) {
        Vec3 start = HeroKit.chest(mob);
        level.sendParticles(puff, start.x, start.y, start.z, 24, 0.4D, 0.8D, 0.4D, 0.05D);
        HeroKit.sound(level, start, sound, 1.0F, 1.2F);
        if (!HeroKit.blink(level, mob, HeroKit.behind(target, 1.5D))) {
            return;
        }
        Vec3 arrive = HeroKit.chest(mob);
        level.sendParticles(puff, arrive.x, arrive.y, arrive.z, 24, 0.4D, 0.8D, 0.4D, 0.05D);
        HeroKit.sound(level, arrive, sound, 1.0F, 0.9F);
        HeroKit.face(mob, target);
        VillainWorld.later(level, 4, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.5D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), 1.0D, 0.3D);
            if (after != null) {
                after.accept(target, mob.position());
            }
        });
    }

    static void cloud(ServerLevel level, Mob mob, Vec3 center, double radius, int pulses, int gap, ParticleOptions main, ParticleOptions accent,
            Consumer<LivingEntity> onPulse) {
        for (int i = 0; i < pulses; i++) {
            VillainWorld.later(level, i * gap, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                level.sendParticles(main, center.x, center.y + 0.8D, center.z, 30, radius * 0.45D, 0.6D, radius * 0.45D, 0.01D);
                if (accent != null) {
                    level.sendParticles(accent, center.x, center.y + 0.8D, center.z, 12, radius * 0.45D, 0.6D, radius * 0.45D, 0.02D);
                }
                for (LivingEntity victim : HeroKit.victims(level, mob, center, radius)) {
                    onPulse.accept(victim);
                }
            });
        }
    }

    static void zap(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions core) {
        Vec3 last = from;
        for (int i = 1; i <= 4; i++) {
            Vec3 point = from.add(to.subtract(from).scale(i / 4.0D));
            if (i < 4) {
                point = point.add((level.random.nextDouble() - 0.5D) * 0.8D, (level.random.nextDouble() - 0.5D) * 0.8D, (level.random.nextDouble() - 0.5D) * 0.8D);
            }
            HeroKit.line(level, last, point, core, 0.2D);
            HeroKit.line(level, last, point, ParticleTypes.ELECTRIC_SPARK, 0.5D);
            last = point;
        }
    }

    static void ankh(ServerLevel level, Vec3 center, Vec3 facing, ParticleOptions particle) {
        Vec3 flat = facing.multiply(1.0D, 0.0D, 1.0D);
        flat = flat.lengthSqr() < 1.0E-4D ? new Vec3(1.0D, 0.0D, 0.0D) : flat.normalize();
        Vec3 side = new Vec3(-flat.z, 0.0D, flat.x);
        Vec3 up = new Vec3(0.0D, 1.0D, 0.0D);
        HeroKit.line(level, center.subtract(up.scale(0.7D)), center.add(up.scale(0.15D)), particle, 0.1D);
        HeroKit.line(level, center.add(up.scale(0.15D)).subtract(side.scale(0.45D)), center.add(up.scale(0.15D)).add(side.scale(0.45D)), particle, 0.1D);
        for (int i = 0; i < 16; i++) {
            double angle = Math.PI * 2.0D * i / 16.0D;
            Vec3 point = center.add(up.scale(0.45D + Math.cos(angle) * 0.28D)).add(side.scale(Math.sin(angle) * 0.2D));
            level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
    }
}
