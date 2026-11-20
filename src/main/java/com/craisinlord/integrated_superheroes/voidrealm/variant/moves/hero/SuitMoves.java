package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Map;

public final class SuitMoves {
    private static final int REPULSOR = 0xBFF4FF;
    private static final int UNIBEAM = 0xE6FBFF;
    private static final int STING = 0xFFE040;

    private SuitMoves() {
    }

    public static void register(Map<String, VariantMoves.Move> map) {
        add(map, "repulsor", 3.0D, 24.0D, SuitMoves::repulsor);
        add(map, "unibeam", 4.0D, 26.0D, SuitMoves::unibeam);
        add(map, "micro_missiles", 6.0D, 30.0D, SuitMoves::missiles);
        add(map, "shoulder_barrage", 4.0D, 24.0D, SuitMoves::barrage);
        add(map, "flamethrower", 0.0D, 7.0D, SuitMoves::flamethrower);
        add(map, "mjolnir_throw", 4.0D, 22.0D, SuitMoves::mjolnir);
        add(map, "pym_shrink_strike", 2.0D, 12.0D, SuitMoves::shrinkStrike);
        add(map, "giant_stomp", 0.0D, 7.0D, SuitMoves::giantStomp);
        add(map, "wasp_sting", 3.0D, 20.0D, SuitMoves::sting);
    }

    private static void add(Map<String, VariantMoves.Move> map, String id, double min, double max, VariantMoves.Runner runner) {
        map.put(id, new VariantMoves.Move(id, min, max, runner));
    }

    private static void repulsor(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 palm = HeroKit.hand(mob);
        level.sendParticles(HeroAbilities.emissive(REPULSOR, 0.8F), palm.x, palm.y, palm.z, 8, 0.15D, 0.15D, 0.15D, 0.0D);
        HeroFx.cue(mob, "marvel:suit.iron_man.repulsor_charge", 1.0F, 1.0F);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            HeroKit.face(mob, target);
            Vec3 from = HeroKit.hand(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            HeroKit.cue(level, from, "marvel:suit.iron_man.repulsor_shoot", SoundEvents.BLAZE_SHOOT, 1.0F, 1.2F);
            ParticleOptions glow = HeroAbilities.emissive(REPULSOR, 1.1F);
            HeroKit.bolt(level, mob, from.add(dir), dir.scale(1.8D), 20, 0.5D, null, 0.0D,
                    (lvl, a, b) -> {
                        HeroKit.line(lvl, a, b, glow, 0.3D);
                        lvl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                    },
                    (victim, at) -> HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.3D), at, 0.9D, 0.25D),
                    end -> {
                        level.sendParticles(HeroAbilities.emissive(REPULSOR, 1.4F), end.x, end.y, end.z, 14, 0.25D, 0.25D, 0.25D, 0.05D);
                        level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    });
        });
    }

    private static void unibeam(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.cue(level, mob.position(), "marvel:suit.iron_man.unibeam_charge", SoundEvents.BEACON_ACTIVATE, 1.0F, 1.0F);
        for (int t = 0; t < 20; t += 4) {
            VillainWorld.later(level, t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 core = HeroKit.chest(mob).add(mob.getLookAngle().scale(0.4D));
                level.sendParticles(HeroAbilities.emissive(UNIBEAM, 1.0F), core.x, core.y, core.z, 6, 0.4D, 0.4D, 0.4D, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, core.x, core.y, core.z, 2, 0.5D, 0.5D, 0.5D, -0.05D);
            });
        }
        HeroKit.ray(level, mob, target, 20, 28.0D, 0.9D, true,
                (lvl, from, to) -> {
                    Vec3 core = HeroKit.chest(mob).add(mob.getLookAngle().scale(0.4D));
                    HeroKit.line(lvl, core, to, HeroAbilities.emissive(UNIBEAM, 2.2F), 0.35D);
                    HeroKit.line(lvl, core, to, HeroAbilities.emissive(REPULSOR, 1.0F), 0.5D);
                    HeroKit.line(lvl, core, to, ParticleTypes.END_ROD, 1.0D);
                    HeroKit.cue(lvl, core, "marvel:suit.iron_man.unibeam_shoot", SoundEvents.BEACON_POWER_SELECT, 1.4F, 1.0F);
                },
                (victim, from) -> HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 2.4D), from, 1.8D, 0.4D),
                end -> {
                    level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    level.sendParticles(HeroAbilities.emissive(UNIBEAM, 1.6F), end.x, end.y, end.z, 20, 0.4D, 0.4D, 0.4D, 0.05D);
                });
    }

    private static void missiles(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 4; i++) {
            VillainWorld.later(level, i * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 from = mob.position().add(0.0D, mob.getBbHeight() * 0.9D, 0.0D);
                Vec3 launch = new Vec3((level.random.nextDouble() - 0.5D) * 0.6D, 0.7D, (level.random.nextDouble() - 0.5D) * 0.6D).normalize().scale(0.9D);
                HeroKit.sound(level, from, SoundEvents.FIREWORK_ROCKET_LAUNCH, 0.8F, 1.2F);
                HeroKit.bolt(level, mob, from, launch, 50, 0.5D, target, 0.22D,
                        (lvl, a, b) -> {
                            HeroKit.line(lvl, a, b, ParticleTypes.SMOKE, 0.4D);
                            lvl.sendParticles(ParticleTypes.FLAME, b.x, b.y, b.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        },
                        (victim, at) -> {
                        },
                        end -> {
                            level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                            HeroKit.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.3F);
                            for (LivingEntity victim : HeroKit.victims(level, mob, end, 2.2D)) {
                                victim.invulnerableTime = 0;
                                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), end, 0.7D, 0.3D);
                            }
                        });
            });
        }
    }

    private static void barrage(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 6; i++) {
            VillainWorld.later(level, 4 + i * 2, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                Vec3 from = mob.position().add(0.0D, mob.getBbHeight() * 0.95D, 0.0D);
                Vec3 dir = HeroKit.chest(target).add((level.random.nextDouble() - 0.5D) * 0.8D, (level.random.nextDouble() - 0.5D) * 0.8D,
                        (level.random.nextDouble() - 0.5D) * 0.8D).subtract(from).normalize();
                level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                HeroKit.sound(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6F, 1.9F);
                HeroKit.bolt(level, mob, from.add(dir), dir.scale(2.4D), 12, 0.35D, null, 0.0D,
                        (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.CRIT, 0.6D),
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.45D), at, 0.1D, 0.0D);
                        },
                        null);
            });
        }
    }

    private static void flamethrower(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        ParticleOptions flame = HeroFx.particle("marvel:iron_man_flame", ParticleTypes.FLAME);
        HeroKit.sound(level, mob.position(), SoundEvents.BLAZE_SHOOT, 1.0F, 0.5F);
        for (int pulse = 0; pulse < 4; pulse++) {
            VillainWorld.later(level, pulse * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
                for (int i = -2; i <= 2; i++) {
                    Vec3 spread = HeroKit.rotate(dir, i * 7.0D);
                    for (int d = 1; d <= 6; d++) {
                        Vec3 at = from.add(spread.scale(d));
                        level.sendParticles(flame, at.x, at.y, at.z, 0, spread.x, spread.y, spread.z, 0.25D);
                    }
                }
                HeroKit.sound(level, from, SoundEvents.FIRE_AMBIENT, 1.0F, 1.0F);
                for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 7.0D, 22.0D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.35D), from, 0.15D, 0.0D);
                    victim.igniteForSeconds(3.0F);
                }
            });
        }
    }

    private static void mjolnir(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        ItemStack hammer = new ItemStack(HeroFx.item("marvel:mjolnir", Items.MACE));
        HeroKit.cue(level, mob.position(), "marvel:item.mjolnir.throw", SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.0F);
        HeroKit.hurl(level, mob, target, hammer, 1.2D, 0, 50.0F,
                (lvl, a, b) -> lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 2, 0.1D, 0.1D, 0.1D, 0.05D),
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.8D), at, 1.2D, 0.35D);
                    HeroKit.cue(level, victim.position(), "marvel:item.mjolnir.hit", SoundEvents.ANVIL_LAND, 1.2F, 1.0F);
                    HeroKit.lightning(level, mob, victim.position(), 1.5D, HeroKit.damage(mob, 0.6D));
                },
                () -> HeroKit.cue(level, mob.position(), "marvel:item.mjolnir.catch", SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F));
    }

    private static void shrinkStrike(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        VariantBody.lockSize(mob, -0.75D, 14);
        HeroKit.cue(level, mob.position(), "marvel:suit.ant_man.shrink", SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.4F);
        mob.setDeltaMovement(dir.x * 1.1D, 0.25D, dir.z * 1.1D);
        mob.hurtMarked = true;
        VillainWorld.later(level, 10, () -> {
            if (!mob.isAlive()) {
                return;
            }
            VariantBody.lockSize(mob, 0.0D, 30);
            HeroKit.cue(level, mob.position(), "marvel:suit.ant_man.grow", SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 0.8F);
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.5D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.8D), mob.position(), 1.0D, 0.7D);
            HeroFx.vfx(level, HeroKit.chest(target), "punch_pressure_wave", 0);
        });
    }

    private static boolean room(ServerLevel level, Mob mob, double scale) {
        double half = mob.getBbWidth() * scale * 0.5D;
        double height = mob.getBbHeight() * scale;
        AABB box = new AABB(mob.getX() - half, mob.getY() + 0.05D, mob.getZ() - half, mob.getX() + half, mob.getY() + height, mob.getZ() + half);
        return level.noCollision(mob, box);
    }

    private static void giantStomp(ServerLevel level, Mob mob, LivingEntity target) {
        boolean giant = mob.getScale() <= 1.05D && room(level, mob, 2.5D);
        if (giant) {
            VariantBody.lockSize(mob, 1.5D, 50);
            HeroKit.cue(level, mob.position(), "marvel:suit.ant_man.grow", SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.4F, 0.6F);
        }
        VillainWorld.later(level, 16, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = mob.position();
            double radius = giant ? 5.5D : 3.5D;
            if (HeroFx.vfx(level, center, "slam_fracture_large", 0, (float) radius)) {
                HeroFx.vfx(level, center, "slam_dust_large", 0, (float) radius);
            } else {
                level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z, 3, radius * 0.3D, 0.1D, radius * 0.3D, 0.0D);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(mob.blockPosition().below())), center.x, center.y + 0.2D,
                        center.z, 50, radius * 0.4D, 0.2D, radius * 0.4D, 0.2D);
            }
            HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.6F);
            HeroFx.shake(level, center, 18.0D, 0.6F, 12);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, radius)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, giant ? 2.0D : 1.3D), center, 1.3D, 0.6D);
            }
        });
        if (giant) {
            VillainWorld.later(level, 50, () -> {
                if (mob.isAlive()) {
                    VariantBody.size(mob, 0.0D);
                    HeroKit.cue(level, mob.position(), "marvel:suit.ant_man.shrink", SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
                }
            });
        }
    }

    private static void sting(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.cue(level, from, "marvel:suit.wasp.sting", SoundEvents.BEE_STING, 1.0F, 1.2F);
        ParticleOptions glow = HeroAbilities.emissive(STING, 0.7F);
        HeroKit.bolt(level, mob, from.add(dir.scale(0.5D)), dir.scale(1.4D), 20, 0.5D, null, 0.0D,
                (lvl, a, b) -> {
                    HeroKit.line(lvl, a, b, glow, 0.25D);
                    lvl.sendParticles(ParticleTypes.ELECTRIC_SPARK, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                },
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), at, 0.3D, 0.1D);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), mob);
                },
                end -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 12, 0.2D, 0.2D, 0.2D, 0.15D));
    }
}
