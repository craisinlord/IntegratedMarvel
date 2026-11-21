package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class MarvelKitMoves {
    private static final int POWER = 0xFFA040FF;
    private static final int DOOM = 0xFF40FF70;
    private static final int MIND = 0xFFFFE040;
    private static final int NOVA = 0xFFFFC23A;
    private static final int TIME = 0xFF40E8FF;
    private static final int VENOM = 0xFFFFE860;
    private static final int PLASMA = 0xFF4FA8FF;
    private static final int RING = 0xFF3D7BFF;
    private static final int CANNON = 0xFFFF7A20;
    private static final int GOLD = 0xFFFFD040;
    private static final DustParticleOptions LOKI_DUST = dust(0.2F, 0.85F, 0.35F, 1.3F);
    private static final DustParticleOptions MYSTIQUE_DUST = dust(0.15F, 0.35F, 0.95F, 1.4F);
    private static final DustParticleOptions HELA_DUST = dust(0.1F, 0.45F, 0.2F, 1.3F);
    private static final DustParticleOptions STEEL_DUST = dust(0.8F, 0.82F, 0.88F, 1.0F);
    private static final DustParticleOptions PSI_DUST = dust(1.0F, 0.3F, 0.85F, 1.4F);
    private static final DustParticleOptions SAI_DUST = dust(0.85F, 0.1F, 0.12F, 1.1F);
    private static final DustParticleOptions SOUL_DUST = dust(1.0F, 0.55F, 0.15F, 1.3F);
    private static final DustParticleOptions CHI_DUST = dust(1.0F, 0.8F, 0.2F, 1.6F);
    private static final DustParticleOptions BUBBLE_DUST = dust(0.55F, 0.9F, 1.0F, 1.2F);
    private static final DustParticleOptions CHARM_DUST = dust(0.35F, 0.95F, 0.4F, 1.2F);
    private static final DustParticleOptions EMPATH_DUST = dust(0.7F, 1.0F, 0.6F, 1.1F);
    private static final DustParticleOptions YAKA_DUST = dust(1.0F, 0.15F, 0.1F, 1.0F);
    private static final DustParticleOptions SURF_DUST = dust(0.85F, 0.9F, 1.0F, 1.5F);
    private static final DustParticleOptions SNAP_DUST = dust(0.35F, 0.3F, 0.28F, 1.4F);

    private MarvelKitMoves() {
    }

    public static void register(Map<String, VariantMoves.Move> map) {
        add(map, "mk_power_stone_blast", 3.0D, 26.0D, MarvelKitMoves::powerStone);
        add(map, "mk_gauntlet_snap", 0.0D, 10.0D, MarvelKitMoves::snap);
        add(map, "mk_loki_daggers", 3.0D, 18.0D, MarvelKitMoves::lokiDaggers);
        add(map, "mk_trickster_feint", 0.0D, 14.0D, (level, mob, target) -> ambush(level, mob, target, LOKI_DUST, MobEffects.CONFUSION));
        add(map, "mk_shapeshift_ambush", 0.0D, 14.0D, (level, mob, target) -> ambush(level, mob, target, MYSTIQUE_DUST, MobEffects.WEAKNESS));
        add(map, "mk_necrosword_volley", 3.0D, 22.0D, MarvelKitMoves::necroswords);
        add(map, "mk_necrosword_slash", 0.0D, 8.0D, (level, mob, target) -> bladeRush(level, mob, target, HELA_DUST, 3, 0.85D,
                victim -> victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0), mob)));
        add(map, "mk_chrono_blast", 3.0D, 24.0D, MarvelKitMoves::chrono);
        add(map, "mk_time_skip", 0.0D, 14.0D, MarvelKitMoves::timeSkip);
        add(map, "mk_doom_bolt", 3.0D, 24.0D, MarvelKitMoves::doomBolt);
        add(map, "mk_mind_stone_beam", 3.0D, 26.0D, MarvelKitMoves::mindStone);
        add(map, "mk_nova_force_blast", 3.0D, 24.0D, MarvelKitMoves::novaForce);
        add(map, "mk_element_guns", 3.0D, 20.0D, MarvelKitMoves::elementGuns);
        add(map, "mk_big_gun", 4.0D, 26.0D, MarvelKitMoves::bigGun);
        add(map, "mk_blade_rush", 0.0D, 8.0D, (level, mob, target) -> bladeRush(level, mob, target, STEEL_DUST, 3, 0.9D, null));
        add(map, "mk_psi_blade", 0.0D, 8.0D, (level, mob, target) -> bladeRush(level, mob, target, PSI_DUST, 2, 1.1D, victim -> {
            control(mob, victim, new MobEffectInstance(MobEffects.CONFUSION, 80, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), mob);
        }));
        add(map, "mk_soulsword_portal", 0.0D, 16.0D, MarvelKitMoves::soulsword);
        add(map, "mk_sai_flurry", 0.0D, 12.0D, MarvelKitMoves::saiFlurry);
        add(map, "mk_chi_punch", 0.0D, 7.0D, MarvelKitMoves::chiPunch);
        add(map, "mk_ten_rings", 3.0D, 20.0D, MarvelKitMoves::tenRings);
        add(map, "mk_ricochet_throw", 4.0D, 20.0D, MarvelKitMoves::ricochet);
        add(map, "mk_yaka_arrow", 4.0D, 24.0D, MarvelKitMoves::yaka);
        add(map, "mk_venom_blast", 3.0D, 18.0D, MarvelKitMoves::venomBlast);
        add(map, "mk_plasma_rifle", 4.0D, 22.0D, MarvelKitMoves::plasmaRifle);
        add(map, "mk_board_rush", 3.0D, 16.0D, MarvelKitMoves::boardRush);
        add(map, "mk_force_bubble", 2.0D, 16.0D, MarvelKitMoves::forceBubble);
        add(map, "mk_enthrall", 2.0D, 14.0D, MarvelKitMoves::enthrall);
        add(map, "mk_empath_touch", 0.0D, 6.0D, MarvelKitMoves::empathTouch);
    }

    private static void add(Map<String, VariantMoves.Move> map, String id, double min, double max, VariantMoves.Runner runner) {
        map.put(id, new VariantMoves.Move(id, min, max, runner));
    }

    private static DustParticleOptions dust(float r, float g, float b, float size) {
        return new DustParticleOptions(new Vector3f(r, g, b), size);
    }

    private static ParticleOptions glow(int argb, float size) {
        return HeroAbilities.emissive(argb & 0xFFFFFF, size);
    }

    private static boolean close(Mob mob, LivingEntity target, double reach) {
        return !HeroKit.gone(mob, target) && mob.distanceTo(target) <= reach && HeroKit.foe(mob, target);
    }

    private static void control(Mob mob, LivingEntity victim, MobEffectInstance effect) {
        if (HeroAbilities.canControl(victim)) {
            victim.addEffect(effect, mob);
        }
    }

    private static void splash(ServerLevel level, Mob mob, Vec3 center, double radius, double scale, double push) {
        for (LivingEntity victim : HeroKit.victims(level, mob, center, radius)) {
            victim.invulnerableTime = 0;
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), center, push, 0.35D);
        }
    }

    private static Vec3 aim(Mob mob, LivingEntity target, Vec3 from) {
        Vec3 path = HeroKit.chest(target).subtract(from);
        return path.lengthSqr() < 1.0E-4D ? mob.getLookAngle() : path.normalize();
    }

    private static void beam(ServerLevel level, Mob mob, LivingEntity target, int argb, int windup, double range, double radius, boolean pierce,
            double scale, double push, SoundEvent sound, float pitch, Consumer<LivingEntity> extra, Consumer<Vec3> onEnd) {
        Vec3 eye = HeroKit.muzzle(mob);
        level.sendParticles(glow(argb, 0.9F), eye.x, eye.y, eye.z, 10, 0.25D, 0.1D, 0.25D, 0.0D);
        HeroFx.cue(mob, "thematic:laser_open", 0.8F, 1.0F);
        HeroKit.ray(level, mob, target, windup, range, radius, pierce,
                (lvl, from, to) -> {
                    HeroFx.beam(lvl, from, to, "pyre_ray", argb, () -> HeroKit.line(lvl, from, to, glow(argb, 1.5F), 0.3D));
                    HeroKit.sound(lvl, from, sound, 1.2F, pitch);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), from, push, 0.3D);
                    if (extra != null) {
                        extra.accept(victim);
                    }
                },
                end -> {
                    HeroFx.vfx(level, end, "impact_starburst", argb, () -> level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D));
                    if (onEnd != null) {
                        onEnd.accept(end);
                    }
                });
    }

    private static void orb(ServerLevel level, Mob mob, Vec3 from, Vec3 dir, int argb, float size, double speed, int life,
            LivingEntity homing, double turn, double scale, double push, Consumer<LivingEntity> extra, Consumer<Vec3> onBurst) {
        ParticleOptions trail = glow(argb, size);
        HeroKit.bolt(level, mob, from, dir.scale(speed), life, 0.6D, homing, turn,
                (lvl, a, b) -> HeroKit.line(lvl, a, b, trail, 0.3D),
                (victim, at) -> {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, push, 0.2D);
                    if (extra != null) {
                        extra.accept(victim);
                    }
                },
                end -> {
                    HeroFx.vfx(level, end, "impact_starburst", argb, () -> level.sendParticles(trail, end.x, end.y, end.z, 12, 0.3D, 0.3D, 0.3D, 0.0D));
                    if (onBurst != null) {
                        onBurst.accept(end);
                    }
                });
    }

    private static void slash(ServerLevel level, Mob mob, LivingEntity target, ParticleOptions dust, double scale, Consumer<LivingEntity> extra) {
        mob.swing(InteractionHand.MAIN_HAND);
        target.invulnerableTime = 0;
        if (HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), 0.35D, 0.05D) && extra != null) {
            extra.accept(target);
        }
        Vec3 at = HeroKit.chest(target);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.2D, 0.2D, 0.2D, 0.0D);
        level.sendParticles(dust, at.x, at.y, at.z, 10, 0.4D, 0.4D, 0.4D, 0.0D);
        HeroKit.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.2F);
    }

    private static void bladeRush(ServerLevel level, Mob mob, LivingEntity target, ParticleOptions dust, int hits, double scale, Consumer<LivingEntity> extra) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        if (mob.distanceTo(target) > 2.5D) {
            mob.setDeltaMovement(dir.x * 1.2D, 0.25D, dir.z * 1.2D);
            mob.hurtMarked = true;
        }
        Vec3 at = mob.position();
        level.sendParticles(dust, at.x, at.y + 1.0D, at.z, 8, 0.3D, 0.5D, 0.3D, 0.0D);
        for (int i = 0; i < hits; i++) {
            VillainWorld.later(level, 6 + i * 4, () -> {
                if (close(mob, target, 3.4D)) {
                    slash(level, mob, target, dust, scale, extra);
                }
            });
        }
    }

    private static void powerStone(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 hand = HeroKit.hand(mob);
        level.sendParticles(glow(POWER, 1.2F), hand.x, hand.y, hand.z, 20, 0.4D, 0.4D, 0.4D, 0.02D);
        HeroKit.sound(level, hand, SoundEvents.BEACON_ACTIVATE, 1.2F, 0.6F);
        beam(level, mob, target, POWER, 14, 26.0D, 0.9D, true, 2.0D, 1.8D, SoundEvents.WARDEN_SONIC_BOOM, 1.4F, null, end -> {
            level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 2, 0.5D, 0.5D, 0.5D, 0.0D);
            HeroKit.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.9F);
            splash(level, mob, end, 2.5D, 0.8D, 1.0D);
        });
    }

    private static void snap(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 hand = HeroKit.hand(mob);
        mob.swing(InteractionHand.MAIN_HAND);
        level.sendParticles(glow(GOLD, 1.4F), hand.x, hand.y, hand.z, 30, 0.3D, 0.3D, 0.3D, 0.05D);
        HeroKit.sound(level, hand, SoundEvents.BEACON_POWER_SELECT, 1.4F, 0.5F);
        VillainWorld.later(level, 20, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = HeroKit.chest(mob);
            level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            HeroFx.vfx(level, mob.position(), "powergirl_slam_wave", GOLD);
            HeroKit.sound(level, center, SoundEvents.BEACON_DEACTIVATE, 1.6F, 0.5F);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 10.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), center, 0.2D, 0.1D);
                victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1), mob);
                victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), mob);
                Vec3 at = HeroKit.chest(victim);
                level.sendParticles(SNAP_DUST, at.x, at.y, at.z, 25, 0.4D, 0.7D, 0.4D, 0.0D);
                level.sendParticles(ParticleTypes.ASH, at.x, at.y, at.z, 30, 0.5D, 0.8D, 0.5D, 0.02D);
            }
        });
    }

    private static void lokiDaggers(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = aim(mob, target, from);
        ParticleOptions blade = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.IRON_SWORD));
        HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.7F);
        for (int i = -1; i <= 1; i++) {
            Vec3 spread = HeroKit.rotate(dir, i * 9.0D);
            HeroKit.bolt(level, mob, from.add(spread), spread.scale(1.6D), 16, 0.45D, target, 0.08D,
                    (lvl, a, b) -> {
                        HeroKit.line(lvl, a, b, LOKI_DUST, 0.35D);
                        lvl.sendParticles(blade, b.x, b.y, b.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    },
                    (victim, at) -> {
                        victim.invulnerableTime = 0;
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.7D), at, 0.2D, 0.05D);
                    },
                    null);
        }
    }

    private static void ambush(ServerLevel level, Mob mob, LivingEntity target, ParticleOptions dust, Holder<MobEffect> effect) {
        Vec3 from = mob.position();
        level.sendParticles(dust, from.x, from.y + 1.0D, from.z, 30, 0.35D, 0.9D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.POOF, from.x, from.y + 1.0D, from.z, 10, 0.3D, 0.6D, 0.3D, 0.02D);
        HeroKit.sound(level, from, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.0F);
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 20, 2, false, false));
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.blink(level, mob, HeroKit.behind(target, 1.4D))) {
                return;
            }
            Vec3 at = mob.position();
            level.sendParticles(dust, at.x, at.y + 1.0D, at.z, 20, 0.3D, 0.8D, 0.3D, 0.0D);
            HeroKit.face(mob, target);
            VillainWorld.later(level, 4, () -> {
                if (close(mob, target, 3.0D)) {
                    slash(level, mob, target, dust, 1.6D, victim -> control(mob, victim, new MobEffectInstance(effect, 60, 0)));
                }
            });
        });
    }

    private static void necroswords(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.EVOKER_PREPARE_SUMMON, 1.0F, 0.7F);
        for (int i = 0; i < 5; i++) {
            int index = i;
            VillainWorld.later(level, 6 + i * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 side = HeroKit.rotate(HeroKit.toward(mob, target), 90.0D).scale((index - 2) * 0.7D);
                Vec3 from = mob.position().add(0.0D, mob.getBbHeight() + 0.8D, 0.0D).add(side);
                Vec3 dir = aim(mob, target, from);
                level.sendParticles(ParticleTypes.SOUL, from.x, from.y, from.z, 3, 0.1D, 0.1D, 0.1D, 0.0D);
                HeroKit.sound(level, from, SoundEvents.WITHER_SHOOT, 0.5F, 1.6F);
                HeroKit.bolt(level, mob, from, dir.scale(1.3D), 24, 0.5D, target, 0.2D,
                        (lvl, a, b) -> {
                            HeroKit.line(lvl, a, b, HELA_DUST, 0.25D);
                            lvl.sendParticles(ParticleTypes.SCULK_SOUL, b.x, b.y, b.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        },
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), at, 0.2D, 0.05D);
                        },
                        end -> level.sendParticles(ParticleTypes.SOUL, end.x, end.y, end.z, 4, 0.2D, 0.2D, 0.2D, 0.02D));
            });
        }
    }

    private static void chrono(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = aim(mob, target, from);
        HeroKit.sound(level, from, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.8F);
        orb(level, mob, from.add(dir), dir, TIME, 1.2F, 1.4D, 30, target, 0.1D, 1.4D, 0.6D, victim -> {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 3), mob);
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 80, 2), mob);
        }, end -> level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 16, 0.3D, 0.3D, 0.3D, 0.05D));
    }

    private static void timeSkip(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = mob.position();
        level.sendParticles(glow(TIME, 1.0F), from.x, from.y + 1.0D, from.z, 20, 0.3D, 0.8D, 0.3D, 0.0D);
        HeroKit.sound(level, from, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 1.6F);
        if (HeroKit.gone(mob, target) || !HeroKit.blink(level, mob, HeroKit.behind(target, 1.4D))) {
            return;
        }
        Vec3 at = mob.position();
        level.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 1.0D, at.z, 20, 0.3D, 0.8D, 0.3D, 0.02D);
        HeroKit.face(mob, target);
        control(mob, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 4));
        VillainWorld.later(level, 3, () -> {
            if (close(mob, target, 3.0D)) {
                slash(level, mob, target, glow(TIME, 1.0F), 1.4D, null);
            }
        });
    }

    private static void doomBolt(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 hand = HeroKit.hand(mob);
        level.sendParticles(ParticleTypes.ENCHANT, hand.x, hand.y, hand.z, 25, 0.5D, 0.5D, 0.5D, 0.5D);
        HeroKit.sound(level, hand, SoundEvents.EVOKER_CAST_SPELL, 1.2F, 0.8F);
        beam(level, mob, target, DOOM, 10, 24.0D, 0.7D, false, 1.7D, 1.2D, SoundEvents.ILLUSIONER_CAST_SPELL, 0.7F,
                victim -> victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), mob),
                end -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 20, 0.4D, 0.4D, 0.4D, 0.2D));
    }

    private static void mindStone(ServerLevel level, Mob mob, LivingEntity target) {
        beam(level, mob, target, MIND, 12, 26.0D, 0.8D, true, 1.8D, 1.4D, SoundEvents.BEACON_ACTIVATE, 1.6F, null,
                end -> level.sendParticles(ParticleTypes.END_ROD, end.x, end.y, end.z, 12, 0.3D, 0.3D, 0.3D, 0.08D));
    }

    private static void novaForce(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = aim(mob, target, from);
        HeroKit.sound(level, from, SoundEvents.BLAZE_SHOOT, 1.0F, 1.4F);
        orb(level, mob, from.add(dir), dir, NOVA, 1.4F, 1.6D, 28, null, 0.0D, 1.5D, 2.0D, null, end -> {
            HeroKit.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.6F);
            splash(level, mob, end, 2.0D, 0.6D, 1.4D);
        });
    }

    private static void elementGuns(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 4; i++) {
            boolean fire = i % 2 == 0;
            VillainWorld.later(level, 4 + i * 4, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = aim(mob, target, from);
                HeroKit.sound(level, from, fire ? SoundEvents.BLAZE_SHOOT : SoundEvents.POWDER_SNOW_BREAK, 0.8F, 1.6F);
                orb(level, mob, from.add(dir), dir, fire ? 0xFFFF5A20 : 0xFF9FE6FF, 0.9F, 2.0D, 14, null, 0.0D, 0.6D, 0.3D,
                        victim -> {
                            if (fire) {
                                victim.igniteForSeconds(3.0F);
                            } else {
                                victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 40, victim.getTicksFrozen() + 60));
                                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1), mob);
                            }
                        }, null);
            });
        }
    }

    private static void bigGun(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 hand = HeroKit.hand(mob);
        level.sendParticles(glow(CANNON, 1.0F), hand.x, hand.y, hand.z, 20, 0.3D, 0.3D, 0.3D, 0.0D);
        HeroKit.sound(level, hand, SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.3F);
        VillainWorld.later(level, 16, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            HeroKit.face(mob, target);
            Vec3 from = HeroKit.hand(mob);
            Vec3 dir = aim(mob, target, from);
            HeroKit.sound(level, from, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.4F, 0.6F);
            level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 6, 0.1D, 0.1D, 0.1D, 0.02D);
            orb(level, mob, from.add(dir), dir, CANNON, 1.8F, 1.8D, 26, null, 0.0D, 2.0D, 1.6D, null, end -> {
                level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 3, 0.8D, 0.6D, 0.8D, 0.0D);
                HeroKit.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.0F);
                splash(level, mob, end, 3.0D, 1.0D, 1.2D);
            });
        });
    }

    private static void soulsword(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = mob.position();
        level.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0D, from.z, 40, 0.5D, 1.0D, 0.5D, 0.6D);
        HeroKit.sound(level, from, SoundEvents.PORTAL_TRIGGER, 0.5F, 1.8F);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.blink(level, mob, HeroKit.behind(target, 1.4D))) {
                return;
            }
            Vec3 at = mob.position();
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0D, at.z, 40, 0.4D, 1.0D, 0.4D, 0.1D);
            HeroKit.sound(level, at, SoundEvents.ENDERMAN_TELEPORT, 1.0F, 0.8F);
            HeroKit.face(mob, target);
            for (int i = 0; i < 2; i++) {
                VillainWorld.later(level, 3 + i * 5, () -> {
                    if (close(mob, target, 3.2D)) {
                        slash(level, mob, target, SOUL_DUST, 1.2D, victim -> victim.igniteForSeconds(2.0F));
                    }
                });
            }
        });
    }

    private static void saiFlurry(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = mob.position();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y + 1.0D, from.z, 12, 0.3D, 0.6D, 0.3D, 0.01D);
        HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.6F);
        if (mob.distanceTo(target) > 3.0D
                && !HeroKit.blink(level, mob, target.position().add(HeroKit.rotate(new Vec3(1.5D, 0.0D, 0.0D), level.random.nextInt(360))))) {
            bladeRush(level, mob, target, SAI_DUST, 3, 0.6D, null);
            return;
        }
        HeroKit.face(mob, target);
        for (int i = 0; i < 3; i++) {
            VillainWorld.later(level, 3 + i * 3, () -> {
                if (close(mob, target, 3.2D)) {
                    slash(level, mob, target, SAI_DUST, 0.65D, null);
                }
            });
        }
    }

    private static void chiPunch(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 hand = HeroKit.hand(mob);
        level.sendParticles(glow(GOLD, 1.3F), hand.x, hand.y, hand.z, 24, 0.25D, 0.25D, 0.25D, 0.02D);
        HeroKit.sound(level, hand, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 0.7F);
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 dir = HeroKit.toward(mob, target);
            if (mob.distanceTo(target) > 2.5D) {
                mob.setDeltaMovement(dir.x * 1.3D, 0.2D, dir.z * 1.3D);
                mob.hurtMarked = true;
            }
            VillainWorld.later(level, 4, () -> {
                if (!close(mob, target, 3.4D)) {
                    return;
                }
                mob.swing(InteractionHand.MAIN_HAND);
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, 2.3D), mob.position(), 2.6D, 0.5D);
                Vec3 at = HeroKit.chest(target);
                HeroFx.vfx(level, at, "punch_pressure_wave", GOLD, () -> level.sendParticles(CHI_DUST, at.x, at.y, at.z, 30, 0.4D, 0.4D, 0.4D, 0.0D));
                level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                HeroKit.sound(level, at, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.4F, 0.6F);
            });
        });
    }

    private static void tenRings(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.CONDUIT_ACTIVATE, 1.0F, 1.6F);
        for (int i = 0; i < 5; i++) {
            int index = i;
            VillainWorld.later(level, 4 + i * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 side = HeroKit.rotate(HeroKit.toward(mob, target), 90.0D).scale((index - 2) * 0.6D);
                Vec3 from = HeroKit.hand(mob).add(side);
                Vec3 dir = aim(mob, target, from);
                HeroKit.sound(level, from, SoundEvents.AMETHYST_BLOCK_CHIME, 1.0F, 1.4F);
                orb(level, mob, from.add(dir), dir, RING, 1.1F, 1.4D, 22, target, 0.18D, 0.6D, 0.6D, null, null);
            });
        }
    }

    private static void ricochet(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.8F);
        HeroKit.hurl(level, mob, target, new ItemStack(Items.IRON_SWORD), 1.5D, 3, 60.0F,
                (lvl, a, b) -> lvl.sendParticles(ParticleTypes.CRIT, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D),
                (victim, at) -> {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), at, 0.4D, 0.1D);
                    HeroKit.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.2F);
                },
                null);
    }

    private static void yaka(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.4F, 1.8F);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            HeroKit.sound(level, mob.position(), SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.4F, 2.0F);
            HeroKit.hurl(level, mob, target, new ItemStack(Items.ARROW), 1.4D, 4, 0.0F,
                    (lvl, a, b) -> HeroKit.line(lvl, a, b, YAKA_DUST, 0.25D),
                    (victim, at) -> {
                        victim.invulnerableTime = 0;
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), at, 0.3D, 0.1D);
                        HeroKit.sound(level, victim.position(), SoundEvents.ARROW_HIT, 1.0F, 1.0F);
                    },
                    () -> HeroKit.sound(level, mob.position(), SoundEvents.NOTE_BLOCK_FLUTE.value(), 1.0F, 1.5F));
        });
    }

    private static void venomBlast(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = aim(mob, target, from);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, from.x, from.y, from.z, 12, 0.2D, 0.2D, 0.2D, 0.1D);
        HeroKit.sound(level, from, SoundEvents.BEEHIVE_WORK, 1.2F, 1.8F);
        orb(level, mob, from.add(dir), dir, VENOM, 1.1F, 1.8D, 16, null, 0.0D, 1.3D, 0.5D,
                victim -> control(mob, victim, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 5)),
                end -> level.sendParticles(ParticleTypes.ELECTRIC_SPARK, end.x, end.y, end.z, 20, 0.3D, 0.3D, 0.3D, 0.2D));
    }

    private static void plasmaRifle(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 3; i++) {
            VillainWorld.later(level, 6 + i * 5, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = aim(mob, target, from);
                HeroKit.sound(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, 0.7F);
                orb(level, mob, from.add(dir), dir, PLASMA, 1.2F, 2.2D, 14, null, 0.0D, 0.85D, 0.6D, null, null);
            });
        }
    }

    private static void boardRush(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 start = mob.position();
        level.sendParticles(SURF_DUST, start.x, start.y + 0.3D, start.z, 20, 0.5D, 0.2D, 0.5D, 0.0D);
        HeroKit.sound(level, start, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.8F);
        VillainWorld.later(level, 8, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = mob.position();
            Vec3 dir = HeroKit.toward(mob, target);
            Vec3 goal = target.position().add(dir.scale(2.5D));
            if (!HeroKit.blink(level, mob, goal) && !HeroKit.blink(level, mob, HeroKit.behind(target, 1.5D))) {
                return;
            }
            Vec3 to = mob.position();
            Vec3 a = from.add(0.0D, 0.9D, 0.0D);
            Vec3 b = to.add(0.0D, 0.9D, 0.0D);
            HeroKit.line(level, a, b, SURF_DUST, 0.3D);
            HeroKit.line(level, a, b, ParticleTypes.END_ROD, 0.8D);
            HeroKit.sound(level, to, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
            Set<Integer> struck = new HashSet<>();
            for (LivingEntity victim : HeroKit.along(level, mob, a, b, 1.0D)) {
                if (struck.add(victim.getId())) {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), from, 1.4D, 0.5D);
                }
            }
            HeroKit.face(mob, target);
        });
    }

    private static void forceBubble(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.4F);
        for (int t = 0; t <= 30; t += 6) {
            boolean last = t == 30;
            boolean first = t == 0;
            VillainWorld.later(level, 6 + t, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                    return;
                }
                Vec3 at = HeroKit.chest(target);
                for (int i = 0; i < 16; i++) {
                    double yaw = level.random.nextDouble() * Math.PI * 2.0D;
                    double pitch = Math.acos(2.0D * level.random.nextDouble() - 1.0D);
                    level.sendParticles(BUBBLE_DUST, at.x + 1.4D * Math.sin(pitch) * Math.cos(yaw), at.y + 1.4D * Math.cos(pitch),
                            at.z + 1.4D * Math.sin(pitch) * Math.sin(yaw), 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                if (first) {
                    control(mob, target, new MobEffectInstance(MobEffects.LEVITATION, 30, 0));
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 3), mob);
                }
                if (last) {
                    target.invulnerableTime = 0;
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.3D), mob.position(), 0.8D, -0.4D);
                    level.sendParticles(ParticleTypes.FLASH, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    HeroKit.sound(level, at, SoundEvents.GLASS_BREAK, 1.2F, 1.2F);
                }
            });
        }
    }

    private static void enthrall(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.5F);
        HeroKit.ray(level, mob, target, 8, 14.0D, 0.8D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, CHARM_DUST, 0.25D);
                    HeroKit.line(lvl, from, to, ParticleTypes.HEART, 1.5D);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.7D), from, 0.0D, 0.0D);
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 1), mob);
                    control(mob, victim, new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                    if (HeroAbilities.canControl(victim)) {
                        Vec3 pull = mob.position().subtract(victim.position());
                        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
                        victim.setDeltaMovement(dir.x * 1.1D, 0.25D, dir.z * 1.1D);
                        victim.hurtMarked = true;
                    }
                },
                end -> level.sendParticles(ParticleTypes.HEART, end.x, end.y, end.z, 6, 0.3D, 0.3D, 0.3D, 0.0D));
    }

    private static void empathTouch(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        if (mob.distanceTo(target) > 2.5D) {
            mob.setDeltaMovement(dir.x, 0.25D, dir.z);
            mob.hurtMarked = true;
        }
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(EMPATH_DUST, at.x, at.y + 0.6D, at.z, 16, 0.2D, 0.2D, 0.2D, 0.0D);
        VillainWorld.later(level, 6, () -> {
            if (!close(mob, target, 3.2D)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.8D), mob.position(), 0.1D, 0.0D);
            control(mob, target, new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 5));
            control(mob, target, new MobEffectInstance(MobEffects.BLINDNESS, 50, 0));
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1), mob);
            Vec3 hit = HeroKit.chest(target);
            level.sendParticles(EMPATH_DUST, hit.x, hit.y, hit.z, 24, 0.4D, 0.5D, 0.4D, 0.0D);
            HeroKit.sound(level, hit, SoundEvents.AMETHYST_BLOCK_CHIME, 1.2F, 0.6F);
        });
    }
}
