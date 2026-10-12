package com.craisinlord.integrated_marvel.hero;

import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class MarvelBalanceConfig {
    public static final List<HeroStats> ALL_HERO_STATS = new ArrayList<>();
    public static final ModConfigSpec SPEC;
    public static final HeroStats SPIDER_POWERS;
    public static final HeroStats WEAPON_X_LOGAN;
    public static final ModConfigSpec.DoubleValue LOGAN_CLAW_DAMAGE;

    public static final HeroStats SUPER_SOLDIER;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_SHIELD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_SPEED;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue RALLY_RADIUS;
    public static final ModConfigSpec.IntValue RALLY_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue RALLY_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SUPER_SOLDIER_JUMP_MULTIPLIER;

    public static final HeroStats PANTHER;
    public static final ModConfigSpec.DoubleValue POUNCE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue POUNCE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CLAW_SWIPE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CLAW_SWIPE_COOLDOWN;
    public static final ModConfigSpec.IntValue CLAW_BLEED_TICKS;
    public static final ModConfigSpec.DoubleValue ANCESTRAL_PLANE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ARROW_DEFLECT_CHANCE;
    public static final ModConfigSpec.DoubleValue SENSES_RADIUS;

    public static final HeroStats DEADPOOL;
    public static final ModConfigSpec.DoubleValue DEADPOOL_REGEN_PERCENT_PER_SECOND;
    public static final ModConfigSpec.IntValue UNLOAD_SHOTS;
    public static final ModConfigSpec.DoubleValue UNLOAD_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue UNLOAD_RANGE;
    public static final ModConfigSpec.DoubleValue UNLOAD_COOLDOWN;
    public static final ModConfigSpec.DoubleValue DEADPOOL_GUN_BONUS;
    public static final ModConfigSpec.DoubleValue GRENADE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue GRENADE_RADIUS;
    public static final ModConfigSpec.DoubleValue GRENADE_FUMBLE_CHANCE;
    public static final ModConfigSpec.DoubleValue GRENADE_FUMBLE_SELF_DAMAGE;
    public static final ModConfigSpec.DoubleValue GRENADE_COOLDOWN;
    public static final ModConfigSpec.IntValue RETCON_SECONDS;
    public static final ModConfigSpec.DoubleValue RETCON_HEAL_CAP;
    public static final ModConfigSpec.DoubleValue RETCON_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SMASH_CUT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue PAUSE_MENU_RADIUS;
    public static final ModConfigSpec.DoubleValue PAUSE_MENU_SECONDS;
    public static final ModConfigSpec.DoubleValue PAUSE_MENU_COOLDOWN;
    public static final ModConfigSpec.DoubleValue RAGE_QUIT_RADIUS;
    public static final ModConfigSpec.DoubleValue RAGE_QUIT_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RAGE_QUIT_KNOCKBACK;
    public static final ModConfigSpec.DoubleValue RAGE_QUIT_CRACK_SECONDS;
    public static final ModConfigSpec.DoubleValue RAGE_QUIT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue READ_THE_SCRIPT_WINDOW;
    public static final ModConfigSpec.DoubleValue READ_THE_SCRIPT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue NARRATION_COOLDOWN;
    public static final ModConfigSpec.DoubleValue NARRATION_RANGE;
    public static final List<ModConfigSpec.DoubleValue> DEADPOOL_DOWNED_SECONDS = new ArrayList<>();
    public static final List<ModConfigSpec.DoubleValue> DEADPOOL_STAND_HEALTH = new ArrayList<>();
    public static final List<ModConfigSpec.DoubleValue> WOLVERINE_DOWNED_SECONDS = new ArrayList<>();
    public static final List<ModConfigSpec.DoubleValue> WOLVERINE_STAND_HEALTH = new ArrayList<>();
    public static final ModConfigSpec.DoubleValue HEALING_FACTOR_STACK_DECAY_SECONDS;
    public static final ModConfigSpec.DoubleValue HEALING_FACTOR_REGEN_PAUSE_SECONDS;
    public static final ModConfigSpec.DoubleValue HEALING_FACTOR_SNAP_SECONDS;
    public static final ModConfigSpec.DoubleValue HEALING_FACTOR_NAH_TIMEOUT_SECONDS;

    public static final HeroStats QUICKSILVER;
    public static final ModConfigSpec.DoubleValue SPEED_FORCE_MAX_RATE;
    public static final ModConfigSpec.DoubleValue SPEED_FORCE_START_RATE;
    public static final ModConfigSpec.DoubleValue SPEED_FORCE_STEP;
    public static final ModConfigSpec.DoubleValue SPEED_FORCE_EXHAUSTION_PER_RATE;
    public static final ModConfigSpec.DoubleValue BULLET_TIME_RADIUS;
    public static final ModConfigSpec.DoubleValue BULLET_TIME_RATE;
    public static final ModConfigSpec.IntValue BULLET_TIME_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue BULLET_TIME_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SONIC_BOOM_DAMAGE;
    public static final ModConfigSpec.DoubleValue SONIC_BOOM_RADIUS;
    public static final ModConfigSpec.DoubleValue SONIC_BOOM_MIN_RATE;
    public static final ModConfigSpec.DoubleValue SONIC_BOOM_COOLDOWN;

    public static final HeroStats CYCLOPS;
    public static final ModConfigSpec.DoubleValue OPTIC_BLAST_DAMAGE;
    public static final ModConfigSpec.DoubleValue OPTIC_BLAST_RANGE;
    public static final ModConfigSpec.DoubleValue OPTIC_BLAST_COOLDOWN;
    public static final ModConfigSpec.IntValue OPTIC_BLAST_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue WIDE_BLAST_DAMAGE;
    public static final ModConfigSpec.DoubleValue WIDE_BLAST_RANGE;
    public static final ModConfigSpec.DoubleValue WIDE_BLAST_COOLDOWN;
    public static final ModConfigSpec.IntValue WIDE_BLAST_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue UNCONTROLLED_DAMAGE;
    public static final ModConfigSpec.DoubleValue UNCONTROLLED_RANGE;
    public static final ModConfigSpec.IntValue OPTIC_FIRE_SECONDS;
    public static final ModConfigSpec.DoubleValue OPTIC_TERRAIN_RADIUS;
    public static final ModConfigSpec.DoubleValue OPTIC_BLOCK_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue OPTIC_MAX_BLOCK_RESISTANCE;

    public static final HeroStats BINARY;
    public static final ModConfigSpec.DoubleValue BINARY_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue PHOTON_BLAST_DAMAGE;
    public static final ModConfigSpec.DoubleValue PHOTON_BLAST_RANGE;
    public static final ModConfigSpec.DoubleValue PHOTON_BLAST_TERRAIN_RADIUS;
    public static final ModConfigSpec.IntValue PHOTON_BLAST_DURATION_TICKS;
    public static final ModConfigSpec.IntValue PHOTON_FIRE_SECONDS;
    public static final ModConfigSpec.DoubleValue PHOTON_BLAST_COOLDOWN;
    public static final ModConfigSpec.IntValue BINARY_MODE_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue BINARY_MODE_AURA_DAMAGE;
    public static final ModConfigSpec.DoubleValue BINARY_MODE_AURA_RADIUS;
    public static final ModConfigSpec.DoubleValue BINARY_MODE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue COSMIC_DASH_SPEED;
    public static final ModConfigSpec.DoubleValue COSMIC_DASH_DAMAGE;
    public static final ModConfigSpec.DoubleValue COSMIC_DASH_COOLDOWN;

    public static final HeroStats HULK;
    public static final HeroStats BANNER;
    public static final ModConfigSpec.DoubleValue HULK_OUT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue THUNDERCLAP_RANGE;
    public static final ModConfigSpec.DoubleValue THUNDERCLAP_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THUNDERCLAP_COOLDOWN;
    public static final ModConfigSpec.IntValue HULK_SMASH_LENGTH;
    public static final ModConfigSpec.DoubleValue HULK_SMASH_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HULK_SMASH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GAMMA_LEAP_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue GAMMA_LEAP_VERTICAL;
    public static final ModConfigSpec.DoubleValue GAMMA_LEAP_STANDING_VERTICAL;
    public static final ModConfigSpec.IntValue GAMMA_LEAP_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue GAMMA_LANDING_RADIUS;
    public static final ModConfigSpec.DoubleValue GAMMA_LANDING_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue RAGE_PER_HEART;
    public static final ModConfigSpec.DoubleValue RAGE_PER_HIT;
    public static final ModConfigSpec.DoubleValue RAGE_PER_IMPACT;
    public static final ModConfigSpec.IntValue RAGE_DECAY_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue RAGE_DECAY_PER_SECOND;
    public static final ModConfigSpec.DoubleValue RAGE_DAMAGE_BONUS;
    public static final ModConfigSpec.BooleanValue RAGE_FORCES_HULK_OUT;
    public static final ModConfigSpec.IntValue RAGE_MAX_STRENGTH_LEVEL;
    public static final ModConfigSpec.DoubleValue RAGE_COOLDOWN_REDUCTION;
    public static final ModConfigSpec.DoubleValue LAST_STAND_COOLDOWN;

    public static final HeroStats MAGNETO;
    public static final ModConfigSpec.DoubleValue MAGNETO_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue GRIP_RANGE;
    public static final ModConfigSpec.IntValue GRIP_HOLD_TICKS;
    public static final ModConfigSpec.DoubleValue GRIP_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue GRIP_THROW_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue GRIP_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CRUSH_RANGE;
    public static final ModConfigSpec.DoubleValue CRUSH_BASE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CRUSH_PER_ARMOR_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CRUSH_COOLDOWN;
    public static final ModConfigSpec.IntValue SWARM_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue SWARM_COOLDOWN;
    public static final ModConfigSpec.IntValue SWARM_MAX_WEAPONS;
    public static final ModConfigSpec.IntValue LIFT_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue LIFT_HOLD_TICKS;
    public static final ModConfigSpec.DoubleValue LIFT_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue MAGNETIC_SHIELD_RADIUS;
    public static final ModConfigSpec.DoubleValue MAGNETIC_SHIELD_EXHAUSTION;

    public static final HeroStats STORM;
    public static final ModConfigSpec.DoubleValue STORM_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue LIGHTNING_RANGE;
    public static final ModConfigSpec.DoubleValue LIGHTNING_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue LIGHTNING_RADIUS;
    public static final ModConfigSpec.IntValue LIGHTNING_CHAIN_TARGETS;
    public static final ModConfigSpec.DoubleValue LIGHTNING_COOLDOWN;
    public static final ModConfigSpec.DoubleValue LIGHTNING_STORM_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GALE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WEATHER_MINOR_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WEATHER_MAJOR_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WEATHER_TORNADO_COOLDOWN;
    public static final ModConfigSpec.IntValue WEATHER_TORNADO_SECONDS;
    public static final ModConfigSpec.DoubleValue WEATHER_CLEAR_RADIUS;
    public static final ModConfigSpec.DoubleValue STORM_RAIN_DAMAGE_BONUS;
    public static final ModConfigSpec.DoubleValue FORCED_WEATHER_RADIUS;
    public static final ModConfigSpec.IntValue FORCED_WEATHER_SECONDS;

    public static final HeroStats ICEMAN;
    public static final ModConfigSpec.DoubleValue FREEZE_RANGE;
    public static final ModConfigSpec.DoubleValue FREEZE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue FREEZE_TICKS;
    public static final ModConfigSpec.DoubleValue FREEZE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SHATTER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CRYOGENESIS_RADIUS;
    public static final ModConfigSpec.IntValue CRYOGENESIS_TICKS;
    public static final ModConfigSpec.DoubleValue CRYOGENESIS_COOLDOWN;
    public static final ModConfigSpec.IntValue ICE_WALL_TICKS;
    public static final ModConfigSpec.DoubleValue ICE_WALL_COOLDOWN;
    public static final ModConfigSpec.IntValue ICE_SLIDE_TICKS;
    public static final ModConfigSpec.DoubleValue ICE_FORM_FIRE_MULTIPLIER;

    public static final HeroStats SCARLET_WITCH;
    public static final ModConfigSpec.DoubleValue WANDA_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_RANGE;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_COOLDOWN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEX_EFFECTS;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_RANGE;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_THROW_SPEED;
    public static final ModConfigSpec.IntValue TELEKINESIS_MAX_BLOCKS;
    public static final ModConfigSpec.IntValue TELEKINESIS_HOLD_TICKS;
    public static final ModConfigSpec.DoubleValue REALITY_WARP_RADIUS;
    public static final ModConfigSpec.IntValue REALITY_WARP_MAX_TARGETS;
    public static final ModConfigSpec.DoubleValue REALITY_WARP_COOLDOWN;
    public static final ModConfigSpec.DoubleValue HEX_FIELD_RADIUS;
    public static final ModConfigSpec.DoubleValue HEX_FIELD_CHANCE;
    public static final ModConfigSpec.DoubleValue PROBABILITY_DODGE_COOLDOWN;

    public static final HeroStats PIOTR;
    public static final HeroStats COLOSSUS;
    public static final ModConfigSpec.DoubleValue STEEL_FORM_COOLDOWN;
    public static final ModConfigSpec.DoubleValue RICOCHET_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STEEL_FIRE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FASTBALL_SPEED;
    public static final ModConfigSpec.DoubleValue FASTBALL_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FASTBALL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STEEL_CHARGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STEEL_CHARGE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue IRON_SLAM_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue IRON_SLAM_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BULWARK_REDUCTION;

    public static final HeroStats DAREDEVIL;
    public static final ModConfigSpec.DoubleValue RADAR_RANGE;
    public static final ModConfigSpec.DoubleValue DAREDEVIL_LEAP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DAREDEVIL_LEAP_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue DAREDEVIL_FLIP_CHANCE;
    public static final ModConfigSpec.DoubleValue RADAR_DARK_RANGE;
    public static final ModConfigSpec.DoubleValue DANGER_SENSE_CHANCE;
    public static final ModConfigSpec.DoubleValue BILLY_CLUB_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue BILLY_CLUB_BOUNCES;
    public static final ModConfigSpec.DoubleValue BILLY_CLUB_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GRAPPLE_RANGE;
    public static final ModConfigSpec.DoubleValue GRAPPLE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TAKEDOWN_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.IntValue TAKEDOWN_STUN_TICKS;
    public static final ModConfigSpec.DoubleValue TAKEDOWN_COOLDOWN;

    public static final HeroStats KITTY;
    public static final ModConfigSpec.DoubleValue PHASE_METER_SECONDS;
    public static final ModConfigSpec.DoubleValue PHASE_STRIKE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PHASE_STRIKE_COOLDOWN;
    public static final ModConfigSpec.IntValue PHASE_SINK_TICKS;
    public static final ModConfigSpec.DoubleValue PHASE_SINK_COOLDOWN;
    public static final ModConfigSpec.IntValue PHASE_OTHERS_TICKS;
    public static final ModConfigSpec.DoubleValue PHASE_OTHERS_COOLDOWN;

    public static final HeroStats SQUIRREL_GIRL;
    public static final ModConfigSpec.IntValue SWARM_SIZE;
    public static final ModConfigSpec.DoubleValue SWARM_RESPAWN_SECONDS;
    public static final ModConfigSpec.DoubleValue SWARM_SQUIRREL_HEALTH;
    public static final ModConfigSpec.DoubleValue SWARM_BITE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SWARM_CALL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SWARM_FORAGE_RANGE;
    public static final ModConfigSpec.DoubleValue SIC_EM_RANGE;
    public static final ModConfigSpec.DoubleValue SIC_EM_COOLDOWN;
    public static final ModConfigSpec.IntValue PILE_TICKS;
    public static final ModConfigSpec.DoubleValue PILE_BITE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue PILE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GNAW_TICKS_PER_HARDNESS;
    public static final ModConfigSpec.IntValue GNAW_MAX_BLOCKS;
    public static final ModConfigSpec.BooleanValue GNAW_DROPS;
    public static final ModConfigSpec.DoubleValue TIPPY_BITE_MULTIPLIER;
    public static final ModConfigSpec.IntValue TIPPY_RESPAWN_SECONDS;

    public static final HeroStats TORCH;
    public static final ModConfigSpec.DoubleValue TORCH_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue FLAME_OUT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FIREBALL_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FIREBALL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FLAME_CAGE_RANGE;
    public static final ModConfigSpec.IntValue FLAME_CAGE_TICKS;
    public static final ModConfigSpec.DoubleValue FLAME_CAGE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FLAME_CAGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue NOVA_RADIUS;
    public static final ModConfigSpec.DoubleValue NOVA_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue NOVA_COOLDOWN;
    public static final ModConfigSpec.DoubleValue NOVA_BURNOUT_SECONDS;

    public static final HeroStats SUE;
    public static final ModConfigSpec.DoubleValue VANISH_COOLDOWN;
    public static final ModConfigSpec.IntValue FORCE_FIELD_TICKS;
    public static final ModConfigSpec.DoubleValue FORCE_FIELD_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BUBBLE_SECONDS;
    public static final ModConfigSpec.DoubleValue BUBBLE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BUBBLE_RADIUS;
    public static final ModConfigSpec.DoubleValue BUBBLE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FORCE_BLAST_RANGE;
    public static final ModConfigSpec.DoubleValue FORCE_BLAST_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FORCE_BLAST_COOLDOWN;

    public static final HeroStats REED;
    public static final ModConfigSpec.DoubleValue STRETCH_PUNCH_RANGE;
    public static final ModConfigSpec.DoubleValue STRETCH_PUNCH_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STRETCH_PUNCH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STRETCH_GRAB_RANGE;
    public static final ModConfigSpec.DoubleValue STRETCH_GRAB_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STRETCH_SWEEP_RADIUS;
    public static final ModConfigSpec.DoubleValue STRETCH_SWEEP_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue STRETCH_SWEEP_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BOUNCE_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue SHAPE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SLINGSHOT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SLINGSHOT_SPEED;
    public static final ModConfigSpec.DoubleValue BALL_SECONDS;
    public static final ModConfigSpec.DoubleValue BALL_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue TRAMPOLINE_MAX_BOUNCE;

    public static final HeroStats THING;
    public static final ModConfigSpec.DoubleValue ROCK_PLATING_MAX;
    public static final ModConfigSpec.DoubleValue ROCK_PLATING_REGEN;
    public static final ModConfigSpec.IntValue ROCK_PLATING_DELAY_SECONDS;
    public static final ModConfigSpec.DoubleValue COMBO_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CLOBBER_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CLOBBER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WRESTLE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue WRESTLE_COOLDOWN;
    public static final ModConfigSpec.IntValue TOSS_MAX_BLOCKS;
    public static final ModConfigSpec.DoubleValue TOSS_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue TOSS_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue TOSS_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BRACE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THING_LEAP_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue THING_LEAP_VERTICAL;
    public static final ModConfigSpec.DoubleValue THING_LEAP_STANDING_VERTICAL;
    public static final ModConfigSpec.IntValue THING_LEAP_COOLDOWN_TICKS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("spiderPowersPeterParker");
        SPIDER_POWERS = new HeroStats(builder, 10.0D, 9.0D, 8.0D, 0.5D, 0.5D, 0.04D, 0.0D,
                List.of("apothic_attributes:dodge_chance;0.16;ADD_VALUE", "sinew:air_speed;0.02;ADD_VALUE"));
        builder.pop();

        builder.push("weaponXLogan");
        WEAPON_X_LOGAN = new HeroStats(builder, 16.0D, 5.0D, 12.0D, 0.2D, 0.5D, 0.02D, 30.0D,
                List.of("apothic_attributes:healing_received;0.6;ADD_VALUE", "apothic_attributes:crit_damage;0.4;ADD_VALUE"));
        LOGAN_CLAW_DAMAGE = builder.defineInRange("clawAttackBaseDamage", 30.0D, 0.0D, 10000.0D);
        double[] wolverineDowned = {5.0D, 8.0D};
        double[] wolverineHealth = {0.4D, 0.2D};
        for (int i = 0; i < wolverineDowned.length; i++) {
            WOLVERINE_DOWNED_SECONDS.add(builder.defineInRange("revive" + (i + 1) + "DownedSeconds", wolverineDowned[i], 0.0D, 600.0D));
            WOLVERINE_STAND_HEALTH.add(builder.defineInRange("revive" + (i + 1) + "StandHealthFraction", wolverineHealth[i], 0.01D, 1.0D));
        }
        builder.pop();

        builder.push("superSoldierSteveRogers");
        SUPER_SOLDIER = new HeroStats(builder, 30.0D, 6.0D, 12.0D, 0.5D, 0.0D, 0.0D, 60.0D,
                List.of("apothic_attributes:healing_received;0.3;ADD_VALUE", "minecraft:generic.knockback_resistance;0.2;ADD_VALUE"));
        SUPER_SOLDIER_JUMP_MULTIPLIER = builder.defineInRange("crouchJumpMultiplier", 3.0D, 1.0D, 20.0D);
        SHIELD_CHARGE_DAMAGE_MULTIPLIER = builder.defineInRange("shieldChargeDamageMultiplier", 1.5D, 0.0D, 100.0D);
        SHIELD_CHARGE_SHIELD_MULTIPLIER = builder.defineInRange("shieldChargeShieldMultiplier", 1.5D, 1.0D, 100.0D);
        SHIELD_CHARGE_SPEED = builder.defineInRange("shieldChargeSpeed", 1.8D, 0.0D, 10.0D);
        SHIELD_CHARGE_COOLDOWN = builder.defineInRange("shieldChargeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        RALLY_RADIUS = builder.defineInRange("rallyRadius", 12.0D, 0.0D, 64.0D);
        RALLY_DURATION_TICKS = builder.defineInRange("rallyDurationTicks", 200, 0, 12000);
        RALLY_COOLDOWN = builder.defineInRange("rallyCooldownSeconds", 45.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("heartShapedHerbTchalla");
        PANTHER = new HeroStats(builder, 30.0D, 7.0D, 12.0D, 0.5D, 0.0D, 0.0D, 60.0D,
                List.of("apothic_attributes:crit_chance;0.08;ADD_VALUE", "apothic_attributes:crit_damage;0.3;ADD_VALUE"));
        POUNCE_DAMAGE_MULTIPLIER = builder.defineInRange("pounceDamageMultiplier", 1.5D, 0.0D, 100.0D);
        POUNCE_COOLDOWN = builder.defineInRange("pounceCooldownSeconds", 6.0D, 0.0D, 600.0D);
        CLAW_SWIPE_DAMAGE_MULTIPLIER = builder.defineInRange("clawSwipeDamageMultiplier", 2.0D, 0.0D, 100.0D);
        CLAW_SWIPE_COOLDOWN = builder.defineInRange("clawSwipeCooldownSeconds", 4.0D, 0.0D, 600.0D);
        CLAW_BLEED_TICKS = builder.defineInRange("clawSwipeBleedTicks", 100, 0, 1200);
        ANCESTRAL_PLANE_COOLDOWN = builder.defineInRange("ancestralPlaneCooldownSeconds", 600.0D, 0.0D, 36000.0D);
        ARROW_DEFLECT_CHANCE = builder.defineInRange("arrowDeflectChance", 0.6D, 0.0D, 1.0D);
        SENSES_RADIUS = builder.defineInRange("heightenedSensesRadius", 24.0D, 0.0D, 128.0D);
        builder.pop();

        builder.push("weaponXWadeWilson");
        DEADPOOL = new HeroStats(builder, 30.0D, 4.0D, 10.0D, 0.3D, 0.0D, 0.0D, 40.0D,
                List.of("apothic_attributes:healing_received;0.4;ADD_VALUE", "apothic_attributes:crit_damage;0.4;ADD_VALUE"));
        DEADPOOL_REGEN_PERCENT_PER_SECOND = builder.defineInRange("regenPercentOfMaxHealthPerSecond", 0.04D, 0.0D, 1.0D);
        UNLOAD_SHOTS = builder.defineInRange("unloadShots", 8, 1, 64);
        UNLOAD_DAMAGE_MULTIPLIER = builder.defineInRange("unloadDamageMultiplier", 0.6D, 0.0D, 100.0D);
        UNLOAD_RANGE = builder.defineInRange("unloadRange", 24.0D, 1.0D, 128.0D);
        UNLOAD_COOLDOWN = builder.defineInRange("unloadCooldownSeconds", 6.0D, 0.0D, 600.0D);
        DEADPOOL_GUN_BONUS = builder.defineInRange("gunDamageBonus", 0.25D, 0.0D, 10.0D);
        GRENADE_DAMAGE_MULTIPLIER = builder.defineInRange("grenadeDamageMultiplier", 2.0D, 0.0D, 100.0D);
        GRENADE_RADIUS = builder.defineInRange("grenadeRadius", 3.5D, 0.5D, 16.0D);
        GRENADE_FUMBLE_CHANCE = builder.defineInRange("grenadeFumbleChance", 0.08D, 0.0D, 1.0D);
        GRENADE_FUMBLE_SELF_DAMAGE = builder.defineInRange("grenadeFumbleSelfDamage", 14.0D, 0.0D, 10000.0D);
        GRENADE_COOLDOWN = builder.defineInRange("grenadeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        RETCON_SECONDS = builder.defineInRange("retconSeconds", 5, 1, 30);
        RETCON_HEAL_CAP = builder.defineInRange("retconHealCap", 6.0D, 0.0D, 1000.0D);
        RETCON_COOLDOWN = builder.defineInRange("retconCooldownSeconds", 20.0D, 0.0D, 600.0D);
        SMASH_CUT_COOLDOWN = builder.defineInRange("smashCutCooldownSeconds", 300.0D, 0.0D, 6000.0D);
        PAUSE_MENU_RADIUS = builder.defineInRange("pauseMenuRadius", 8.0D, 1.0D, 64.0D);
        PAUSE_MENU_SECONDS = builder.defineInRange("pauseMenuSeconds", 3.0D, 0.5D, 60.0D);
        PAUSE_MENU_COOLDOWN = builder.defineInRange("pauseMenuCooldownSeconds", 30.0D, 0.0D, 600.0D);
        RAGE_QUIT_RADIUS = builder.defineInRange("rageQuitRadius", 6.0D, 1.0D, 32.0D);
        RAGE_QUIT_DAMAGE_MULTIPLIER = builder.defineInRange("rageQuitDamageMultiplier", 1.5D, 0.0D, 100.0D);
        RAGE_QUIT_KNOCKBACK = builder.defineInRange("rageQuitKnockback", 2.0D, 0.0D, 10.0D);
        RAGE_QUIT_CRACK_SECONDS = builder.defineInRange("rageQuitCrackSeconds", 4.0D, 0.5D, 30.0D);
        RAGE_QUIT_COOLDOWN = builder.defineInRange("rageQuitCooldownSeconds", 20.0D, 0.0D, 600.0D);
        READ_THE_SCRIPT_WINDOW = builder.defineInRange("readTheScriptWindowSeconds", 10.0D, 0.0D, 120.0D);
        READ_THE_SCRIPT_COOLDOWN = builder.defineInRange("readTheScriptCooldownSeconds", 8.0D, 0.0D, 600.0D);
        NARRATION_COOLDOWN = builder.defineInRange("narrationCooldownSeconds", 8.0D, 0.0D, 600.0D);
        NARRATION_RANGE = builder.defineInRange("narrationRange", 16.0D, 0.0D, 128.0D);
        double[] deadpoolDowned = {3.0D, 5.0D, 8.0D};
        double[] deadpoolHealth = {0.5D, 0.3D, 0.15D};
        for (int i = 0; i < deadpoolDowned.length; i++) {
            DEADPOOL_DOWNED_SECONDS.add(builder.defineInRange("revive" + (i + 1) + "DownedSeconds", deadpoolDowned[i], 0.0D, 600.0D));
            DEADPOOL_STAND_HEALTH.add(builder.defineInRange("revive" + (i + 1) + "StandHealthFraction", deadpoolHealth[i], 0.01D, 1.0D));
        }
        HEALING_FACTOR_STACK_DECAY_SECONDS = builder.defineInRange("reviveStackDecaySeconds", 180.0D, 1.0D, 86400.0D);
        HEALING_FACTOR_REGEN_PAUSE_SECONDS = builder.defineInRange("regenPauseAfterReviveSeconds", 5.0D, 0.0D, 600.0D);
        HEALING_FACTOR_SNAP_SECONDS = builder.defineInRange("snapReformSeconds", 10.0D, 0.0D, 600.0D);
        HEALING_FACTOR_NAH_TIMEOUT_SECONDS = builder.defineInRange("nahButtonTimeoutSeconds", 30.0D, 1.0D, 600.0D);
        builder.pop();

        builder.push("superSpeedPietroMaximoff");
        QUICKSILVER = new HeroStats(builder, 20.0D, 2.0D, 8.0D, 0.2D, 0.0D, 0.0D, 20.0D,
                List.of("apothic_attributes:dodge_chance;0.16;ADD_VALUE"));
        SPEED_FORCE_MAX_RATE = builder.defineInRange("speedForceMaxRate", 10.0D, 1.0D, 20.0D);
        SPEED_FORCE_START_RATE = builder.defineInRange("speedForceStartRate", 2.0D, 1.0D, 20.0D);
        SPEED_FORCE_STEP = builder.defineInRange("speedForceScrollStep", 0.5D, 0.05D, 5.0D);
        SPEED_FORCE_EXHAUSTION_PER_RATE = builder.defineInRange("speedForceExhaustionPerRatePerTick", 0.02D, 0.0D, 10.0D);
        BULLET_TIME_RADIUS = builder.defineInRange("bulletTimeRadius", 64.0D, 1.0D, 160.0D);
        BULLET_TIME_RATE = builder.defineInRange("bulletTimeRate", 0.1D, 0.05D, 1.0D);
        BULLET_TIME_DURATION_TICKS = builder.defineInRange("bulletTimeDurationTicks", 240, 20, 12000);
        BULLET_TIME_COOLDOWN = builder.defineInRange("bulletTimeCooldownSeconds", 60.0D, 0.0D, 600.0D);
        SONIC_BOOM_DAMAGE = builder.defineInRange("sonicBoomDamageAtMinRate", 40.0D, 0.0D, 10000.0D);
        SONIC_BOOM_RADIUS = builder.defineInRange("sonicBoomRadius", 5.0D, 0.0D, 32.0D);
        SONIC_BOOM_MIN_RATE = builder.defineInRange("sonicBoomMinimumRate", 3.0D, 1.0D, 20.0D);
        SONIC_BOOM_COOLDOWN = builder.defineInRange("sonicBoomCooldownSeconds", 6.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("opticBlastsScottSummers");
        CYCLOPS = new HeroStats(builder, 20.0D, 3.0D, 8.0D, 0.2D, 0.0D, 0.0D, 10.0D,
                List.of("apothic_attributes:crit_chance;0.12;ADD_VALUE", "apothic_attributes:armor_pierce;10.0;ADD_VALUE"));
        OPTIC_BLAST_DAMAGE = builder.defineInRange("opticBlastDamagePerHit", 150.0D, 0.0D, 10000.0D);
        OPTIC_BLAST_DURATION_TICKS = builder.defineInRange("opticBlastDurationTicks", 20, 1, 200);
        OPTIC_BLAST_RANGE = builder.defineInRange("opticBlastRange", 64.0D, 1.0D, 256.0D);
        OPTIC_BLAST_COOLDOWN = builder.defineInRange("opticBlastCooldownSeconds", 2.0D, 0.0D, 600.0D);
        WIDE_BLAST_DAMAGE = builder.defineInRange("wideBlastDamagePerHit", 120.0D, 0.0D, 10000.0D);
        WIDE_BLAST_RANGE = builder.defineInRange("wideBlastRange", 18.0D, 1.0D, 64.0D);
        WIDE_BLAST_COOLDOWN = builder.defineInRange("wideBlastCooldownSeconds", 4.0D, 0.0D, 600.0D);
        WIDE_BLAST_DURATION_TICKS = builder.defineInRange("wideBlastDurationTicks", 14, 1, 200);
        UNCONTROLLED_DAMAGE = builder.defineInRange("uncontrolledBeamDamagePerHit", 80.0D, 0.0D, 10000.0D);
        UNCONTROLLED_RANGE = builder.defineInRange("uncontrolledBeamRange", 40.0D, 1.0D, 256.0D);
        OPTIC_FIRE_SECONDS = builder.defineInRange("beamFireSeconds", 5, 0, 60);
        OPTIC_TERRAIN_RADIUS = builder.defineInRange("beamTerrainRadius", 1.5D, 0.0D, 8.0D);
        OPTIC_BLOCK_DROP_CHANCE = builder.defineInRange("beamBlockDropChance", 0.1D, 0.0D, 1.0D);
        OPTIC_MAX_BLOCK_RESISTANCE = builder.defineInRange("beamMaxBlockBlastResistance", 50.0D, 0.0D, 3600000.0D);
        builder.pop();

        builder.push("binaryCarolDanvers");
        BINARY = new HeroStats(builder, 60.0D, 25.0D, 20.0D, 3.0D, 0.0D, 0.0D, 450.0D,
                List.of("apothic_attributes:armor_pierce;32.0;ADD_VALUE", "apothic_attributes:crit_damage;0.6;ADD_VALUE"));
        BINARY_FLY_SPEED = builder.defineInRange("flySpeed", 0.1D, 0.01D, 1.0D);
        PHOTON_BLAST_DAMAGE = builder.defineInRange("photonBlastDamagePerHit", 300.0D, 0.0D, 10000.0D);
        PHOTON_BLAST_RANGE = builder.defineInRange("photonBlastRange", 64.0D, 1.0D, 256.0D);
        PHOTON_BLAST_TERRAIN_RADIUS = builder.defineInRange("photonBlastTerrainRadius", 2.5D, 0.0D, 10.0D);
        PHOTON_BLAST_DURATION_TICKS = builder.defineInRange("photonBlastDurationTicks", 24, 1, 200);
        PHOTON_FIRE_SECONDS = builder.defineInRange("photonBlastFireSeconds", 8, 0, 60);
        PHOTON_BLAST_COOLDOWN = builder.defineInRange("photonBlastCooldownSeconds", 2.0D, 0.0D, 600.0D);
        BINARY_MODE_DURATION_TICKS = builder.defineInRange("binaryModeDurationTicks", 300, 20, 12000);
        BINARY_MODE_AURA_DAMAGE = builder.defineInRange("binaryModeAuraDamage", 120.0D, 0.0D, 10000.0D);
        BINARY_MODE_AURA_RADIUS = builder.defineInRange("binaryModeAuraRadius", 6.0D, 0.0D, 32.0D);
        BINARY_MODE_COOLDOWN = builder.defineInRange("binaryModeCooldownSeconds", 120.0D, 0.0D, 3600.0D);
        COSMIC_DASH_SPEED = builder.defineInRange("cosmicDashSpeed", 3.0D, 0.0D, 10.0D);
        COSMIC_DASH_DAMAGE = builder.defineInRange("cosmicDashDamage", 300.0D, 0.0D, 10000.0D);
        COSMIC_DASH_COOLDOWN = builder.defineInRange("cosmicDashCooldownSeconds", 5.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("gammaMutationBruceBanner");
        HULK = new HeroStats(builder, 85.0D, 26.0D, 22.0D, 4.0D, 1.0D, 0.0D, 450.0D,
                List.of("minecraft:generic.scale;0.5;ADD_MULTIPLIED_BASE", "minecraft:generic.knockback_resistance;1.0;ADD_VALUE",
                        "apothic_attributes:healing_received;0.5;ADD_VALUE", "minecraft:player.entity_interaction_range;1.0;ADD_VALUE",
                        "minecraft:generic.safe_fall_distance;40.0;ADD_VALUE"));
        HULK_OUT_COOLDOWN = builder.defineInRange("hulkOutCooldownSeconds", 3.0D, 0.0D, 600.0D);
        THUNDERCLAP_RANGE = builder.defineInRange("thunderclapRange", 12.0D, 1.0D, 64.0D);
        THUNDERCLAP_DAMAGE_MULTIPLIER = builder.defineInRange("thunderclapDamageMultiplier", 1.0D, 0.0D, 100.0D);
        THUNDERCLAP_COOLDOWN = builder.defineInRange("thunderclapCooldownSeconds", 10.0D, 0.0D, 600.0D);
        HULK_SMASH_LENGTH = builder.defineInRange("hulkSmashWaveLength", 10, 1, 32);
        HULK_SMASH_DAMAGE_MULTIPLIER = builder.defineInRange("hulkSmashDamageMultiplier", 0.8D, 0.0D, 100.0D);
        HULK_SMASH_COOLDOWN = builder.defineInRange("hulkSmashCooldownSeconds", 4.0D, 0.0D, 600.0D);
        GAMMA_LEAP_HORIZONTAL = builder.defineInRange("gammaLeapHorizontal", 1.8D, 0.0D, 10.0D);
        GAMMA_LEAP_VERTICAL = builder.defineInRange("gammaLeapVertical", 1.6D, 0.0D, 10.0D);
        GAMMA_LEAP_STANDING_VERTICAL = builder.defineInRange("gammaLeapStandingVertical", 2.6D, 0.0D, 10.0D);
        GAMMA_LEAP_COOLDOWN_TICKS = builder.defineInRange("gammaLeapCooldownTicks", 20, 0, 1200);
        GAMMA_LANDING_RADIUS = builder.defineInRange("gammaLandingRadius", 5.0D, 0.0D, 32.0D);
        GAMMA_LANDING_DAMAGE_MULTIPLIER = builder.defineInRange("gammaLandingMaxDamageMultiplier", 1.5D, 0.0D, 100.0D);
        RAGE_PER_HEART = builder.defineInRange("ragePerHeartLost", 3.0D, 0.0D, 100.0D);
        RAGE_PER_HIT = builder.defineInRange("ragePerMeleeHit", 1.0D, 0.0D, 100.0D);
        RAGE_PER_IMPACT = builder.defineInRange("ragePerImpact", 5.0D, 0.0D, 100.0D);
        RAGE_DECAY_DELAY_TICKS = builder.defineInRange("rageDecayDelayTicks", 120, 0, 12000);
        RAGE_DECAY_PER_SECOND = builder.defineInRange("rageDecayPerSecond", 4.0D, 0.0D, 100.0D);
        RAGE_DAMAGE_BONUS = builder.defineInRange("rageMaxDamageBonus", 0.4D, 0.0D, 10.0D);
        RAGE_FORCES_HULK_OUT = builder.define("fullRageForcesHulkOut", true);
        RAGE_MAX_STRENGTH_LEVEL = builder.defineInRange("rageMaxStrengthLevel", 4, 0, 10);
        RAGE_COOLDOWN_REDUCTION = builder.defineInRange("rageMaxCooldownReduction", 0.5D, 0.0D, 0.95D);
        LAST_STAND_COOLDOWN = builder.defineInRange("lastStandHulkOutCooldownSeconds", 180.0D, 0.0D, 36000.0D);
        builder.push("banner");
        BANNER = new HeroStats(builder, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);
        builder.pop();
        builder.pop();

        builder.push("magnetismErikLehnsherr");
        MAGNETO = new HeroStats(builder, 40.0D, 8.0D, 16.0D, 0.5D, 0.0D, 0.0D, 100.0D,
                List.of("apothic_attributes:armor_pierce;16.0;ADD_VALUE", "minecraft:generic.knockback_resistance;0.5;ADD_VALUE"));
        MAGNETO_FLY_SPEED = builder.defineInRange("flySpeed", 0.06D, 0.01D, 1.0D);
        GRIP_RANGE = builder.defineInRange("magneticGripMetalRange", 24.0D, 1.0D, 64.0D);
        GRIP_HOLD_TICKS = builder.defineInRange("magneticGripHoldTicks", 120, 1, 1200);
        GRIP_THROW_SPEED = builder.defineInRange("magneticGripThrowSpeed", 2.5D, 0.0D, 10.0D);
        GRIP_THROW_DAMAGE_MULTIPLIER = builder.defineInRange("magneticGripThrowDamageMultiplier", 1.0D, 0.0D, 100.0D);
        GRIP_COOLDOWN = builder.defineInRange("magneticGripCooldownSeconds", 6.0D, 0.0D, 600.0D);
        CRUSH_RANGE = builder.defineInRange("magneticCrushRange", 24.0D, 1.0D, 64.0D);
        CRUSH_BASE_MULTIPLIER = builder.defineInRange("magneticCrushBaseMultiplier", 0.5D, 0.0D, 100.0D);
        CRUSH_PER_ARMOR_MULTIPLIER = builder.defineInRange("magneticCrushPerArmorPointMultiplier", 0.1D, 0.0D, 10.0D);
        CRUSH_COOLDOWN = builder.defineInRange("magneticCrushCooldownSeconds", 8.0D, 0.0D, 600.0D);
        SWARM_DURATION_TICKS = builder.defineInRange("weaponSwarmDurationTicks", 400, 20, 12000);
        SWARM_COOLDOWN = builder.defineInRange("weaponSwarmCooldownSeconds", 25.0D, 0.0D, 600.0D);
        SWARM_MAX_WEAPONS = builder.defineInRange("weaponSwarmMaxWeapons", 4, 1, 9);
        LIFT_MAX_BLOCKS = builder.defineInRange("metalLiftMaxBlocks", 48, 1, 512);
        LIFT_HOLD_TICKS = builder.defineInRange("metalLiftHoldTicks", 300, 20, 6000);
        LIFT_THROW_SPEED = builder.defineInRange("metalLiftThrowSpeed", 1.6D, 0.0D, 10.0D);
        MAGNETIC_SHIELD_RADIUS = builder.defineInRange("magneticShieldRadius", 5.0D, 0.0D, 32.0D);
        MAGNETIC_SHIELD_EXHAUSTION = builder.defineInRange("magneticShieldExhaustionPerDeflection", 0.3D, 0.0D, 40.0D);
        builder.pop();

        builder.push("weatherManipulationOroroMunroe");
        STORM = new HeroStats(builder, 30.0D, 5.0D, 12.0D, 0.3D, 0.0D, 0.0D, 60.0D,
                List.of("apothic_attributes:dodge_chance;0.08;ADD_VALUE", "minecraft:generic.safe_fall_distance;20.0;ADD_VALUE"));
        STORM_FLY_SPEED = builder.defineInRange("flySpeed", 0.08D, 0.01D, 1.0D);
        LIGHTNING_RANGE = builder.defineInRange("lightningStrikeRange", 48.0D, 1.0D, 128.0D);
        LIGHTNING_DAMAGE_MULTIPLIER = builder.defineInRange("lightningStrikeDamageMultiplier", 1.2D, 0.0D, 100.0D);
        LIGHTNING_RADIUS = builder.defineInRange("lightningStrikeRadius", 3.0D, 0.0D, 16.0D);
        LIGHTNING_CHAIN_TARGETS = builder.defineInRange("lightningStrikeChainTargets", 2, 0, 16);
        LIGHTNING_COOLDOWN = builder.defineInRange("lightningStrikeCooldownSeconds", 6.0D, 0.0D, 600.0D);
        LIGHTNING_STORM_COOLDOWN = builder.defineInRange("lightningStrikeStormCooldownSeconds", 3.0D, 0.0D, 600.0D);
        GALE_COOLDOWN = builder.defineInRange("galeCooldownSeconds", 12.0D, 0.0D, 600.0D);
        WEATHER_MINOR_COOLDOWN = builder.defineInRange("weatherMinorCooldownSeconds", 10.0D, 0.0D, 3600.0D);
        WEATHER_MAJOR_COOLDOWN = builder.defineInRange("weatherMajorCooldownSeconds", 30.0D, 0.0D, 3600.0D);
        WEATHER_TORNADO_COOLDOWN = builder.defineInRange("weatherTornadoCooldownSeconds", 90.0D, 0.0D, 3600.0D);
        WEATHER_TORNADO_SECONDS = builder.defineInRange("weatherTornadoSeconds", 30, 1, 600);
        WEATHER_CLEAR_RADIUS = builder.defineInRange("weatherClearRadius", 512.0D, 0.0D, 4096.0D);
        STORM_RAIN_DAMAGE_BONUS = builder.defineInRange("stormAuraRainDamageBonus", 0.2D, 0.0D, 10.0D);
        FORCED_WEATHER_RADIUS = builder.defineInRange("forcedWeatherRadius", 192.0D, 16.0D, 2048.0D);
        FORCED_WEATHER_SECONDS = builder.defineInRange("forcedWeatherSeconds", 300, 10, 36000);
        builder.pop();

        builder.push("cryokinesisBobbyDrake");
        ICEMAN = new HeroStats(builder, 24.0D, 4.0D, 10.0D, 0.2D, 1.0D, 0.02D, 40.0D,
                List.of("apothic_attributes:crit_damage;0.3;ADD_VALUE", "apothic_attributes:dodge_chance;0.06;ADD_VALUE"));
        FREEZE_RANGE = builder.defineInRange("freezeBlastRange", 14.0D, 1.0D, 64.0D);
        FREEZE_DAMAGE_MULTIPLIER = builder.defineInRange("freezeBlastDamageMultiplier", 0.6D, 0.0D, 100.0D);
        FREEZE_TICKS = builder.defineInRange("freezeBlastFreezeTicks", 80, 1, 1200);
        FREEZE_COOLDOWN = builder.defineInRange("freezeBlastCooldownSeconds", 7.0D, 0.0D, 600.0D);
        SHATTER_MULTIPLIER = builder.defineInRange("shatterDamageMultiplier", 1.5D, 1.0D, 100.0D);
        CRYOGENESIS_RADIUS = builder.defineInRange("cryogenesisRadius", 6.0D, 1.0D, 32.0D);
        CRYOGENESIS_TICKS = builder.defineInRange("cryogenesisFreezeTicks", 60, 1, 1200);
        CRYOGENESIS_COOLDOWN = builder.defineInRange("cryogenesisCooldownSeconds", 18.0D, 0.0D, 600.0D);
        ICE_WALL_TICKS = builder.defineInRange("iceWallTicks", 160, 1, 12000);
        ICE_WALL_COOLDOWN = builder.defineInRange("iceWallCooldownSeconds", 10.0D, 0.0D, 600.0D);
        ICE_SLIDE_TICKS = builder.defineInRange("iceSlideTicks", 100, 1, 12000);
        ICE_FORM_FIRE_MULTIPLIER = builder.defineInRange("iceFormFireDamageMultiplier", 1.5D, 1.0D, 10.0D);
        builder.pop();

        builder.push("chaosMagicWandaMaximoff");
        SCARLET_WITCH = new HeroStats(builder, 30.0D, 4.0D, 10.0D, 0.2D, 0.0D, 0.0D, 40.0D,
                List.of("minecraft:generic.luck;3.0;ADD_VALUE", "apothic_attributes:dodge_chance;0.12;ADD_VALUE"));
        WANDA_FLY_SPEED = builder.defineInRange("flySpeed", 0.05D, 0.01D, 1.0D);
        HEX_BOLT_RANGE = builder.defineInRange("hexBoltRange", 32.0D, 1.0D, 128.0D);
        HEX_BOLT_DAMAGE_MULTIPLIER = builder.defineInRange("hexBoltDamageMultiplier", 0.8D, 0.0D, 100.0D);
        HEX_BOLT_COOLDOWN = builder.defineInRange("hexBoltCooldownSeconds", 1.0D, 0.0D, 600.0D);
        HEX_EFFECTS = builder.comment("Hexes Hex Bolt can roll: jinxed, misfire, turned, pratfall, bad_luck, unravel, dazed.")
                .defineListAllowEmpty("hexEffects", List.of("jinxed", "misfire", "turned", "pratfall", "bad_luck", "unravel", "dazed"),
                        () -> "", entry -> entry instanceof String);
        TELEKINESIS_RANGE = builder.defineInRange("telekinesisRange", 12.0D, 1.0D, 64.0D);
        TELEKINESIS_COOLDOWN = builder.defineInRange("telekinesisCooldownSeconds", 0.0D, 0.0D, 600.0D);
        TELEKINESIS_THROW_SPEED = builder.defineInRange("telekinesisThrowSpeed", 2.0D, 0.0D, 10.0D);
        TELEKINESIS_MAX_BLOCKS = builder.defineInRange("telekinesisMaxBlocks", 1, 1, 64);
        TELEKINESIS_HOLD_TICKS = builder.defineInRange("telekinesisBlockHoldTicks", 300, 20, 6000);
        REALITY_WARP_RADIUS = builder.defineInRange("realityWarpRadius", 10.0D, 1.0D, 64.0D);
        REALITY_WARP_MAX_TARGETS = builder.defineInRange("realityWarpMaxTargets", 8, 1, 64);
        REALITY_WARP_COOLDOWN = builder.defineInRange("realityWarpCooldownSeconds", 180.0D, 0.0D, 3600.0D);
        HEX_FIELD_RADIUS = builder.defineInRange("hexFieldRadius", 6.0D, 0.0D, 32.0D);
        HEX_FIELD_CHANCE = builder.defineInRange("hexFieldFizzleChance", 0.25D, 0.0D, 1.0D);
        PROBABILITY_DODGE_COOLDOWN = builder.defineInRange("probabilityDodgeCooldownSeconds", 60.0D, 0.0D, 3600.0D);
        builder.pop();

        builder.push("organicSteelPiotrRasputin");
        PIOTR = new HeroStats(builder, 10.0D, 3.0D, 4.0D, 0.3D, 0.0D, 0.0D, 30.0D);
        builder.push("steelForm");
        COLOSSUS = new HeroStats(builder, 60.0D, 16.0D, 20.0D, 2.0D, 0.5D, 0.0D, 220.0D,
                List.of("minecraft:generic.scale;0.2;ADD_MULTIPLIED_BASE", "minecraft:generic.armor_toughness;12.0;ADD_VALUE",
                        "minecraft:generic.knockback_resistance;1.0;ADD_VALUE", "minecraft:generic.movement_speed;-0.2;ADD_MULTIPLIED_TOTAL",
                        "minecraft:generic.safe_fall_distance;20.0;ADD_VALUE"));
        builder.pop();
        STEEL_FORM_COOLDOWN = builder.defineInRange("steelFormOffCooldownSeconds", 2.0D, 0.0D, 600.0D);
        RICOCHET_MULTIPLIER = builder.defineInRange("ricochetDamageMultiplier", 0.25D, 0.0D, 1.0D);
        STEEL_FIRE_MULTIPLIER = builder.defineInRange("steelFireDamageMultiplier", 0.5D, 0.0D, 10.0D);
        FASTBALL_SPEED = builder.defineInRange("fastballSpeed", 2.8D, 0.0D, 10.0D);
        FASTBALL_DAMAGE_MULTIPLIER = builder.defineInRange("fastballNextHitMultiplier", 2.0D, 1.0D, 100.0D);
        FASTBALL_COOLDOWN = builder.defineInRange("fastballCooldownSeconds", 6.0D, 0.0D, 600.0D);
        STEEL_CHARGE_COOLDOWN = builder.defineInRange("steelChargeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        STEEL_CHARGE_DAMAGE_MULTIPLIER = builder.defineInRange("steelChargeDamageMultiplier", 0.8D, 0.0D, 100.0D);
        IRON_SLAM_DAMAGE_MULTIPLIER = builder.defineInRange("ironSlamDamageMultiplier", 1.4D, 0.0D, 100.0D);
        IRON_SLAM_COOLDOWN = builder.defineInRange("ironSlamCooldownSeconds", 10.0D, 0.0D, 600.0D);
        BULWARK_REDUCTION = builder.defineInRange("bulwarkProjectileMultiplier", 0.5D, 0.0D, 1.0D);
        builder.pop();

        builder.push("radarSenseMattMurdock");
        DAREDEVIL = new HeroStats(builder, 20.0D, 5.0D, 8.0D, 0.3D, 0.0D, 0.0D, 45.0D,
                List.of("apothic_attributes:dodge_chance;0.12;ADD_VALUE", "minecraft:generic.safe_fall_distance;8.0;ADD_VALUE"));
        DAREDEVIL_LEAP_MULTIPLIER = builder.defineInRange("crouchJumpMultiplier", 1.9D, 1.0D, 20.0D);
        DAREDEVIL_LEAP_HORIZONTAL = builder.defineInRange("sprintLeapHorizontalMultiplier", 1.4D, 1.0D, 10.0D);
        DAREDEVIL_FLIP_CHANCE = builder.defineInRange("sprintJumpFlipChance", 0.45D, 0.0D, 1.0D);
        RADAR_RANGE = builder.defineInRange("radarRange", 32.0D, 4.0D, 128.0D);
        RADAR_DARK_RANGE = builder.defineInRange("radarDarkRange", 48.0D, 4.0D, 128.0D);
        DANGER_SENSE_CHANCE = builder.defineInRange("dangerSenseDodgeChance", 0.2D, 0.0D, 1.0D);
        BILLY_CLUB_DAMAGE_MULTIPLIER = builder.defineInRange("billyClubDamageMultiplier", 0.7D, 0.0D, 100.0D);
        BILLY_CLUB_BOUNCES = builder.defineInRange("billyClubHits", 3, 1, 16);
        BILLY_CLUB_COOLDOWN = builder.defineInRange("billyClubCooldownSeconds", 3.0D, 0.0D, 600.0D);
        GRAPPLE_RANGE = builder.defineInRange("grappleRange", 32.0D, 4.0D, 128.0D);
        GRAPPLE_COOLDOWN = builder.defineInRange("grappleCooldownSeconds", 4.0D, 0.0D, 600.0D);
        TAKEDOWN_DAMAGE_MULTIPLIER = builder.defineInRange("takedownDamageMultiplier", 1.2D, 0.0D, 100.0D);
        TAKEDOWN_STUN_TICKS = builder.defineInRange("takedownStunTicks", 40, 0, 1200);
        TAKEDOWN_COOLDOWN = builder.defineInRange("takedownCooldownSeconds", 10.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("phasingKittyPryde");
        KITTY = new HeroStats(builder, 16.0D, 3.0D, 4.0D, 0.0D, 0.0D, 0.0D, 25.0D);
        PHASE_METER_SECONDS = builder.defineInRange("phaseMeterSeconds", 20.0D, 1.0D, 3600.0D);
        PHASE_STRIKE_DAMAGE_MULTIPLIER = builder.defineInRange("phaseStrikeDamageMultiplier", 0.9D, 0.0D, 100.0D);
        PHASE_STRIKE_COOLDOWN = builder.defineInRange("phaseStrikeCooldownSeconds", 5.0D, 0.0D, 600.0D);
        PHASE_SINK_TICKS = builder.defineInRange("phaseSinkTicks", 60, 1, 1200);
        PHASE_SINK_COOLDOWN = builder.defineInRange("phaseSinkCooldownSeconds", 12.0D, 0.0D, 600.0D);
        PHASE_OTHERS_TICKS = builder.defineInRange("phaseOthersTicks", 160, 1, 6000);
        PHASE_OTHERS_COOLDOWN = builder.defineInRange("phaseOthersCooldownSeconds", 20.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("squirrelPowersDoreenGreen");
        SQUIRREL_GIRL = new HeroStats(builder, 14.0D, 3.0D, 4.0D, 0.2D, 0.5D, 0.0D, 30.0D,
                List.of("apothic_attributes:dodge_chance;0.08;ADD_VALUE", "minecraft:generic.safe_fall_distance;10.0;ADD_VALUE"));
        SWARM_SIZE = builder.defineInRange("swarmSize", 8, 1, 32);
        SWARM_RESPAWN_SECONDS = builder.defineInRange("swarmRespawnSeconds", 4.0D, 0.0D, 600.0D);
        SWARM_SQUIRREL_HEALTH = builder.defineInRange("swarmSquirrelHealth", 30.0D, 1.0D, 10000.0D);
        SWARM_BITE_MULTIPLIER = builder.defineInRange("swarmBiteDamageMultiplier", 0.12D, 0.0D, 100.0D);
        SWARM_CALL_COOLDOWN = builder.defineInRange("callSwarmCooldownSeconds", 2.0D, 0.0D, 600.0D);
        SWARM_FORAGE_RANGE = builder.defineInRange("forageRange", 16.0D, 2.0D, 64.0D);
        SIC_EM_RANGE = builder.defineInRange("sicEmRange", 32.0D, 4.0D, 128.0D);
        SIC_EM_COOLDOWN = builder.defineInRange("sicEmCooldownSeconds", 1.0D, 0.0D, 600.0D);
        PILE_TICKS = builder.defineInRange("squirrelPileTicks", 60, 1, 1200);
        PILE_BITE_MULTIPLIER = builder.defineInRange("squirrelPileBiteDamageMultiplier", 0.1D, 0.0D, 100.0D);
        PILE_COOLDOWN = builder.defineInRange("squirrelPileCooldownSeconds", 20.0D, 0.0D, 600.0D);
        GNAW_TICKS_PER_HARDNESS = builder.defineInRange("gnawTicksPerHardness", 60.0D, 1.0D, 10000.0D);
        GNAW_MAX_BLOCKS = builder.defineInRange("gnawMaxBlocks", 64, 1, 4096);
        GNAW_DROPS = builder.define("gnawDropsItems", true);
        TIPPY_BITE_MULTIPLIER = builder.defineInRange("tippyToeBiteDamageMultiplier", 0.25D, 0.0D, 100.0D);
        TIPPY_RESPAWN_SECONDS = builder.defineInRange("tippyToeRespawnSeconds", 60, 1, 3600);
        builder.pop();

        builder.push("flameOnJohnnyStorm");
        TORCH = new HeroStats(builder, 28.0D, 5.0D, 10.0D, 0.3D, 0.0D, 0.0D, 60.0D);
        TORCH_FLY_SPEED = builder.defineInRange("flySpeed", 0.09D, 0.01D, 1.0D);
        FLAME_OUT_COOLDOWN = builder.defineInRange("flameOutCooldownSeconds", 5.0D, 0.0D, 600.0D);
        FIREBALL_DAMAGE_MULTIPLIER = builder.defineInRange("fireballDamageMultiplier", 0.6D, 0.0D, 100.0D);
        FIREBALL_COOLDOWN = builder.defineInRange("fireballCooldownSeconds", 0.4D, 0.0D, 600.0D);
        FLAME_CAGE_RANGE = builder.defineInRange("flameCageRange", 24.0D, 2.0D, 64.0D);
        FLAME_CAGE_TICKS = builder.defineInRange("flameCageTicks", 80, 1, 1200);
        FLAME_CAGE_DAMAGE_MULTIPLIER = builder.defineInRange("flameCageDamagePerSecondMultiplier", 0.35D, 0.0D, 100.0D);
        FLAME_CAGE_COOLDOWN = builder.defineInRange("flameCageCooldownSeconds", 12.0D, 0.0D, 600.0D);
        NOVA_RADIUS = builder.defineInRange("novaFlareRadius", 8.0D, 1.0D, 64.0D);
        NOVA_DAMAGE_MULTIPLIER = builder.defineInRange("novaFlareDamageMultiplier", 1.5D, 0.0D, 100.0D);
        NOVA_COOLDOWN = builder.defineInRange("novaFlareCooldownSeconds", 30.0D, 0.0D, 600.0D);
        NOVA_BURNOUT_SECONDS = builder.defineInRange("novaFlareBurnoutSeconds", 10.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("invisibilitySueStorm");
        SUE = new HeroStats(builder, 28.0D, 4.0D, 10.0D, 0.2D, 0.0D, 0.0D, 40.0D);
        VANISH_COOLDOWN = builder.defineInRange("vanishCooldownSeconds", 3.0D, 0.0D, 600.0D);
        FORCE_FIELD_TICKS = builder.defineInRange("forceFieldTicks", 200, 1, 6000);
        FORCE_FIELD_COOLDOWN = builder.defineInRange("forceFieldCooldownSeconds", 12.0D, 0.0D, 600.0D);
        BUBBLE_SECONDS = builder.defineInRange("forceBubbleSeconds", 10.0D, 0.5D, 600.0D);
        BUBBLE_COOLDOWN = builder.defineInRange("forceBubbleCooldownSeconds", 15.0D, 0.0D, 600.0D);
        BUBBLE_RADIUS = builder.defineInRange("forceBubbleRadius", 3.5D, 1.0D, 16.0D);
        BUBBLE_DAMAGE_MULTIPLIER = builder.defineInRange("forceBubbleDamageTakenMultiplier", 0.25D, 0.0D, 1.0D);
        FORCE_BLAST_RANGE = builder.defineInRange("forceBlastRange", 12.0D, 2.0D, 64.0D);
        FORCE_BLAST_DAMAGE_MULTIPLIER = builder.defineInRange("forceBlastDamageMultiplier", 0.8D, 0.0D, 100.0D);
        FORCE_BLAST_COOLDOWN = builder.defineInRange("forceBlastCooldownSeconds", 5.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("elasticityReedRichards");
        REED = new HeroStats(builder, 26.0D, 4.0D, 10.0D, 0.3D, 0.5D, 0.0D, 50.0D,
                List.of("minecraft:player.entity_interaction_range;3.0;ADD_VALUE", "minecraft:player.block_interaction_range;3.0;ADD_VALUE"));
        STRETCH_PUNCH_RANGE = builder.defineInRange("stretchPunchRange", 10.0D, 2.0D, 64.0D);
        STRETCH_PUNCH_DAMAGE_MULTIPLIER = builder.defineInRange("stretchPunchDamageMultiplier", 1.2D, 0.0D, 100.0D);
        STRETCH_PUNCH_COOLDOWN = builder.defineInRange("stretchPunchCooldownSeconds", 1.5D, 0.0D, 600.0D);
        STRETCH_GRAB_RANGE = builder.defineInRange("stretchGrabRange", 20.0D, 2.0D, 64.0D);
        STRETCH_GRAB_COOLDOWN = builder.defineInRange("stretchGrabCooldownSeconds", 4.0D, 0.0D, 600.0D);
        STRETCH_SWEEP_RADIUS = builder.defineInRange("stretchSweepRadius", 6.0D, 1.0D, 32.0D);
        STRETCH_SWEEP_DAMAGE_MULTIPLIER = builder.defineInRange("stretchSweepDamageMultiplier", 0.9D, 0.0D, 100.0D);
        STRETCH_SWEEP_COOLDOWN = builder.defineInRange("stretchSweepCooldownSeconds", 8.0D, 0.0D, 600.0D);
        BOUNCE_MAX_SPEED = builder.defineInRange("bounceMaxSpeed", 1.4D, 0.0D, 10.0D);
        SHAPE_COOLDOWN = builder.defineInRange("shapeShiftCooldownSeconds", 1.0D, 0.0D, 600.0D);
        SLINGSHOT_COOLDOWN = builder.defineInRange("slingshotCooldownSeconds", 3.0D, 0.0D, 600.0D);
        SLINGSHOT_SPEED = builder.defineInRange("slingshotSpeed", 2.6D, 0.1D, 10.0D);
        BALL_SECONDS = builder.defineInRange("bouncyBallSeconds", 8.0D, 0.5D, 600.0D);
        BALL_DAMAGE_MULTIPLIER = builder.defineInRange("bouncyBallDamageMultiplier", 0.4D, 0.0D, 100.0D);
        TRAMPOLINE_MAX_BOUNCE = builder.defineInRange("trampolineMaxBounce", 2.4D, 0.1D, 10.0D);
        builder.pop();

        builder.push("rockHideBenGrimm");
        THING = new HeroStats(builder, 60.0D, 16.0D, 20.0D, 2.0D, 0.5D, 0.0D, 220.0D,
                List.of("minecraft:generic.knockback_resistance;1.0;ADD_VALUE", "minecraft:generic.max_absorption;40.0;ADD_VALUE"));
        ROCK_PLATING_MAX = builder.defineInRange("rockPlatingMax", 40.0D, 0.0D, 10000.0D);
        ROCK_PLATING_REGEN = builder.defineInRange("rockPlatingRegenPerSecond", 2.0D, 0.0D, 1000.0D);
        ROCK_PLATING_DELAY_SECONDS = builder.defineInRange("rockPlatingRegenDelaySeconds", 5, 0, 600);
        COMBO_MULTIPLIER = builder.defineInRange("thirdPunchDamageMultiplier", 2.0D, 1.0D, 100.0D);
        CLOBBER_DAMAGE_MULTIPLIER = builder.defineInRange("clobberDamageMultiplier", 2.5D, 0.0D, 100.0D);
        CLOBBER_COOLDOWN = builder.defineInRange("clobberCooldownSeconds", 8.0D, 0.0D, 600.0D);
        WRESTLE_DAMAGE_MULTIPLIER = builder.defineInRange("wrestleDamageMultiplier", 2.0D, 0.0D, 100.0D);
        WRESTLE_COOLDOWN = builder.defineInRange("wrestleCooldownSeconds", 10.0D, 0.0D, 600.0D);
        TOSS_MAX_BLOCKS = builder.defineInRange("ripAndTossMaxBlocks", 4, 1, 64);
        TOSS_THROW_SPEED = builder.defineInRange("ripAndTossThrowSpeed", 1.6D, 0.1D, 10.0D);
        TOSS_DAMAGE_MULTIPLIER = builder.defineInRange("ripAndTossDamageMultiplier", 1.5D, 0.0D, 100.0D);
        TOSS_COOLDOWN = builder.defineInRange("ripAndTossCooldownSeconds", 6.0D, 0.0D, 600.0D);
        BRACE_DAMAGE_MULTIPLIER = builder.defineInRange("braceDamageTakenMultiplier", 0.3D, 0.0D, 1.0D);
        THING_LEAP_HORIZONTAL = builder.defineInRange("superJumpHorizontal", 1.6D, 0.0D, 10.0D);
        THING_LEAP_VERTICAL = builder.defineInRange("superJumpVertical", 1.5D, 0.0D, 10.0D);
        THING_LEAP_STANDING_VERTICAL = builder.defineInRange("superJumpStandingVertical", 2.3D, 0.0D, 10.0D);
        THING_LEAP_COOLDOWN_TICKS = builder.defineInRange("superJumpCooldownTicks", 20, 0, 1200);
        builder.pop();

        IronManSuits.define(builder);

        SPEC = builder.build();
    }

    private MarvelBalanceConfig() {
    }

    public static final class HeroStats {
        public final ModConfigSpec.DoubleValue health;
        public final ModConfigSpec.DoubleValue attack;
        public final ModConfigSpec.DoubleValue armor;
        public final ModConfigSpec.DoubleValue attackKnockback;
        public final ModConfigSpec.DoubleValue stepHeight;
        public final ModConfigSpec.DoubleValue movementSpeed;
        public final ModConfigSpec.DoubleValue unarmedDamage;
        public final ModConfigSpec.ConfigValue<List<? extends String>> extraAttributes;

        HeroStats(ModConfigSpec.Builder builder, double health, double attack, double armor, double attackKnockback,
                  double stepHeight, double movementSpeed, double unarmedDamage) {
            this(builder, health, attack, armor, attackKnockback, stepHeight, movementSpeed, unarmedDamage, List.of());
        }

        HeroStats(ModConfigSpec.Builder builder, double health, double attack, double armor, double attackKnockback,
                  double stepHeight, double movementSpeed, double unarmedDamage, List<String> extraAttributes) {
            this.health = builder.defineInRange("bonusHealth", health, 0.0D, 10000.0D);
            this.attack = builder.defineInRange("bonusAttackDamage", attack, 0.0D, 10000.0D);
            this.armor = builder.defineInRange("bonusArmor", armor, 0.0D, 1000.0D);
            this.attackKnockback = builder.defineInRange("bonusAttackKnockback", attackKnockback, 0.0D, 100.0D);
            this.stepHeight = builder.defineInRange("bonusStepHeight", stepHeight, 0.0D, 10.0D);
            this.movementSpeed = builder.defineInRange("bonusMovementSpeed", movementSpeed, 0.0D, 1.0D);
            this.unarmedDamage = builder.defineInRange("bonusUnarmedDamage", unarmedDamage, 0.0D, 10000.0D);
            this.extraAttributes = builder
                    .comment("Extra permanent attribute modifiers while this power is injected.",
                            "Format: \"namespace:attribute;amount;OPERATION\", OPERATION = ADD_VALUE, ADD_MULTIPLIED_BASE or ADD_MULTIPLIED_TOTAL.")
                    .defineListAllowEmpty("extraAttributes", extraAttributes, () -> "", entry -> entry instanceof String);
            ALL_HERO_STATS.add(this);
        }
    }
}
