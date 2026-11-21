package com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.HeroStun;
import com.craisinlord.integrated_superheroes.villain.VillainFx;
import com.craisinlord.integrated_superheroes.villain.VillainWorld;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.VariantMoves;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class IconKitMoves {
    private static final int BLUE = 0x3FA9FF;
    private static final int GREEN = 0x4CFF5A;
    private static final int VIOLET = 0xB24CFF;
    private static final int WHITE = 0xE8F4FF;
    private static final int RED = 0xFF2424;
    private static final int SITH_BOLT = 0xB070FF;
    private static final int STORM_BOLT = 0x8FE8FF;
    private static final int GOLD = 0xFFC63A;
    private static final int ORANGE = 0xFF8A20;
    private static final int PINK = 0xFF5ACD;
    private static final DustParticleOptions FORCE_DUST = dust(0xA8D8FF, 1.3F);
    private static final DustParticleOptions SHADOW_DUST = dust(0x101018, 1.8F);
    private static final DustParticleOptions BLOOD_DUST = dust(0x7A0A0A, 1.1F);
    private static final DustParticleOptions LEATHER_DUST = dust(0x5A3A1E, 0.9F);
    private static final DustParticleOptions SAND_DUST = dust(0xD8C08A, 1.5F);
    private static final DustParticleOptions TIME_DUST = dust(0xE8C060, 1.2F);
    private static final DustParticleOptions ACID_DUST = dust(0x7CFF2A, 1.2F);
    private static final Vector3f GOLD_RING = new Vector3f(1.0F, 0.78F, 0.23F);
    private static final Vector3f BLOOD_RING = new Vector3f(0.6F, 0.05F, 0.05F);
    private static final String SWEEP = "minecraft:entity.player.attack.sweep";
    private static final String THROW = "minecraft:item.trident.throw";

    private IconKitMoves() {
    }

    public static void register(Map<String, VariantMoves.Move> map) {
        add(map, "ic_force_push", 0.0D, 10.0D, IconKitMoves::forcePush);
        add(map, "ic_force_pull", 4.0D, 16.0D, IconKitMoves::forcePull);
        add(map, "ic_force_choke", 2.0D, 14.0D, IconKitMoves::forceChoke);
        add(map, "ic_force_freeze", 3.0D, 16.0D, IconKitMoves::forceFreeze);
        add(map, "ic_force_lightning", 2.0D, 14.0D, (level, mob, target) -> lightningHands(level, mob, target, SITH_BOLT, 7));
        add(map, "ic_lightning_hands", 2.0D, 14.0D, (level, mob, target) -> lightningHands(level, mob, target, STORM_BOLT, 5));

        add(map, "ic_saber_flurry", 0.0D, 4.0D, (level, mob, target) -> saberFlurry(level, mob, target, BLUE));
        add(map, "ic_green_saber_flurry", 0.0D, 4.0D, (level, mob, target) -> saberFlurry(level, mob, target, GREEN));
        add(map, "ic_violet_saber_flurry", 0.0D, 4.0D, (level, mob, target) -> saberFlurry(level, mob, target, VIOLET));
        add(map, "ic_white_saber_flurry", 0.0D, 4.0D, (level, mob, target) -> saberFlurry(level, mob, target, WHITE));
        add(map, "ic_sith_flurry", 0.0D, 4.0D, (level, mob, target) -> saberFlurry(level, mob, target, RED));
        add(map, "ic_saber_throw", 4.0D, 18.0D, (level, mob, target) -> saberThrow(level, mob, target, BLUE));
        add(map, "ic_green_saber_throw", 4.0D, 18.0D, (level, mob, target) -> saberThrow(level, mob, target, GREEN));
        add(map, "ic_violet_saber_throw", 4.0D, 18.0D, (level, mob, target) -> saberThrow(level, mob, target, VIOLET));
        add(map, "ic_white_saber_throw", 4.0D, 18.0D, (level, mob, target) -> saberThrow(level, mob, target, WHITE));
        add(map, "ic_sith_saber_throw", 4.0D, 18.0D, (level, mob, target) -> saberThrow(level, mob, target, RED));
        add(map, "ic_saber_dash", 3.0D, 12.0D, (level, mob, target) -> dash(level, mob, target, 3.0D, 0.8D, 2, glow(BLUE, 1.2F),
                SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, null));
        add(map, "ic_green_saber_dash", 3.0D, 12.0D, (level, mob, target) -> dash(level, mob, target, 3.0D, 0.8D, 2, glow(GREEN, 1.2F),
                SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F, null));
        add(map, "ic_saber_spin", 0.0D, 5.0D, (level, mob, target) -> spin(level, mob, target, 4, 2.6D, 0.55D, glow(RED, 1.1F),
                SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 0.6F));
        add(map, "ic_grievous_spin", 0.0D, 6.0D, (level, mob, target) -> spin(level, mob, target, 7, 3.2D, 0.45D, glow(STORM_BOLT, 1.1F),
                SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 0.55F));

        add(map, "ic_blaster_volley", 5.0D, 24.0D, (level, mob, target) -> volley(level, mob, target, 4, 4, 2.4D, 0.5D, 0.6D, glow(RED, 1.0F), 0.25D,
                "thematic:dc15s_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 1.1F, null, null));
        add(map, "ic_clone_volley", 5.0D, 24.0D, (level, mob, target) -> volley(level, mob, target, 4, 4, 2.4D, 0.5D, 0.5D, glow(BLUE, 1.0F), 0.25D,
                "thematic:dc15s_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, null, null));
        add(map, "ic_dual_blasters", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 6, 2, 2.6D, 0.35D, 0.7D, glow(BLUE, 0.8F), 0.3D,
                "thematic:dc17_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 1.2F, null, null));
        add(map, "ic_heavy_blaster", 4.0D, 20.0D, (level, mob, target) -> volley(level, mob, target, 3, 7, 2.2D, 0.85D, 0.3D, glow(RED, 1.4F), 0.2D,
                "thematic:westar34_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 0.9F, null, null));
        add(map, "ic_bowcaster", 5.0D, 22.0D, (level, mob, target) -> volley(level, mob, target, 1, 1, 2.0D, 1.7D, 0.0D, glow(ORANGE, 1.8F), 0.2D,
                "thematic:ee3carbine_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 0.6F, victim -> shove(mob, victim, 1.6D, 0.35D), end -> pop(level, end, ORANGE)));
        add(map, "ic_thermal_detonator", 5.0D, 18.0D, (level, mob, target) -> grenade(level, mob, target, 20, glow(RED, 0.9F), 3.8D, 1.7D));
        add(map, "ic_frag_grenade", 5.0D, 18.0D, (level, mob, target) -> grenade(level, mob, target, 12, ParticleTypes.SMOKE, 3.5D, 1.5D));
        add(map, "ic_molotov", 4.0D, 16.0D, IconKitMoves::molotov);
        add(map, "ic_assault_rifle", 5.0D, 24.0D, (level, mob, target) -> volley(level, mob, target, 8, 2, 3.0D, 0.3D, 0.6D, ParticleTypes.CRIT, 0.6D,
                "thematic:m4a1", SoundEvents.FIREWORK_ROCKET_BLAST, 1.6F, null, null));
        add(map, "ic_dead_eye", 4.0D, 22.0D, (level, mob, target) -> volley(level, mob, target, 5, 2, 3.2D, 0.55D, 0.0D, ParticleTypes.CRIT, 0.6D,
                "scguns:item.revolver.fire", SoundEvents.FIREWORK_ROCKET_BLAST, 1.0F, null, null));
        add(map, "ic_rifle_shots", 6.0D, 28.0D, (level, mob, target) -> volley(level, mob, target, 3, 10, 3.4D, 0.9D, 0.1D, ParticleTypes.CRIT, 0.5D,
                "thematic:rifle_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 0.9F, null, null));
        add(map, "ic_samaritan", 5.0D, 24.0D, (level, mob, target) -> volley(level, mob, target, 1, 1, 3.0D, 1.8D, 0.0D, ParticleTypes.CRIT, 0.4D,
                "thematic:magnum_fire", SoundEvents.FIREWORK_ROCKET_BLAST, 0.7F, victim -> shove(mob, victim, 1.2D, 0.3D), end -> blast(level, mob, end, 2.2D, 0.6D)));
        add(map, "ic_shotgun_blast", 0.0D, 7.0D, IconKitMoves::shotgun);

        add(map, "ic_ice_ball", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 1, 1, 1.2D, 0.6D, 0.0D, ParticleTypes.SNOWFLAKE, 0.15D,
                "thematic:frost_breath", SoundEvents.SNOW_GOLEM_SHOOT, 1.2F, victim -> freeze(victim, 60, true), end -> shatter(level, end)));
        add(map, "ic_ice_slide", 3.0D, 10.0D, (level, mob, target) -> dash(level, mob, target, 2.0D, 1.0D, 1, ParticleTypes.SNOWFLAKE,
                "minecraft:block.powder_snow.break", SoundEvents.POWDER_SNOW_BREAK, 0.8F, victim -> freeze(victim, 40, false)));
        add(map, "ic_spear_pull", 4.0D, 16.0D, IconKitMoves::spearPull);
        add(map, "ic_raiden_teleport", 3.0D, 20.0D, (level, mob, target) -> blinkStrike(level, mob, target, at -> sparkFx(level, at),
                "minecraft:entity.lightning_bolt.impact", SoundEvents.LIGHTNING_BOLT_IMPACT, 4, 1.3D, 0.8D, 0.3D,
                victim -> zap(level, HeroKit.hand(mob), HeroKit.chest(victim), glow(STORM_BOLT, 1.0F))));
        add(map, "ic_dragon_fireball", 4.0D, 20.0D, (level, mob, target) -> volley(level, mob, target, 2, 8, 1.4D, 0.9D, 0.1D, ParticleTypes.FLAME, 0.15D,
                "minecraft:entity.blaze.shoot", SoundEvents.BLAZE_SHOOT, 0.8F, victim -> victim.igniteForSeconds(4.0F), end -> flare(level, end)));
        add(map, "ic_bicycle_kick", 3.0D, 10.0D, (level, mob, target) -> dash(level, mob, target, 2.0D, 0.45D, 4, ParticleTypes.CLOUD,
                "minecraft:entity.player.attack.knockback", SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.2F, null));
        add(map, "ic_fan_throw", 4.0D, 18.0D, (level, mob, target) -> toss(level, mob, target, new ItemStack(HeroFx.item("thematic:kitana_fan", Items.FEATHER)),
                1.2D, 2, 50.0F, glow(BLUE, 0.7F), 1.0D, null));
        add(map, "ic_hat_throw", 4.0D, 18.0D, (level, mob, target) -> toss(level, mob, target, new ItemStack(Items.IRON_HELMET),
                1.3D, 1, 70.0F, ParticleTypes.CRIT, 1.3D, null));
        add(map, "ic_staff_throw", 4.0D, 18.0D, (level, mob, target) -> toss(level, mob, target, new ItemStack(HeroFx.item("thematic:jade_staff", Items.STICK)),
                1.2D, 1, 55.0F, glow(GREEN, 0.8F), 1.1D, null));
        add(map, "ic_sai_throw", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 2, 4, 2.2D, 0.7D, 0.3D,
                item("thematic:mileena_sai", Items.IRON_SWORD), 0.4D, THROW, SoundEvents.ARROW_SHOOT, 1.3F, IconKitMoves::bleed, null));
        add(map, "ic_knife_throw", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 2, 5, 2.2D, 0.7D, 0.2D,
                item("thematic:kano_knife", Items.IRON_SWORD), 0.4D, THROW, SoundEvents.ARROW_SHOOT, 1.4F, IconKitMoves::bleed, null));
        add(map, "ic_throwing_knives", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 3, 2, 2.4D, 0.55D, 0.8D,
                item("thematic:knife", Items.IRON_SWORD), 0.4D, THROW, SoundEvents.ARROW_SHOOT, 1.5F, null, null));
        add(map, "ic_shuriken", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 3, 2, 2.4D, 0.45D, 0.9D,
                item("minecraft:iron_nugget", Items.IRON_NUGGET), 0.3D, "minecraft:entity.arrow.shoot", SoundEvents.ARROW_SHOOT, 1.8F, null, null));
        add(map, "ic_soul_steal", 2.0D, 14.0D, IconKitMoves::soulSteal);
        add(map, "ic_shadow_strike", 2.0D, 14.0D, IconKitMoves::shadowStrike);
        add(map, "ic_smoke_teleport", 3.0D, 18.0D, (level, mob, target) -> blinkStrike(level, mob, target, at -> smokeFx(level, at),
                "minecraft:entity.enderman.teleport", SoundEvents.ENDERMAN_TELEPORT, 4, 1.2D, 1.0D, 0.5D, null));
        add(map, "ic_teleport_kick", 3.0D, 18.0D, (level, mob, target) -> blinkStrike(level, mob, target, at -> portalFx(level, at),
                "minecraft:entity.enderman.teleport", SoundEvents.ENDERMAN_TELEPORT, 3, 1.2D, 1.4D, 0.4D, null));
        add(map, "ic_tele_uppercut", 3.0D, 18.0D, (level, mob, target) -> blinkStrike(level, mob, target, at -> flameFx(level, at),
                "minecraft:entity.blaze.shoot", SoundEvents.BLAZE_SHOOT, 4, 1.3D, 0.3D, 1.0D, null));
        add(map, "ic_acid_spit", 3.0D, 16.0D, (level, mob, target) -> volley(level, mob, target, 2, 6, 1.4D, 0.5D, 0.2D, ACID_DUST, 0.2D,
                "minecraft:entity.llama.spit", SoundEvents.LLAMA_SPIT, 0.8F, victim -> victim.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 1), mob),
                end -> level.sendParticles(ParticleTypes.ITEM_SLIME, end.x, end.y, end.z, 10, 0.3D, 0.3D, 0.3D, 0.05D)));
        add(map, "ic_grab_pummel", 0.0D, 3.5D, IconKitMoves::grabPummel);
        add(map, "ic_sun_beam", 3.0D, 20.0D, IconKitMoves::sunBeam);
        add(map, "ic_energy_rings", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 3, 5, 1.6D, 0.55D, 0.2D, glow(PINK, 1.4F), 0.15D,
                "minecraft:block.beacon.power_select", SoundEvents.BEACON_POWER_SELECT, 1.8F, null, end -> pop(level, end, PINK)));
        add(map, "ic_force_ball", 4.0D, 18.0D, (level, mob, target) -> volley(level, mob, target, 1, 1, 1.5D, 1.0D, 0.0D, glow(GREEN, 1.6F), 0.12D,
                "minecraft:block.beacon.power_select", SoundEvents.BEACON_POWER_SELECT, 1.4F, victim -> shove(mob, victim, 1.0D, 0.3D), end -> pop(level, end, GREEN)));
        add(map, "ic_shadow_kick", 3.0D, 11.0D, (level, mob, target) -> dash(level, mob, target, 2.0D, 1.4D, 1, glow(GREEN, 1.3F),
                "minecraft:entity.player.attack.knockback", SoundEvents.PLAYER_ATTACK_KNOCKBACK, 0.9F, victim -> shove(mob, victim, 1.6D, 0.4D)));
        add(map, "ic_cannonball", 3.0D, 11.0D, (level, mob, target) -> dash(level, mob, target, 1.5D, 1.5D, 1, ParticleTypes.CRIT,
                "minecraft:entity.player.attack.strong", SoundEvents.PLAYER_ATTACK_STRONG, 0.8F, victim -> shove(mob, victim, 1.8D, 0.4D)));
        add(map, "ic_net", 4.0D, 16.0D, (level, mob, target) -> volley(level, mob, target, 1, 1, 1.4D, 0.4D, 0.0D, ParticleTypes.CRIT, 0.2D,
                "minecraft:entity.arrow.shoot", SoundEvents.ARROW_SHOOT, 0.6F, IconKitMoves::netted, null));
        add(map, "ic_time_stop", 0.0D, 8.0D, IconKitMoves::timeStop);
        add(map, "ic_sand_storm", 0.0D, 9.0D, IconKitMoves::sandStorm);
        add(map, "ic_sand_grasp", 3.0D, 16.0D, IconKitMoves::sandGrasp);
        add(map, "ic_blade_spin", 0.0D, 4.5D, (level, mob, target) -> spin(level, mob, target, 4, 2.6D, 0.55D, ParticleTypes.CRIT,
                SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F));
        add(map, "ic_nunchuck_spin", 0.0D, 4.5D, (level, mob, target) -> spin(level, mob, target, 4, 2.4D, 0.5D, ParticleTypes.CRIT,
                "thematic:nunchuck_twirl", SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F));
        add(map, "ic_blade_flurry", 0.0D, 4.0D, (level, mob, target) -> flurry(level, mob, target, 3, 4, 0.7D, ParticleTypes.CRIT,
                SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 1.2F));
        add(map, "ic_roundhouse", 0.0D, 5.0D, (level, mob, target) -> heavy(level, mob, target, 4, 1.2D, 1.8D, 0.4D, "air_burst_ring", ParticleTypes.SWEEP_ATTACK,
                "minecraft:entity.player.attack.knockback", SoundEvents.PLAYER_ATTACK_KNOCKBACK, null));

        add(map, "ic_machete_lunge", 2.0D, 9.0D, IconKitMoves::macheteLunge);
        add(map, "ic_stalk_teleport", 6.0D, 28.0D, IconKitMoves::stalk);
        add(map, "ic_slasher_stab", 0.0D, 4.5D, (level, mob, target) -> heavy(level, mob, target, 10, 1.8D, 0.4D, 0.05D, "impact_starburst", BLOOD_DUST,
                "thematic:myers_knife_hit", SoundEvents.PLAYER_ATTACK_CRIT, IconKitMoves::bleed));
        add(map, "ic_strangle", 0.0D, 3.0D, IconKitMoves::strangle);
        add(map, "ic_dream_claws", 0.0D, 5.0D, IconKitMoves::dreamClaws);
        add(map, "ic_deadlights", 3.0D, 14.0D, IconKitMoves::deadlights);
        add(map, "ic_hell_chains", 3.0D, 16.0D, IconKitMoves::hellChains);
        add(map, "ic_chainsaw_rush", 0.0D, 8.0D, IconKitMoves::chainsaw);
        add(map, "ic_bear_trap", 2.0D, 14.0D, IconKitMoves::bearTrap);

        add(map, "ic_axe_throw", 4.0D, 20.0D, (level, mob, target) -> toss(level, mob, target, new ItemStack(HeroFx.item("thematic:kratos_hachet", Items.IRON_AXE)),
                1.3D, 1, 40.0F, ParticleTypes.SNOWFLAKE, 1.5D, victim -> freeze(victim, 50, true)));
        add(map, "ic_spartan_rage", 0.0D, 6.0D, IconKitMoves::spartanRage);
        add(map, "ic_chaos_blades", 3.0D, 11.0D, IconKitMoves::chaosBlades);
        add(map, "ic_hidden_blade", 2.0D, 14.0D, (level, mob, target) -> blinkStrike(level, mob, target, at -> smokeFx(level, at),
                "thematic:myers_knife_hit", SoundEvents.PLAYER_ATTACK_CRIT, 2, 2.0D, 0.2D, 0.0D, IconKitMoves::bleed));
        add(map, "ic_doom_punch", 0.0D, 5.0D, (level, mob, target) -> heavy(level, mob, target, 10, 2.0D, 2.6D, 0.6D, "punch_pressure_wave", ParticleTypes.EXPLOSION,
                "minecraft:entity.iron_golem.attack", SoundEvents.IRON_GOLEM_ATTACK, victim -> HeroFx.shake(level, HeroKit.chest(victim), 10.0D, 0.6F, 8)));
        add(map, "ic_boulder_punch", 0.0D, 5.0D, (level, mob, target) -> heavy(level, mob, target, 8, 1.7D, 2.2D, 0.5D, "punch_pressure_wave", ParticleTypes.CRIT,
                "minecraft:entity.player.attack.strong", SoundEvents.PLAYER_ATTACK_STRONG, null));
        add(map, "ic_zorro_slash", 0.0D, 4.0D, IconKitMoves::zorro);
        add(map, "ic_whip_crack", 2.0D, 9.0D, IconKitMoves::whip);
    }

    private static void add(Map<String, VariantMoves.Move> map, String id, double min, double max, VariantMoves.Runner runner) {
        map.put(id, new VariantMoves.Move(id, min, max, runner));
    }

    private static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F), size);
    }

    private static ParticleOptions glow(int rgb, float size) {
        return HeroAbilities.emissive(rgb, size);
    }

    private static ParticleOptions item(String id, Item fallback) {
        return new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(HeroFx.item(id, fallback)));
    }

    private static boolean near(Mob mob, LivingEntity target, double reach) {
        return !HeroKit.gone(mob, target) && HeroKit.foe(mob, target) && mob.distanceTo(target) <= reach;
    }

    private static void stun(LivingEntity target, int ticks) {
        if (HeroAbilities.canControl(target)) {
            HeroStun.stun(target, ticks);
        }
    }

    private static void shove(Mob mob, LivingEntity victim, double push, double lift) {
        Vec3 away = HeroKit.toward(mob, victim);
        victim.push(away.x * push, lift, away.z * push);
        victim.hurtMarked = true;
    }

    private static void yank(Mob mob, LivingEntity victim, double strength) {
        if (!HeroAbilities.canControl(victim)) {
            return;
        }
        Vec3 pull = mob.position().subtract(victim.position()).multiply(1.0D, 0.0D, 1.0D);
        Vec3 dir = pull.lengthSqr() < 1.0E-4D ? Vec3.ZERO : pull.normalize();
        victim.setDeltaMovement(dir.x * strength, 0.3D, dir.z * strength);
        victim.hurtMarked = true;
    }

    private static void bleed(LivingEntity victim) {
        victim.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
        Vec3 at = HeroKit.chest(victim);
        if (victim.level() instanceof ServerLevel level) {
            level.sendParticles(BLOOD_DUST, at.x, at.y, at.z, 10, 0.3D, 0.4D, 0.3D, 0.0D);
        }
    }

    private static void freeze(LivingEntity victim, int ticks, boolean hold) {
        victim.setTicksFrozen(Math.min(victim.getTicksRequiredToFreeze() + 60, victim.getTicksFrozen() + ticks));
        victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ticks, 2));
        if (hold && HeroAbilities.canControl(victim)) {
            VillainWorld.stick(victim, ticks / 2);
        }
    }

    private static void netted(LivingEntity victim) {
        if (HeroAbilities.canControl(victim)) {
            VillainWorld.stick(victim, 50);
        } else {
            victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
        }
        Vec3 at = HeroKit.chest(victim);
        if (victim.level() instanceof ServerLevel level) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.COBWEB.defaultBlockState()), at.x, at.y, at.z, 24, 0.5D, 0.6D, 0.5D, 0.05D);
        }
    }

    private static void zap(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions fx) {
        Vec3 prev = from;
        int segments = 6;
        for (int s = 1; s <= segments; s++) {
            Vec3 point = from.lerp(to, s / (double) segments);
            if (s < segments) {
                point = point.add((level.random.nextDouble() - 0.5D) * 0.7D, (level.random.nextDouble() - 0.5D) * 0.7D, (level.random.nextDouble() - 0.5D) * 0.7D);
            }
            HeroKit.line(level, prev, point, fx, 0.15D);
            prev = point;
        }
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, to.x, to.y, to.z, 6, 0.2D, 0.2D, 0.2D, 0.1D);
    }

    private static void sparkFx(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, at.x, at.y + 1.0D, at.z, 24, 0.4D, 0.8D, 0.4D, 0.2D);
        level.sendParticles(glow(STORM_BOLT, 1.2F), at.x, at.y + 1.0D, at.z, 10, 0.3D, 0.7D, 0.3D, 0.0D);
    }

    private static void smokeFx(ServerLevel level, Vec3 at) {
        ParticleOptions smoke = HeroFx.particle("thematic:thick_smoke", ParticleTypes.LARGE_SMOKE);
        level.sendParticles(smoke, at.x, at.y + 1.0D, at.z, 8, 0.4D, 0.6D, 0.4D, 0.02D);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 1.0D, at.z, 10, 0.4D, 0.6D, 0.4D, 0.03D);
    }

    private static void portalFx(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.PORTAL, at.x, at.y + 1.0D, at.z, 30, 0.4D, 0.7D, 0.4D, 0.4D);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, at.x, at.y + 1.0D, at.z, 10, 0.3D, 0.6D, 0.3D, 0.05D);
    }

    private static void flameFx(ServerLevel level, Vec3 at) {
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.0D, at.z, 16, 0.3D, 0.7D, 0.3D, 0.03D);
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 1.0D, at.z, 8, 0.3D, 0.6D, 0.3D, 0.02D);
    }

    private static void fogFx(ServerLevel level, Vec3 at) {
        level.sendParticles(HeroFx.particle("thematic:thick_smoke", ParticleTypes.LARGE_SMOKE), at.x, at.y + 0.6D, at.z, 14, 0.9D, 0.5D, 0.9D, 0.01D);
        level.sendParticles(ParticleTypes.CLOUD, at.x, at.y + 0.3D, at.z, 10, 1.0D, 0.2D, 1.0D, 0.01D);
    }

    private static void pop(ServerLevel level, Vec3 at, int color) {
        level.sendParticles(glow(color, 1.2F), at.x, at.y, at.z, 12, 0.3D, 0.3D, 0.3D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.2D, 0.2D, 0.2D, 0.1D);
    }

    private static void flare(ServerLevel level, Vec3 at) {
        HeroFx.vfx(level, at, "ember_flare", 0, () -> level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 14, 0.25D, 0.25D, 0.25D, 0.05D));
    }

    private static void shatter(ServerLevel level, Vec3 at) {
        HeroFx.vfx(level, at, "powergirl_ice_shatter", 0, () -> level.sendParticles(
                new BlockParticleOption(ParticleTypes.BLOCK, Blocks.PACKED_ICE.defaultBlockState()), at.x, at.y, at.z, 24, 0.4D, 0.4D, 0.4D, 0.1D));
        HeroKit.sound(level, at, SoundEvents.GLASS_BREAK, 0.9F, 0.8F);
    }

    private static void blast(ServerLevel level, Mob mob, Vec3 at, double radius, double scale) {
        HeroFx.vfx(level, at, "impact_starburst", 0, () -> level.sendParticles(ParticleTypes.EXPLOSION, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D));
        HeroKit.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 0.7F, 1.4F);
        for (LivingEntity victim : HeroKit.victims(level, mob, at, radius)) {
            victim.invulnerableTime = 0;
            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, 0.8D, 0.25D);
        }
    }

    private static void flurry(ServerLevel level, Mob mob, LivingEntity target, int hits, int gap, double scale, ParticleOptions fx,
            String sound, SoundEvent fallback, float pitch) {
        HeroKit.face(mob, target);
        for (int i = 0; i < hits; i++) {
            int index = i;
            VillainWorld.later(level, 2 + i * gap, () -> {
                if (!near(mob, target, 3.6D)) {
                    return;
                }
                HeroKit.face(mob, target);
                mob.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                target.invulnerableTime = 0;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), 0.25D, 0.05D);
                Vec3 at = HeroKit.chest(target);
                Vec3 side = HeroKit.rotate(HeroKit.toward(mob, target), 90.0D).scale(index % 2 == 0 ? 0.8D : -0.8D);
                HeroKit.line(level, at.add(side).add(0.0D, 0.5D, 0.0D), at.subtract(side).add(0.0D, -0.4D, 0.0D), fx, 0.12D);
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, at.y, at.z, 1, 0.1D, 0.1D, 0.1D, 0.0D);
                HeroKit.cue(level, at, sound, fallback, 0.9F, pitch + level.random.nextFloat() * 0.2F);
            });
        }
    }

    private static void saberFlurry(ServerLevel level, Mob mob, LivingEntity target, int color) {
        HeroKit.sound(level, mob.position(), SoundEvents.BEACON_ACTIVATE, 0.5F, 1.8F);
        flurry(level, mob, target, 4, 4, 0.6D, glow(color, 1.0F), SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 0.7F);
    }

    private static ItemStack held(Mob mob, String id, Item fallback) {
        ItemStack main = mob.getMainHandItem();
        return main.isEmpty() ? new ItemStack(HeroFx.item(id, fallback)) : main.copy();
    }

    private static void saberThrow(ServerLevel level, Mob mob, LivingEntity target, int color) {
        toss(level, mob, target, held(mob, "thematic:vader_saber", Items.NETHERITE_SWORD), 1.2D, 1, 60.0F, glow(color, 1.0F), 1.2D, null);
    }

    private static void toss(ServerLevel level, Mob mob, LivingEntity target, ItemStack stack, double speed, int bounces, float spin,
            ParticleOptions trail, double scale, Consumer<LivingEntity> extra) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.cue(level, mob.position(), THROW, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.0F);
        HeroKit.hurl(level, mob, target, stack, speed, bounces, spin,
                (lvl, a, b) -> HeroKit.line(lvl, a, b, trail, 0.35D),
                (victim, at) -> {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, 0.6D, 0.15D);
                    if (extra != null) {
                        extra.accept(victim);
                    }
                    Vec3 hitAt = HeroKit.chest(victim);
                    level.sendParticles(ParticleTypes.CRIT, hitAt.x, hitAt.y, hitAt.z, 8, 0.3D, 0.3D, 0.3D, 0.1D);
                    HeroKit.sound(level, hitAt, SoundEvents.PLAYER_ATTACK_STRONG, 0.9F, 1.1F);
                },
                () -> HeroKit.sound(level, mob.position(), SoundEvents.ARMOR_EQUIP_IRON.value(), 0.7F, 1.3F));
    }

    private static void volley(ServerLevel level, Mob mob, LivingEntity target, int shots, int gap, double speed, double scale, double spread,
            ParticleOptions trail, double spacing, String sound, SoundEvent fallback, float pitch, Consumer<LivingEntity> extra, Consumer<Vec3> burst) {
        HeroKit.face(mob, target);
        int life = (int) Math.ceil(30.0D / speed) + 6;
        for (int i = 0; i < shots; i++) {
            VillainWorld.later(level, 4 + i * gap, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                mob.swing(InteractionHand.MAIN_HAND);
                Vec3 from = HeroKit.hand(mob);
                Vec3 aim = HeroKit.chest(target).add((level.random.nextDouble() - 0.5D) * spread, (level.random.nextDouble() - 0.5D) * spread,
                        (level.random.nextDouble() - 0.5D) * spread);
                Vec3 dir = aim.subtract(from).normalize();
                level.sendParticles(ParticleTypes.SMOKE, from.x + dir.x * 0.6D, from.y + dir.y * 0.6D, from.z + dir.z * 0.6D, 2, 0.05D, 0.05D, 0.05D, 0.01D);
                HeroKit.cue(level, from, sound, fallback, 0.8F, pitch);
                HeroKit.bolt(level, mob, from.add(dir.scale(0.6D)), dir.scale(speed), life, 0.35D, null, 0.0D,
                        (lvl, a, b) -> HeroKit.line(lvl, a, b, trail, spacing),
                        (victim, at) -> {
                            victim.invulnerableTime = 0;
                            HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), at, 0.15D, 0.0D);
                            if (extra != null) {
                                extra.accept(victim);
                            }
                        },
                        burst);
            });
        }
    }

    private static void lob(ServerLevel level, Mob mob, LivingEntity target, int flight, ParticleOptions trail, Consumer<Vec3> onLand) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.MAIN_HAND);
        Vec3 from = HeroKit.hand(mob);
        Vec3 to = target.position().add(target.getDeltaMovement().multiply(flight * 0.5D, 0.0D, flight * 0.5D));
        double gravity = 0.06D;
        Vec3 velocity = new Vec3((to.x - from.x) / flight, (to.y - from.y) / flight + 0.5D * gravity * flight, (to.z - from.z) / flight);
        HeroKit.sound(level, from, SoundEvents.SNOWBALL_THROW, 0.8F, 0.6F);
        arc(level, mob, from, velocity, gravity, flight + 12, trail, onLand);
    }

    private static void arc(ServerLevel level, Mob mob, Vec3 at, Vec3 velocity, double gravity, int life, ParticleOptions trail, Consumer<Vec3> onLand) {
        if (!mob.isAlive()) {
            return;
        }
        Vec3 next = at.add(velocity);
        Vec3 stop = HeroKit.clip(level, mob, at, next);
        List<LivingEntity> struck = HeroKit.along(level, mob, at, stop, 0.3D);
        Vec3 end = struck.isEmpty() ? stop : HeroKit.chest(struck.get(0));
        HeroKit.line(level, at, end, trail, 0.4D);
        if (!struck.isEmpty() || stop != next || life <= 1) {
            onLand.accept(end.add(0.0D, 0.2D, 0.0D));
            return;
        }
        Vec3 carry = velocity.subtract(0.0D, gravity, 0.0D);
        VillainWorld.later(level, 1, () -> arc(level, mob, next, carry, gravity, life - 1, trail, onLand));
    }

    private static void grenade(ServerLevel level, Mob mob, LivingEntity target, int fuse, ParticleOptions trail, double radius, double scale) {
        lob(level, mob, target, 16, trail, at -> {
            for (int t = 0; t < fuse; t += 5) {
                int tick = t;
                VillainWorld.later(level, tick, () -> {
                    level.sendParticles(glow(RED, 1.0F), at.x, at.y, at.z, 2, 0.05D, 0.05D, 0.05D, 0.0D);
                    HeroKit.sound(level, at, SoundEvents.NOTE_BLOCK_PLING.value(), 0.6F, 1.6F + tick * 0.02F);
                });
            }
            VillainWorld.later(level, fuse, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                if (!HeroFx.vfx(level, at, "ignition_burst", 0, (float) radius)) {
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, at.x, at.y, at.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y, at.z, 20, radius * 0.4D, 0.4D, radius * 0.4D, 0.02D);
                HeroKit.sound(level, at, SoundEvents.GENERIC_EXPLODE.value(), 1.2F, 1.0F);
                for (LivingEntity victim : HeroKit.victims(level, mob, at, radius)) {
                    double falloff = 1.0D - 0.5D * victim.position().distanceTo(at) / radius;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale * falloff), at, 1.2D, 0.45D);
                }
            });
        });
    }

    private static void molotov(ServerLevel level, Mob mob, LivingEntity target) {
        lob(level, mob, target, 14, ParticleTypes.FLAME, at -> {
            HeroKit.sound(level, at, SoundEvents.GLASS_BREAK, 1.0F, 1.0F);
            HeroKit.sound(level, at, SoundEvents.FIRECHARGE_USE, 1.0F, 0.8F);
            for (int pulse = 0; pulse < 4; pulse++) {
                VillainWorld.later(level, pulse * 10, () -> {
                    if (!mob.isAlive()) {
                        return;
                    }
                    level.sendParticles(ParticleTypes.FLAME, at.x, at.y, at.z, 30, 1.4D, 0.2D, 1.4D, 0.02D);
                    level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.5D, at.z, 10, 1.2D, 0.3D, 1.2D, 0.02D);
                    for (LivingEntity victim : HeroKit.victims(level, mob, at, 3.0D)) {
                        victim.invulnerableTime = 0;
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.35D), at, 0.0D, 0.0D);
                        victim.igniteForSeconds(5.0F);
                    }
                });
            }
        });
    }

    private static void shotgun(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            HeroKit.face(mob, target);
            mob.swing(InteractionHand.MAIN_HAND);
            Vec3 from = HeroKit.hand(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            for (int i = -2; i <= 2; i++) {
                Vec3 spread = HeroKit.rotate(dir, i * 7.0D);
                Vec3 end = HeroKit.clip(level, mob, from, from.add(spread.scale(7.0D)));
                HeroKit.line(level, from, end, ParticleTypes.CRIT, 0.5D);
            }
            level.sendParticles(ParticleTypes.LARGE_SMOKE, from.x + dir.x, from.y + dir.y, from.z + dir.z, 4, 0.1D, 0.1D, 0.1D, 0.02D);
            HeroKit.cue(level, from, "thematic:shotgun_fire", SoundEvents.GENERIC_EXPLODE.value(), 1.0F, 1.0F);
            for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 7.0D, 25.0D)) {
                double close = 1.0D - 0.5D * victim.position().distanceTo(mob.position()) / 7.0D;
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D * close), from, 1.5D, 0.3D);
            }
        });
    }

    private static void spin(ServerLevel level, Mob mob, LivingEntity target, int pulses, double radius, double scale, ParticleOptions fx,
            String sound, SoundEvent fallback, float pitch) {
        for (int i = 0; i < pulses; i++) {
            int index = i;
            VillainWorld.later(level, i * 4, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                if (target.isAlive() && target.level() == mob.level()) {
                    Vec3 dir = HeroKit.toward(mob, target);
                    mob.setDeltaMovement(dir.x * 0.35D, mob.getDeltaMovement().y, dir.z * 0.35D);
                    mob.hurtMarked = true;
                }
                mob.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                Vec3 center = mob.position().add(0.0D, mob.getBbHeight() * 0.55D, 0.0D);
                for (int k = 0; k < 16; k++) {
                    double angle = Math.toRadians(k * 22.5D + index * 40.0D);
                    level.sendParticles(fx, center.x + Math.cos(angle) * radius * 0.8D, center.y, center.z + Math.sin(angle) * radius * 0.8D,
                            1, 0.0D, 0.0D, 0.0D, 0.0D);
                }
                level.sendParticles(ParticleTypes.SWEEP_ATTACK, center.x, center.y, center.z, 2, radius * 0.4D, 0.1D, radius * 0.4D, 0.0D);
                HeroKit.cue(level, center, sound, fallback, 0.8F, pitch + index * 0.05F);
                for (LivingEntity victim : HeroKit.victims(level, mob, center, radius)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), center, 0.5D, 0.1D);
                }
            });
        }
    }

    private static void dash(ServerLevel level, Mob mob, LivingEntity target, double past, double scale, int hits, ParticleOptions fx,
            String sound, SoundEvent fallback, float pitch, Consumer<LivingEntity> extra) {
        HeroKit.face(mob, target);
        Vec3 dir = HeroKit.toward(mob, target);
        Vec3 from = mob.position();
        Vec3 to = HeroKit.clip(level, mob, from.add(0.0D, 0.5D, 0.0D), target.position().add(dir.scale(past)).add(0.0D, 0.5D, 0.0D)).subtract(0.0D, 0.5D, 0.0D);
        HeroKit.cue(level, from, sound, fallback, 1.0F, pitch);
        for (LivingEntity victim : HeroKit.along(level, mob, from.add(0.0D, 1.0D, 0.0D), to.add(0.0D, 1.0D, 0.0D), 1.0D)) {
            for (int h = 0; h < hits; h++) {
                int index = h;
                VillainWorld.later(level, h * 3, () -> {
                    if (!victim.isAlive() || !mob.isAlive()) {
                        return;
                    }
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, scale), from, 0.4D, 0.15D);
                    Vec3 at = HeroKit.chest(victim);
                    level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 6, 0.3D, 0.3D, 0.3D, 0.1D);
                    if (index == hits - 1 && extra != null) {
                        extra.accept(victim);
                    }
                });
            }
        }
        if (HeroKit.blink(level, mob, to)) {
            HeroKit.line(level, from.add(0.0D, 1.0D, 0.0D), mob.position().add(0.0D, 1.0D, 0.0D), fx, 0.3D);
            HeroKit.face(mob, target);
        }
    }

    private static void heavy(ServerLevel level, Mob mob, LivingEntity target, int windup, double scale, double push, double lift, String vfx,
            ParticleOptions fx, String sound, SoundEvent fallback, Consumer<LivingEntity> extra) {
        HeroKit.face(mob, target);
        Vec3 dir = HeroKit.toward(mob, target);
        if (mob.distanceTo(target) > 2.5D) {
            mob.setDeltaMovement(dir.x * 0.8D, 0.15D, dir.z * 0.8D);
            mob.hurtMarked = true;
        }
        VillainWorld.later(level, windup, () -> {
            if (!near(mob, target, 3.6D)) {
                return;
            }
            HeroKit.face(mob, target);
            mob.swing(InteractionHand.MAIN_HAND);
            if (!HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), push, lift)) {
                return;
            }
            Vec3 at = HeroKit.chest(target);
            HeroFx.vfx(level, at, vfx, 0);
            level.sendParticles(fx, at.x, at.y, at.z, fx == ParticleTypes.EXPLOSION ? 1 : 8, 0.3D, 0.3D, 0.3D, 0.05D);
            HeroKit.cue(level, at, sound, fallback, 1.0F, 1.0F);
            if (extra != null) {
                extra.accept(target);
            }
        });
    }

    private static void blinkStrike(ServerLevel level, Mob mob, LivingEntity target, Consumer<Vec3> fx, String sound, SoundEvent fallback,
            int delay, double scale, double push, double lift, Consumer<LivingEntity> extra) {
        Vec3 from = mob.position();
        boolean moved = HeroKit.blink(level, mob, HeroKit.behind(target, 1.5D));
        for (int i = 1; i < 4 && !moved; i++) {
            moved = HeroKit.blink(level, mob, target.position().add(HeroKit.rotate(new Vec3(1.5D, 0.0D, 0.0D), i * 90.0D)));
        }
        if (!moved) {
            return;
        }
        fx.accept(from);
        fx.accept(mob.position());
        HeroKit.cue(level, mob.position(), sound, fallback, 1.0F, 1.0F);
        HeroKit.face(mob, target);
        VillainWorld.later(level, delay, () -> {
            if (!near(mob, target, 3.2D)) {
                return;
            }
            HeroKit.face(mob, target);
            mob.swing(InteractionHand.MAIN_HAND);
            if (HeroKit.hit(level, mob, target, HeroKit.damage(mob, scale), mob.position(), push, lift)) {
                Vec3 at = HeroKit.chest(target);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 10, 0.3D, 0.3D, 0.3D, 0.1D);
                HeroKit.sound(level, at, SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 1.0F);
                if (extra != null) {
                    extra.accept(target);
                }
            }
        });
    }

    private static void forcePush(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.OFF_HAND);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.hand(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            if (!HeroFx.vfx(level, from.add(dir.scale(2.0D)), "air_burst_ring", 0)) {
                level.sendParticles(ParticleTypes.CLOUD, from.x + dir.x * 2.0D, from.y + dir.y * 2.0D, from.z + dir.z * 2.0D, 10, 0.4D, 0.4D, 0.4D, 0.05D);
            }
            for (int i = -2; i <= 2; i++) {
                Vec3 spread = HeroKit.rotate(dir, i * 12.0D);
                HeroKit.line(level, from.add(spread), from.add(spread.scale(10.0D)), FORCE_DUST, 0.6D);
            }
            HeroKit.cue(level, from, "minecraft:entity.breeze.wind_burst", SoundEvents.PHANTOM_FLAP, 1.2F, 0.7F);
            for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 10.0D, 35.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), from, HeroAbilities.canControl(victim) ? 2.6D : 1.0D, 0.55D);
            }
        });
    }

    private static void forcePull(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.OFF_HAND);
        Vec3 hand = HeroKit.hand(mob);
        Vec3 at = HeroKit.chest(target);
        HeroKit.line(level, at, hand, FORCE_DUST, 0.4D);
        HeroKit.cue(level, hand, "minecraft:entity.illusioner.cast_spell", SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.8F);
        yank(mob, target, 1.5D);
        VillainWorld.later(level, 9, () -> {
            if (!near(mob, target, 3.6D)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.3D), mob.position(), 1.0D, 0.3D);
            Vec3 hit = HeroKit.chest(target);
            level.sendParticles(ParticleTypes.SWEEP_ATTACK, hit.x, hit.y, hit.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
            HeroKit.sound(level, hit, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.7F);
        });
    }

    private static void forceChoke(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        mob.swing(InteractionHand.OFF_HAND);
        HeroKit.cue(level, mob.position(), "minecraft:entity.illusioner.cast_spell", SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.6F);
        stun(target, 40);
        for (int t = 0; t <= 30; t += 6) {
            int tick = t;
            VillainWorld.later(level, 4 + tick, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 16.0D) {
                    return;
                }
                HeroKit.face(mob, target);
                if (tick < 12 && HeroAbilities.canControl(target)) {
                    target.setDeltaMovement(0.0D, 0.18D, 0.0D);
                    target.hurtMarked = true;
                    target.fallDistance = 0.0F;
                }
                Vec3 neck = target.getEyePosition().subtract(0.0D, 0.25D, 0.0D);
                level.sendParticles(FORCE_DUST, neck.x, neck.y, neck.z, 6, 0.2D, 0.1D, 0.2D, 0.0D);
                HeroKit.line(level, HeroKit.hand(mob), neck, FORCE_DUST, 1.2D);
                target.invulnerableTime = 0;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.3D), mob.position(), 0.0D, 0.0D);
                HeroKit.sound(level, neck, SoundEvents.PLAYER_HURT_DROWN, 0.7F, 0.7F);
            });
        }
    }

    private static void forceFreeze(ServerLevel level, Mob mob, LivingEntity target) {
        mob.swing(InteractionHand.OFF_HAND);
        HeroKit.ray(level, mob, target, 5, 16.0D, 0.6D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, FORCE_DUST, 0.5D);
                    HeroKit.cue(lvl, from, "minecraft:entity.illusioner.cast_spell", SoundEvents.ILLUSIONER_CAST_SPELL, 1.0F, 0.5F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.4D), from, 0.0D, 0.0D);
                    stun(victim, 50);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 3), mob);
                    Vec3 at = HeroKit.chest(victim);
                    level.sendParticles(FORCE_DUST, at.x, at.y, at.z, 20, 0.4D, 0.7D, 0.4D, 0.0D);
                },
                null);
    }

    private static void lightningHands(ServerLevel level, Mob mob, LivingEntity target, int color, int zaps) {
        HeroKit.face(mob, target);
        ParticleOptions bolt = glow(color, 0.9F);
        for (int i = 0; i < zaps; i++) {
            int index = i;
            VillainWorld.later(level, 4 + i * 3, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 15.0D) {
                    return;
                }
                HeroKit.face(mob, target);
                mob.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                Vec3 from = HeroKit.hand(mob);
                Vec3 to = HeroKit.chest(target);
                zap(level, from, to, bolt);
                target.invulnerableTime = 0;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.35D), from, 0.15D, 0.05D);
                if (index == 0) {
                    stun(target, 12);
                }
                if (index % 3 == 2) {
                    for (LivingEntity other : HeroKit.victims(level, mob, to, 4.0D)) {
                        if (other != target) {
                            zap(level, to, HeroKit.chest(other), bolt);
                            other.invulnerableTime = 0;
                            HeroKit.hit(level, mob, other, HeroKit.damage(mob, 0.3D), to, 0.1D, 0.0D);
                            break;
                        }
                    }
                }
                HeroKit.sound(level, to, SoundEvents.LIGHTNING_BOLT_IMPACT, 0.4F, 1.6F + level.random.nextFloat() * 0.3F);
            });
        }
    }

    private static void spearPull(ServerLevel level, Mob mob, LivingEntity target) {
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.ray(level, mob, target, 6, 16.0D, 0.6D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, item("minecraft:chain", Items.CHAIN), 0.3D);
                    HeroKit.cue(lvl, from, "thematic:devils_chain", SoundEvents.CHAIN_PLACE, 1.0F, 1.0F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.7D), from, 0.0D, 0.0D);
                    yank(mob, victim, 1.6D);
                    stun(victim, 14);
                    VillainWorld.later(level, 10, () -> {
                        if (!near(mob, victim, 3.6D)) {
                            return;
                        }
                        mob.swing(InteractionHand.MAIN_HAND);
                        HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.2D), mob.position(), 0.4D, 0.8D);
                        HeroKit.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, 0.9F);
                    });
                },
                null);
    }

    private static void soulSteal(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.cue(level, mob.position(), "minecraft:entity.evoker.cast_spell", SoundEvents.EVOKER_CAST_SPELL, 1.0F, 0.7F);
        for (int t = 0; t < 4; t++) {
            VillainWorld.later(level, 6 + t * 5, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 14.0D) {
                    return;
                }
                Vec3 from = HeroKit.chest(target);
                Vec3 to = HeroKit.hand(mob);
                HeroKit.line(level, from, to, ParticleTypes.SOUL, 0.5D);
                HeroKit.line(level, from, to, glow(GREEN, 0.8F), 0.4D);
                float amount = HeroKit.damage(mob, 0.4D);
                target.invulnerableTime = 0;
                if (HeroKit.hit(level, mob, target, amount, mob.position(), 0.0D, 0.0D)) {
                    mob.heal(amount * 0.6F);
                }
            });
        }
    }

    private static void shadowStrike(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 start = mob.position();
        level.sendParticles(SHADOW_DUST, start.x, start.y + 1.0D, start.z, 20, 0.3D, 0.7D, 0.3D, 0.0D);
        HeroKit.cue(level, start, "minecraft:entity.warden.sonic_charge", SoundEvents.ENDERMAN_STARE, 0.7F, 1.6F);
        VillainWorld.later(level, 8, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 dir = HeroKit.toward(mob, target);
            Vec3 from = mob.position().add(0.0D, 1.0D, 0.0D);
            Vec3 to = HeroKit.clip(level, mob, from, target.position().add(dir.scale(3.0D)).add(0.0D, 1.0D, 0.0D));
            HeroKit.line(level, from, to, SHADOW_DUST, 0.25D);
            HeroKit.line(level, from, to, ParticleTypes.SQUID_INK, 0.8D);
            HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_KNOCKBACK, 1.0F, 0.6F);
            for (LivingEntity victim : HeroKit.along(level, mob, from, to, 0.9D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.4D), from, 1.6D, 0.4D);
            }
        });
    }

    private static void grabPummel(ServerLevel level, Mob mob, LivingEntity target) {
        if (!near(mob, target, 3.5D)) {
            return;
        }
        HeroKit.face(mob, target);
        stun(target, 28);
        HeroKit.sound(level, target.position(), SoundEvents.RAVAGER_ROAR, 0.6F, 1.3F);
        for (int i = 0; i < 4; i++) {
            int index = i;
            VillainWorld.later(level, 5 + i * 6, () -> {
                if (!near(mob, target, 4.0D)) {
                    return;
                }
                HeroKit.face(mob, target);
                mob.swing(index % 2 == 0 ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
                target.invulnerableTime = 0;
                boolean last = index == 3;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, last ? 1.0D : 0.55D), mob.position(), last ? 2.2D : 0.0D, last ? 0.7D : 0.0D);
                Vec3 at = HeroKit.chest(target);
                level.sendParticles(ParticleTypes.CRIT, at.x, at.y, at.z, 8, 0.3D, 0.3D, 0.3D, 0.1D);
                HeroKit.sound(level, at, last ? SoundEvents.IRON_GOLEM_ATTACK : SoundEvents.PLAYER_ATTACK_STRONG, 1.0F, last ? 0.8F : 1.0F);
                if (last) {
                    HeroFx.vfx(level, at, "punch_pressure_wave", 0);
                }
            });
        }
    }

    private static void sunBeam(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 mark = target.position();
        VillainFx.ring(level, mark, 2.4D, GOLD_RING);
        level.sendParticles(glow(GOLD, 1.2F), mark.x, mark.y + 0.2D, mark.z, 20, 1.2D, 0.1D, 1.2D, 0.0D);
        HeroKit.sound(level, mark, SoundEvents.BEACON_ACTIVATE, 1.0F, 1.4F);
        VillainWorld.later(level, 14, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 at = target.isAlive() && target.position().distanceTo(mark) < 3.0D ? mark.add(target.position().subtract(mark).scale(0.5D)) : mark;
            Vec3 top = at.add(0.0D, 14.0D, 0.0D);
            HeroFx.beam(level, top, at, "pyre_ray", 0xFF000000 | GOLD, () -> HeroKit.line(level, top, at, glow(GOLD, 2.0F), 0.25D));
            HeroFx.vfx(level, at, "pyre_pillar", 0, () -> level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 0.5D, at.z, 30, 0.8D, 1.0D, 0.8D, 0.02D));
            HeroKit.sound(level, at, SoundEvents.BLAZE_SHOOT, 1.2F, 0.6F);
            for (LivingEntity victim : HeroKit.victims(level, mob, at.add(0.0D, 1.0D, 0.0D), 2.6D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.8D), at, 0.4D, 0.3D);
                victim.igniteForSeconds(4.0F);
            }
        });
    }

    private static void timeStop(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 center = mob.position();
        level.sendParticles(TIME_DUST, center.x, center.y + 1.0D, center.z, 30, 1.5D, 1.0D, 1.5D, 0.0D);
        HeroKit.sound(level, center, SoundEvents.BELL_BLOCK, 1.0F, 0.6F);
        VillainWorld.later(level, 10, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 at = mob.position();
            for (int k = 0; k < 24; k++) {
                double angle = Math.toRadians(k * 15.0D);
                level.sendParticles(TIME_DUST, at.x + Math.cos(angle) * 6.0D, at.y + 0.3D, at.z + Math.sin(angle) * 6.0D, 2, 0.1D, 0.3D, 0.1D, 0.0D);
            }
            level.sendParticles(ParticleTypes.ENCHANT, at.x, at.y + 1.0D, at.z, 40, 3.0D, 1.0D, 3.0D, 0.5D);
            HeroKit.sound(level, at, SoundEvents.BELL_RESONATE, 1.0F, 0.5F);
            for (LivingEntity victim : HeroKit.victims(level, mob, at, 6.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.4D), at, 0.0D, 0.0D);
                stun(victim, 40);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), mob);
            }
        });
    }

    private static void sandStorm(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        VillainWorld.later(level, 6, () -> {
            if (HeroKit.gone(mob, target)) {
                return;
            }
            Vec3 from = HeroKit.hand(mob);
            Vec3 dir = HeroKit.chest(target).subtract(from).normalize();
            ParticleOptions sand = new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.SAND.defaultBlockState());
            for (int i = -2; i <= 2; i++) {
                Vec3 spread = HeroKit.rotate(dir, i * 10.0D);
                HeroKit.line(level, from.add(spread), from.add(spread.scale(9.0D)), sand, 0.4D);
                HeroKit.line(level, from.add(spread), from.add(spread.scale(9.0D)), SAND_DUST, 0.8D);
            }
            HeroKit.sound(level, from, SoundEvents.SAND_BREAK, 1.2F, 0.6F);
            HeroKit.cue(level, from, "minecraft:entity.breeze.wind_burst", SoundEvents.PHANTOM_FLAP, 0.8F, 0.5F);
            for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 9.0D, 30.0D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), from, 0.8D, 0.1D);
                victim.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 40, 0), mob);
                victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1), mob);
            }
        });
    }

    private static void sandGrasp(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 mark = target.position();
        level.sendParticles(SAND_DUST, mark.x, mark.y + 0.1D, mark.z, 24, 1.0D, 0.05D, 1.0D, 0.0D);
        HeroKit.sound(level, mark, SoundEvents.SAND_BREAK, 1.0F, 0.5F);
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive()) {
                return;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.SAND.defaultBlockState()), mark.x, mark.y + 0.5D, mark.z, 40, 0.6D, 0.8D, 0.6D, 0.1D);
            HeroKit.sound(level, mark, SoundEvents.SAND_BREAK, 1.2F, 0.8F);
            for (LivingEntity victim : HeroKit.victims(level, mob, mark.add(0.0D, 0.5D, 0.0D), 2.2D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), mark, 0.0D, 0.0D);
                if (HeroAbilities.canControl(victim)) {
                    VillainWorld.stick(victim, 50);
                }
            }
        });
    }

    private static void macheteLunge(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 dir = HeroKit.toward(mob, target);
        HeroKit.face(mob, target);
        mob.setDeltaMovement(dir.x * 1.3D, 0.3D, dir.z * 1.3D);
        mob.hurtMarked = true;
        HeroKit.cue(level, mob.position(), "thematic:myers_breath", SoundEvents.ZOMBIE_AMBIENT, 0.8F, 0.6F);
        VillainWorld.later(level, 8, () -> {
            if (!near(mob, target, 3.6D)) {
                return;
            }
            mob.swing(InteractionHand.MAIN_HAND);
            if (HeroKit.hit(level, mob, target, HeroKit.damage(mob, 1.7D), mob.position(), 0.8D, 0.2D)) {
                bleed(target);
                HeroKit.cue(level, target.position(), "thematic:jason_machete_hit_" + (1 + level.random.nextInt(4)), SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.0F);
            }
        });
    }

    private static void stalk(ServerLevel level, Mob mob, LivingEntity target) {
        fogFx(level, mob.position());
        HeroKit.cue(level, target.position(), "thematic:myers_heartbeat", SoundEvents.WARDEN_HEARTBEAT, 1.0F, 1.0F);
        mob.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, false, false));
        VillainWorld.later(level, 12, () -> {
            if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target)) {
                return;
            }
            fogFx(level, target.position());
            blinkStrike(level, mob, target, at -> fogFx(level, at), "thematic:myers_knife_swing", SoundEvents.PLAYER_ATTACK_SWEEP, 6, 1.3D, 0.4D, 0.1D,
                    IconKitMoves::bleed);
        });
    }

    private static void strangle(ServerLevel level, Mob mob, LivingEntity target) {
        if (!near(mob, target, 3.0D)) {
            return;
        }
        HeroKit.face(mob, target);
        stun(target, 36);
        for (int i = 0; i < 4; i++) {
            int index = i;
            VillainWorld.later(level, 4 + i * 8, () -> {
                if (!near(mob, target, 3.6D)) {
                    return;
                }
                HeroKit.face(mob, target);
                if (index < 3 && HeroAbilities.canControl(target)) {
                    target.setDeltaMovement(0.0D, 0.12D, 0.0D);
                    target.hurtMarked = true;
                    target.fallDistance = 0.0F;
                }
                target.invulnerableTime = 0;
                boolean last = index == 3;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, last ? 0.9D : 0.4D), mob.position(), last ? 1.8D : 0.0D, last ? 0.3D : 0.0D);
                HeroKit.sound(level, target.position(), last ? SoundEvents.PLAYER_ATTACK_KNOCKBACK : SoundEvents.PLAYER_HURT_DROWN, 0.8F, 0.6F);
            });
        }
    }

    private static void dreamClaws(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.cue(level, mob.position(), "thematic:claw_open", SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.8F);
        for (int s = 0; s < 2; s++) {
            int index = s;
            VillainWorld.later(level, 4 + s * 6, () -> {
                if (HeroKit.gone(mob, target)) {
                    return;
                }
                HeroKit.face(mob, target);
                mob.swing(InteractionHand.MAIN_HAND);
                Vec3 from = HeroKit.hand(mob);
                Vec3 dir = HeroKit.toward(mob, target);
                for (int k = -2; k <= 2; k++) {
                    Vec3 a = from.add(HeroKit.rotate(dir, -40.0D + k * 4.0D).scale(2.4D)).add(0.0D, 0.4D - k * 0.15D, 0.0D);
                    Vec3 b = from.add(HeroKit.rotate(dir, 40.0D + k * 4.0D).scale(2.4D)).add(0.0D, -0.4D - k * 0.15D, 0.0D);
                    HeroKit.line(level, index == 0 ? a : b, index == 0 ? b : a, BLOOD_DUST, 0.2D);
                }
                HeroKit.sound(level, from, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 0.6F);
                for (LivingEntity victim : HeroKit.cone(level, mob, from, dir, 4.5D, 60.0D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), from, 0.4D, 0.05D);
                    victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0), mob);
                }
            });
        }
    }

    private static void deadlights(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 eye = mob.getEyePosition();
        level.sendParticles(glow(ORANGE, 1.4F), eye.x, eye.y, eye.z, 16, 0.4D, 0.3D, 0.4D, 0.0D);
        HeroKit.cue(level, eye, "thematic:chattering_teeth", SoundEvents.ENDERMAN_STARE, 0.9F, 0.7F);
        HeroKit.ray(level, mob, target, 10, 14.0D, 0.8D, false,
                (lvl, from, to) -> HeroKit.line(lvl, from, to, glow(ORANGE, 1.6F), 0.3D),
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.6D), from, 0.0D, 0.0D);
                    stun(victim, 50);
                    victim.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 120, 0), mob);
                    Vec3 at = HeroKit.chest(victim);
                    level.sendParticles(glow(ORANGE, 1.0F), at.x, at.y, at.z, 20, 0.5D, 0.7D, 0.5D, 0.0D);
                },
                null);
    }

    private static void hellChains(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, target.position(), SoundEvents.BELL_RESONATE, 0.8F, 0.5F);
        ParticleOptions chain = item("minecraft:chain", Items.CHAIN);
        for (int i = 0; i < 3; i++) {
            int index = i;
            VillainWorld.later(level, 6 + i * 5, () -> {
                if (HeroKit.gone(mob, target) || !HeroKit.foe(mob, target) || mob.distanceTo(target) > 18.0D) {
                    return;
                }
                Vec3 at = HeroKit.chest(target);
                Vec3 anchor = at.add(HeroKit.rotate(new Vec3(4.0D, 0.0D, 0.0D), index * 120.0D + level.random.nextInt(40))).add(0.0D, 1.5D, 0.0D);
                level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, anchor.x, anchor.y, anchor.z, 6, 0.2D, 0.2D, 0.2D, 0.01D);
                HeroKit.line(level, anchor, at, chain, 0.25D);
                HeroKit.cue(level, at, "thematic:devils_chain", SoundEvents.CHAIN_PLACE, 1.0F, 0.8F + index * 0.1F);
                target.invulnerableTime = 0;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.5D), anchor, 0.0D, 0.0D);
                bleed(target);
                if (index == 2 && HeroAbilities.canControl(target)) {
                    VillainWorld.stick(target, 50);
                }
            });
        }
    }

    private static void chainsaw(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        HeroKit.sound(level, mob.position(), SoundEvents.RAVAGER_ROAR, 0.7F, 1.6F);
        for (int t = 0; t < 8; t++) {
            int index = t;
            VillainWorld.later(level, 2 + t * 2, () -> {
                if (!mob.isAlive()) {
                    return;
                }
                if (target.isAlive() && target.level() == mob.level()) {
                    HeroKit.face(mob, target);
                    Vec3 dir = HeroKit.toward(mob, target);
                    mob.setDeltaMovement(dir.x * 0.45D, mob.getDeltaMovement().y, dir.z * 0.45D);
                    mob.hurtMarked = true;
                }
                mob.swing(InteractionHand.MAIN_HAND);
                Vec3 look = mob.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
                Vec3 front = HeroKit.hand(mob).add(look.lengthSqr() < 1.0E-4D ? Vec3.ZERO : look.normalize().scale(1.2D));
                level.sendParticles(ParticleTypes.SMOKE, front.x, front.y, front.z, 3, 0.1D, 0.1D, 0.1D, 0.02D);
                level.sendParticles(ParticleTypes.CRIT, front.x, front.y, front.z, 3, 0.2D, 0.2D, 0.2D, 0.1D);
                if (index % 2 == 0) {
                    HeroKit.sound(level, front, SoundEvents.SHEEP_SHEAR, 0.9F, 0.5F + level.random.nextFloat() * 0.2F);
                }
                for (LivingEntity victim : HeroKit.victims(level, mob, front, 1.8D)) {
                    victim.invulnerableTime = 0;
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.35D), mob.position(), 0.1D, 0.0D);
                    Vec3 at = HeroKit.chest(victim);
                    level.sendParticles(BLOOD_DUST, at.x, at.y, at.z, 6, 0.3D, 0.3D, 0.3D, 0.0D);
                }
            });
        }
    }

    private static void bearTrap(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        Vec3 mark = target.position();
        VillainFx.ring(level, mark, 1.2D, BLOOD_RING);
        level.sendParticles(item("thematic:jason_bear_trap", Items.IRON_BARS), mark.x, mark.y + 0.2D, mark.z, 12, 0.4D, 0.05D, 0.4D, 0.0D);
        HeroKit.sound(level, mark, SoundEvents.CHAIN_PLACE, 1.0F, 0.6F);
        VillainWorld.later(level, 10, () -> {
            if (!mob.isAlive()) {
                return;
            }
            HeroKit.sound(level, mark, SoundEvents.IRON_DOOR_CLOSE, 1.2F, 1.4F);
            level.sendParticles(ParticleTypes.CRIT, mark.x, mark.y + 0.3D, mark.z, 12, 0.4D, 0.2D, 0.4D, 0.1D);
            for (LivingEntity victim : HeroKit.victims(level, mob, mark, 1.8D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.1D), mark, 0.0D, 0.0D);
                bleed(victim);
                if (HeroAbilities.canControl(victim)) {
                    VillainWorld.stick(victim, 60);
                } else {
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2), mob);
                }
            }
        });
    }

    private static void spartanRage(ServerLevel level, Mob mob, LivingEntity target) {
        Vec3 at = mob.position();
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 160, 1));
        mob.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
        mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 160, 0));
        level.sendParticles(dust(0xD01010, 1.6F), at.x, at.y + 1.0D, at.z, 30, 0.5D, 0.8D, 0.5D, 0.0D);
        level.sendParticles(ParticleTypes.FLAME, at.x, at.y + 1.0D, at.z, 16, 0.5D, 0.8D, 0.5D, 0.02D);
        HeroKit.sound(level, at, SoundEvents.RAVAGER_ROAR, 1.2F, 0.7F);
        mob.setDeltaMovement(mob.getDeltaMovement().x * 0.2D, 0.55D, mob.getDeltaMovement().z * 0.2D);
        mob.hurtMarked = true;
        VillainWorld.later(level, 12, () -> {
            if (!mob.isAlive()) {
                return;
            }
            Vec3 center = mob.position();
            if (!HeroFx.vfx(level, center, "slam_fracture", 0, 4.0F)) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(mob.blockPosition().below())), center.x, center.y + 0.2D,
                        center.z, 50, 1.6D, 0.2D, 1.6D, 0.2D);
            }
            HeroKit.sound(level, center, SoundEvents.GENERIC_EXPLODE.value(), 0.9F, 0.8F);
            HeroFx.shake(level, center, 12.0D, 0.7F, 10);
            for (LivingEntity victim : HeroKit.victims(level, mob, center, 4.5D)) {
                HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.5D), center, 1.4D, 0.5D);
            }
        });
    }

    private static void chaosBlades(ServerLevel level, Mob mob, LivingEntity target) {
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.ray(level, mob, target, 6, 11.0D, 0.7D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, item("minecraft:chain", Items.CHAIN), 0.3D);
                    HeroKit.line(lvl, from, to, ParticleTypes.FLAME, 0.4D);
                    HeroKit.cue(lvl, from, "thematic:devils_chain", SoundEvents.CHAIN_PLACE, 1.0F, 1.2F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 1.0D), from, 0.0D, 0.0D);
                    victim.igniteForSeconds(3.0F);
                    yank(mob, victim, 1.3D);
                    VillainWorld.later(level, 8, () -> flurry(level, mob, victim, 2, 4, 0.8D, ParticleTypes.FLAME,
                            SWEEP, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F));
                },
                null);
    }

    private static void zorro(ServerLevel level, Mob mob, LivingEntity target) {
        HeroKit.face(mob, target);
        for (int i = 0; i < 3; i++) {
            int index = i;
            VillainWorld.later(level, 3 + i * 4, () -> {
                if (!near(mob, target, 3.6D)) {
                    return;
                }
                HeroKit.face(mob, target);
                mob.swing(InteractionHand.MAIN_HAND);
                Vec3 toward = HeroKit.toward(mob, target);
                Vec3 at = HeroKit.chest(target).subtract(toward.scale(0.4D));
                Vec3 side = HeroKit.rotate(toward, 90.0D).scale(0.6D);
                Vec3 topLeft = at.add(side).add(0.0D, 0.6D, 0.0D);
                Vec3 topRight = at.subtract(side).add(0.0D, 0.6D, 0.0D);
                Vec3 lowLeft = at.add(side).add(0.0D, -0.6D, 0.0D);
                Vec3 lowRight = at.subtract(side).add(0.0D, -0.6D, 0.0D);
                Vec3 a = index == 0 ? topLeft : index == 1 ? topRight : lowLeft;
                Vec3 b = index == 0 ? topRight : index == 1 ? lowLeft : lowRight;
                HeroKit.line(level, a, b, ParticleTypes.CRIT, 0.1D);
                target.invulnerableTime = 0;
                HeroKit.hit(level, mob, target, HeroKit.damage(mob, 0.7D), mob.position(), index == 2 ? 1.0D : 0.15D, 0.05D);
                HeroKit.sound(level, at, SoundEvents.PLAYER_ATTACK_SWEEP, 0.9F, 1.4F + index * 0.1F);
            });
        }
    }

    private static void whip(ServerLevel level, Mob mob, LivingEntity target) {
        mob.swing(InteractionHand.MAIN_HAND);
        HeroKit.ray(level, mob, target, 4, 9.0D, 0.5D, false,
                (lvl, from, to) -> {
                    HeroKit.line(lvl, from, to, LEATHER_DUST, 0.2D);
                    HeroKit.sound(lvl, to, SoundEvents.PLAYER_ATTACK_SWEEP, 1.0F, 1.8F);
                },
                (victim, from) -> {
                    HeroKit.hit(level, mob, victim, HeroKit.damage(mob, 0.8D), from, 0.0D, 0.0D);
                    victim.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1), mob);
                    yank(mob, victim, 0.9D);
                    HeroKit.sound(level, victim.position(), SoundEvents.PLAYER_ATTACK_CRIT, 1.0F, 1.6F);
                },
                null);
    }
}
