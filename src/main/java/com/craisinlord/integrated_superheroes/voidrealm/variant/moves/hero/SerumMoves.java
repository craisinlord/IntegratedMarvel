package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.KineticBurst;
import com.craisinlord.integrated_superheroes.villain.VillainFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SerumMoves {
    private static final int HEAT = 0xFFFF2A1A;
    private static final int OPTIC = 0xFFFF3030;
    private static final int FROST = 0xB3D8F4FF;
    private static final int GAMMA = 0xFF3CFF3C;
    private static final int PHOTON = 0xFFFFC040;
    private static final int LASSO = 0xE0FFC83D;
    private static final DustParticleOptions BAMF_DUST = new DustParticleOptions(new Vector3f(0.18F, 0.1F, 0.28F), 1.6F);
    private static final DustParticleOptions TK_DUST = new DustParticleOptions(new Vector3f(0.95F, 0.35F, 0.7F), 1.2F);
    private static final DustParticleOptions HEX_DUST = new DustParticleOptions(new Vector3f(0.85F, 0.1F, 0.25F), 1.3F);
    private static final DustParticleOptions ARM_DUST = new DustParticleOptions(new Vector3f(0.2F, 0.35F, 0.85F), 1.4F);
    private static final DustParticleOptions FORCE_DUST = new DustParticleOptions(new Vector3f(0.55F, 0.85F, 1.0F), 1.3F);

    private SerumMoves() {
    }

    public static void register(Map<String, VariantMoves.Move> map) {
        add(map, "heat_vision", 3.0D, 24.0D, SerumMoves::heatVision);
        add(map, "freeze_breath", 0.0D, 9.0D, SerumMoves::freezeBreath);
        add(map, "optic_blast", 3.0D, 28.0D, SerumMoves::opticBlast);
        add(map, "optic_sweep", 2.0D, 16.0D, SerumMoves::opticSweep);
        add(map, "thunder_call", 0.0D, 26.0D, (level, mob, target) -> thunder(level, mob, target, 1));
        add(map, "lightning_storm", 0.0D, 22.0D, (level, mob, target) -> thunder(level, mob, target, 3));
        add(map, "storm_gust", 0.0D, 10.0D, SerumMoves::gust);
        add(map, "fireball_volley", 3.0D, 22.0D, SerumMoves::fireballs);
        add(map, "nova_burst", 0.0D, 5.0D, SerumMoves::nova);
        add(map, "ice_prison", 2.0D, 16.0D, SerumMoves::icePrison);
        add(map, "magnet_pull", 3.0D, 20.0D, SerumMoves::magnetPull);
        add(map, "magnetic_crush", 0.0D, 18.0D, (level, mob, target) -> lift(level, mob, target, true));
        add(map, "telekinetic_throw", 0.0D, 18.0D, (level, mob, target) -> lift(level, mob, target, false));
        add(map, "phoenix_burst", 0.0D, 6.0D, SerumMoves::phoenix);
        add(map, "bamf_strike", 0.0D, 16.0D, (level, mob, target) -> bamf(level, mob, target, 1));
        add(map, "bamf_ballet", 0.0D, 12.0D, (level, mob, target) -> bamf(level, mob, target, 3));
        add(map, "blitz", 2.0D, 16.0D, SerumMoves::blitz);
        add(map, "speed_dash", 3.0D, 12.0D, SerumMoves::speedDash);
        add(map, "hulk_smash", 0.0D, 5.0D, (level, mob, target) -> smash(level, mob, 5.5D, 2.0D, GAMMA, "gamma_shockwave"));
        add(map, "ground_pound", 0.0D, 4.5D, (level, mob, target) -> smash(level, mob, 4.0D, 1.6D, 0, "slam_fracture"));
        add(map, "gamma_leap", 6.0D, 20.0D, SerumMoves::gammaLeap);
        add(map, "thunder_clap", 0.0D, 9.0D, SerumMoves::thunderClap);
        add(map, "hulk_roar", 0.0D, 10.0D, SerumMoves::roar);
        add(map, "unstoppable_charge", 4.0D, 18.0D, SerumMoves::charge);
        add(map, "photon_blast", 3.0D, 26.0D, SerumMoves::photon);
        add(map, "binary_nova", 0.0D, 6.0D, SerumMoves::binaryNova);
        add(map, "chaos_hex", 3.0D, 22.0D, SerumMoves::hex);
        add(map, "hex_storm", 0.0D, 8.0D, SerumMoves::hexStorm);
        add(map, "kinetic_cards", 3.0D, 18.0D, SerumMoves::cards);
        add(map, "sonic_scream", 0.0D, 12.0D, SerumMoves::scream);
        add(map, "psychic_blast", 0.0D, 20.0D, SerumMoves::psychicBlast);
        add(map, "psychic_wave", 0.0D, 8.0D, SerumMoves::psychicWave);
        add(map, "force_wave", 0.0D, 12.0D, SerumMoves::forceWave);
        add(map, "vanish", 0.0D, 14.0D, SerumMoves::vanish);
        add(map, "stretch_punch", 2.0D, 9.0D, (level, mob, target) -> stretch(level, mob, target, false));
        add(map, "stretch_grab", 3.0D, 12.0D, (level, mob, target) -> stretch(level, mob, target, true));
        add(map, "phase_strike", 0.0D, 12.0D, SerumMoves::phase);
        add(map, "life_drain", 0.0D, 6.0D, SerumMoves::drain);
        add(map, "plasmoids", 0.0D, 12.0D, SerumMoves::plasmoids);
        add(map, "tidal_surge", 0.0D, 10.0D, SerumMoves::tidal);
        add(map, "lasso_pull", 3.0D, 14.0D, SerumMoves::lasso);
        add(map, "hellfire", 0.0D, 9.0D, SerumMoves::hellfire);
        add(map, "penance_chain", 3.0D, 14.0D, SerumMoves::chain);
        add(map, "root_snare", 0.0D, 14.0D, (level, mob, target) -> roots(level, mob, target, Blocks.OAK_LOG.defaultBlockState()));
        add(map, "vine_snare", 0.0D, 14.0D, (level, mob, target) -> roots(level, mob, target, Blocks.MOSS_BLOCK.defaultBlockState()));
        add(map, "cosmic_blast", 3.0D, 26.0D, SerumMoves::cosmic);
        add(map, "web_shot", 3.0D, 18.0D, SerumMoves::webShot);
        add(map, "web_yank", 3.0D, 14.0D, SerumMoves::webYank);
        add(map, "claw_flurry", 0.0D, 4.0D, (level, mob, target) -> flurry(level, mob, target, 3, 0.8D, true));
        add(map, "combo_flurry", 0.0D, 4.0D, (level, mob, target) -> flurry(level, mob, target, 3, 0.8D, false));
        add(map, "berserker_pounce", 3.0D, 10.0D, SerumMoves::pounce);
        add(map, "pistol_shots", 4.0D, 20.0D, SerumMoves::pistols);
        add(map, "kinetic_burst", 0.0D, 5.0D, SerumMoves::kineticBurst);
        add(map, "shield_throw", 4.0D, 22.0D, SerumMoves::shieldThrow);
        add(map, "shield_bash", 2.0D, 8.0D, SerumMoves::shieldBash);
    }

    private static void add(Map<String, VariantMoves.Move> map, String id, double min, double max, VariantMoves.Runner runner) {
        map.put(id, new VariantMoves.Move(id, min, max, runner));
    }

    private static void burn(LivingEntity victim, float seconds) {
        victim.igniteForSeconds(seconds);
    }

    private static void freeze(LivingEntity victim, int ticks) {
        victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 60, victim.getTicksFrozen() + ticks));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
        if (HeroAbilities.canControl(victim)) {
            HeroFx.effect("iceandfire:frozen").ifPresent(effect -> victim.addEffect(new MobEffectInstance(effect, ticks / 2)));
        }
    }

    private static void heatVision(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 eye = HeroKit.muzzle(mob);
        level.sendParticles(HeroAbilities.emissive(HEAT & 0xFFFFFF, 0.8F), eye.x, eye.y, eye.z, 6, 0.15D, 0.05D, 0.15D, 0.0D);
        HeroFx.cue(mob, "thematic:laser_open", 0.8F, 1.0F);
        HeroKit.ray(level, mob, target, 12, 24.0D, 0.6D, false,
                (lvl, from, to) -> HeroFx.beam(lvl, from, to, "pyre_ray", HEAT, () -> {
                    HeroKit.line(lvl, from, to, HeroAbilities.emissive(HEAT & 0xFFFFFF, 1.0F), 0.3D);
                    HeroKit.line(lvl, from, to, ParticleTypes.FLAME, 1.2D);
                }),
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), from, 0.4D, 0.1D);
                    burn(victim, 4.0F);
                },
                end -> {
                    HeroFx.vfx(level, end, "impact_starburst", HEAT, () -> level.sendParticles(ParticleTypes.LAVA, end.x, end.y, end.z, 6, 0.2D, 0.2D, 0.2D, 0.0D));
                    HeroKit.cue(level, end, "thematic:heat_vision", SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
                });
    }

    private static void freezeBreath(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.muzzle(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            boolean themed = HeroFx.thematic() && HeroFx.beam(level, from.add(dir), from.add(dir.scale(9.0D)), "pyre_cone", FROST);
            if (themed) {
                HeroFx.vfx(level, from.add(dir.scale(6.0D)), "powergirl_frost_mist", 0);
            } else {
                for (int i = -2; i <= 2; i++) {
                    Vec3 spread = HeroKit.rotate(dir, i * 10.0D);
                    HeroKit.line(level, from.add(spread), from.add(spread.scale(9.0D)), ParticleTypes.SNOWFLAKE, 0.8D);
                }
            }
            HeroKit.cue(level, from, "thematic:frost_breath", SoundEvents.POWDER_SNOW_BREAK, 1.0F, 0.8F);
            for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 9.0D, 30.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), from, 0.6D, 0.05D);
                freeze(victim, 80);
            }
        });
    }

    private static void opticBlast(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 eye = HeroKit.muzzle(mob);
        level.sendParticles(HeroAbilities.emissive(OPTIC & 0xFFFFFF, 0.9F), eye.x, eye.y, eye.z, 8, 0.2D, 0.05D, 0.2D, 0.0D);
        HeroKit.ray(level, mob, target, 8, 28.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroFx.beam(lvl, from, to, "pyre_ray", OPTIC, () -> HeroKit.line(lvl, from, to, HeroAbilities.emissive(OPTIC & 0xFFFFFF, 1.4F), 0.3D));
                    HeroKit.cue(lvl, from, "marvel:suit.cyclops.optic_blast", SoundEvents.BLAZE_SHOOT, 1.2F, 1.0F);
                    HeroFx.cue(lvl, from, "thematic:optic_blast", 0.9F, 1.0F);
                },
                (victim, from) -> HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.8D), from, 1.6D, 0.4D),
                end -> {
                    HeroFx.vfx(level, end, "impact_starburst", OPTIC, () -> level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D));
                    HeroKit.cue(level, end, "marvel:suit.cyclops.optic_blast_explosion", SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 1.2F);
                });
    }

    private static void opticSweep(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.cue(level, mob.position(), "marvel:suit.cyclops.optic_blast_big", SoundEvents.BLAZE_SHOOT, 1.2F, 0.9F);
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.muzzle(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            Set<Integer> struck = new HashSet<>();
            for (int i = -1; i <= 1; i++) {
                Vec3 to = HeroKit.clip(level, mob, from, from.add(HeroKit.rotate(dir, i * 15.0D).scale(16.0D)));
                HeroKit.line(level, from, to, HeroAbilities.emissive(OPTIC & 0xFFFFFF, 1.6F), 0.3D);
                for (LivingEntity victim : HeroKit.along(level, mob, from, to, 0.8D)) {
                    if (struck.add(victim.getId())) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.2D), from, 1.4D, 0.3D);
                    }
                }
                HeroFx.vfx(level, to, "impact_starburst", OPTIC);
            }
        });
    }

    private static void thunder(ServerLevel level, Mob mob, LivingEntity target, int bolts) {
        HeroKit.face(mob, target);
        for (int i = 0; i < bolts; i++) {
            int index = i;
            Vec3 mark = index == 0 ? target.position()
                    : target.position().add((level.random.nextDouble() - 0.5D) * 6.0D, 0.0D, (level.random.nextDouble() - 0.5D) * 6.0D);
            VillainWorld.later(level, index * 8, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 at = index == 0 || !target.isAlive() ? mark : mark.add(target.position().subtract(mark).scale(0.5D));
                VillainFx.ring(level, at, 1.8D, VillainFx.CYAN);
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 0.2D, at.z, 12, 0.8D, 0.1D, 0.8D, 0.05D);
                HeroKit.sound(level, at, SoundEvents.BEACON_POWER_SELECT, 0.6F, 1.8F);
                VillainWorld.later(level, 16, () -> {
                    if (mob.isAlive()) {
                        HeroKit.lightning(level, mob, at, 2.0D, HeroKit.damage(mob, 1.8D));
                    }
                });
            });
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 15, 0.4D, 0.4D, 0.4D, 0.1D);
    }

    private static void gust(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.face(mob, target);
        if (!HeroFx.vfx(level, from.add(dir.scale(3.0D)), "gust_tornado", 0)) {
            for (int i = -1; i <= 1; i++) {
                Vec3 spread = HeroKit.rotate(dir, i * 12.0D);
                HeroKit.line(level, from.add(spread), from.add(spread.scale(10.0D)), ParticleTypes.CLOUD, 0.7D);
            }
        }
        HeroKit.cue(level, from, "minecraft:entity.breeze.wind_burst", SoundEvents.PHANTOM_FLAP, 1.2F, 0.8F);
        for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 10.0D, 35.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.5D), from, 2.2D, 0.6D);
        }
    }

    private static void fireballs(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 3; i++) {
            VillainWorld.later(level, i * 5, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
                HeroKit.sound(level, from, SoundEvents.BLAZE_SHOOT, 0.9F, 1.2F);
                HeroKit.bolt(level, mob, from.add(dir), dir.scale(1.2D), 30, 0.5D, null, 0.0D,
                        (lvl, a, b) -> {
                            HeroKit.line(lvl, a, b, ParticleTypes.FLAME, 0.3D);
                            lvl.sendParticles(ParticleTypes.SMOKE, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                        },
                        (victim, at) -> {
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.9D), at, 0.4D, 0.1D);
                            burn(victim, 4.0F);
                        },
                        end -> HeroFx.vfx(level, end, "ember_flare", 0, () -> level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 12, 0.2D, 0.2D, 0.2D, 0.05D)));
            });
        }
    }

    private static void nova(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 30, 1.5D, 1.0D, 1.5D, -0.05D);
        HeroFx.cue(mob, "thematic:ignite", 1.0F, 0.9F);
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = HeroKit.chest(mob);
            if (HeroFx.vfx(level, center, "supernova_bubble", 0, 2.5F)) {
                HeroFx.vfx(level, center, "ignition_burst", 0, 2.0F);
            } else {
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 80, 0.3D, 0.3D, 0.3D, 0.35D);
                level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 2, 0.6D, 0.4D, 0.6D, 0.0D);
            }
            HeroKit.sound(level, center, SoundEvents.FIRECHARGE_USE, 1.2F, 0.6F);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 5.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.4D), center, 1.2D, 0.4D);
                burn(victim, 5.0F);
            }
        });
    }

    private static void icePrison(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.ray(level, mob, target, 6, 16.0D, 0.6D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, ParticleTypes.SNOWFLAKE, 0.3D);
                    HeroKit.line(lvl, from, to, new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.PACKED_ICE)), 0.8D);
                    HeroKit.cue(lvl, from, "iceandfire:icedragon_breath", SoundEvents.POWDER_SNOW_STEP, 0.9F, 1.4F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), from, 0.0D, 0.0D);
                    freeze(victim, 70);
                    VillainWorld.stick(victim, 40);
                    Vec3 center = HeroKit.chest(victim);
                    HeroFx.vfx(level, center, "powergirl_ice_shatter", 0, () -> level.sendParticles(
                            new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), center.x, center.y, center.z, 30, 0.4D, 0.6D, 0.4D, 0.1D));
                    HeroKit.sound(level, center, SoundEvents.GLASS_BREAK, 1.0F, 0.6F);
                },
                null);
    }

    private static ParticleOptions magnetic(int index) {
        return HeroFx.particle(index % 2 == 0 ? "alexscaves:scarlet_magnetic_flow" : "alexscaves:azure_magnetic_flow", ParticleTypes.ENCHANTED_HIT);
    }

    private static void magnetPull(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 hand = HeroKit.hand(mob);
        Vec3 from = HeroKit.chest(target);
        for (int i = 0; i < 16; i++) {
            level.sendParticles(magnetic(i), from.x + (level.random.nextDouble() - 0.5D), from.y + (level.random.nextDouble() - 0.5D),
                    from.z + (level.random.nextDouble() - 0.5D), 0, hand.x, hand.y, hand.z, 1.0D);
        }
        HeroKit.line(level, hand, from, ParticleTypes.ENCHANTED_HIT, 0.6D);
        HeroKit.cue(level, hand, "alexscaves:magnetron_assemble", SoundEvents.ANVIL_USE, 1.0F, 1.2F);
        boolean metal = com.craisinlord.integrated_superheroes.compat.ModCompat.ALEXS_CAVES
                && com.craisinlord.integrated_superheroes.hero.MagnetoPower.isMetal(target);
        Vec3 pull = mob.position().subtract(target.position());
        double strength = metal ? 1.8D : 1.0D;
        if (HeroAbilities.canControl(target)) {
            Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
            target.setDeltaMovement(dir.x * strength, 0.35D, dir.z * strength);
            target.hurtMarked = true;
        }
        VillainWorld.later(level, 8, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.5D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, metal ? 1.9D : 1.4D), mob.position(), 1.2D, 0.4D);
            HeroFx.vfx(level, HeroKit.chest(target), "punch_pressure_wave", 0);
        });
    }

    private static void lift(ServerLevel level, Mob mob, LivingEntity target, boolean magnetic) {
        if (!HeroAbilities.canControl(target)) {
            return;
        }
        HeroKit.face(mob, target);
        HeroKit.sound(level, target.position(), magnetic ? HeroFx.sound("alexscaves:galena_gauntlet_use_loop", SoundEvents.BEACON_AMBIENT)
                : SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, magnetic ? 1.0F : 1.3F);
        for (int t = 0; t <= 14; t += 2) {
            int tick = t;
            VillainWorld.later(level, tick, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                    return;
                }
                target.setDeltaMovement(target.getDeltaMovement().x * 0.3D, tick < 8 ? 0.45D : 0.02D, target.getDeltaMovement().z * 0.3D);
                target.hurtMarked = true;
                target.fallDistance = 0.0F;
                Vec3 center = HeroKit.chest(target);
                if (magnetic) {
                    com.craisinlord.integrated_superheroes.hero.MagnetoPower.orbitAround(level, center, 3);
                    level.sendParticles(ParticleTypes.ENCHANTED_HIT, center.x, center.y, center.z, 4, 0.5D, 0.6D, 0.5D, 0.05D);
                } else {
                    level.sendParticles(TK_DUST, center.x, center.y, center.z, 6, 0.5D, 0.7D, 0.5D, 0.0D);
                    level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z, 6, 0.5D, 0.7D, 0.5D, 0.3D);
                }
            });
        }
        VillainWorld.later(level, 16, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                return;
            }
            if (magnetic) {
                target.setDeltaMovement(0.0D, -2.2D, 0.0D);
            } else {
                Vec3 away = HeroKit.toward(mob, target);
                target.setDeltaMovement(away.x * 2.0D, 0.3D, away.z * 2.0D);
            }
            target.hurtMarked = true;
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.6D), mob.position(), 0.0D, 0.0D);
            HeroKit.sound(level, target.position(), SoundEvents.ANVIL_LAND, 0.8F, magnetic ? 0.6F : 1.2F);
        });
    }

    private static void phoenix(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 24, 1.0D, 1.0D, 1.0D, -0.04D);
        HeroFx.cue(mob, "thematic:ignite", 1.0F, 0.8F);
        VillainWorld.later(level, 14, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = HeroKit.chest(mob);
            if (HeroFx.vfx(level, center, "pyre_detonation", 0, 2.5F)) {
                HeroFx.vfx(level, mob.position(), "pyre_pillar", 0);
            } else {
                level.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z, 70, 0.3D, 0.3D, 0.3D, 0.3D);
                level.sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 12, 1.0D, 0.5D, 1.0D, 0.0D);
            }
            HeroKit.sound(level, center, SoundEvents.BLAZE_SHOOT, 1.4F, 0.5F);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 6.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), center, 1.0D, 0.5D);
                burn(victim, 6.0F);
            }
        });
    }

    private static void bamfFx(ServerLevel level, Vec3 at) {
        ParticleOptions smoke = HeroFx.particle("thematic:thick_smoke", ParticleTypes.LARGE_SMOKE);
        level.sendParticles(smoke, at.x, at.y + 1.0D, at.z, 6, 0.3D, 0.5D, 0.3D, 0.02D);
        level.sendParticles(BAMF_DUST, at.x, at.y + 1.0D, at.z, 8, 0.3D, 0.5D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 1.0D, at.z, 4, 0.3D, 0.5D, 0.3D, 0.03D);
        HeroKit.sound(level, at, SoundEvents.ENDERMAN_TELEPORT, 0.9F, 1.3F);
        HeroKit.sound(level, at, SoundEvents.FIRE_EXTINGUISH, 0.35F, 1.6F);
    }

    private static void bamf(ServerLevel level, Mob mob, LivingEntity target, int hits) {
        for (int i = 0; i < hits; i++) {
            int index = i;
            VillainWorld.later(level, i * 9, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                    return;
                }
                Vec3 from = mob.position();
                Vec3 to = index == 0 ? HeroKit.behind(target, 1.6D)
                        : target.position().add(HeroKit.rotate(new Vec3(1.6D, 0.0D, 0.0D), level.random.nextInt(360)));
                if (!HeroKit.blink(level, mob, to)) {
                    return;
                }
                bamfFx(level, from);
                bamfFx(level, mob.position());
                HeroKit.face(mob, target);
                VillainWorld.later(level, 3, () -> {
                    if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.0D || !HeroKit.foe(mob, target)) {
                        return;
                    }
                    mob.swing(InteractionHand.MAIN_HAND);
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, hits > 1 ? 0.9D : 1.4D), mob.position(), 0.6D, 0.25D);
                    HeroKit.sound(level, target.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                });
            });
        }
    }

    private static void speedTrail(ServerLevel level, Vec3 from, Vec3 to) {
        ParticleOptions streak = HeroFx.particle("marvel:quicksilver", ParticleTypes.CLOUD);
        HeroKit.line(level, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D), streak, 0.5D);
    }

    private static void blitz(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.cue(level, mob.position(), "marvel:suit.quicksilver.sonic_boom", SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
        for (int i = 0; i < 5; i++) {
            VillainWorld.later(level, i * 2, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                    return;
                }
                Vec3 from = mob.position();
                Vec3 to = target.position().add(HeroKit.rotate(new Vec3(1.4D, 0.0D, 0.0D), level.random.nextInt(360)));
                if (HeroKit.blink(level, mob, to)) {
                    speedTrail(level, from, mob.position());
                }
                HeroKit.face(mob, target);
                if (mob.distanceTo(target) <= 3.0D) {
                    mob.swing(InteractionHand.MAIN_HAND);
                    target.invulnerableTime = 0;
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.55D), mob.position(), 0.2D, 0.05D);
                }
            });
        }
    }

    private static void speedDash(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        Vec3 from = mob.position();
        Vec3 to = HeroKit.clip(level, mob, from.add(0.0D, 0.5D, 0.0D), target.position().add(dir.scale(4.0D)).add(0.0D, 0.5D, 0.0D)).subtract(0.0D, 0.5D, 0.0D);
        HeroKit.cue(level, from, "marvel:suit.quicksilver.sonic_boom", SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.2F);
        for (LivingEntity victim : HeroKit.along(level, mob, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D), 1.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.3D), from, 1.2D, 0.4D);
        }
        if (HeroKit.blink(level, mob, to)) {
            speedTrail(level, from, mob.position());
            HeroFx.vfx(level, mob.position().add(0.0D, 1.0D, 0.0D), "air_burst_ring", 0);
        }
    }

    private static void smash(ServerLevel level, Mob mob, double radius, double scale, int color, String wave) {
        mob.setDeltaMovement(mob.getDeltaMovement().x * 0.2D, 0.6D, mob.getDeltaMovement().z * 0.2D);
        mob.hurtMarked = true;
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = mob.position();
            boolean themed = HeroFx.vfx(level, center, wave, color, (float) radius);
            if (themed) {
                HeroFx.vfx(level, center, radius > 5.0D ? "slam_dust_large" : "slam_dust", 0, (float) radius);
            } else {
                level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z, 3, radius * 0.3D, 0.1D, radius * 0.3D, 0.0D);
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(mob.blockPosition().below())), center.x, center.y + 0.2D,
                        center.z, 60, radius * 0.4D, 0.2D, radius * 0.4D, 0.2D);
            }
            if (color == GAMMA) {
                HeroFx.cue(mob, "thematic:thunder_clap", 1.0F, 0.7F);
            }
            HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.8F);
            HeroFx.shake(level, center, radius * 3.0D, 0.6F, 12);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, radius)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), center, 1.5D, 0.7D);
            }
        });
    }

    private static void gammaLeap(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 path = target.position().subtract(mob.position());
        Vec3 flat = path.multiply(1.0D, 0.0D, 1.0D);
        double distance = Math.min(flat.length(), 20.0D);
        Vec3 dir = flat.lengthSqr() < 1.0E-4D ? Vec3.ZERO : flat.normalize();
        mob.setDeltaMovement(dir.x * distance * 0.09D, 1.0D, dir.z * distance * 0.09D);
        mob.hurtMarked = true;
        HeroKit.sound(level, mob.position(), SoundEvents.RAVAGER_ROAR, 0.8F, 1.1F);
        VillainWorld.later(level, 6, () -> land(level, mob, 0));
    }

    private static void land(ServerLevel level, Mob mob, int waited) {
        if (!mob.isAlive()) {
            return;
        }
        if (!mob.onGround() && waited < 40) {
            VillainWorld.later(level, 2, () -> land(level, mob, waited + 2));
            return;
        }
        mob.fallDistance = 0.0F;
        Vec3 center = mob.position();
        if (HeroFx.vfx(level, center, "slam_fracture_large", 0, 4.5F)) {
            HeroFx.vfx(level, center, "slam_dust_large", 0, 4.5F);
        } else {
            level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y + 0.2D, center.z, 3, 1.2D, 0.1D, 1.2D, 0.0D);
        }
        HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 0.7F);
        HeroFx.shake(level, center, 16.0D, 0.6F, 12);
        for (LivingEntity victim : HeroKit.victims(level, mob, center, 4.5D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.8D), center, 1.4D, 0.6D);
        }
    }

    private static void thunderClap(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        if (!HeroFx.vfx(level, from.add(dir), "air_burst_ring", 0)) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, from.x + dir.x * 2.0D, from.y, from.z + dir.z * 2.0D, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        level.sendParticles(ParticleTypes.CLOUD, from.x + dir.x * 3.0D, from.y, from.z + dir.z * 3.0D, 20, 1.0D, 0.6D, 1.0D, 0.1D);
        HeroKit.cue(level, from, "thematic:thunder_clap", SoundEvents.LIGHTNING_BOLT_THUNDER, 1.2F, 1.4F);
        for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 9.0D, 40.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), from, 2.2D, 0.5D);
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), mob);
        }
        HeroFx.shake(level, from, 12.0D, 0.4F, 8);
    }

    private static void roar(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.cue(level, mob.position(), "thematic:hulk_roar", SoundEvents.RAVAGER_ROAR, 1.4F, 0.8F);
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y, at.z, 30, 1.5D, 0.5D, 1.5D, 0.1D);
        HeroFx.shake(level, at, 14.0D, 0.5F, 20);
        for (LivingEntity victim : HeroKit.victims(level, mob, at, 10.0D)) {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), mob);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), mob);
        }
    }

    private static void charge(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.RAVAGER_ROAR, 1.2F, 0.7F);
        Set<Integer> struck = new HashSet<>();
        for (int t = 0; t < 16; t += 2) {
            VillainWorld.later(level, t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                mob.setDeltaMovement(dir.x * 0.9D, mob.getDeltaMovement().y, dir.z * 0.9D);
                mob.hurtMarked = true;
                Vec3 at = mob.position();
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(mob.blockPosition().below())), at.x, at.y + 0.1D, at.z,
                        6, 0.4D, 0.1D, 0.4D, 0.1D);
                for (LivingEntity victim : HeroKit.victims(level, mob, HeroKit.chest(mob), 1.8D)) {
                    if (struck.add(victim.getId())) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.8D), at, 2.2D, 0.6D);
                        HeroKit.sound(level, at, SoundEvents.IRON_GOLEM_ATTACK, 1.0F, 0.7F);
                        HeroFx.vfx(level, HeroKit.chest(victim), "impact_starburst", 0);
                    }
                }
            });
        }
    }

    private static void energyBolt(ServerLevel level, Mob mob, LivingEntity target, int color, float size, double speed, double scale,
            String sound, int vfxColor) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.cue(level, from, sound, SoundEvents.BLAZE_SHOOT, 1.0F, 1.0F);
        ParticleOptions glow = HeroAbilities.emissive(color & 0xFFFFFF, size);
        HeroKit.bolt(level, mob, from.add(dir), dir.scale(speed), 30, 0.6D, null, 0.0D,
                (lvl, a, b) -> {
                    HeroKit.line(lvl, a, b, glow, 0.3D);
                    lvl.sendParticles(ParticleTypes.END_ROD, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D);
                },
                (victim, at) -> HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, 1.0D, 0.25D),
                end -> HeroFx.vfx(level, end, "impact_starburst", vfxColor, () -> level.sendParticles(ParticleTypes.FLASH, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D)));
    }

    private static void photon(ServerLevel level, Mob mob, LivingEntity target) {
        energyBolt(level, mob, target, PHOTON, 1.3F, 1.5D, 1.6D, "marvel:suit.captain_marvel.photon_blast", PHOTON);
    }

    private static void cosmic(ServerLevel level, Mob mob, LivingEntity target) {
        energyBolt(level, mob, target, 0xFFE8F4FF, 1.2F, 1.7D, 1.7D, "minecraft:block.beacon.power_select", 0xFFFFFFFF);
    }

    private static void binaryNova(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(HeroAbilities.emissive(PHOTON & 0xFFFFFF, 1.0F), at.x, at.y, at.z, 30, 1.2D, 1.2D, 1.2D, 0.0D);
        HeroKit.cue(level, at, "marvel:suit.captain_marvel.photon_blast_loop", SoundEvents.BEACON_ACTIVATE, 1.0F, 1.2F);
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = HeroKit.chest(mob);
            HeroFx.vfx(level, mob.position(), "powergirl_slam_wave", PHOTON, () -> {
                level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 50, 0.3D, 0.3D, 0.3D, 0.4D);
            });
            HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.4F);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 5.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D), center, 1.4D, 0.5D);
            }
        });
    }

    private static void hex(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.sound(level, from, SoundEvents.EVOKER_CAST_SPELL, 1.0F, 1.2F);
        HeroKit.bolt(level, mob, from.add(dir), dir.scale(0.9D), 45, 0.6D, target, 0.25D,
                (lvl, a, b) -> {
                    HeroKit.line(lvl, a, b, HEX_DUST, 0.25D);
                    lvl.sendParticles(ParticleTypes.WITCH, b.x, b.y, b.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
                },
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.2D), at, 0.3D, 0.1D);
                    if (HeroAbilities.canControl(victim)) {
                        victim.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 30, 0), mob);
                    }
                    victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), mob);
                },
                end -> HeroFx.vfx(level, end, "void_twist", 0, () -> level.sendParticles(ParticleTypes.WITCH, end.x, end.y, end.z, 20, 0.3D, 0.3D, 0.3D, 0.1D)));
    }

    private static void hexStorm(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = HeroKit.chest(target);
        if (!HeroFx.vfx(level, center, "void_maelstrom", 0)) {
            level.sendParticles(HEX_DUST, center.x, center.y, center.z, 40, 2.0D, 0.8D, 2.0D, 0.0D);
            level.sendParticles(ParticleTypes.WITCH, center.x, center.y, center.z, 30, 2.0D, 0.8D, 2.0D, 0.1D);
        }
        HeroKit.sound(level, center, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2F, 0.7F);
        for (int t = 0; t < 12; t += 3) {
            boolean last = t == 9;
            VillainWorld.later(level, t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                for (LivingEntity victim : HeroKit.victims(level, mob, center, 4.5D)) {
                    Vec3 pull = center.subtract(victim.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (pull.lengthSqr() > 0.25D && HeroAbilities.canControl(victim)) {
                        Vec3 dir = pull.normalize().scale(0.35D);
                        victim.push(dir.x, 0.05D, dir.z);
                        victim.hurtMarked = true;
                    }
                    if (last) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), center, 0.0D, 0.4D);
                    }
                }
            });
        }
    }

    private static void cards(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        ParticleOptions charge = KineticBurst.blast(KineticBurst.PINK);
        HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.8F);
        for (int i = -1; i <= 1; i++) {
            Vec3 spread = HeroKit.rotate(dir, i * 8.0D);
            HeroKit.bolt(level, mob, from.add(spread), spread.scale(1.3D), 25, 0.5D, null, 0.0D,
                    (lvl, a, b) -> {
                        HeroKit.line(lvl, a, b, HeroAbilities.emissive(0xFF40C0, 0.6F), 0.35D);
                        lvl.sendParticles(charge, b.x, b.y, b.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    },
                    (victim, at) -> {
                    },
                    end -> {
                        level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                        level.sendParticles(ParticleTypes.WITCH, end.x, end.y, end.z, 10, 0.4D, 0.4D, 0.4D, 0.1D);
                        HeroKit.sound(level, end, SoundEvents.FIREWORK_ROCKET_BLAST, 0.8F, 0.9F);
                        for (LivingEntity victim : HeroKit.victims(level, mob, end, 1.8D)) {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), end, 0.6D, 0.2D);
                        }
                    });
        }
    }

    private static void scream(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        VillainWorld.later(level, 5, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.muzzle(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            if (!HeroFx.vfx(level, from.add(dir.scale(2.0D)), "sonic_scream", 0)) {
                for (int i = 1; i <= 4; i++) {
                    Vec3 at = from.add(dir.scale(i * 2.5D));
                    level.sendParticles(ParticleTypes.SONIC_BOOM, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
            }
            HeroKit.cue(level, from, "thematic:canary_cry", SoundEvents.WARDEN_SONIC_BOOM, 1.2F, 1.6F);
            for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 12.0D, 30.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), from, 2.0D, 0.4D);
                victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0), mob);
            }
        });
    }

    private static void psychicBlast(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 at = target.position();
        HeroFx.vfx(level, at.add(0.0D, 0.1D, 0.0D), "circle_ring", 0xFFB070FF, 1.5F);
        level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 2.2D, at.z, 20, 0.4D, 0.2D, 0.4D, 0.5D);
        HeroKit.sound(level, at, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.0F, 1.2F);
        VillainWorld.later(level, 12, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 24.0D) {
                return;
            }
            Vec3 head = target.getEyePosition();
            level.sendParticles(TK_DUST, head.x, head.y, head.z, 20, 0.3D, 0.3D, 0.3D, 0.0D);
            HeroKit.line(level, HeroKit.muzzle(mob), head, ParticleTypes.ENCHANT, 0.5D);
            HeroKit.sound(level, head, SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 1.6F);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.3D), mob.position(), 0.0D, 0.0D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), mob);
            target.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0), mob);
        });
    }

    private static void psychicWave(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = mob.position();
        if (!HeroFx.vfx(level, center.add(0.0D, 0.1D, 0.0D), "circle_ring", 0xFFB070FF, 6.0F)) {
            VillainFx.ring(level, center, 6.0D, VillainFx.PURPLE);
        }
        level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 1.0D, center.z, 40, 2.0D, 0.6D, 2.0D, 0.6D);
        HeroKit.sound(level, center, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2F, 0.8F);
        for (LivingEntity victim : HeroKit.victims(level, mob, center, 8.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), center, 1.4D, 0.3D);
            victim.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), mob);
            victim.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 100, 1), mob);
        }
    }

    private static void forceWave(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        Vec3 to = HeroKit.clip(level, mob, from, from.add(dir.scale(12.0D)));
        if (!HeroFx.beam(level, mob.position(), mob.position().add(dir.scale(12.0D)), "invisible_woman/force_wave", 0)) {
            HeroKit.line(level, from, to, FORCE_DUST, 0.3D);
            level.sendParticles(ParticleTypes.END_ROD, to.x, to.y, to.z, 10, 0.4D, 0.4D, 0.4D, 0.05D);
        }
        HeroKit.sound(level, from, SoundEvents.BEACON_DEACTIVATE, 1.0F, 1.6F);
        for (LivingEntity victim : HeroKit.along(level, mob, from, to, 1.3D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.9D), from, 2.2D, 0.5D);
        }
    }

    private static void vanish(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(FORCE_DUST, at.x, at.y, at.z, 20, 0.4D, 0.8D, 0.4D, 0.0D);
        HeroKit.sound(level, at, SoundEvents.ILLUSIONER_MIRROR_MOVE, 1.0F, 1.2F);
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 80, 0, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 80, 1, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false));
    }

    private static void stretch(ServerLevel level, Mob mob, LivingEntity target, boolean grab) {
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.ray(level, mob, target, 4, grab ? 12.0D : 9.0D, 0.8D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, HeroKit.hand(mob), to, ARM_DUST, 0.2D);
                    HeroKit.sound(lvl, from, SoundEvents.SLIME_JUMP, 1.0F, 0.6F);
                },
                (victim, from) -> {
                    if (grab) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), from, 0.0D, 0.0D);
                        if (HeroAbilities.canControl(victim)) {
                            Vec3 pull = mob.position().subtract(victim.position());
                            Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
                            victim.setDeltaMovement(dir.x * 1.4D, 0.3D, dir.z * 1.4D);
                            victim.hurtMarked = true;
                        }
                    } else {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.4D), from, 1.6D, 0.35D);
                        HeroFx.vfx(level, HeroKit.chest(victim), "punch_pressure_wave", 0);
                        HeroKit.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.8F);
                    }
                },
                null);
    }

    private static void phase(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = mob.position();
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 14, 0, false, false));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 24, 3, false, false));
        level.sendParticles(FORCE_DUST, from.x, from.y + 1.0D, from.z, 16, 0.3D, 0.7D, 0.3D, 0.0D);
        HeroKit.sound(level, from, SoundEvents.ILLUSIONER_MIRROR_MOVE, 0.8F, 1.5F);
        VillainWorld.later(level, 8, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.blink(level, mob, HeroKit.behind(target, 1.4D))) {
                return;
            }
            Vec3 at = mob.position();
            level.sendParticles(FORCE_DUST, at.x, at.y + 1.0D, at.z, 16, 0.3D, 0.7D, 0.3D, 0.0D);
            HeroKit.face(mob, target);
            VillainWorld.later(level, 4, () -> {
                if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.0D || !HeroKit.foe(mob, target)) {
                    return;
                }
                mob.swing(InteractionHand.MAIN_HAND);
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.4D), mob.position(), 0.8D, 0.3D);
            });
        });
    }

    private static void drain(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        if (mob.distanceTo(target) > 2.5D) {
            mob.setDeltaMovement(dir.x * 1.1D, 0.3D, dir.z * 1.1D);
            mob.hurtMarked = true;
        }
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.0D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            float amount = HeroKit.damage(mob, 1.3D);
            if (HeroKit.hit(level, mob, target, amount, mob.position(), 0.3D, 0.1D)) {
                mob.heal(amount * 0.6F);
            }
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0), mob);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), mob);
            HeroKit.line(level, HeroKit.chest(target), HeroKit.chest(mob), ParticleTypes.SCULK_SOUL, 0.4D);
            HeroKit.sound(level, target.position(), SoundEvents.SOUL_ESCAPE.value(), 1.2F, 0.8F);
        });
    }

    private static void plasmoids(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 center = target.position();
        for (int i = 0; i < 6; i++) {
            VillainWorld.later(level, i * 2, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 at = center.add((level.random.nextDouble() - 0.5D) * 4.0D, 0.5D + level.random.nextDouble() * 1.5D, (level.random.nextDouble() - 0.5D) * 4.0D);
                DustParticleOptions spark = new DustParticleOptions(new Vector3f(level.random.nextFloat(), level.random.nextFloat(), level.random.nextFloat()), 1.4F);
                level.sendParticles(spark, at.x, at.y, at.z, 14, 0.4D, 0.4D, 0.4D, 0.0D);
                level.sendParticles(ParticleTypes.FIREWORK, at.x, at.y, at.z, 8, 0.1D, 0.1D, 0.1D, 0.15D);
                HeroKit.sound(level, at, SoundEvents.FIREWORK_ROCKET_TWINKLE, 0.7F, 1.2F);
                for (LivingEntity victim : HeroKit.victims(level, mob, at, 1.6D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.45D), at, 0.3D, 0.1D);
                    victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0), mob);
                }
            });
        }
    }

    private static void tidal(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.face(mob, target);
        if (!HeroFx.vfx(level, mob.position(), "whirlpool", 0)) {
            level.sendParticles(ParticleTypes.SPLASH, mob.getX(), mob.getY() + 0.5D, mob.getZ(), 40, 1.5D, 0.3D, 1.5D, 0.2D);
        }
        for (int i = -1; i <= 1; i++) {
            Vec3 spread = HeroKit.rotate(dir, i * 15.0D);
            HeroKit.line(level, from.add(spread), from.add(spread.scale(10.0D)), ParticleTypes.BUBBLE, 0.5D);
            HeroKit.line(level, from.add(spread), from.add(spread.scale(10.0D)), ParticleTypes.SPLASH, 0.4D);
        }
        HeroKit.sound(level, from, SoundEvents.GENERIC_SPLASH, 1.2F, 0.7F);
        for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 10.0D, 35.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), from, 1.8D, 0.5D);
            victim.clearFire();
        }
    }

    private static void lasso(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.ray(level, mob, target, 4, 14.0D, 0.7D, false,
                (lvl, from, to) -> {
                    Vec3 hand = HeroKit.hand(mob);
                    if (!HeroFx.beam(lvl, hand, to, "harpoon_beam", LASSO)) {
                        HeroKit.line(lvl, hand, to, HeroAbilities.emissive(LASSO & 0xFFFFFF, 0.7F), 0.25D);
                    }
                    HeroKit.sound(lvl, from, SoundEvents.FISHING_BOBBER_THROW, 1.0F, 0.7F);
                },
                (victim, from) -> {
                    if (HeroAbilities.canControl(victim)) {
                        Vec3 pull = mob.position().subtract(victim.position());
                        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
                        victim.setDeltaMovement(dir.x * 1.5D, 0.35D, dir.z * 1.5D);
                        victim.hurtMarked = true;
                    }
                    VillainWorld.later(level, 8, () -> {
                        if (HeroKit.gone(mob, victim) || mob.distanceTo(victim) > 3.5D || !HeroKit.foe(mob, victim)) {
                            return;
                        }
                        mob.swing(InteractionHand.MAIN_HAND);
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D), mob.position(), 1.4D, 0.5D);
                        HeroFx.vfx(level, HeroKit.chest(victim), "impact_starburst", 0);
                        HeroFx.cue(level, victim.position(), "thematic:thunder_clap", 0.8F, 1.2F);
                    });
                },
                null);
    }

    private static void hellfire(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.muzzle(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.sound(level, from, SoundEvents.BLAZE_SHOOT, 1.2F, 0.6F);
        for (int pulse = 0; pulse < 3; pulse++) {
            VillainWorld.later(level, pulse * 4, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 origin = HeroKit.muzzle(mob);
                for (int i = -2; i <= 2; i++) {
                    Vec3 spread = HeroKit.rotate(dir, i * 9.0D);
                    HeroKit.line(level, origin.add(spread), origin.add(spread.scale(9.0D)), ParticleTypes.SOUL_FIRE_FLAME, 0.6D);
                }
                for (LivingEntity victim : HeroKit.cone(level, mob, origin, dir, 9.0D, 25.0D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.5D), origin, 0.2D, 0.0D);
                    burn(victim, 5.0F);
                }
            });
        }
    }

    private static void chain(ServerLevel level, Mob mob, LivingEntity target) {
        ParticleOptions links = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CHAIN));
        HeroKit.ray(level, mob, target, 5, 14.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, HeroKit.hand(mob), to, links, 0.25D);
                    HeroKit.line(lvl, HeroKit.hand(mob), to, ParticleTypes.SOUL_FIRE_FLAME, 0.7D);
                    HeroKit.sound(lvl, from, SoundEvents.CHAIN_PLACE, 1.2F, 0.6F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.9D), from, 0.0D, 0.0D);
                    burn(victim, 3.0F);
                    if (HeroAbilities.canControl(victim)) {
                        Vec3 pull = mob.position().subtract(victim.position());
                        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
                        victim.setDeltaMovement(dir.x * 1.6D, 0.35D, dir.z * 1.6D);
                        victim.hurtMarked = true;
                    }
                },
                null);
    }

    private static void roots(ServerLevel level, Mob mob, LivingEntity target, net.minecraft.world.level.block.state.BlockState block) {
        Vec3 at = target.position();
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, block);
        level.sendParticles(debris, at.x, at.y + 0.1D, at.z, 10, 0.6D, 0.05D, 0.6D, 0.05D);
        HeroKit.sound(level, at, SoundEvents.ROOTED_DIRT_BREAK, 1.0F, 0.7F);
        VillainWorld.later(level, 10, () -> {
            if (!mob.isAlive()) {
                return;
            }
            level.sendParticles(debris, at.x, at.y + 0.6D, at.z, 50, 0.6D, 0.8D, 0.6D, 0.15D);
            HeroKit.sound(level, at, SoundEvents.ROOTED_DIRT_PLACE, 1.4F, 0.5F);
            for (LivingEntity victim : HeroKit.victims(level, mob, at, 2.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), at, 0.0D, 0.3D);
                if (HeroAbilities.canControl(victim)) {
                    VillainWorld.stick(victim, 50);
                }
            }
        });
    }

    private static void webShot(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        ParticleOptions web = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.COBWEB));
        HeroKit.cue(level, from, "marvel:suit.spider_man.web_shoot", SoundEvents.SLIME_SQUISH, 1.0F, 1.2F);
        HeroKit.bolt(level, mob, from.add(dir), dir.scale(1.6D), 20, 0.5D, null, 0.0D,
                (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.WHITE_ASH, 0.25D),
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.4D), at, 0.0D, 0.0D);
                    if (HeroAbilities.canControl(victim)) {
                        VillainWorld.stick(victim, 50);
                    }
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 70, 3), mob);
                },
                end -> level.sendParticles(web, end.x, end.y, end.z, 16, 0.4D, 0.4D, 0.4D, 0.05D));
    }

    private static void webYank(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.ray(level, mob, target, 3, 14.0D, 0.6D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, HeroKit.hand(mob), to, ParticleTypes.WHITE_ASH, 0.15D);
                    HeroKit.cue(lvl, from, "marvel:suit.spider_man.web_shoot", SoundEvents.SLIME_SQUISH, 1.0F, 0.9F);
                },
                (victim, from) -> {
                    if (HeroAbilities.canControl(victim)) {
                        Vec3 pull = mob.position().subtract(victim.position());
                        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
                        victim.setDeltaMovement(dir.x * 1.5D, 0.4D, dir.z * 1.5D);
                        victim.hurtMarked = true;
                    }
                    VillainWorld.later(level, 8, () -> {
                        if (HeroKit.gone(mob, victim) || mob.distanceTo(victim) > 3.5D || !HeroKit.foe(mob, victim)) {
                            return;
                        }
                        mob.swing(InteractionHand.MAIN_HAND);
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.4D), mob.position(), 1.3D, 0.5D);
                    });
                },
                null);
    }

    private static void flurry(ServerLevel level, Mob mob, LivingEntity target, int hits, double scale, boolean claws) {
        HeroKit.face(mob, target);
        if (claws) {
            HeroKit.cue(level, mob.position(), "marvel:suit.wolverine.claws_out", SoundEvents.PLAYER_ATTACK_SWEEP, 0.8F, 1.0F);
        }
        for (int i = 0; i < hits; i++) {
            VillainWorld.later(level, 2 + i * 4, () -> {
                if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.4D || !HeroKit.foe(mob, target)) {
                    return;
                }
                mob.swing(InteractionHand.MAIN_HAND);
                target.invulnerableTime = 0;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), 0.3D, 0.05D);
                Vec3 at = HeroKit.chest(target);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.2D, 0.2D, 0.2D, 0.0D);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.3D, 0.3D, 0.3D, 0.1D);
                HeroKit.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.3F);
            });
        }
    }

    private static void pounce(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        mob.setDeltaMovement(dir.x * 1.2D, 0.55D, dir.z * 1.2D);
        mob.hurtMarked = true;
        HeroKit.sound(level, mob.position(), SoundEvents.WOLF_GROWL, 1.0F, 0.7F);
        VillainWorld.later(level, 8, () -> flurry(level, mob, target, 2, 1.0D, false));
    }

    private static void pistols(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 4; i++) {
            VillainWorld.later(level, 4 + i * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = HeroKit.chest(target).add((level.random.nextDouble() - 0.5D) * 0.6D, (level.random.nextDouble() - 0.5D) * 0.6D,
                        (level.random.nextDouble() - 0.5D) * 0.6D).subtract(from).normalize();
                level.sendParticles(ParticleTypes.SMOKE, from.x + dir.x * 0.6D, from.y, from.z + dir.z * 0.6D, 3, 0.05D, 0.05D, 0.05D, 0.01D);
                HeroKit.sound(level, from, SoundEvents.FIREWORK_ROCKET_BLAST, 0.7F, 1.9F);
                HeroKit.bolt(level, mob, from.add(dir), dir.scale(2.6D), 12, 0.35D, null, 0.0D,
                        (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.CRIT, 0.8D),
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.5D), at, 0.1D, 0.0D);
                        },
                        null);
            });
        }
    }

    private static void kineticBurst(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        KineticBurst.trail(level, at, 20);
        HeroKit.cue(level, at, "marvel:suit.kinetic_black_panther.helmet_up", SoundEvents.AMETHYST_BLOCK_RESONATE, 1.0F, 1.0F);
        VillainWorld.later(level, 10, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = HeroKit.chest(mob);
            KineticBurst.visuals(level, center, 4.0F, 0);
            HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 1.5F);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 4.5D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D), center, 1.5D, 0.5D);
            }
        });
    }

    private static void shieldThrow(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.OFF_HAND);
        ItemStack shield = new ItemStack(HeroFx.item("marvel:vibranium_shield", Items.SHIELD));
        HeroKit.cue(level, mob.position(), "marvel:item.vibranium_shield.throw", SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.0F);
        HeroFx.cue(mob, "thematic:shield_throw", 0.8F, 1.0F);
        HeroKit.hurl(level, mob, target, shield, 1.3D, 1, 45.0F,
                (lvl, a, b) -> lvl.sendParticles(ParticleTypes.CRIT, b.x, b.y, b.z, 1, 0.05D, 0.05D, 0.05D, 0.0D),
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.4D), at, 0.9D, 0.25D);
                    HeroKit.cue(level, victim.position(), "marvel:item.vibranium_shield.hit", SoundEvents.SHIELD_BLOCK, 1.2F, 1.0F);
                    HeroFx.vfx(level, HeroKit.chest(victim), "impact_starburst", 0);
                },
                () -> HeroKit.cue(level, mob.position(), "marvel:item.vibranium_shield.catch", SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F));
    }

    private static void shieldBash(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        mob.setDeltaMovement(dir.x * 1.3D, 0.2D, dir.z * 1.3D);
        mob.hurtMarked = true;
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.0D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(InteractionHand.OFF_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.3D), mob.position(), 2.0D, 0.45D);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 2), mob);
            HeroKit.cue(level, target.position(), "marvel:item.vibranium_shield.hit", SoundEvents.SHIELD_BLOCK, 1.2F, 0.8F);
            HeroFx.vfx(level, HeroKit.chest(target), "impact_starburst", 0);
        });
    }
}
