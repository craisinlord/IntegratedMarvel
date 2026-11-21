package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.villain.VillainFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VoidVariants;
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
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.DcKitFx.dust;
import static com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.DcKitFx.effect;
import static com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.DcKitFx.glow;
import static com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.DcKitFx.self;

public final class DcKitMoves {
    private static final int GREEN = 0xFF3CFF5A;
    private static final int YELLOW = 0xFFFFE030;
    private static final int RED = 0xFFE01010;
    private static final int ORANGE = 0xFFFF8A10;
    private static final int BLUE = 0xFF3080FF;
    private static final int INDIGO = 0xFF6A3DD8;
    private static final int VIOLET = 0xFFE040C8;
    private static final int BLACK = 0xFF3A2A50;
    private static final int WHITE = 0xFFF4F8FF;
    private static final int OMEGA = 0xFFFF2A1A;
    private static final int TRIGON = 0xFFFF3A10;
    private static final int KRYPTO = 0xFF50FF40;
    private static final int ANKH = 0xFFFFC830;
    private static final int SPECTRE = 0xFF50FF90;
    private static final int MANHATTAN = 0xFF40C8FF;
    private static final int PARASITE = 0xFFA040FF;
    private static final int ARC = 0xFF9AD8FF;
    private static final int COLD = 0xFFB8ECFF;
    private static final int FIRE_GREEN = 0xFF60FF30;
    private static final int RAVEN = 0xFF5A1A90;
    private static final int CIRCE = 0xFFB040FF;
    private static final int GOLD = 0xFFFFD040;
    private static final int BEETLE = 0xFF40A0FF;
    private static final int STAFF = 0xFFFFE8A0;
    private static final int ZETA = 0xFFFF6050;
    private static final int MANTA = 0xFFFF2A2A;
    private static final int BRAINIAC = 0xFF70FF70;
    private static final int SPHERE = 0xFFE0E4FF;
    private static final int FEAR = 0xFF9ACD32;
    private static final int PSI = 0xFFB070FF;

    private DcKitMoves() {
    }

    public static void register(Map<String, VariantMoves.Move> map) {
        lantern(map, "green", GREEN);
        lantern(map, "yellow", YELLOW);
        lantern(map, "red", RED);
        lantern(map, "orange", ORANGE);
        lantern(map, "blue", BLUE);
        lantern(map, "indigo", INDIGO);
        lantern(map, "violet", VIOLET);
        lantern(map, "black", BLACK);
        lantern(map, "white", WHITE);
        add(map, "dc_blood_napalm", 0.0D, 9.0D, DcKitMoves::bloodNapalm);
        add(map, "dc_black_lantern_drain", 2.0D, 16.0D, DcKitMoves::blackDrain);
        add(map, "dc_indigo_blink", 2.0D, 16.0D, (level, mob, target) -> DcKitFx.blinkStrike(level, mob, target, glow(INDIGO, 1.2F),
                SoundEvents.ENDERMAN_TELEPORT, 1.5D, (victim, at) -> effect(mob, victim, MobEffects.WEAKNESS, 100, 0)));
        add(map, "dc_avarice_grab", 3.0D, 16.0D, DcKitMoves::avarice);
        add(map, "dc_hope_light", 0.0D, 10.0D, DcKitMoves::hope);
        add(map, "dc_crystal_prison", 2.0D, 16.0D, DcKitMoves::crystalPrison);
        add(map, "dc_white_life_burst", 0.0D, 7.0D, DcKitMoves::lifeBurst);
        add(map, "dc_fear_construct", 2.0D, 16.0D, DcKitMoves::fearConstruct);
        add(map, "dc_omega_beams", 4.0D, 26.0D, DcKitMoves::omegaBeams);
        add(map, "dc_hellfire_eyes", 3.0D, 26.0D, DcKitMoves::hellfireEyes);
        add(map, "dc_demon_quake", 0.0D, 7.0D, DcKitMoves::demonQuake);
        add(map, "dc_war_axe", 4.0D, 20.0D, DcKitMoves::warAxe);
        add(map, "dc_war_cry", 0.0D, 8.0D, DcKitMoves::warCry);
        add(map, "dc_drone_swarm", 3.0D, 22.0D, (level, mob, target) -> drones(level, mob, target, BRAINIAC, 4));
        add(map, "dc_t_spheres", 3.0D, 18.0D, (level, mob, target) -> drones(level, mob, target, SPHERE, 3));
        add(map, "dc_kryptonite_beam", 3.0D, 24.0D, DcKitMoves::kryptoniteBeam);
        add(map, "dc_lobo_hook", 3.0D, 14.0D, DcKitMoves::loboHook);
        add(map, "dc_parasite_siphon", 2.0D, 14.0D, DcKitMoves::siphon);
        add(map, "dc_lightning_arc", 2.0D, 16.0D, DcKitMoves::lightningArc);
        add(map, "dc_backwards_spell", 2.0D, 18.0D, DcKitMoves::backwardsSpell);
        add(map, "dc_ankh_blast", 3.0D, 24.0D, DcKitMoves::ankhBlast);
        add(map, "dc_spectre_judgment", 0.0D, 20.0D, DcKitMoves::judgment);
        add(map, "dc_possess", 0.0D, 10.0D, DcKitMoves::possess);
        add(map, "dc_soul_self", 3.0D, 22.0D, DcKitMoves::soulSelf);
        add(map, "dc_animal_charge", 4.0D, 16.0D, DcKitMoves::animalCharge);
        add(map, "dc_cold_gun", 3.0D, 20.0D, DcKitMoves::coldGun);
        add(map, "dc_ice_shards", 3.0D, 18.0D, DcKitMoves::iceShards);
        add(map, "dc_green_flame", 0.0D, 9.0D, DcKitMoves::greenFlame);
        add(map, "dc_green_fireball", 3.0D, 20.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, FIRE_GREEN, 1.6F, 1.2D, 1.4D, 0.0D,
                SoundEvents.BLAZE_SHOOT, 0.8F, ParticleTypes.FLAME, (victim, at) -> victim.igniteForSeconds(4.0F)));
        add(map, "dc_vine_lash", 3.0D, 14.0D, DcKitMoves::vineLash);
        add(map, "dc_pollen_cloud", 0.0D, 14.0D, DcKitMoves::pollen);
        add(map, "dc_fear_gas", 0.0D, 10.0D, DcKitMoves::fearGas);
        add(map, "dc_telepathic_crush", 0.0D, 18.0D, DcKitMoves::telepathicCrush);
        add(map, "dc_mirror_step", 2.0D, 16.0D, (level, mob, target) -> DcKitFx.blinkStrike(level, mob, target,
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState()), SoundEvents.GLASS_BREAK, 1.4D,
                (victim, at) -> effect(mob, victim, MobEffects.CONFUSION, 60, 0)));
        add(map, "dc_mirror_shards", 3.0D, 16.0D, DcKitMoves::mirrorShards);
        add(map, "dc_venom_surge", 0.0D, 8.0D, DcKitMoves::venomSurge);
        add(map, "dc_backbreaker", 0.0D, 3.5D, DcKitMoves::backbreaker);
        add(map, "dc_shark_bite", 0.0D, 6.0D, (level, mob, target) -> bite(level, mob, target, 1.8D, true));
        add(map, "dc_snake_bite", 0.0D, 6.0D, (level, mob, target) -> bite(level, mob, target, 1.2D, false));
        add(map, "dc_sword_slash", 0.0D, 5.0D, DcKitMoves::swordSlash);
        add(map, "dc_whip_crack", 2.0D, 10.0D, DcKitMoves::whipCrack);
        add(map, "dc_crossbow_volley", 4.0D, 20.0D, DcKitMoves::crossbow);
        add(map, "dc_umbrella_gun", 0.0D, 8.0D, DcKitMoves::umbrellaGun);
        add(map, "dc_coin_flip", 3.0D, 18.0D, DcKitMoves::coinFlip);
        add(map, "dc_quantum_step", 2.0D, 18.0D, (level, mob, target) -> DcKitFx.blinkStrike(level, mob, target, glow(MANHATTAN, 1.2F),
                SoundEvents.BEACON_POWER_SELECT, 1.6D, null));
        add(map, "dc_disintegrate", 3.0D, 26.0D, (level, mob, target) -> DcKitFx.beam(level, mob, target, 14, 26.0D, true, MANHATTAN, 1.6F, 2.0D, 0.8D,
                SoundEvents.BEACON_ACTIVATE, 1.6F, (victim, at) -> effect(mob, victim, MobEffects.GLOWING, 100, 0), null));
        add(map, "dc_mystic_bolt", 3.0D, 22.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, WHITE, 1.2F, 0.9D, 1.3D, 0.22D,
                SoundEvents.EVOKER_CAST_SPELL, 1.3F, ParticleTypes.ENCHANT, (victim, at) -> effect(mob, victim, MobEffects.WEAKNESS, 120, 0)));
        add(map, "dc_circe_curse", 3.0D, 22.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, CIRCE, 1.2F, 0.9D, 1.1D, 0.25D,
                SoundEvents.EVOKER_CAST_SPELL, 0.8F, ParticleTypes.WITCH, (victim, at) -> {
                    effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 100, 2);
                    effect(mob, victim, MobEffects.WEAKNESS, 160, 1);
                    effect(mob, victim, MobEffects.CONFUSION, 100, 0);
                }));
        add(map, "dc_shadow_tendrils", 3.0D, 14.0D, DcKitMoves::shadowTendrils);
        add(map, "dc_cyclone", 0.0D, 16.0D, DcKitMoves::cyclone);
        add(map, "dc_top_spin", 2.0D, 10.0D, DcKitMoves::topSpin);
        add(map, "dc_trick_bomb", 3.0D, 16.0D, DcKitMoves::trickBomb);
        add(map, "dc_zeta_blaster", 3.0D, 22.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, ZETA, 1.0F, 1.8D, 1.4D, 0.0D,
                SoundEvents.BLAZE_SHOOT, 1.6F, ParticleTypes.END_ROD, null));
        add(map, "dc_mega_rod", 3.0D, 20.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, GOLD, 1.5F, 1.4D, 1.6D, 0.0D,
                SoundEvents.BEACON_DEACTIVATE, 1.4F, ParticleTypes.END_ROD, (victim, at) -> {
                    Vec3 away = victim.position().subtract(at).multiply(1.0D, 0.0D, 1.0D);
                    if (away.lengthSqr() > 1.0E-4D && HeroAbilities.canControl(victim)) {
                        Vec3 dir = away.normalize();
                        victim.push(dir.x * 1.6D, 0.5D, dir.z * 1.6D);
                        victim.hurtMarked = true;
                    }
                }));
        add(map, "dc_power_glove", 3.0D, 22.0D, DcKitMoves::powerGlove);
        add(map, "dc_beetle_blaster", 3.0D, 20.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, BEETLE, 0.9F, 1.9D, 1.1D, 0.0D,
                SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, null, (victim, at) -> effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 40, 1)));
        add(map, "dc_beetle_cannon", 4.0D, 24.0D, (level, mob, target) -> DcKitFx.beam(level, mob, target, 12, 24.0D, false, BEETLE, 1.6F, 1.8D, 1.8D,
                SoundEvents.BEACON_ACTIVATE, 1.8F, null, null));
        add(map, "dc_cosmic_staff", 3.0D, 22.0D, (level, mob, target) -> DcKitFx.energy(level, mob, target, STAFF, 1.3F, 1.1D, 1.4D, 0.18D,
                SoundEvents.AMETHYST_BLOCK_CHIME, 1.4F, ParticleTypes.END_ROD, (victim, at) -> effect(mob, victim, MobEffects.GLOWING, 80, 0)));
        add(map, "dc_manta_beams", 3.0D, 22.0D, (level, mob, target) -> DcKitFx.beam(level, mob, target, 8, 22.0D, false, MANTA, 1.1F, 1.5D, 1.2D,
                SoundEvents.BLAZE_SHOOT, 1.4F, null, null));
        add(map, "dc_hammer_throw", 4.0D, 20.0D, DcKitMoves::hammerThrow);
        add(map, "dc_boomerang", 4.0D, 20.0D, DcKitMoves::boomerang);
        add(map, "dc_polka_dots", 3.0D, 18.0D, DcKitMoves::polkaDots);
    }

    private static void add(Map<String, VariantMoves.Move> map, String id, double min, double max, VariantMoves.Runner runner) {
        map.put(id, new VariantMoves.Move(id, min, max, runner));
    }

    private static void lantern(Map<String, VariantMoves.Move> map, String corps, int color) {
        boolean dark = color == BLACK;
        add(map, "dc_lantern_beam_" + corps, 3.0D, 24.0D, (level, mob, target) -> DcKitFx.beam(level, mob, target, 8, 24.0D, false, color, 1.3F, 1.6D, 1.2D,
                SoundEvents.BEACON_POWER_SELECT, 1.4F, dark ? (victim, at) -> effect(mob, victim, MobEffects.WITHER, 60, 0) : null, null));
        add(map, "dc_lantern_fist_" + corps, 3.0D, 18.0D, (level, mob, target) -> constructFist(level, mob, target, color));
    }

    private static void constructFist(ServerLevel level, Mob mob, LivingEntity target, int color) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        level.sendParticles(glow(color, 1.6F), from.x, from.y, from.z, 16, 0.4D, 0.4D, 0.4D, 0.0D);
        HeroKit.sound(level, from, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.6F);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 start = HeroKit.hand(mob);
            Vec3 dir = HeroKit.chest(target).subtract(start).normalize();
            ParticleOptions big = glow(color, 2.4F);
            ParticleOptions small = glow(color, 1.2F);
            HeroKit.bolt(level, mob, start.add(dir), dir.scale(1.1D), 30, 1.0D, target, 0.12D,
                    (lvl, a, b) -> {
                        lvl.sendParticles(big, b.x, b.y, b.z, 6, 0.35D, 0.35D, 0.35D, 0.0D);
                        HeroKit.line(lvl, a, b, small, 0.3D);
                    },
                    (victim, at) -> {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.7D), at, 2.2D, 0.5D);
                        HeroKit.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
                    },
                    end -> HeroFx.vfx(level, end, "punch_pressure_wave", color, () -> level.sendParticles(big, end.x, end.y, end.z, 20, 0.5D, 0.5D, 0.5D, 0.0D)));
        });
    }

    private static void bloodNapalm(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.muzzle(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.sound(level, from, SoundEvents.RAVAGER_ROAR, 1.0F, 1.3F);
        ParticleOptions blood = dust(0xFF8A0000, 1.6F);
        ParticleOptions rage = glow(RED, 1.0F);
        for (int pulse = 0; pulse < 3; pulse++) {
            VillainWorld.later(level, 4 + pulse * 4, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 origin = HeroKit.muzzle(mob);
                for (int i = -2; i <= 2; i++) {
                    Vec3 spread = HeroKit.rotate(dir, i * 9.0D);
                    HeroKit.line(level, origin.add(spread), origin.add(spread.scale(9.0D)), blood, 0.5D);
                    HeroKit.line(level, origin.add(spread), origin.add(spread.scale(9.0D)), rage, 1.0D);
                }
                level.sendParticles(ParticleTypes.FALLING_LAVA, origin.x + dir.x * 4.0D, origin.y, origin.z + dir.z * 4.0D, 8, 1.5D, 0.3D, 1.5D, 0.0D);
                HeroKit.sound(level, origin, SoundEvents.BLAZE_SHOOT, 0.8F, 0.6F);
                for (LivingEntity victim : HeroKit.cone(level, mob, origin, dir, 9.0D, 25.0D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.55D), origin, 0.2D, 0.0D);
                    victim.igniteForSeconds(4.0F);
                    effect(mob, victim, MobEffects.WITHER, 50, 0);
                }
            });
        }
    }

    private static void blackDrain(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.ray(level, mob, target, 8, 16.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, dust(0xFF201030, 1.4F), 0.25D);
                    HeroKit.line(lvl, from, to, ParticleTypes.SOUL, 0.8D);
                    HeroKit.sound(lvl, from, SoundEvents.WITHER_SHOOT, 0.8F, 0.6F);
                },
                (victim, from) -> {
                    float amount = HeroKit.damage(mob, 1.3D);
                    if (HeroKit.hit(level, mob, victim, amount, from, 0.0D, 0.0D)) {
                        mob.heal(amount * 0.5F);
                    }
                    effect(mob, victim, MobEffects.WITHER, 80, 1);
                    effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
                    HeroKit.line(level, HeroKit.chest(victim), HeroKit.chest(mob), ParticleTypes.SCULK_SOUL, 0.4D);
                    HeroKit.sound(level, victim.position(), SoundEvents.SOUL_ESCAPE.value(), 1.2F, 0.6F);
                },
                null);
    }

    private static void avarice(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.ray(level, mob, target, 5, 16.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroFx.beam(lvl, HeroKit.hand(mob), to, "harpoon_beam", ORANGE, () -> HeroKit.line(lvl, HeroKit.hand(mob), to, glow(ORANGE, 1.0F), 0.25D));
                    HeroKit.sound(lvl, from, SoundEvents.BEACON_POWER_SELECT, 1.0F, 0.7F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), from, 0.0D, 0.0D);
                    DcKitFx.pull(mob, victim, 1.4D);
                    List<MobEffectInstance> stolen = new ArrayList<>();
                    for (MobEffectInstance instance : victim.getActiveEffects()) {
                        if (instance.getEffect().value().isBeneficial() && stolen.size() < 2) {
                            stolen.add(new MobEffectInstance(instance));
                        }
                    }
                    for (MobEffectInstance instance : stolen) {
                        victim.removeEffect(instance.getEffect());
                        mob.addEffect(instance);
                    }
                    self(mob, MobEffects.ABSORPTION, 200, 1);
                    level.sendParticles(glow(ORANGE, 1.2F), mob.getX(), mob.getY() + 1.0D, mob.getZ(), 20, 0.5D, 0.8D, 0.5D, 0.0D);
                    HeroKit.sound(level, mob.position(), SoundEvents.ITEM_PICKUP, 1.2F, 0.6F);
                },
                null);
    }

    private static void hope(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = mob.position();
        ParticleOptions light = glow(BLUE, 1.4F);
        if (!HeroFx.vfx(level, center.add(0.0D, 0.1D, 0.0D), "circle_ring", BLUE, 8.0F)) {
            level.sendParticles(light, center.x, center.y + 1.0D, center.z, 40, 3.0D, 0.6D, 3.0D, 0.0D);
        }
        HeroKit.sound(level, center, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.4F);
        mob.heal(mob.getMaxHealth() * 0.15F);
        self(mob, MobEffects.REGENERATION, 100, 1);
        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(8.0D),
                entity -> entity != mob && entity.isAlive() && VoidVariants.allied(mob, entity))) {
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            level.sendParticles(light, ally.getX(), ally.getY() + 1.0D, ally.getZ(), 10, 0.3D, 0.6D, 0.3D, 0.0D);
        }
        for (LivingEntity victim : HeroKit.victims(level, mob, center, 8.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), center, 1.4D, 0.3D);
            effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
        }
    }

    private static void crystalPrison(ServerLevel level, Mob mob, LivingEntity target) {
        BlockParticleOption crystal = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.AMETHYST_BLOCK.defaultBlockState());
        HeroKit.ray(level, mob, target, 8, 16.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroFx.beam(lvl, from, to, "pyre_ray", VIOLET, () -> HeroKit.line(lvl, from, to, glow(VIOLET, 1.2F), 0.3D));
                    HeroKit.sound(lvl, from, SoundEvents.AMETHYST_BLOCK_RESONATE, 1.2F, 1.0F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), from, 0.0D, 0.0D);
                    if (HeroAbilities.canControl(victim)) {
                        VillainWorld.stick(victim, 60);
                    }
                    for (int t = 0; t < 40; t += 8) {
                        VillainWorld.later(level, t, () -> {
                            if (victim.isAlive()) {
                                level.sendParticles(crystal, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 14, 0.45D, 0.8D, 0.45D, 0.0D);
                                level.sendParticles(glow(VIOLET, 1.0F), victim.getX(), victim.getY() + 1.0D, victim.getZ(), 6, 0.5D, 0.8D, 0.5D, 0.0D);
                            }
                        });
                    }
                    VillainWorld.later(level, 40, () -> {
                        if (HeroKit.gone(mob, victim) || !HeroKit.foe(mob, victim)) {
                            return;
                        }
                        victim.invulnerableTime = 0;
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.3D), victim.position(), 0.3D, 0.3D);
                        level.sendParticles(crystal, victim.getX(), victim.getY() + 1.0D, victim.getZ(), 40, 0.5D, 0.8D, 0.5D, 0.2D);
                        HeroKit.sound(level, victim.position(), SoundEvents.AMETHYST_BLOCK_BREAK, 1.4F, 0.8F);
                    });
                },
                null);
    }

    private static void lifeBurst(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(glow(WHITE, 1.2F), at.x, at.y, at.z, 30, 1.5D, 1.5D, 1.5D, 0.0D);
        HeroKit.sound(level, at, SoundEvents.BEACON_ACTIVATE, 1.2F, 1.8F);
        VillainWorld.later(level, 10, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = HeroKit.chest(mob);
            HeroFx.vfx(level, mob.position(), "supernova_bubble", WHITE, () -> {
                level.sendParticles(ParticleTypes.FLASH, center.x, center.y, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 60, 0.3D, 0.3D, 0.3D, 0.45D);
            });
            HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.5F);
            int struck = 0;
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 6.0D)) {
                if (HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), center, 1.6D, 0.5D)) {
                    struck++;
                }
            }
            mob.heal(2.0F + struck * 2.0F);
        });
    }

    private static void fearConstruct(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 at = HeroKit.chest(target);
        ParticleOptions light = glow(YELLOW, 1.4F);
        HeroKit.line(level, HeroKit.hand(mob), at, glow(YELLOW, 0.8F), 0.4D);
        for (int i = 0; i < 12; i++) {
            double angle = Math.PI * 2.0D * i / 12.0D;
            level.sendParticles(light, at.x + Math.cos(angle) * 1.4D, at.y + 0.8D, at.z + Math.sin(angle) * 1.4D, 2, 0.05D, 0.3D, 0.05D, 0.0D);
            level.sendParticles(light, at.x + Math.cos(angle) * 1.4D, at.y - 0.8D, at.z + Math.sin(angle) * 1.4D, 2, 0.05D, 0.3D, 0.05D, 0.0D);
        }
        HeroKit.sound(level, at, SoundEvents.WARDEN_ROAR, 0.8F, 1.4F);
        VillainWorld.later(level, 8, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                return;
            }
            Vec3 bite = HeroKit.chest(target);
            level.sendParticles(light, bite.x, bite.y, bite.z, 30, 0.6D, 0.6D, 0.6D, 0.0D);
            HeroKit.sound(level, bite, SoundEvents.EVOKER_FANGS_ATTACK, 1.2F, 0.7F);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.4D), mob.position(), 0.2D, 0.2D);
            effect(mob, target, MobEffects.DARKNESS, 80, 0);
            effect(mob, target, MobEffects.BLINDNESS, 30, 0);
            effect(mob, target, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
        });
    }

    private static void omegaBeams(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 eye = HeroKit.muzzle(mob);
        level.sendParticles(glow(OMEGA, 1.0F), eye.x, eye.y, eye.z, 12, 0.2D, 0.05D, 0.2D, 0.0D);
        HeroKit.sound(level, eye, SoundEvents.BEACON_ACTIVATE, 1.2F, 0.5F);
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.muzzle(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            ParticleOptions core = glow(OMEGA, 1.1F);
            HeroKit.sound(level, from, SoundEvents.BLAZE_SHOOT, 1.2F, 0.5F);
            for (int side = -1; side <= 1; side += 2) {
                Vec3 launch = HeroKit.rotate(dir, side * 40.0D).add(0.0D, 0.35D, 0.0D).normalize();
                HeroKit.bolt(level, mob, from.add(launch.scale(0.6D)), launch.scale(1.1D), 45, 0.6D, target, 0.24D,
                        (lvl, a, b) -> {
                            HeroKit.line(lvl, a, b, core, 0.2D);
                            lvl.sendParticles(ParticleTypes.SMALL_FLAME, b.x, b.y, b.z, 1, 0.02D, 0.02D, 0.02D, 0.0D);
                        },
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.3D), at, 0.8D, 0.2D);
                            victim.igniteForSeconds(3.0F);
                        },
                        end -> {
                            HeroFx.vfx(level, end, "impact_starburst", OMEGA, () -> level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D));
                            HeroKit.sound(level, end, SoundEvents.GENERIC_EXPLODE.value(), 0.6F, 1.4F);
                        });
            }
        });
    }

    private static void hellfireEyes(ServerLevel level, Mob mob, LivingEntity target) {
        DcKitFx.beam(level, mob, target, 12, 26.0D, false, TRIGON, 1.5F, 1.7D, 0.6D, SoundEvents.BLAZE_SHOOT, 0.5F,
                (victim, at) -> victim.igniteForSeconds(6.0F),
                end -> {
                    HeroFx.vfx(level, end, "pyre_detonation", TRIGON, () -> level.sendParticles(ParticleTypes.FLAME, end.x, end.y, end.z, 30, 1.0D, 0.4D, 1.0D, 0.05D));
                    for (LivingEntity victim : HeroKit.victims(level, mob, end, 2.5D)) {
                        victim.igniteForSeconds(4.0F);
                    }
                });
    }

    private static void demonQuake(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = mob.position();
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.5D, center.z, 20, 1.0D, 0.3D, 1.0D, 0.02D);
        HeroKit.sound(level, center, SoundEvents.WARDEN_SONIC_CHARGE, 1.0F, 0.6F);
        Set<Integer> struck = new HashSet<>();
        for (int ring = 1; ring <= 3; ring++) {
            double radius = ring * 2.2D;
            VillainWorld.later(level, 8 + ring * 4, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                for (int i = 0; i < 24; i++) {
                    double angle = Math.PI * 2.0D * i / 24.0D;
                    double x = center.x + Math.cos(angle) * radius;
                    double z = center.z + Math.sin(angle) * radius;
                    level.sendParticles(ParticleTypes.FLAME, x, center.y + 0.2D, z, 3, 0.15D, 0.3D, 0.15D, 0.02D);
                    level.sendParticles(ParticleTypes.LAVA, x, center.y + 0.2D, z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                }
                HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 0.8F, 0.6F);
                for (LivingEntity victim : HeroKit.victims(level, mob, center, radius + 0.8D)) {
                    if (struck.add(victim.getId())) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.3D), center, 1.0D, 0.6D);
                        victim.igniteForSeconds(4.0F);
                    }
                }
            });
        }
        VillainWorld.later(level, 12, () -> {
            if (mob.isAlive()) {
                HeroFx.vfx(level, center, "slam_fracture_large", TRIGON, 6.0F);
                HeroFx.shake(level, center, 16.0D, 0.6F, 14);
            }
        });
    }

    private static void warAxe(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.TRIDENT_THROW.value(), 1.2F, 0.6F);
        HeroKit.hurl(level, mob, target, new ItemStack(Items.NETHERITE_AXE), 1.1D, 1, 35.0F,
                (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.CRIT, 0.6D),
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), at, 1.0D, 0.3D);
                    HeroKit.sound(level, victim.position(), SoundEvents.ANVIL_LAND, 0.6F, 1.4F);
                },
                null);
    }

    private static void warCry(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = mob.position();
        HeroKit.sound(level, center, SoundEvents.RAVAGER_ROAR, 1.4F, 0.6F);
        if (!HeroFx.vfx(level, center.add(0.0D, 1.0D, 0.0D), "air_burst_ring", RED)) {
            VillainFx.ring(level, center, 6.0D, VillainFx.RED);
        }
        self(mob, MobEffects.DAMAGE_BOOST, 140, 1);
        self(mob, MobEffects.DAMAGE_RESISTANCE, 80, 0);
        for (LivingEntity victim : HeroKit.victims(level, mob, center, 8.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.5D), center, 1.3D, 0.3D);
            effect(mob, victim, MobEffects.WEAKNESS, 140, 0);
            effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
        }
    }

    private static void drones(ServerLevel level, Mob mob, LivingEntity target, int color, int count) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.BEACON_POWER_SELECT, 1.0F, 1.8F);
        ParticleOptions body = glow(color, 1.5F);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count;
            int delay = 6 + i * 4;
            VillainWorld.later(level, delay, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 drone = mob.position().add(Math.cos(angle) * 2.0D, mob.getBbHeight() + 0.8D, Math.sin(angle) * 2.0D);
                level.sendParticles(body, drone.x, drone.y, drone.z, 8, 0.12D, 0.12D, 0.12D, 0.0D);
                level.sendParticles(ParticleTypes.END_ROD, drone.x, drone.y, drone.z, 2, 0.1D, 0.1D, 0.1D, 0.0D);
                Vec3 dir = HeroKit.chest(target).subtract(drone).normalize();
                HeroKit.sound(level, drone, SoundEvents.FIREWORK_ROCKET_BLAST, 0.6F, 1.8F);
                DcKitFx.shot(level, mob, target, drone, dir, color, 0.8F, 1.6D, 0.6D, 0.15D, null,
                        (victim, at) -> effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            });
        }
    }

    private static void kryptoniteBeam(ServerLevel level, Mob mob, LivingEntity target) {
        HeroFx.cue(mob, "thematic:laser_open", 0.8F, 0.8F);
        DcKitFx.beam(level, mob, target, 10, 24.0D, false, KRYPTO, 1.3F, 1.5D, 0.8D, SoundEvents.BEACON_DEACTIVATE, 0.6F,
                (victim, at) -> {
                    effect(mob, victim, MobEffects.WEAKNESS, 160, 1);
                    effect(mob, victim, MobEffects.POISON, 60, 0);
                    effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
                },
                null);
    }

    private static void loboHook(ServerLevel level, Mob mob, LivingEntity target) {
        ParticleOptions links = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.CHAIN));
        HeroKit.ray(level, mob, target, 5, 14.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, HeroKit.hand(mob), to, links, 0.25D);
                    HeroKit.line(lvl, HeroKit.hand(mob), to, ParticleTypes.CRIT, 0.8D);
                    HeroKit.sound(lvl, from, SoundEvents.CHAIN_PLACE, 1.2F, 0.6F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.9D), from, 0.0D, 0.0D);
                    DcKitFx.pull(mob, victim, 1.6D);
                    VillainWorld.later(level, 8, () -> {
                        if (HeroKit.gone(mob, victim) || mob.distanceTo(victim) > 3.5D || !HeroKit.foe(mob, victim)) {
                            return;
                        }
                        mob.swing(InteractionHand.MAIN_HAND);
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D), mob.position(), 1.6D, 0.5D);
                        HeroFx.vfx(level, HeroKit.chest(victim), "punch_pressure_wave", 0);
                        HeroKit.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, 0.7F);
                    });
                },
                null);
    }

    private static void siphon(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.ray(level, mob, target, 6, 14.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, glow(PARASITE, 1.0F), 0.3D);
                    HeroKit.sound(lvl, from, SoundEvents.BEACON_AMBIENT, 1.4F, 1.6F);
                },
                (victim, from) -> {
                    float amount = HeroKit.damage(mob, 1.1D);
                    if (HeroKit.hit(level, mob, victim, amount, from, 0.0D, 0.0D)) {
                        mob.heal(amount * 0.7F);
                    }
                    effect(mob, victim, MobEffects.WEAKNESS, 160, 1);
                    effect(mob, victim, MobEffects.HUNGER, 100, 1);
                    self(mob, MobEffects.DAMAGE_BOOST, 100, 0);
                    HeroKit.line(level, HeroKit.chest(victim), HeroKit.chest(mob), ParticleTypes.SCULK_SOUL, 0.4D);
                    HeroKit.sound(level, victim.position(), SoundEvents.SOUL_ESCAPE.value(), 1.2F, 1.2F);
                },
                null);
    }

    private static void lightningArc(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 hand = HeroKit.hand(mob);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hand.x, hand.y, hand.z, 12, 0.2D, 0.2D, 0.2D, 0.1D);
        HeroKit.sound(level, hand, SoundEvents.BEACON_POWER_SELECT, 1.0F, 2.0F);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 18.0D) {
                return;
            }
            ParticleOptions core = glow(ARC, 0.8F);
            Set<Integer> struck = new HashSet<>();
            Vec3 from = HeroKit.hand(mob);
            LivingEntity current = target;
            double scale = 1.3D;
            for (int jump = 0; jump < 4 && current != null; jump++) {
                Vec3 to = HeroKit.chest(current);
                DcKitFx.zap(level, from, to, core);
                struck.add(current.getId());
                current.invulnerableTime = 0;
                HeroKit.hit(level, mob, current, HeroKit.damage(mob, scale), from, 0.4D, 0.1D);
                effect(mob, current, MobEffects.MOVEMENT_SLOWDOWN, 30, 2);
                HeroKit.sound(level, to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.6F, 1.6F);
                from = to;
                scale *= 0.75D;
                LivingEntity next = null;
                double best = 49.0D;
                for (LivingEntity other : HeroKit.victims(level, mob, to, 7.0D)) {
                    double d = other.distanceToSqr(to);
                    if (!struck.contains(other.getId()) && d < best) {
                        best = d;
                        next = other;
                    }
                }
                current = next;
            }
        });
    }

    private static void backwardsSpell(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        Vec3 hand = HeroKit.hand(mob);
        level.sendParticles(ParticleTypes.ENCHANT, hand.x, hand.y + 0.5D, hand.z, 30, 0.4D, 0.4D, 0.4D, 0.6D);
        HeroKit.sound(level, hand, SoundEvents.EVOKER_PREPARE_ATTACK, 1.0F, 1.5F);
        int spell = level.random.nextInt(4);
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 20.0D) {
                return;
            }
            Vec3 at = HeroKit.chest(target);
            HeroKit.line(level, HeroKit.hand(mob), at, ParticleTypes.ENCHANT, 0.4D);
            level.sendParticles(ParticleTypes.WITCH, at.x, at.y, at.z, 20, 0.4D, 0.6D, 0.4D, 0.1D);
            HeroKit.sound(level, at, SoundEvents.EVOKER_CAST_SPELL, 1.2F, 1.4F);
            switch (spell) {
                case 0 -> {
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.3D), mob.position(), 0.4D, 0.2D);
                    target.igniteForSeconds(5.0F);
                    level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 30, 0.4D, 0.8D, 0.4D, 0.05D);
                }
                case 1 -> {
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.1D), mob.position(), 0.2D, 0.1D);
                    DcKitFx.freeze(target, 80);
                    level.sendParticles(ParticleTypes.SNOWFLAKE, at.x, at.y, at.z, 30, 0.4D, 0.8D, 0.4D, 0.05D);
                }
                case 2 -> {
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.9D), mob.position(), 0.0D, 0.0D);
                    effect(mob, target, MobEffects.LEVITATION, 40, 1);
                }
                default -> {
                    HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.0D), mob.position(), 0.2D, 0.1D);
                    effect(mob, target, MobEffects.BLINDNESS, 60, 0);
                    effect(mob, target, MobEffects.CONFUSION, 100, 0);
                }
            }
        });
    }

    private static void ankhBlast(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 facing = HeroKit.toward(mob, target);
        Vec3 front = HeroKit.muzzle(mob).add(facing);
        DcKitFx.ankh(level, front, facing, glow(ANKH, 0.9F));
        HeroKit.sound(level, front, SoundEvents.ILLUSIONER_PREPARE_MIRROR, 1.0F, 0.8F);
        DcKitFx.beam(level, mob, target, 10, 24.0D, false, ANKH, 1.4F, 1.6D, 1.4D, SoundEvents.BEACON_ACTIVATE, 1.2F,
                (victim, at) -> effect(mob, victim, MobEffects.WEAKNESS, 100, 0),
                end -> {
                    DcKitFx.ankh(level, end, facing, glow(ANKH, 1.2F));
                    HeroFx.vfx(level, end, "circle_ring", ANKH, 2.0F);
                });
    }

    private static void judgment(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = target.position();
        ParticleOptions spirit = glow(SPECTRE, 1.6F);
        if (!HeroFx.vfx(level, at.add(0.0D, 0.1D, 0.0D), "circle_ring", SPECTRE, 3.0F)) {
            VillainFx.ring(level, at, 3.0D, new Vector3f(0.3F, 1.0F, 0.55F));
        }
        level.sendParticles(spirit, at.x, at.y + 8.0D, at.z, 30, 1.2D, 0.6D, 1.2D, 0.0D);
        HeroKit.sound(level, at, SoundEvents.WITHER_AMBIENT, 1.0F, 0.6F);
        VillainWorld.later(level, 16, () -> {
            if (!mob.isAlive()) {
                return;
            }
            for (int i = -1; i <= 1; i++) {
                HeroKit.line(level, at.add(i * 0.6D, 8.0D, 0.0D), at.add(i * 0.6D, 0.2D, 0.0D), spirit, 0.3D);
            }
            HeroFx.vfx(level, at, "slam_fracture_large", SPECTRE, 3.5F);
            HeroKit.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 0.7F);
            HeroFx.shake(level, at, 12.0D, 0.5F, 10);
            for (LivingEntity victim : HeroKit.victims(level, mob, at, 3.2D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 2.0D), at, 0.6D, 0.1D);
                effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
            }
        });
    }

    private static void possess(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.chest(mob);
        Vec3 to = HeroKit.chest(target);
        HeroKit.line(level, from, to, ParticleTypes.SOUL, 0.4D);
        level.sendParticles(ParticleTypes.SOUL, to.x, to.y, to.z, 20, 0.3D, 0.6D, 0.3D, 0.02D);
        HeroKit.sound(level, to, SoundEvents.SOUL_ESCAPE.value(), 1.4F, 0.7F);
        self(mob, MobEffects.INVISIBILITY, 40, 0);
        HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.9D), mob.position(), 0.0D, 0.0D);
        effect(mob, target, MobEffects.CONFUSION, 140, 0);
        effect(mob, target, MobEffects.WEAKNESS, 120, 1);
        effect(mob, target, MobEffects.MOVEMENT_SLOWDOWN, 60, 2);
        if (HeroAbilities.canControl(target)) {
            for (int t = 4; t <= 16; t += 6) {
                VillainWorld.later(level, t, () -> {
                    if (target.isAlive()) {
                        double angle = level.random.nextDouble() * Math.PI * 2.0D;
                        target.push(Math.cos(angle) * 0.5D, 0.1D, Math.sin(angle) * 0.5D);
                        target.hurtMarked = true;
                    }
                });
            }
        }
    }

    private static void soulSelf(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.chest(mob);
        level.sendParticles(glow(RAVEN, 2.0F), from.x, from.y, from.z, 20, 0.6D, 0.6D, 0.6D, 0.0D);
        HeroKit.sound(level, from, SoundEvents.PHANTOM_SWOOP, 1.2F, 0.6F);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 start = HeroKit.chest(mob);
            Vec3 dir = HeroKit.chest(target).subtract(start).normalize();
            Vec3 side = new Vec3(-dir.z, 0.0D, dir.x);
            ParticleOptions body = glow(RAVEN, 2.2F);
            HeroKit.bolt(level, mob, start.add(dir), dir.scale(0.95D), 40, 0.9D, target, 0.2D,
                    (lvl, a, b) -> {
                        lvl.sendParticles(body, b.x, b.y, b.z, 5, 0.35D, 0.2D, 0.35D, 0.0D);
                        HeroKit.line(lvl, b.add(side).add(0.0D, 0.3D, 0.0D), b, body, 0.25D);
                        HeroKit.line(lvl, b.subtract(side).add(0.0D, 0.3D, 0.0D), b, body, 0.25D);
                        lvl.sendParticles(ParticleTypes.SQUID_INK, b.x, b.y, b.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                    },
                    (victim, at) -> {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.6D), at, 1.2D, 0.3D);
                        effect(mob, victim, MobEffects.DARKNESS, 80, 0);
                        effect(mob, victim, MobEffects.BLINDNESS, 30, 0);
                    },
                    end -> HeroFx.vfx(level, end, "void_twist", RAVEN, () -> level.sendParticles(ParticleTypes.SQUID_INK, end.x, end.y, end.z, 20, 0.4D, 0.4D, 0.4D, 0.05D)));
        });
    }

    private static void animalCharge(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        ParticleOptions morph = glow(GREEN, 1.2F);
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(morph, at.x, at.y, at.z, 30, 0.5D, 0.8D, 0.5D, 0.0D);
        HeroKit.sound(level, at, SoundEvents.RAVAGER_ROAR, 1.2F, 1.0F);
        Set<Integer> struck = new HashSet<>();
        for (int t = 4; t < 18; t += 2) {
            VillainWorld.later(level, t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                mob.setDeltaMovement(dir.x * 1.1D, mob.getDeltaMovement().y, dir.z * 1.1D);
                mob.hurtMarked = true;
                Vec3 pos = mob.position();
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(mob.blockPosition().below())), pos.x, pos.y + 0.1D, pos.z,
                        6, 0.4D, 0.1D, 0.4D, 0.1D);
                for (LivingEntity victim : HeroKit.victims(level, mob, HeroKit.chest(mob), 1.8D)) {
                    if (struck.add(victim.getId())) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.7D), pos, 1.4D, 0.9D);
                        HeroKit.sound(level, pos, SoundEvents.RAVAGER_ATTACK, 1.0F, 0.9F);
                        HeroFx.vfx(level, HeroKit.chest(victim), "impact_starburst", GREEN);
                    }
                }
            });
        }
    }

    private static void coldGun(ServerLevel level, Mob mob, LivingEntity target) {
        DcKitFx.beam(level, mob, target, 8, 20.0D, false, COLD, 1.3F, 1.2D, 0.3D, SoundEvents.POWDER_SNOW_BREAK, 0.6F,
                (victim, at) -> {
                    DcKitFx.freeze(victim, 100);
                    if (HeroAbilities.canControl(victim)) {
                        VillainWorld.stick(victim, 40);
                    }
                },
                end -> HeroFx.vfx(level, end, "powergirl_ice_shatter", 0, () -> level.sendParticles(ParticleTypes.SNOWFLAKE, end.x, end.y, end.z, 30, 0.5D, 0.5D, 0.5D, 0.05D)));
    }

    private static void iceShards(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        ParticleOptions ice = new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.ICE));
        HeroKit.sound(level, from, SoundEvents.GLASS_BREAK, 1.0F, 1.6F);
        for (int i = -1; i <= 1; i++) {
            Vec3 spread = HeroKit.rotate(dir, i * 10.0D);
            DcKitFx.shot(level, mob, target, from, spread, COLD, 0.7F, 1.6D, 0.75D, 0.0D, ice, (victim, at) -> DcKitFx.freeze(victim, 40));
        }
    }

    private static void greenFlame(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        HeroKit.sound(level, from, SoundEvents.BLAZE_SHOOT, 1.2F, 0.8F);
        ParticleOptions flame = glow(FIRE_GREEN, 1.2F);
        for (int pulse = 0; pulse < 3; pulse++) {
            VillainWorld.later(level, pulse * 4, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                Vec3 origin = HeroKit.hand(mob);
                for (int i = -2; i <= 2; i++) {
                    Vec3 spread = HeroKit.rotate(dir, i * 9.0D);
                    HeroKit.line(level, origin.add(spread), origin.add(spread.scale(9.0D)), flame, 0.6D);
                }
                for (LivingEntity victim : HeroKit.cone(level, mob, origin, dir, 9.0D, 25.0D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.5D), origin, 0.2D, 0.0D);
                    victim.igniteForSeconds(5.0F);
                }
            });
        }
    }

    private static void vineLash(ServerLevel level, Mob mob, LivingEntity target) {
        BlockParticleOption moss = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.MOSS_BLOCK.defaultBlockState());
        DustParticleOptions vine = dust(0xFF2E8B2E, 1.4F);
        HeroKit.ray(level, mob, target, 4, 14.0D, 0.7D, false,
                (lvl, from, to) -> {
                    Vec3 start = mob.position().add(0.0D, 0.2D, 0.0D);
                    HeroKit.line(lvl, start, to, vine, 0.2D);
                    HeroKit.line(lvl, start, to, moss, 0.6D);
                    HeroKit.sound(lvl, from, SoundEvents.ROOTED_DIRT_BREAK, 1.2F, 0.8F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), from, 0.0D, 0.0D);
                    effect(mob, victim, MobEffects.POISON, 60, 0);
                    DcKitFx.pull(mob, victim, 1.3D);
                },
                null);
    }

    private static void pollen(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 center = target.position();
        HeroKit.line(level, HeroKit.hand(mob), HeroKit.chest(target), dust(0xFFFFB0D0, 1.0F), 0.4D);
        HeroKit.sound(level, center, SoundEvents.AZALEA_LEAVES_BREAK, 1.4F, 0.7F);
        DcKitFx.cloud(level, mob, center, 3.5D, 5, 8, dust(0xFFFFE070, 1.3F), ParticleTypes.SPORE_BLOSSOM_AIR, victim -> {
            effect(mob, victim, MobEffects.POISON, 60, 0);
            effect(mob, victim, MobEffects.CONFUSION, 120, 0);
            effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 40, 0);
        });
    }

    private static void fearGas(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 center = mob.position().add(HeroKit.toward(mob, target).scale(Math.min(4.0D, mob.distanceTo(target))));
        HeroKit.sound(level, center, SoundEvents.FIRE_EXTINGUISH, 1.4F, 0.5F);
        DcKitFx.cloud(level, mob, center, 4.0D, 6, 8, dust(FEAR, 1.6F), ParticleTypes.LARGE_SMOKE, victim -> {
            effect(mob, victim, MobEffects.DARKNESS, 100, 0);
            effect(mob, victim, MobEffects.CONFUSION, 140, 0);
            effect(mob, victim, MobEffects.MOVEMENT_SLOWDOWN, 60, 1);
            effect(mob, victim, MobEffects.WEAKNESS, 100, 0);
            if (victim.invulnerableTime <= 0) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.3D), center, 0.0D, 0.0D);
            }
        });
    }

    private static void telepathicCrush(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 at = target.position();
        HeroFx.vfx(level, at.add(0.0D, 0.1D, 0.0D), "circle_ring", PSI, 1.5F);
        HeroKit.line(level, HeroKit.muzzle(mob), target.getEyePosition(), ParticleTypes.ENCHANT, 0.4D);
        HeroKit.sound(level, at, SoundEvents.ILLUSIONER_PREPARE_BLINDNESS, 1.2F, 0.6F);
        boolean lifted = HeroAbilities.canControl(target);
        if (lifted) {
            target.setDeltaMovement(0.0D, 0.9D, 0.0D);
            target.hurtMarked = true;
        }
        VillainWorld.later(level, 12, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                return;
            }
            if (lifted) {
                target.setDeltaMovement(0.0D, -1.6D, 0.0D);
                target.hurtMarked = true;
            }
            Vec3 head = target.getEyePosition();
            level.sendParticles(dust(PSI, 1.4F), head.x, head.y, head.z, 24, 0.4D, 0.4D, 0.4D, 0.0D);
            HeroKit.sound(level, head, SoundEvents.ILLUSIONER_CAST_SPELL, 1.2F, 0.5F);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, lifted ? 1.8D : 1.4D), mob.position(), 0.0D, 0.0D);
            effect(mob, target, MobEffects.CONFUSION, 80, 0);
            effect(mob, target, MobEffects.MOVEMENT_SLOWDOWN, 40, 1);
        });
        VillainWorld.later(level, 18, () -> {
            if (mob.isAlive() && target.isAlive() && lifted) {
                target.fallDistance = 0.0F;
                HeroFx.vfx(level, target.position(), "slam_dust", 0);
            }
        });
    }

    private static void mirrorShards(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
        ParticleOptions glass = new BlockParticleOption(ParticleTypes.BLOCK, Blocks.GLASS.defaultBlockState());
        HeroKit.sound(level, from, SoundEvents.GLASS_BREAK, 1.2F, 1.2F);
        for (int i = -2; i <= 2; i++) {
            Vec3 spread = HeroKit.rotate(dir, i * 7.0D);
            DcKitFx.shot(level, mob, target, from, spread, 0xFFE8F8FF, 0.6F, 1.8D, 0.45D, 0.0D, glass, null);
        }
    }

    private static void venomSurge(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = HeroKit.chest(mob);
        level.sendParticles(dust(0xFF40FF40, 1.4F), at.x, at.y, at.z, 30, 0.5D, 0.8D, 0.5D, 0.0D);
        HeroKit.sound(level, at, SoundEvents.RAVAGER_ROAR, 1.4F, 0.8F);
        self(mob, MobEffects.DAMAGE_BOOST, 200, 1);
        self(mob, MobEffects.DAMAGE_RESISTANCE, 200, 0);
        self(mob, MobEffects.MOVEMENT_SPEED, 100, 0);
        mob.heal(mob.getMaxHealth() * 0.08F);
        for (LivingEntity victim : HeroKit.victims(level, mob, mob.position(), 4.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), mob.position(), 1.4D, 0.3D);
        }
    }

    private static void backbreaker(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        boolean lifted = HeroAbilities.canControl(target);
        if (lifted) {
            target.setDeltaMovement(0.0D, 0.75D, 0.0D);
            target.hurtMarked = true;
        }
        HeroKit.sound(level, target.position(), SoundEvents.IRON_GOLEM_ATTACK, 1.2F, 0.6F);
        VillainWorld.later(level, 10, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 4.5D || !HeroKit.foe(mob, target)) {
                return;
            }
            if (lifted) {
                target.setDeltaMovement(0.0D, -1.4D, 0.0D);
                target.hurtMarked = true;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 2.2D), mob.position(), 0.2D, 0.0D);
            effect(mob, target, MobEffects.MOVEMENT_SLOWDOWN, 80, 2);
            Vec3 ground = target.position();
            HeroFx.vfx(level, ground, "slam_fracture", 0, () -> level.sendParticles(ParticleTypes.EXPLOSION, ground.x, ground.y, ground.z, 1, 0.0D, 0.0D, 0.0D, 0.0D));
            HeroKit.sound(level, ground, SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR, 1.2F, 0.6F);
        });
    }

    private static void bite(ServerLevel level, Mob mob, LivingEntity target, double scale, boolean shark) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        if (mob.distanceTo(target) > 2.5D) {
            mob.setDeltaMovement(dir.x * 1.1D, 0.3D, dir.z * 1.1D);
            mob.hurtMarked = true;
        }
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target) || mob.distanceTo(target) > 3.2D || !HeroKit.foe(mob, target)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), 0.5D, 0.2D);
            if (shark) {
                effect(mob, target, MobEffects.WITHER, 60, 0);
                level.sendParticles(dust(0xFF8A0000, 1.2F), target.getX(), target.getY() + 1.0D, target.getZ(), 16, 0.3D, 0.4D, 0.3D, 0.0D);
            } else {
                effect(mob, target, MobEffects.POISON, 100, 1);
                level.sendParticles(ParticleTypes.ITEM_SLIME, target.getX(), target.getY() + 1.0D, target.getZ(), 12, 0.3D, 0.4D, 0.3D, 0.0D);
            }
            HeroKit.sound(level, target.position(), SoundEvents.EVOKER_FANGS_ATTACK, 1.2F, shark ? 0.6F : 1.4F);
        });
    }

    private static void swordSlash(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        Vec3 from = HeroKit.hand(mob);
        Vec3 dir = HeroKit.toward(mob, target);
        for (int i = -3; i <= 3; i++) {
            Vec3 point = from.add(HeroKit.rotate(dir, i * 18.0D).scale(2.4D));
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, point.x, point.y, point.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        }
        HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 0.8F);
        for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 5.0D, 65.0D)) {
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D), from, 0.8D, 0.2D);
        }
    }

    private static void whipCrack(ServerLevel level, Mob mob, LivingEntity target) {
        mob.swing(InteractionHand.MAIN_HAND);
        DustParticleOptions leather = dust(0xFF2A1E18, 0.9F);
        HeroKit.ray(level, mob, target, 3, 10.0D, 0.6D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, HeroKit.hand(mob), to, leather, 0.15D);
                    lvl.sendParticles(ParticleTypes.CRIT, to.x, to.y, to.z, 8, 0.2D, 0.2D, 0.2D, 0.1D);
                    HeroKit.sound(lvl, from, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.6F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), from, 0.0D, 0.0D);
                    DcKitFx.pull(mob, victim, 1.2D);
                },
                null);
    }

    private static void crossbow(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 3; i++) {
            VillainWorld.later(level, 4 + i * 5, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
                HeroKit.sound(level, from, SoundEvents.CROSSBOW_SHOOT, 1.0F, 1.1F);
                HeroKit.bolt(level, mob, from.add(dir), dir.scale(2.0D), 15, 0.4D, null, 0.0D,
                        (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.CRIT, 0.5D),
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.7D), at, 0.3D, 0.05D);
                            HeroKit.sound(level, victim.position(), SoundEvents.ARROW_HIT, 1.0F, 1.0F);
                        },
                        null);
            });
        }
    }

    private static void umbrellaGun(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        VillainWorld.later(level, 5, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.hand(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            for (int i = -2; i <= 2; i++) {
                Vec3 spread = HeroKit.rotate(dir, i * 7.0D);
                HeroKit.line(level, from, HeroKit.clip(level, mob, from, from.add(spread.scale(8.0D))), ParticleTypes.CRIT, 0.6D);
            }
            level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x, from.y, from.z, 6, 0.2D, 0.2D, 0.2D, 0.02D);
            HeroKit.sound(level, from, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.8F);
            for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 8.0D, 25.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.4D), from, 1.0D, 0.2D);
            }
        });
    }

    private static void coinFlip(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 top = mob.getEyePosition().add(0.0D, 0.6D, 0.0D);
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(Items.GOLD_NUGGET)), top.x, top.y, top.z, 6, 0.05D, 0.3D, 0.05D, 0.05D);
        HeroKit.sound(level, top, SoundEvents.CHAIN_HIT, 1.0F, 2.0F);
        boolean heads = level.random.nextBoolean();
        int shots = heads ? 3 : 1;
        double scale = heads ? 0.9D : 0.6D;
        for (int i = 0; i < shots; i++) {
            VillainWorld.later(level, 14 + i * 4, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
                HeroKit.sound(level, from, SoundEvents.GENERIC_EXPLODE.value(), 0.5F, 2.0F);
                level.sendParticles(ParticleTypes.SMOKE, from.x, from.y, from.z, 3, 0.05D, 0.05D, 0.05D, 0.01D);
                HeroKit.bolt(level, mob, from.add(dir), dir.scale(2.4D), 12, 0.4D, null, 0.0D,
                        (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.CRIT, 0.6D),
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, 0.4D, 0.05D);
                        },
                        null);
            });
        }
    }

    private static void shadowTendrils(ServerLevel level, Mob mob, LivingEntity target) {
        DustParticleOptions shadow = dust(0xFF100818, 1.6F);
        HeroKit.ray(level, mob, target, 6, 14.0D, 0.8D, false,
                (lvl, from, to) -> {
                    Vec3 start = mob.position().add(0.0D, 0.3D, 0.0D);
                    HeroKit.line(lvl, start, to, shadow, 0.2D);
                    HeroKit.line(lvl, start, to, ParticleTypes.SQUID_INK, 0.7D);
                    HeroKit.sound(lvl, from, SoundEvents.SCULK_CATALYST_BLOOM, 1.2F, 0.6F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), from, 0.0D, 0.0D);
                    effect(mob, victim, MobEffects.DARKNESS, 100, 0);
                    effect(mob, victim, MobEffects.BLINDNESS, 30, 0);
                    DcKitFx.pull(mob, victim, 1.2D);
                },
                null);
    }

    private static void cyclone(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = target.position();
        boolean themed = HeroFx.vfx(level, center, "gust_tornado", 0);
        HeroKit.sound(level, center, SoundEvents.ELYTRA_FLYING, 1.2F, 0.8F);
        for (int t = 0; t < 16; t += 2) {
            boolean last = t == 14;
            double spin = t * 0.5D;
            VillainWorld.later(level, t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                if (!themed) {
                    for (int i = 0; i < 6; i++) {
                        double angle = spin + Math.PI * 2.0D * i / 6.0D;
                        double r = 0.6D + i * 0.3D;
                        level.sendParticles(ParticleTypes.CLOUD, center.x + Math.cos(angle) * r, center.y + i * 0.6D, center.z + Math.sin(angle) * r, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                    }
                }
                for (LivingEntity victim : HeroKit.victims(level, mob, center, 4.0D)) {
                    if (!HeroAbilities.canControl(victim)) {
                        if (last) {
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.2D), center, 0.6D, 0.2D);
                        }
                        continue;
                    }
                    Vec3 pull = center.subtract(victim.position()).multiply(1.0D, 0.0D, 1.0D);
                    if (pull.lengthSqr() > 0.25D) {
                        Vec3 dir = pull.normalize().scale(0.3D);
                        victim.push(dir.x, 0.08D, dir.z);
                        victim.hurtMarked = true;
                    }
                    if (last) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.2D), center, 0.6D, 0.9D);
                    }
                }
            });
        }
    }

    private static void topSpin(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F, 1.6F);
        Set<Integer> struck = new HashSet<>();
        for (int t = 0; t < 14; t += 2) {
            VillainWorld.later(level, t, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                mob.setYRot(mob.getYRot() + 70.0F);
                mob.setDeltaMovement(dir.x * 0.8D, mob.getDeltaMovement().y, dir.z * 0.8D);
                mob.hurtMarked = true;
                Vec3 at = HeroKit.chest(mob);
                level.sendParticles(ParticleTypes.CLOUD, at.x, at.y - 0.5D, at.z, 4, 0.6D, 0.1D, 0.6D, 0.05D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.3D, 0.1D, 0.3D, 0.0D);
                for (LivingEntity victim : HeroKit.victims(level, mob, at, 2.2D)) {
                    if (struck.add(victim.getId())) {
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), at, 1.2D, 0.3D);
                        effect(mob, victim, MobEffects.CONFUSION, 100, 0);
                    }
                }
            });
        }
    }

    private static void trickBomb(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.FIREWORK_ROCKET_LAUNCH, 1.0F, 1.2F);
        for (int i = 0; i < 3; i++) {
            VillainWorld.later(level, i * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 from = HeroKit.hand(mob);
                Vec3 spot = target.position().add((level.random.nextDouble() - 0.5D) * 3.0D, 0.3D, (level.random.nextDouble() - 0.5D) * 3.0D);
                Vec3 dir = spot.subtract(from).normalize();
                DustParticleOptions color = new DustParticleOptions(new Vector3f(level.random.nextFloat(), level.random.nextFloat(), level.random.nextFloat()), 1.2F);
                HeroKit.bolt(level, mob, from.add(dir), dir.scale(0.9D), 30, 0.5D, null, 0.0D,
                        (lvl, a, b) -> HeroKit.line(lvl, a, b, color, 0.4D),
                        (victim, at) -> {
                        },
                        end -> {
                            level.sendParticles(ParticleTypes.EXPLOSION, end.x, end.y, end.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                            level.sendParticles(ParticleTypes.FIREWORK, end.x, end.y, end.z, 16, 0.2D, 0.2D, 0.2D, 0.15D);
                            HeroKit.sound(level, end, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 1.0F, 1.2F);
                            for (LivingEntity victim : HeroKit.victims(level, mob, end, 2.2D)) {
                                victim.invulnerableTime = 0;
                                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.75D), end, 0.8D, 0.4D);
                            }
                        });
            });
        }
    }

    private static void powerGlove(ServerLevel level, Mob mob, LivingEntity target) {
        for (int i = 0; i < 2; i++) {
            VillainWorld.later(level, i * 6, () -> {
                if (!HeroKit.gone(mob, target)) {
                    DcKitFx.energy(level, mob, target, GOLD, 1.1F, 1.7D, 0.9D, 0.05D, SoundEvents.BEACON_POWER_SELECT, 1.8F, ParticleTypes.END_ROD, null);
                }
            });
        }
    }

    private static void hammerThrow(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.2F, 0.6F);
        HeroKit.hurl(level, mob, target, new ItemStack(Items.MACE), 1.0D, 0, 25.0F,
                (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.ELECTRIC_SPARK, 0.6D),
                (victim, at) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.7D), at, 1.6D, 0.4D);
                    HeroKit.sound(level, victim.position(), SoundEvents.ANVIL_LAND, 0.8F, 0.8F);
                    HeroFx.vfx(level, HeroKit.chest(victim), "impact_starburst", 0);
                },
                null);
    }

    private static void boomerang(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.sound(level, mob.position(), SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.6F);
        HeroKit.hurl(level, mob, target, new ItemStack(HeroFx.item("thematic:boomerang", Items.STICK)), 1.2D, 2, 45.0F,
                (lvl, a, b) -> HeroKit.line(lvl, a, b, ParticleTypes.CRIT, 0.7D),
                (victim, at) -> {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), at, 0.6D, 0.2D);
                },
                null);
    }

    private static void polkaDots(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.BUBBLE_COLUMN_UPWARDS_INSIDE, 1.2F, 1.4F);
        for (int i = 0; i < 5; i++) {
            int index = i;
            VillainWorld.later(level, i * 3, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                Vec3 from = HeroKit.chest(mob);
                Vec3 dir = HeroKit.rotate(HeroKit.chest(target).subtract(from).normalize(), (index - 2) * 22.0D).add(0.0D, 0.25D, 0.0D).normalize();
                int color = 0xFF000000 | level.random.nextInt(0x1000000);
                DcKitFx.shot(level, mob, target, from, dir, color, 1.4F, 1.0D, 0.5D, 0.2D, null, null);
            });
        }
    }
}
