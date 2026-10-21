package com.craisinlord.integrated_superheroes.hero;

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
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_DAMAGE;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_SHIELD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_SPEED;
    public static final ModConfigSpec.DoubleValue SHIELD_CHARGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue RALLY_RADIUS;
    public static final ModConfigSpec.IntValue RALLY_DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue RALLY_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SUPER_SOLDIER_JUMP_MULTIPLIER;

    public static final HeroStats PANTHER;
    public static final ModConfigSpec.DoubleValue POUNCE_DAMAGE;
    public static final ModConfigSpec.DoubleValue POUNCE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CLAW_SWIPE_DAMAGE;
    public static final ModConfigSpec.DoubleValue CLAW_SWIPE_COOLDOWN;
    public static final ModConfigSpec.IntValue CLAW_BLEED_TICKS;
    public static final ModConfigSpec.DoubleValue ANCESTRAL_PLANE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ARROW_DEFLECT_CHANCE;
    public static final ModConfigSpec.DoubleValue SENSES_RADIUS;

    public static final HeroStats DEADPOOL;
    public static final ModConfigSpec.DoubleValue DEADPOOL_REGEN_PERCENT_PER_SECOND;
    public static final ModConfigSpec.IntValue UNLOAD_SHOTS;
    public static final ModConfigSpec.DoubleValue UNLOAD_DAMAGE;
    public static final ModConfigSpec.DoubleValue UNLOAD_RANGE;
    public static final ModConfigSpec.DoubleValue UNLOAD_COOLDOWN;
    public static final ModConfigSpec.DoubleValue DEADPOOL_GUN_BONUS;
    public static final ModConfigSpec.DoubleValue GRENADE_DAMAGE;
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
    public static final ModConfigSpec.DoubleValue RAGE_QUIT_DAMAGE;
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
    public static final ModConfigSpec.DoubleValue THUNDERCLAP_DAMAGE;
    public static final ModConfigSpec.DoubleValue THUNDERCLAP_COOLDOWN;
    public static final ModConfigSpec.IntValue HULK_SMASH_LENGTH;
    public static final ModConfigSpec.DoubleValue HULK_SMASH_DAMAGE;
    public static final ModConfigSpec.DoubleValue HULK_SMASH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GAMMA_LEAP_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue GAMMA_LEAP_VERTICAL;
    public static final ModConfigSpec.DoubleValue GAMMA_LEAP_STANDING_VERTICAL;
    public static final ModConfigSpec.IntValue GAMMA_LEAP_COOLDOWN_TICKS;
    public static final ModConfigSpec.DoubleValue GAMMA_LANDING_RADIUS;
    public static final ModConfigSpec.DoubleValue GAMMA_LANDING_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue GAMMA_LANDING_DAMAGE;
    public static final ModConfigSpec.DoubleValue RAGE_PER_HEART;
    public static final ModConfigSpec.DoubleValue RAGE_PER_HIT;
    public static final ModConfigSpec.DoubleValue RAGE_PER_IMPACT;
    public static final ModConfigSpec.IntValue RAGE_DECAY_DELAY_TICKS;
    public static final ModConfigSpec.DoubleValue RAGE_DECAY_PER_SECOND;
    public static final ModConfigSpec.DoubleValue RAGE_DAMAGE_BONUS;
    public static final ModConfigSpec.BooleanValue RAGE_FORCES_HULK_OUT;
    public static final ModConfigSpec.IntValue RAGE_MAX_STRENGTH_LEVEL;
    public static final ModConfigSpec.DoubleValue RAGE_COOLDOWN_REDUCTION;
    public static final ModConfigSpec.DoubleValue HULK_RAGE_GROWTH;
    public static final ModConfigSpec.BooleanValue HULK_RAMPAGE;
    public static final ModConfigSpec.DoubleValue HULK_RAMPAGE_CALM_HARDNESS;
    public static final ModConfigSpec.DoubleValue HULK_RAMPAGE_ANGRY_HARDNESS;
    public static final ModConfigSpec.DoubleValue HULK_RAMPAGE_FURIOUS_HARDNESS;
    public static final ModConfigSpec.DoubleValue HULK_RAMPAGE_UNSTOPPABLE_HARDNESS;
    public static final ModConfigSpec.IntValue HULK_RAMPAGE_ANGRY_RAGE;
    public static final ModConfigSpec.IntValue HULK_RAMPAGE_FURIOUS_RAGE;
    public static final ModConfigSpec.IntValue HULK_RAMPAGE_BLOCKS_PER_TICK;
    public static final ModConfigSpec.DoubleValue HULK_RAMPAGE_DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue HULK_CRATER_MAX_RADIUS;
    public static final ModConfigSpec.IntValue HULK_THUNDERCLAP_FLUNG_BLOCKS;
    public static final ModConfigSpec.DoubleValue LAST_STAND_COOLDOWN;

    public static final HeroStats MAGNETO;
    public static final ModConfigSpec.DoubleValue MAGNETO_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue GRIP_RANGE;
    public static final ModConfigSpec.IntValue GRIP_HOLD_TICKS;
    public static final ModConfigSpec.DoubleValue GRIP_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue GRIP_THROW_DAMAGE;
    public static final ModConfigSpec.DoubleValue GRIP_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CRUSH_RANGE;
    public static final ModConfigSpec.DoubleValue CRUSH_BASE_DAMAGE;
    public static final ModConfigSpec.DoubleValue CRUSH_PER_ARMOR_DAMAGE;
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
    public static final ModConfigSpec.DoubleValue LIGHTNING_DAMAGE;
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
    public static final ModConfigSpec.DoubleValue FREEZE_DAMAGE;
    public static final ModConfigSpec.IntValue FREEZE_TICKS;
    public static final ModConfigSpec.DoubleValue FREEZE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SHATTER_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CRYOGENESIS_RADIUS;
    public static final ModConfigSpec.DoubleValue CRYOGENESIS_DAMAGE;
    public static final ModConfigSpec.IntValue CRYOGENESIS_TICKS;
    public static final ModConfigSpec.DoubleValue CRYOGENESIS_COOLDOWN;
    public static final ModConfigSpec.IntValue ICE_WALL_TICKS;
    public static final ModConfigSpec.DoubleValue ICE_WALL_COOLDOWN;
    public static final ModConfigSpec.IntValue ICE_SLIDE_TICKS;
    public static final ModConfigSpec.DoubleValue ICE_FORM_FIRE_MULTIPLIER;

    public static final HeroStats SCARLET_WITCH;
    public static final ModConfigSpec.DoubleValue WANDA_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_RANGE;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_DAMAGE;
    public static final ModConfigSpec.DoubleValue HEX_BOLT_COOLDOWN;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HEX_EFFECTS;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_RANGE;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue TELEKINESIS_THROW_DAMAGE;
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
    public static final ModConfigSpec.DoubleValue FASTBALL_THROW_DAMAGE;
    public static final ModConfigSpec.DoubleValue FASTBALL_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue FASTBALL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STEEL_CHARGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STEEL_CHARGE_DAMAGE;
    public static final ModConfigSpec.DoubleValue IRON_SLAM_DAMAGE;
    public static final ModConfigSpec.DoubleValue IRON_SLAM_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BULWARK_REDUCTION;

    public static final HeroStats DAREDEVIL;
    public static final ModConfigSpec.DoubleValue RADAR_RANGE;
    public static final ModConfigSpec.DoubleValue DAREDEVIL_LEAP_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DAREDEVIL_LEAP_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue DAREDEVIL_FLIP_CHANCE;
    public static final ModConfigSpec.DoubleValue RADAR_DARK_RANGE;
    public static final ModConfigSpec.DoubleValue DANGER_SENSE_CHANCE;
    public static final ModConfigSpec.DoubleValue BILLY_CLUB_DAMAGE;
    public static final ModConfigSpec.IntValue BILLY_CLUB_BOUNCES;
    public static final ModConfigSpec.DoubleValue BILLY_CLUB_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GRAPPLE_RANGE;
    public static final ModConfigSpec.DoubleValue GRAPPLE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue TAKEDOWN_DAMAGE;
    public static final ModConfigSpec.IntValue TAKEDOWN_STUN_TICKS;
    public static final ModConfigSpec.DoubleValue TAKEDOWN_COOLDOWN;

    public static final HeroStats KITTY;
    public static final ModConfigSpec.DoubleValue PHASE_METER_SECONDS;
    public static final ModConfigSpec.DoubleValue PHASE_STRIKE_DAMAGE;
    public static final ModConfigSpec.DoubleValue PHASE_STRIKE_COOLDOWN;
    public static final ModConfigSpec.IntValue PHASE_SINK_TICKS;
    public static final ModConfigSpec.DoubleValue PHASE_SINK_COOLDOWN;
    public static final ModConfigSpec.IntValue PHASE_OTHERS_TICKS;
    public static final ModConfigSpec.DoubleValue PHASE_OTHERS_COOLDOWN;

    public static final HeroStats SQUIRREL_GIRL;
    public static final ModConfigSpec.IntValue SWARM_SIZE;
    public static final ModConfigSpec.DoubleValue SWARM_RESPAWN_SECONDS;
    public static final ModConfigSpec.DoubleValue SWARM_SQUIRREL_HEALTH;
    public static final ModConfigSpec.DoubleValue SWARM_BITE_DAMAGE;
    public static final ModConfigSpec.DoubleValue SWARM_CALL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SWARM_FORAGE_RANGE;
    public static final ModConfigSpec.DoubleValue SIC_EM_RANGE;
    public static final ModConfigSpec.DoubleValue SIC_EM_COOLDOWN;
    public static final ModConfigSpec.IntValue PILE_TICKS;
    public static final ModConfigSpec.DoubleValue PILE_BITE_DAMAGE;
    public static final ModConfigSpec.DoubleValue PILE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue GNAW_TICKS_PER_HARDNESS;
    public static final ModConfigSpec.IntValue GNAW_MAX_BLOCKS;
    public static final ModConfigSpec.BooleanValue GNAW_DROPS;
    public static final ModConfigSpec.DoubleValue TIPPY_BITE_DAMAGE;
    public static final ModConfigSpec.IntValue TIPPY_RESPAWN_SECONDS;

    public static final HeroStats TORCH;
    public static final ModConfigSpec.DoubleValue TORCH_FLY_SPEED;
    public static final ModConfigSpec.DoubleValue FLAME_OUT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FIREBALL_DAMAGE;
    public static final ModConfigSpec.DoubleValue FIREBALL_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FLAME_CAGE_RANGE;
    public static final ModConfigSpec.IntValue FLAME_CAGE_TICKS;
    public static final ModConfigSpec.DoubleValue FLAME_CAGE_DAMAGE;
    public static final ModConfigSpec.DoubleValue FLAME_CAGE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue NOVA_RADIUS;
    public static final ModConfigSpec.DoubleValue NOVA_DAMAGE;
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
    public static final ModConfigSpec.DoubleValue FORCE_BLAST_DAMAGE;
    public static final ModConfigSpec.DoubleValue FORCE_BLAST_COOLDOWN;

    public static final HeroStats REED;
    public static final ModConfigSpec.DoubleValue STRETCH_PUNCH_RANGE;
    public static final ModConfigSpec.DoubleValue STRETCH_PUNCH_DAMAGE;
    public static final ModConfigSpec.DoubleValue STRETCH_PUNCH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STRETCH_HAMMER_RANGE;
    public static final ModConfigSpec.DoubleValue STRETCH_HAMMER_RADIUS;
    public static final ModConfigSpec.DoubleValue STRETCH_HAMMER_DAMAGE;
    public static final ModConfigSpec.DoubleValue STRETCH_HAMMER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue STRETCH_SWEEP_RADIUS;
    public static final ModConfigSpec.DoubleValue STRETCH_SWEEP_DAMAGE;
    public static final ModConfigSpec.DoubleValue STRETCH_SWEEP_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BOUNCE_MAX_SPEED;
    public static final ModConfigSpec.DoubleValue SHAPE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SLINGSHOT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue SLINGSHOT_SPEED;
    public static final ModConfigSpec.DoubleValue BALL_SECONDS;
    public static final ModConfigSpec.DoubleValue BALL_DAMAGE;
    public static final ModConfigSpec.DoubleValue POGO_BOUNCE;
    public static final ModConfigSpec.DoubleValue POGO_BOOST_BOUNCE;
    public static final ModConfigSpec.DoubleValue POGO_STOMP_DAMAGE;
    public static final ModConfigSpec.IntValue BRIDGE_LENGTH;
    public static final ModConfigSpec.DoubleValue BRIDGE_SECONDS;
    public static final ModConfigSpec.DoubleValue STILTS_STEP_HEIGHT;
    public static final ModConfigSpec.DoubleValue STILTS_SPEED_BONUS;
    public static final ModConfigSpec.DoubleValue BOAT_SPEED;

    public static final HeroStats THING;
    public static final ModConfigSpec.DoubleValue ROCK_PLATING_MAX;
    public static final ModConfigSpec.DoubleValue ROCK_PLATING_REGEN;
    public static final ModConfigSpec.IntValue ROCK_PLATING_DELAY_SECONDS;
    public static final ModConfigSpec.DoubleValue COMBO_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CLOBBER_DAMAGE;
    public static final ModConfigSpec.DoubleValue CRATER_DAMAGE;
    public static final ModConfigSpec.DoubleValue CLOBBER_COOLDOWN;
    public static final ModConfigSpec.DoubleValue WRESTLE_DAMAGE;
    public static final ModConfigSpec.DoubleValue WRESTLE_COOLDOWN;
    public static final ModConfigSpec.IntValue TOSS_MAX_BLOCKS;
    public static final ModConfigSpec.DoubleValue TOSS_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue TOSS_DAMAGE;
    public static final ModConfigSpec.DoubleValue TOSS_COOLDOWN;
    public static final ModConfigSpec.DoubleValue BRACE_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue THING_LEAP_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue THING_LEAP_VERTICAL;
    public static final ModConfigSpec.DoubleValue THING_LEAP_STANDING_VERTICAL;
    public static final ModConfigSpec.IntValue THING_LEAP_COOLDOWN_TICKS;

    public static final HeroStats GAMBIT;
    public static final ModConfigSpec.DoubleValue KINETIC_DAMAGE;
    public static final ModConfigSpec.DoubleValue KINETIC_RADIUS;
    public static final ModConfigSpec.DoubleValue KINETIC_IMPACT_DAMAGE;
    public static final ModConfigSpec.DoubleValue KINETIC_WEAPON_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue KINETIC_THROW_SPEED;
    public static final ModConfigSpec.DoubleValue KINETIC_THROW_COOLDOWN;
    public static final ModConfigSpec.DoubleValue PHANTOM_CARD_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue CARD_RADIUS_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue HEAVY_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue EXPLOSIVE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue KINETIC_BLOCK_DAMAGE;
    public static final ModConfigSpec.IntValue RETURN_DURABILITY_COST;
    public static final ModConfigSpec.IntValue CARD_FAN_COUNT;
    public static final ModConfigSpec.DoubleValue CARD_FAN_SPREAD;
    public static final ModConfigSpec.DoubleValue CARD_FAN_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CHARGE_TOUCH_RANGE;
    public static final ModConfigSpec.IntValue CHARGE_TOUCH_FUSE_TICKS;
    public static final ModConfigSpec.DoubleValue CHARGE_TOUCH_DAMAGE;
    public static final ModConfigSpec.DoubleValue CHARGE_TOUCH_RADIUS;
    public static final ModConfigSpec.DoubleValue CHARGE_TOUCH_COOLDOWN;
    public static final ModConfigSpec.DoubleValue VAULT_HORIZONTAL;
    public static final ModConfigSpec.DoubleValue VAULT_VERTICAL;
    public static final ModConfigSpec.DoubleValue VAULT_STAFF_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue VAULT_SLAM_DAMAGE;
    public static final ModConfigSpec.DoubleValue VAULT_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FULL_HOUSE_RANGE;
    public static final ModConfigSpec.IntValue FULL_HOUSE_TARGETS;
    public static final ModConfigSpec.DoubleValue FULL_HOUSE_DAMAGE;
    public static final ModConfigSpec.DoubleValue FULL_HOUSE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue CHARGED_AMMO_RADIUS;
    public static final ModConfigSpec.DoubleValue CHARGED_AMMO_DAMAGE;
    public static final ModConfigSpec.DoubleValue CHARGED_AMMO_EXHAUSTION;
    public static final ModConfigSpec.BooleanValue CHARGE_GUN_PROJECTILES;
    public static final ModConfigSpec.BooleanValue CAJUN_CHARM;

    public static final HeroStats ROGUE;
    public static final ModConfigSpec.IntValue MEMORY_SLOTS;
    public static final ModConfigSpec.DoubleValue MEMORY_SECONDS;
    public static final ModConfigSpec.DoubleValue BOSS_MEMORY_SECONDS;
    public static final ModConfigSpec.DoubleValue MEMORY_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue MEMORY_COOLDOWN_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue DRAIN_DAMAGE;
    public static final ModConfigSpec.DoubleValue DRAIN_HEAL;
    public static final ModConfigSpec.IntValue DRAIN_WEAKEN_TICKS;
    public static final ModConfigSpec.DoubleValue INVOLUNTARY_DRAIN_DAMAGE;
    public static final ModConfigSpec.DoubleValue SIPHON_RANGE;
    public static final ModConfigSpec.DoubleValue SIPHON_DAMAGE;
    public static final ModConfigSpec.DoubleValue SIPHON_COOLDOWN;
    public static final ModConfigSpec.BooleanValue ABSORB_PLAYERS;
    public static final ModConfigSpec.DoubleValue BORROW_SECONDS;
    public static final ModConfigSpec.DoubleValue VICTIM_SHUTDOWN_SECONDS;
    public static final ModConfigSpec.BooleanValue PETRIFY_MOBS;

    public static final HeroStats JUBILEE;
    public static final ModConfigSpec.DoubleValue PAFF_DAMAGE;
    public static final ModConfigSpec.DoubleValue PAFF_SPEED;
    public static final ModConfigSpec.DoubleValue PAFF_COOLDOWN;
    public static final ModConfigSpec.IntValue VOLLEY_ROCKETS;
    public static final ModConfigSpec.DoubleValue VOLLEY_DAMAGE;
    public static final ModConfigSpec.DoubleValue VOLLEY_RANGE;
    public static final ModConfigSpec.DoubleValue VOLLEY_COOLDOWN;
    public static final ModConfigSpec.DoubleValue FLASHBANG_RADIUS;
    public static final ModConfigSpec.IntValue FLASHBANG_TICKS;
    public static final ModConfigSpec.DoubleValue FLASHBANG_DAMAGE;
    public static final ModConfigSpec.DoubleValue FLASHBANG_COOLDOWN;
    public static final ModConfigSpec.DoubleValue PYRO_JUMP_VERTICAL;
    public static final ModConfigSpec.DoubleValue PYRO_JUMP_HORIZONTAL;
    public static final ModConfigSpec.IntValue PYRO_JUMP_ELYTRA_FLIGHT;
    public static final ModConfigSpec.DoubleValue PYRO_JUMP_COOLDOWN;
    public static final ModConfigSpec.IntValue FINALE_ROCKETS;
    public static final ModConfigSpec.DoubleValue FINALE_RADIUS;
    public static final ModConfigSpec.DoubleValue FINALE_DAMAGE;
    public static final ModConfigSpec.IntValue FINALE_TICKS;
    public static final ModConfigSpec.DoubleValue FINALE_COOLDOWN;
    public static final ModConfigSpec.DoubleValue ELYTRA_BOOST_MULTIPLIER;
    public static final ModConfigSpec.IntValue LIGHT_SHOW_LEVEL;

    public static final HeroStats VILTRUMITE;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_BASE_DAMAGE;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_DAMAGE_REDUCTION;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_DAMAGE_IGNORE;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_HEAL_FACTOR;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_MAX_FLIGHT_SPEED;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_THROTTLE;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_SYRINGE_ONLY;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_TAKEOFF_BREAKS_BLOCKS;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_HUMANS_BLEED;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_NPC_MAX_REDUCTION;
    public static final ModConfigSpec.IntValue VILTRUMITE_NPC_BLOOD_DROPS;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_NPC_DAMAGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_NPC_PER_HIT_CAP;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_NPC_SQUAD_HEALTH;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_NPC_CONQUEST_HEALTH;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_NPC_ONLY_PLAYER_DAMAGE;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_NPC_PICKUP;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_NPC_BREAK_BLOCK_ENTITIES;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_INVASION_NEEDS_DRAGON;
    public static final ModConfigSpec.ConfigValue<String> VILTRUMITE_INVASION_ADVANCEMENT;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_SUIT;
    public static final ModConfigSpec.ConfigValue<String> VILTRUMITE_SUIT_SKIN;
    public static final ModConfigSpec.ConfigValue<String> VILTRUMITE_SUIT_CAPE;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_ANIMATIONS;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_CAPTAIN_MARVEL_FLIGHT;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_ROGUE_BASE_DAMAGE;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_ROGUE_DAMAGE_REDUCTION;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_ROGUE_DASH_DAMAGE;
    public static final ModConfigSpec.DoubleValue VILTRUMITE_ROGUE_DASH_COOLDOWN;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_GUARD_FLIGHT;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_GUARD_TELEPORTS;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_GUARD_DAMAGE;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_GUARD_EFFECTS;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_GUARD_VISUALS;
    public static final ModConfigSpec.BooleanValue VILTRUMITE_GUARD_PERFORMANCE;

    public static final ModConfigSpec.DoubleValue X_GENES_HYDRA_BASE_CHANCE;
    public static final ModConfigSpec.DoubleValue X_GENES_HYDRA_OUTPOST_CHANCE;
    public static final ModConfigSpec.DoubleValue X_GENES_WITCH_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.push("spiderPowersPeterParker");
        SPIDER_POWERS = new HeroStats(builder, 10.0D, 8.0D, 0.5D, 0.5D, 0.04D, 0.0D, 2,
                List.of("apothic_attributes:dodge_chance;0.16;ADD_VALUE", "sinew:air_speed;0.02;ADD_VALUE"));
        builder.pop();

        builder.push("weaponXLogan");
        WEAPON_X_LOGAN = new HeroStats(builder, 16.0D, 12.0D, 0.2D, 0.5D, 0.02D, 21.0D, 1,
                List.of("apothic_attributes:healing_received;0.6;ADD_VALUE", "apothic_attributes:crit_damage;0.4;ADD_VALUE"));
        LOGAN_CLAW_DAMAGE = builder.defineInRange("clawAttackBaseDamage", 22.0D, 0.0D, 10000.0D);
        double[] wolverineDowned = {5.0D, 8.0D};
        double[] wolverineHealth = {0.4D, 0.2D};
        for (int i = 0; i < wolverineDowned.length; i++) {
            WOLVERINE_DOWNED_SECONDS.add(builder.defineInRange("revive" + (i + 1) + "DownedSeconds", wolverineDowned[i], 0.0D, 600.0D));
            WOLVERINE_STAND_HEALTH.add(builder.defineInRange("revive" + (i + 1) + "StandHealthFraction", wolverineHealth[i], 0.01D, 1.0D));
        }
        builder.pop();

        builder.push("superSoldierSteveRogers");
        SUPER_SOLDIER = new HeroStats(builder, 30.0D, 12.0D, 0.5D, 0.0D, 0.0D, 42.0D, 1,
                List.of("apothic_attributes:healing_received;0.3;ADD_VALUE", "minecraft:generic.knockback_resistance;0.2;ADD_VALUE"));
        SUPER_SOLDIER_JUMP_MULTIPLIER = builder.defineInRange("crouchJumpMultiplier", 3.0D, 1.0D, 20.0D);
        SHIELD_CHARGE_DAMAGE = builder.defineInRange("shieldChargeDamage", 79.0D, 0.0D, 10000.0D);
        SHIELD_CHARGE_SHIELD_MULTIPLIER = builder.defineInRange("shieldChargeShieldMultiplier", 1.5D, 1.0D, 100.0D);
        SHIELD_CHARGE_SPEED = builder.defineInRange("shieldChargeSpeed", 1.8D, 0.0D, 10.0D);
        SHIELD_CHARGE_COOLDOWN = builder.defineInRange("shieldChargeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        RALLY_RADIUS = builder.defineInRange("rallyRadius", 12.0D, 0.0D, 64.0D);
        RALLY_DURATION_TICKS = builder.defineInRange("rallyDurationTicks", 200, 0, 12000);
        RALLY_COOLDOWN = builder.defineInRange("rallyCooldownSeconds", 45.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("heartShapedHerbTchalla");
        PANTHER = new HeroStats(builder, 30.0D, 12.0D, 0.5D, 0.0D, 0.0D, 42.0D, 2,
                List.of("apothic_attributes:crit_chance;0.08;ADD_VALUE", "apothic_attributes:crit_damage;0.3;ADD_VALUE"));
        POUNCE_DAMAGE = builder.defineInRange("pounceDamage", 83.0D, 0.0D, 10000.0D);
        POUNCE_COOLDOWN = builder.defineInRange("pounceCooldownSeconds", 6.0D, 0.0D, 600.0D);
        CLAW_SWIPE_DAMAGE = builder.defineInRange("clawSwipeDamage", 111.0D, 0.0D, 10000.0D);
        CLAW_SWIPE_COOLDOWN = builder.defineInRange("clawSwipeCooldownSeconds", 4.0D, 0.0D, 600.0D);
        CLAW_BLEED_TICKS = builder.defineInRange("clawSwipeBleedTicks", 100, 0, 1200);
        ANCESTRAL_PLANE_COOLDOWN = builder.defineInRange("ancestralPlaneCooldownSeconds", 600.0D, 0.0D, 36000.0D);
        ARROW_DEFLECT_CHANCE = builder.defineInRange("arrowDeflectChance", 0.6D, 0.0D, 1.0D);
        SENSES_RADIUS = builder.defineInRange("heightenedSensesRadius", 24.0D, 0.0D, 128.0D);
        builder.pop();

        builder.push("weaponXWadeWilson");
        DEADPOOL = new HeroStats(builder, 30.0D, 10.0D, 0.3D, 0.0D, 0.0D, 28.0D, 1,
                List.of("apothic_attributes:healing_received;0.4;ADD_VALUE", "apothic_attributes:crit_damage;0.4;ADD_VALUE"));
        DEADPOOL_REGEN_PERCENT_PER_SECOND = builder.defineInRange("regenPercentOfMaxHealthPerSecond", 0.04D, 0.0D, 1.0D);
        UNLOAD_SHOTS = builder.defineInRange("unloadShots", 8, 1, 64);
        UNLOAD_DAMAGE = builder.defineInRange("unloadDamagePerShot", 20.0D, 0.0D, 10000.0D);
        UNLOAD_RANGE = builder.defineInRange("unloadRange", 24.0D, 1.0D, 128.0D);
        UNLOAD_COOLDOWN = builder.defineInRange("unloadCooldownSeconds", 6.0D, 0.0D, 600.0D);
        DEADPOOL_GUN_BONUS = builder.defineInRange("gunDamageBonus", 0.25D, 0.0D, 10.0D);
        GRENADE_DAMAGE = builder.defineInRange("grenadeDamage", 68.0D, 0.0D, 10000.0D);
        GRENADE_RADIUS = builder.defineInRange("grenadeRadius", 3.5D, 0.5D, 16.0D);
        GRENADE_FUMBLE_CHANCE = builder.defineInRange("grenadeFumbleChance", 0.08D, 0.0D, 1.0D);
        GRENADE_FUMBLE_SELF_DAMAGE = builder.defineInRange("grenadeFumbleSelfDamage", 10.5D, 0.0D, 10000.0D);
        GRENADE_COOLDOWN = builder.defineInRange("grenadeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        RETCON_SECONDS = builder.defineInRange("retconSeconds", 5, 1, 30);
        RETCON_HEAL_CAP = builder.defineInRange("retconHealCap", 6.0D, 0.0D, 1000.0D);
        RETCON_COOLDOWN = builder.defineInRange("retconCooldownSeconds", 20.0D, 0.0D, 600.0D);
        SMASH_CUT_COOLDOWN = builder.defineInRange("smashCutCooldownSeconds", 300.0D, 0.0D, 6000.0D);
        PAUSE_MENU_RADIUS = builder.defineInRange("pauseMenuRadius", 8.0D, 1.0D, 64.0D);
        PAUSE_MENU_SECONDS = builder.defineInRange("pauseMenuSeconds", 3.0D, 0.5D, 60.0D);
        PAUSE_MENU_COOLDOWN = builder.defineInRange("pauseMenuCooldownSeconds", 30.0D, 0.0D, 600.0D);
        RAGE_QUIT_RADIUS = builder.defineInRange("rageQuitRadius", 6.0D, 1.0D, 32.0D);
        RAGE_QUIT_DAMAGE = builder.defineInRange("rageQuitDamage", 51.0D, 0.0D, 10000.0D);
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
        QUICKSILVER = new HeroStats(builder, 20.0D, 8.0D, 0.2D, 0.0D, 0.0D, 14.0D, 0,
                List.of("apothic_attributes:dodge_chance;0.16;ADD_VALUE"));
        SPEED_FORCE_MAX_RATE = builder.defineInRange("speedForceMaxRate", 10.0D, 1.0D, 20.0D);
        SPEED_FORCE_START_RATE = builder.defineInRange("speedForceStartRate", 2.0D, 1.0D, 20.0D);
        SPEED_FORCE_STEP = builder.defineInRange("speedForceScrollStep", 0.5D, 0.05D, 5.0D);
        SPEED_FORCE_EXHAUSTION_PER_RATE = builder.defineInRange("speedForceExhaustionPerRatePerTick", 0.02D, 0.0D, 10.0D);
        BULLET_TIME_RADIUS = builder.defineInRange("bulletTimeRadius", 64.0D, 1.0D, 160.0D);
        BULLET_TIME_RATE = builder.defineInRange("bulletTimeRate", 0.1D, 0.05D, 1.0D);
        BULLET_TIME_DURATION_TICKS = builder.defineInRange("bulletTimeDurationTicks", 240, 20, 12000);
        BULLET_TIME_COOLDOWN = builder.defineInRange("bulletTimeCooldownSeconds", 60.0D, 0.0D, 600.0D);
        SONIC_BOOM_DAMAGE = builder.defineInRange("sonicBoomDamageAtMinRate", 30.0D, 0.0D, 10000.0D);
        SONIC_BOOM_RADIUS = builder.defineInRange("sonicBoomRadius", 5.0D, 0.0D, 32.0D);
        SONIC_BOOM_MIN_RATE = builder.defineInRange("sonicBoomMinimumRate", 3.0D, 1.0D, 20.0D);
        SONIC_BOOM_COOLDOWN = builder.defineInRange("sonicBoomCooldownSeconds", 6.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("opticBlastsScottSummers");
        CYCLOPS = new HeroStats(builder, 20.0D, 8.0D, 0.2D, 0.0D, 0.0D, 2.8D, 0,
                List.of("apothic_attributes:crit_chance;0.12;ADD_VALUE", "apothic_attributes:armor_pierce;10.0;ADD_VALUE"));
        OPTIC_BLAST_DAMAGE = builder.defineInRange("opticBlastDamagePerHit", 112.0D, 0.0D, 10000.0D);
        OPTIC_BLAST_DURATION_TICKS = builder.defineInRange("opticBlastDurationTicks", 20, 1, 200);
        OPTIC_BLAST_RANGE = builder.defineInRange("opticBlastRange", 64.0D, 1.0D, 256.0D);
        OPTIC_BLAST_COOLDOWN = builder.defineInRange("opticBlastCooldownSeconds", 2.0D, 0.0D, 600.0D);
        WIDE_BLAST_DAMAGE = builder.defineInRange("wideBlastDamagePerHit", 90.0D, 0.0D, 10000.0D);
        WIDE_BLAST_RANGE = builder.defineInRange("wideBlastRange", 18.0D, 1.0D, 64.0D);
        WIDE_BLAST_COOLDOWN = builder.defineInRange("wideBlastCooldownSeconds", 4.0D, 0.0D, 600.0D);
        WIDE_BLAST_DURATION_TICKS = builder.defineInRange("wideBlastDurationTicks", 14, 1, 200);
        UNCONTROLLED_DAMAGE = builder.defineInRange("uncontrolledBeamDamagePerHit", 60.0D, 0.0D, 10000.0D);
        UNCONTROLLED_RANGE = builder.defineInRange("uncontrolledBeamRange", 40.0D, 1.0D, 256.0D);
        OPTIC_FIRE_SECONDS = builder.defineInRange("beamFireSeconds", 5, 0, 60);
        OPTIC_TERRAIN_RADIUS = builder.defineInRange("beamTerrainRadius", 1.5D, 0.0D, 8.0D);
        OPTIC_BLOCK_DROP_CHANCE = builder.defineInRange("beamBlockDropChance", 0.1D, 0.0D, 1.0D);
        OPTIC_MAX_BLOCK_RESISTANCE = builder.defineInRange("beamMaxBlockBlastResistance", 50.0D, 0.0D, 3600000.0D);
        builder.pop();

        builder.push("binaryCarolDanvers");
        BINARY = new HeroStats(builder, 60.0D, 20.0D, 3.0D, 0.0D, 0.0D, 126.0D, 2,
                List.of("apothic_attributes:armor_pierce;32.0;ADD_VALUE", "apothic_attributes:crit_damage;0.6;ADD_VALUE"));
        BINARY_FLY_SPEED = builder.defineInRange("flySpeed", 0.1D, 0.01D, 1.0D);
        PHOTON_BLAST_DAMAGE = builder.defineInRange("photonBlastDamagePerHit", 90.0D, 0.0D, 10000.0D);
        PHOTON_BLAST_RANGE = builder.defineInRange("photonBlastRange", 64.0D, 1.0D, 256.0D);
        PHOTON_BLAST_TERRAIN_RADIUS = builder.defineInRange("photonBlastTerrainRadius", 2.5D, 0.0D, 10.0D);
        PHOTON_BLAST_DURATION_TICKS = builder.defineInRange("photonBlastDurationTicks", 24, 1, 200);
        PHOTON_FIRE_SECONDS = builder.defineInRange("photonBlastFireSeconds", 8, 0, 60);
        PHOTON_BLAST_COOLDOWN = builder.defineInRange("photonBlastCooldownSeconds", 2.0D, 0.0D, 600.0D);
        BINARY_MODE_DURATION_TICKS = builder.defineInRange("binaryModeDurationTicks", 300, 20, 12000);
        BINARY_MODE_AURA_DAMAGE = builder.defineInRange("binaryModeAuraDamage", 38.0D, 0.0D, 10000.0D);
        BINARY_MODE_AURA_RADIUS = builder.defineInRange("binaryModeAuraRadius", 6.0D, 0.0D, 32.0D);
        BINARY_MODE_COOLDOWN = builder.defineInRange("binaryModeCooldownSeconds", 120.0D, 0.0D, 3600.0D);
        COSMIC_DASH_SPEED = builder.defineInRange("cosmicDashSpeed", 3.0D, 0.0D, 10.0D);
        COSMIC_DASH_DAMAGE = builder.defineInRange("cosmicDashDamage", 90.0D, 0.0D, 10000.0D);
        COSMIC_DASH_COOLDOWN = builder.defineInRange("cosmicDashCooldownSeconds", 5.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("gammaMutationBruceBanner");
        HULK = new HeroStats(builder, 85.0D, 22.0D, 4.0D, 1.0D, 0.0D, 115.0D, 0,
                List.of("minecraft:generic.scale;0.5;ADD_MULTIPLIED_BASE", "minecraft:generic.knockback_resistance;1.0;ADD_VALUE",
                        "apothic_attributes:healing_received;0.5;ADD_VALUE", "minecraft:player.entity_interaction_range;1.0;ADD_VALUE",
                        "minecraft:generic.safe_fall_distance;40.0;ADD_VALUE"));
        HULK_OUT_COOLDOWN = builder.defineInRange("hulkOutCooldownSeconds", 3.0D, 0.0D, 600.0D);
        THUNDERCLAP_RANGE = builder.defineInRange("thunderclapRange", 12.0D, 1.0D, 64.0D);
        THUNDERCLAP_DAMAGE = builder.defineInRange("thunderclapDamage", 131.0D, 0.0D, 10000.0D);
        THUNDERCLAP_COOLDOWN = builder.defineInRange("thunderclapCooldownSeconds", 10.0D, 0.0D, 600.0D);
        HULK_SMASH_LENGTH = builder.defineInRange("hulkSmashWaveLength", 10, 1, 32);
        HULK_SMASH_DAMAGE = builder.defineInRange("hulkSmashDamage", 105.0D, 0.0D, 10000.0D);
        HULK_SMASH_COOLDOWN = builder.defineInRange("hulkSmashCooldownSeconds", 4.0D, 0.0D, 600.0D);
        GAMMA_LEAP_HORIZONTAL = builder.defineInRange("gammaLeapHorizontal", 1.8D, 0.0D, 10.0D);
        GAMMA_LEAP_VERTICAL = builder.defineInRange("gammaLeapVertical", 1.6D, 0.0D, 10.0D);
        GAMMA_LEAP_STANDING_VERTICAL = builder.defineInRange("gammaLeapStandingVertical", 2.6D, 0.0D, 10.0D);
        GAMMA_LEAP_COOLDOWN_TICKS = builder.defineInRange("gammaLeapCooldownTicks", 20, 0, 1200);
        GAMMA_LANDING_RADIUS = builder.defineInRange("gammaLandingRadius", 5.0D, 0.0D, 32.0D);
        GAMMA_LANDING_DAMAGE = builder.defineInRange("gammaLandingBaseDamage", 131.0D, 0.0D, 10000.0D);
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
        HULK_RAGE_GROWTH = builder.defineInRange("rageMaxExtraScale", 0.2D, 0.0D, 2.0D);
        HULK_RAMPAGE = builder.define("rampageBreaksTerrain", true);
        HULK_RAMPAGE_CALM_HARDNESS = builder.defineInRange("rampageCalmMaxHardness", 0.6D, 0.0D, 100.0D);
        HULK_RAMPAGE_ANGRY_HARDNESS = builder.defineInRange("rampageAngryMaxHardness", 3.0D, 0.0D, 100.0D);
        HULK_RAMPAGE_FURIOUS_HARDNESS = builder.defineInRange("rampageFuriousMaxHardness", 10.0D, 0.0D, 100.0D);
        HULK_RAMPAGE_UNSTOPPABLE_HARDNESS = builder.defineInRange("rampageUnstoppableMaxHardness", 49.0D, 0.0D, 100.0D);
        HULK_RAMPAGE_ANGRY_RAGE = builder.defineInRange("rampageAngryRage", 25, 0, 100);
        HULK_RAMPAGE_FURIOUS_RAGE = builder.defineInRange("rampageFuriousRage", 60, 0, 100);
        HULK_RAMPAGE_BLOCKS_PER_TICK = builder.defineInRange("rampageMaxBlocksPerTick", 200, 1, 2000);
        HULK_RAMPAGE_DROP_CHANCE = builder.defineInRange("rampageBlockDropChance", 0.15D, 0.0D, 1.0D);
        HULK_CRATER_MAX_RADIUS = builder.defineInRange("rampageCraterMaxRadius", 6.0D, 0.0D, 16.0D);
        HULK_THUNDERCLAP_FLUNG_BLOCKS = builder.defineInRange("rampageThunderclapFlungBlocks", 24, 0, 128);
        LAST_STAND_COOLDOWN = builder.defineInRange("lastStandHulkOutCooldownSeconds", 180.0D, 0.0D, 36000.0D);
        builder.push("banner");
        BANNER = new HeroStats(builder, 4.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0);
        builder.pop();
        builder.pop();

        builder.push("magnetismErikLehnsherr");
        MAGNETO = new HeroStats(builder, 40.0D, 16.0D, 0.5D, 0.0D, 0.0D, 7.0D, 0,
                List.of("apothic_attributes:armor_pierce;16.0;ADD_VALUE", "minecraft:generic.knockback_resistance;0.5;ADD_VALUE"));
        MAGNETO_FLY_SPEED = builder.defineInRange("flySpeed", 0.06D, 0.01D, 1.0D);
        GRIP_RANGE = builder.defineInRange("magneticGripMetalRange", 24.0D, 1.0D, 64.0D);
        GRIP_HOLD_TICKS = builder.defineInRange("magneticGripHoldTicks", 120, 1, 1200);
        GRIP_THROW_SPEED = builder.defineInRange("magneticGripThrowSpeed", 2.5D, 0.0D, 10.0D);
        GRIP_THROW_DAMAGE = builder.defineInRange("magneticGripThrowDamage", 82.0D, 0.0D, 10000.0D);
        GRIP_COOLDOWN = builder.defineInRange("magneticGripCooldownSeconds", 6.0D, 0.0D, 600.0D);
        CRUSH_RANGE = builder.defineInRange("magneticCrushRange", 24.0D, 1.0D, 64.0D);
        CRUSH_BASE_DAMAGE = builder.defineInRange("magneticCrushBaseDamage", 41.0D, 0.0D, 10000.0D);
        CRUSH_PER_ARMOR_DAMAGE = builder.defineInRange("magneticCrushDamagePerArmorPoint", 8.2D, 0.0D, 10000.0D);
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
        STORM = new HeroStats(builder, 30.0D, 12.0D, 0.3D, 0.0D, 0.0D, 4.2D, 0,
                List.of("apothic_attributes:dodge_chance;0.08;ADD_VALUE", "minecraft:generic.safe_fall_distance;20.0;ADD_VALUE"));
        STORM_FLY_SPEED = builder.defineInRange("flySpeed", 0.08D, 0.01D, 1.0D);
        LIGHTNING_RANGE = builder.defineInRange("lightningStrikeRange", 48.0D, 1.0D, 128.0D);
        LIGHTNING_DAMAGE = builder.defineInRange("lightningStrikeDamage", 59.0D, 0.0D, 10000.0D);
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
        ICEMAN = new HeroStats(builder, 24.0D, 10.0D, 0.2D, 1.0D, 0.02D, 4.2D, 0,
                List.of("apothic_attributes:crit_damage;0.3;ADD_VALUE", "apothic_attributes:dodge_chance;0.06;ADD_VALUE"));
        FREEZE_RANGE = builder.defineInRange("freezeBlastRange", 14.0D, 1.0D, 64.0D);
        FREEZE_DAMAGE = builder.defineInRange("freezeBlastDamage", 20.0D, 0.0D, 10000.0D);
        FREEZE_TICKS = builder.defineInRange("freezeBlastFreezeTicks", 80, 1, 1200);
        FREEZE_COOLDOWN = builder.defineInRange("freezeBlastCooldownSeconds", 7.0D, 0.0D, 600.0D);
        SHATTER_MULTIPLIER = builder.defineInRange("shatterDamageMultiplier", 1.5D, 1.0D, 100.0D);
        CRYOGENESIS_RADIUS = builder.defineInRange("cryogenesisRadius", 6.0D, 1.0D, 32.0D);
        CRYOGENESIS_DAMAGE = builder.defineInRange("cryogenesisDamage", 10.0D, 0.0D, 10000.0D);
        CRYOGENESIS_TICKS = builder.defineInRange("cryogenesisFreezeTicks", 60, 1, 1200);
        CRYOGENESIS_COOLDOWN = builder.defineInRange("cryogenesisCooldownSeconds", 18.0D, 0.0D, 600.0D);
        ICE_WALL_TICKS = builder.defineInRange("iceWallTicks", 160, 1, 12000);
        ICE_WALL_COOLDOWN = builder.defineInRange("iceWallCooldownSeconds", 10.0D, 0.0D, 600.0D);
        ICE_SLIDE_TICKS = builder.defineInRange("iceSlideTicks", 100, 1, 12000);
        ICE_FORM_FIRE_MULTIPLIER = builder.defineInRange("iceFormFireDamageMultiplier", 1.5D, 1.0D, 10.0D);
        builder.pop();

        builder.push("chaosMagicWandaMaximoff");
        SCARLET_WITCH = new HeroStats(builder, 30.0D, 10.0D, 0.2D, 0.0D, 0.0D, 3.5D, 0,
                List.of("minecraft:generic.luck;3.0;ADD_VALUE", "apothic_attributes:dodge_chance;0.12;ADD_VALUE"));
        WANDA_FLY_SPEED = builder.defineInRange("flySpeed", 0.05D, 0.01D, 1.0D);
        HEX_BOLT_RANGE = builder.defineInRange("hexBoltRange", 32.0D, 1.0D, 128.0D);
        HEX_BOLT_DAMAGE = builder.defineInRange("hexBoltDamage", 27.0D, 0.0D, 10000.0D);
        HEX_BOLT_COOLDOWN = builder.defineInRange("hexBoltCooldownSeconds", 1.0D, 0.0D, 600.0D);
        HEX_EFFECTS = builder.comment("Hexes Hex Bolt can roll: jinxed, misfire, turned, pratfall, bad_luck, unravel, dazed.")
                .defineListAllowEmpty("hexEffects", List.of("jinxed", "misfire", "turned", "pratfall", "bad_luck", "unravel", "dazed"),
                        () -> "", entry -> entry instanceof String);
        TELEKINESIS_RANGE = builder.defineInRange("telekinesisRange", 12.0D, 1.0D, 64.0D);
        TELEKINESIS_COOLDOWN = builder.defineInRange("telekinesisCooldownSeconds", 0.0D, 0.0D, 600.0D);
        TELEKINESIS_THROW_SPEED = builder.defineInRange("telekinesisThrowSpeed", 2.0D, 0.0D, 10.0D);
        TELEKINESIS_THROW_DAMAGE = builder.defineInRange("telekinesisThrowDamage", 27.0D, 0.0D, 10000.0D);
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
        PIOTR = new HeroStats(builder, 10.0D, 4.0D, 0.3D, 0.0D, 0.0D, 21.0D, 0);
        builder.push("steelForm");
        COLOSSUS = new HeroStats(builder, 60.0D, 20.0D, 2.0D, 0.5D, 0.0D, 80.0D, 0,
                List.of("minecraft:generic.scale;0.2;ADD_MULTIPLIED_BASE", "minecraft:generic.armor_toughness;12.0;ADD_VALUE",
                        "minecraft:generic.knockback_resistance;1.0;ADD_VALUE", "minecraft:generic.movement_speed;-0.2;ADD_MULTIPLIED_TOTAL",
                        "minecraft:generic.safe_fall_distance;20.0;ADD_VALUE"));
        builder.pop();
        STEEL_FORM_COOLDOWN = builder.defineInRange("steelFormOffCooldownSeconds", 2.0D, 0.0D, 600.0D);
        RICOCHET_MULTIPLIER = builder.defineInRange("ricochetDamageMultiplier", 0.25D, 0.0D, 1.0D);
        STEEL_FIRE_MULTIPLIER = builder.defineInRange("steelFireDamageMultiplier", 0.5D, 0.0D, 10.0D);
        FASTBALL_SPEED = builder.defineInRange("fastballSpeed", 2.8D, 0.0D, 10.0D);
        FASTBALL_THROW_DAMAGE = builder.defineInRange("fastballThrowDamage", 75.0D, 0.0D, 10000.0D);
        FASTBALL_DAMAGE_MULTIPLIER = builder.defineInRange("fastballNextHitMultiplier", 2.0D, 1.0D, 100.0D);
        FASTBALL_COOLDOWN = builder.defineInRange("fastballCooldownSeconds", 6.0D, 0.0D, 600.0D);
        STEEL_CHARGE_COOLDOWN = builder.defineInRange("steelChargeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        STEEL_CHARGE_DAMAGE = builder.defineInRange("steelChargeDamage", 75.0D, 0.0D, 10000.0D);
        IRON_SLAM_DAMAGE = builder.defineInRange("ironSlamDamage", 131.0D, 0.0D, 10000.0D);
        IRON_SLAM_COOLDOWN = builder.defineInRange("ironSlamCooldownSeconds", 10.0D, 0.0D, 600.0D);
        BULWARK_REDUCTION = builder.defineInRange("bulwarkProjectileMultiplier", 0.5D, 0.0D, 1.0D);
        builder.pop();

        builder.push("radarSenseMattMurdock");
        DAREDEVIL = new HeroStats(builder, 20.0D, 8.0D, 0.3D, 0.0D, 0.0D, 31.0D, 1,
                List.of("apothic_attributes:dodge_chance;0.12;ADD_VALUE", "minecraft:generic.safe_fall_distance;8.0;ADD_VALUE"));
        DAREDEVIL_LEAP_MULTIPLIER = builder.defineInRange("crouchJumpMultiplier", 1.9D, 1.0D, 20.0D);
        DAREDEVIL_LEAP_HORIZONTAL = builder.defineInRange("sprintLeapHorizontalMultiplier", 1.4D, 1.0D, 10.0D);
        DAREDEVIL_FLIP_CHANCE = builder.defineInRange("sprintJumpFlipChance", 0.45D, 0.0D, 1.0D);
        RADAR_RANGE = builder.defineInRange("radarRange", 32.0D, 4.0D, 128.0D);
        RADAR_DARK_RANGE = builder.defineInRange("radarDarkRange", 48.0D, 4.0D, 128.0D);
        DANGER_SENSE_CHANCE = builder.defineInRange("dangerSenseDodgeChance", 0.2D, 0.0D, 1.0D);
        BILLY_CLUB_DAMAGE = builder.defineInRange("billyClubDamage", 27.0D, 0.0D, 10000.0D);
        BILLY_CLUB_BOUNCES = builder.defineInRange("billyClubHits", 3, 1, 16);
        BILLY_CLUB_COOLDOWN = builder.defineInRange("billyClubCooldownSeconds", 3.0D, 0.0D, 600.0D);
        GRAPPLE_RANGE = builder.defineInRange("grappleRange", 32.0D, 4.0D, 128.0D);
        GRAPPLE_COOLDOWN = builder.defineInRange("grappleCooldownSeconds", 4.0D, 0.0D, 600.0D);
        TAKEDOWN_DAMAGE = builder.defineInRange("takedownDamage", 46.0D, 0.0D, 10000.0D);
        TAKEDOWN_STUN_TICKS = builder.defineInRange("takedownStunTicks", 40, 0, 1200);
        TAKEDOWN_COOLDOWN = builder.defineInRange("takedownCooldownSeconds", 10.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("phasingKittyPryde");
        KITTY = new HeroStats(builder, 16.0D, 4.0D, 0.0D, 0.0D, 0.0D, 4.2D, 0);
        PHASE_METER_SECONDS = builder.defineInRange("phaseMeterSeconds", 20.0D, 1.0D, 3600.0D);
        PHASE_STRIKE_DAMAGE = builder.defineInRange("phaseStrikeDamage", 20.0D, 0.0D, 10000.0D);
        PHASE_STRIKE_COOLDOWN = builder.defineInRange("phaseStrikeCooldownSeconds", 5.0D, 0.0D, 600.0D);
        PHASE_SINK_TICKS = builder.defineInRange("phaseSinkTicks", 60, 1, 1200);
        PHASE_SINK_COOLDOWN = builder.defineInRange("phaseSinkCooldownSeconds", 12.0D, 0.0D, 600.0D);
        PHASE_OTHERS_TICKS = builder.defineInRange("phaseOthersTicks", 160, 1, 6000);
        PHASE_OTHERS_COOLDOWN = builder.defineInRange("phaseOthersCooldownSeconds", 20.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("squirrelPowersDoreenGreen");
        SQUIRREL_GIRL = new HeroStats(builder, 14.0D, 4.0D, 0.2D, 0.5D, 0.0D, 21.0D, 0,
                List.of("apothic_attributes:dodge_chance;0.08;ADD_VALUE", "minecraft:generic.safe_fall_distance;10.0;ADD_VALUE"));
        SWARM_SIZE = builder.defineInRange("swarmSize", 8, 1, 32);
        SWARM_RESPAWN_SECONDS = builder.defineInRange("swarmRespawnSeconds", 4.0D, 0.0D, 600.0D);
        SWARM_SQUIRREL_HEALTH = builder.defineInRange("swarmSquirrelHealth", 30.0D, 1.0D, 10000.0D);
        SWARM_BITE_DAMAGE = builder.defineInRange("swarmBiteDamage", 3.0D, 0.0D, 10000.0D);
        SWARM_CALL_COOLDOWN = builder.defineInRange("callSwarmCooldownSeconds", 2.0D, 0.0D, 600.0D);
        SWARM_FORAGE_RANGE = builder.defineInRange("forageRange", 16.0D, 2.0D, 64.0D);
        SIC_EM_RANGE = builder.defineInRange("sicEmRange", 32.0D, 4.0D, 128.0D);
        SIC_EM_COOLDOWN = builder.defineInRange("sicEmCooldownSeconds", 1.0D, 0.0D, 600.0D);
        PILE_TICKS = builder.defineInRange("squirrelPileTicks", 60, 1, 1200);
        PILE_BITE_DAMAGE = builder.defineInRange("squirrelPileBiteDamage", 2.6D, 0.0D, 10000.0D);
        PILE_COOLDOWN = builder.defineInRange("squirrelPileCooldownSeconds", 20.0D, 0.0D, 600.0D);
        GNAW_TICKS_PER_HARDNESS = builder.defineInRange("gnawTicksPerHardness", 60.0D, 1.0D, 10000.0D);
        GNAW_MAX_BLOCKS = builder.defineInRange("gnawMaxBlocks", 64, 1, 4096);
        GNAW_DROPS = builder.define("gnawDropsItems", true);
        TIPPY_BITE_DAMAGE = builder.defineInRange("tippyToeBiteDamage", 6.4D, 0.0D, 10000.0D);
        TIPPY_RESPAWN_SECONDS = builder.defineInRange("tippyToeRespawnSeconds", 60, 1, 3600);
        builder.pop();

        builder.push("flameOnJohnnyStorm");
        TORCH = new HeroStats(builder, 28.0D, 10.0D, 0.3D, 0.0D, 0.0D, 5.6D, 0);
        TORCH_FLY_SPEED = builder.defineInRange("flySpeed", 0.09D, 0.01D, 1.0D);
        FLAME_OUT_COOLDOWN = builder.defineInRange("flameOutCooldownSeconds", 5.0D, 0.0D, 600.0D);
        FIREBALL_DAMAGE = builder.defineInRange("fireballDamage", 30.0D, 0.0D, 10000.0D);
        FIREBALL_COOLDOWN = builder.defineInRange("fireballCooldownSeconds", 0.4D, 0.0D, 600.0D);
        FLAME_CAGE_RANGE = builder.defineInRange("flameCageRange", 24.0D, 2.0D, 64.0D);
        FLAME_CAGE_TICKS = builder.defineInRange("flameCageTicks", 80, 1, 1200);
        FLAME_CAGE_DAMAGE = builder.defineInRange("flameCageDamagePerSecond", 17.0D, 0.0D, 10000.0D);
        FLAME_CAGE_COOLDOWN = builder.defineInRange("flameCageCooldownSeconds", 12.0D, 0.0D, 600.0D);
        NOVA_RADIUS = builder.defineInRange("novaFlareRadius", 8.0D, 1.0D, 64.0D);
        NOVA_DAMAGE = builder.defineInRange("novaFlareDamage", 74.0D, 0.0D, 10000.0D);
        NOVA_COOLDOWN = builder.defineInRange("novaFlareCooldownSeconds", 30.0D, 0.0D, 600.0D);
        NOVA_BURNOUT_SECONDS = builder.defineInRange("novaFlareBurnoutSeconds", 10.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("invisibilitySueStorm");
        SUE = new HeroStats(builder, 28.0D, 10.0D, 0.2D, 0.0D, 0.0D, 3.5D, 0);
        VANISH_COOLDOWN = builder.defineInRange("vanishCooldownSeconds", 3.0D, 0.0D, 600.0D);
        FORCE_FIELD_TICKS = builder.defineInRange("forceFieldTicks", 200, 1, 6000);
        FORCE_FIELD_COOLDOWN = builder.defineInRange("forceFieldCooldownSeconds", 12.0D, 0.0D, 600.0D);
        BUBBLE_SECONDS = builder.defineInRange("forceBubbleSeconds", 10.0D, 0.5D, 600.0D);
        BUBBLE_COOLDOWN = builder.defineInRange("forceBubbleCooldownSeconds", 15.0D, 0.0D, 600.0D);
        BUBBLE_RADIUS = builder.defineInRange("forceBubbleRadius", 3.5D, 1.0D, 16.0D);
        BUBBLE_DAMAGE_MULTIPLIER = builder.defineInRange("forceBubbleDamageTakenMultiplier", 0.25D, 0.0D, 1.0D);
        FORCE_BLAST_RANGE = builder.defineInRange("forceBlastRange", 12.0D, 2.0D, 64.0D);
        FORCE_BLAST_DAMAGE = builder.defineInRange("forceBlastDamage", 27.0D, 0.0D, 10000.0D);
        FORCE_BLAST_COOLDOWN = builder.defineInRange("forceBlastCooldownSeconds", 5.0D, 0.0D, 600.0D);
        builder.pop();

        builder.push("elasticityReedRichards");
        REED = new HeroStats(builder, 26.0D, 10.0D, 0.3D, 0.5D, 0.0D, 35.0D, 0,
                List.of("minecraft:player.entity_interaction_range;3.0;ADD_VALUE", "minecraft:player.block_interaction_range;3.0;ADD_VALUE"));
        STRETCH_PUNCH_RANGE = builder.defineInRange("stretchPunchRange", 10.0D, 2.0D, 64.0D);
        STRETCH_PUNCH_DAMAGE = builder.defineInRange("stretchPunchDamage", 50.0D, 0.0D, 10000.0D);
        STRETCH_PUNCH_COOLDOWN = builder.defineInRange("stretchPunchCooldownSeconds", 1.5D, 0.0D, 600.0D);
        STRETCH_HAMMER_RANGE = builder.defineInRange("stretchHammerRange", 6.0D, 2.0D, 32.0D);
        STRETCH_HAMMER_RADIUS = builder.defineInRange("stretchHammerRadius", 3.0D, 0.5D, 16.0D);
        STRETCH_HAMMER_DAMAGE = builder.defineInRange("stretchHammerDamage", 66.0D, 0.0D, 10000.0D);
        STRETCH_HAMMER_COOLDOWN = builder.defineInRange("stretchHammerCooldownSeconds", 10.0D, 0.0D, 600.0D);
        STRETCH_SWEEP_RADIUS = builder.defineInRange("stretchSweepRadius", 6.0D, 1.0D, 32.0D);
        STRETCH_SWEEP_DAMAGE = builder.defineInRange("stretchSweepDamage", 38.0D, 0.0D, 10000.0D);
        STRETCH_SWEEP_COOLDOWN = builder.defineInRange("stretchSweepCooldownSeconds", 8.0D, 0.0D, 600.0D);
        BOUNCE_MAX_SPEED = builder.defineInRange("bounceMaxSpeed", 1.4D, 0.0D, 10.0D);
        SHAPE_COOLDOWN = builder.defineInRange("shapeShiftCooldownSeconds", 1.0D, 0.0D, 600.0D);
        SLINGSHOT_COOLDOWN = builder.defineInRange("slingshotCooldownSeconds", 3.0D, 0.0D, 600.0D);
        SLINGSHOT_SPEED = builder.defineInRange("slingshotSpeed", 2.6D, 0.1D, 10.0D);
        BALL_SECONDS = builder.defineInRange("bouncyBallSeconds", 8.0D, 0.5D, 600.0D);
        BALL_DAMAGE = builder.defineInRange("bouncyBallDamage", 16.0D, 0.0D, 10000.0D);
        POGO_BOUNCE = builder.defineInRange("pogoBounce", 0.75D, 0.1D, 5.0D);
        POGO_BOOST_BOUNCE = builder.defineInRange("pogoBoostBounce", 1.25D, 0.1D, 5.0D);
        POGO_STOMP_DAMAGE = builder.defineInRange("pogoStompDamage", 25.0D, 0.0D, 10000.0D);
        BRIDGE_LENGTH = builder.defineInRange("bridgeLength", 8, 2, 32);
        BRIDGE_SECONDS = builder.defineInRange("bridgeSeconds", 30.0D, 1.0D, 600.0D);
        STILTS_STEP_HEIGHT = builder.defineInRange("stiltsStepHeight", 1.5D, 0.0D, 5.0D);
        STILTS_SPEED_BONUS = builder.defineInRange("stiltsSpeedBonus", 0.25D, 0.0D, 5.0D);
        BOAT_SPEED = builder.defineInRange("boatSpeed", 0.4D, 0.05D, 2.0D);
        builder.pop();

        builder.push("rockHideBenGrimm");
        THING = new HeroStats(builder, 60.0D, 20.0D, 2.0D, 0.5D, 0.0D, 80.0D, 1,
                List.of("minecraft:generic.knockback_resistance;1.0;ADD_VALUE", "minecraft:generic.max_absorption;40.0;ADD_VALUE"));
        ROCK_PLATING_MAX = builder.defineInRange("rockPlatingMax", 40.0D, 0.0D, 10000.0D);
        ROCK_PLATING_REGEN = builder.defineInRange("rockPlatingRegenPerSecond", 2.0D, 0.0D, 1000.0D);
        ROCK_PLATING_DELAY_SECONDS = builder.defineInRange("rockPlatingRegenDelaySeconds", 5, 0, 600);
        COMBO_MULTIPLIER = builder.defineInRange("thirdPunchDamageMultiplier", 2.0D, 1.0D, 100.0D);
        CLOBBER_DAMAGE = builder.defineInRange("clobberDamage", 188.0D, 0.0D, 10000.0D);
        CRATER_DAMAGE = builder.defineInRange("landingCraterDamage", 38.0D, 0.0D, 10000.0D);
        CLOBBER_COOLDOWN = builder.defineInRange("clobberCooldownSeconds", 8.0D, 0.0D, 600.0D);
        WRESTLE_DAMAGE = builder.defineInRange("wrestleDamage", 150.0D, 0.0D, 10000.0D);
        WRESTLE_COOLDOWN = builder.defineInRange("wrestleCooldownSeconds", 10.0D, 0.0D, 600.0D);
        TOSS_MAX_BLOCKS = builder.defineInRange("ripAndTossMaxBlocks", 4, 1, 64);
        TOSS_THROW_SPEED = builder.defineInRange("ripAndTossThrowSpeed", 1.6D, 0.1D, 10.0D);
        TOSS_DAMAGE = builder.defineInRange("ripAndTossDamage", 112.0D, 0.0D, 10000.0D);
        TOSS_COOLDOWN = builder.defineInRange("ripAndTossCooldownSeconds", 6.0D, 0.0D, 600.0D);
        BRACE_DAMAGE_MULTIPLIER = builder.defineInRange("braceDamageTakenMultiplier", 0.3D, 0.0D, 1.0D);
        THING_LEAP_HORIZONTAL = builder.defineInRange("superJumpHorizontal", 1.6D, 0.0D, 10.0D);
        THING_LEAP_VERTICAL = builder.defineInRange("superJumpVertical", 1.5D, 0.0D, 10.0D);
        THING_LEAP_STANDING_VERTICAL = builder.defineInRange("superJumpStandingVertical", 2.3D, 0.0D, 10.0D);
        THING_LEAP_COOLDOWN_TICKS = builder.defineInRange("superJumpCooldownTicks", 20, 0, 1200);
        builder.pop();

        builder.push("kineticChargeRemyLebeau");
        GAMBIT = new HeroStats(builder, 22.0D, 8.0D, 0.3D, 0.0D, 0.02D, 14.0D, 0,
                List.of("minecraft:generic.safe_fall_distance;6.0;ADD_VALUE"));
        KINETIC_DAMAGE = builder.comment("Damage at the center of a kinetic burst from a normal thrown item.")
                .defineInRange("kineticBurstDamage", 24.0D, 0.0D, 10000.0D);
        KINETIC_RADIUS = builder.defineInRange("kineticBurstRadius", 2.5D, 0.5D, 16.0D);
        KINETIC_IMPACT_DAMAGE = builder.defineInRange("kineticImpactDamage", 8.0D, 0.0D, 10000.0D);
        KINETIC_WEAPON_MULTIPLIER = builder.comment("Thrown weapons add their attack damage times this to the impact.")
                .defineInRange("thrownWeaponDamageMultiplier", 1.5D, 0.0D, 100.0D);
        KINETIC_THROW_SPEED = builder.defineInRange("throwSpeed", 2.0D, 0.1D, 10.0D);
        KINETIC_THROW_COOLDOWN = builder.defineInRange("kineticThrowCooldownSeconds", 0.6D, 0.0D, 600.0D);
        PHANTOM_CARD_MULTIPLIER = builder.comment("Damage and radius multiplier for the free card thrown with an empty hand.")
                .defineInRange("phantomCardMultiplier", 0.6D, 0.0D, 10.0D);
        CARD_RADIUS_MULTIPLIER = builder.defineInRange("cardRadiusMultiplier", 0.7D, 0.0D, 10.0D);
        HEAVY_MULTIPLIER = builder.defineInRange("heavyItemMultiplier", 1.5D, 0.0D, 10.0D);
        EXPLOSIVE_MULTIPLIER = builder.defineInRange("explosiveItemMultiplier", 1.8D, 0.0D, 10.0D);
        KINETIC_BLOCK_DAMAGE = builder.comment("Kinetic bursts break blocks. The superpower griefing game rule must also be on.")
                .define("kineticBlockDamage", true);
        RETURN_DURABILITY_COST = builder.comment("Durability a returning item loses each time it is thrown.")
                .defineInRange("returnDurabilityCost", 4, 0, 1000);
        CARD_FAN_COUNT = builder.defineInRange("cardFanCount", 5, 1, 16);
        CARD_FAN_SPREAD = builder.defineInRange("cardFanSpreadDegrees", 30.0D, 0.0D, 180.0D);
        CARD_FAN_COOLDOWN = builder.defineInRange("cardFanCooldownSeconds", 4.0D, 0.0D, 600.0D);
        CHARGE_TOUCH_RANGE = builder.defineInRange("chargeTouchRange", 6.0D, 1.0D, 32.0D);
        CHARGE_TOUCH_FUSE_TICKS = builder.defineInRange("chargeTouchFuseTicks", 50, 1, 1200);
        CHARGE_TOUCH_DAMAGE = builder.defineInRange("chargeTouchDamage", 40.0D, 0.0D, 10000.0D);
        CHARGE_TOUCH_RADIUS = builder.defineInRange("chargeTouchRadius", 3.5D, 0.5D, 16.0D);
        CHARGE_TOUCH_COOLDOWN = builder.defineInRange("chargeTouchCooldownSeconds", 6.0D, 0.0D, 600.0D);
        VAULT_HORIZONTAL = builder.defineInRange("boVaultHorizontal", 1.4D, 0.0D, 10.0D);
        VAULT_VERTICAL = builder.defineInRange("boVaultVertical", 0.9D, 0.0D, 10.0D);
        VAULT_STAFF_MULTIPLIER = builder.comment("Vault boost while holding an item in the gambit/staffs tag.")
                .defineInRange("boVaultStaffMultiplier", 1.4D, 1.0D, 10.0D);
        VAULT_SLAM_DAMAGE = builder.defineInRange("boVaultStaffSlamDamage", 20.0D, 0.0D, 10000.0D);
        VAULT_COOLDOWN = builder.defineInRange("boVaultCooldownSeconds", 5.0D, 0.0D, 600.0D);
        FULL_HOUSE_RANGE = builder.defineInRange("fullHouseRange", 32.0D, 4.0D, 128.0D);
        FULL_HOUSE_TARGETS = builder.defineInRange("fullHouseTargets", 5, 1, 16);
        FULL_HOUSE_DAMAGE = builder.defineInRange("fullHouseDamage", 36.0D, 0.0D, 10000.0D);
        FULL_HOUSE_COOLDOWN = builder.defineInRange("fullHouseCooldownSeconds", 20.0D, 0.0D, 600.0D);
        CHARGED_AMMO_RADIUS = builder.defineInRange("chargedAmmoRadius", 2.0D, 0.5D, 16.0D);
        CHARGED_AMMO_DAMAGE = builder.defineInRange("chargedAmmoDamage", 14.0D, 0.0D, 10000.0D);
        CHARGED_AMMO_EXHAUSTION = builder.comment("Hunger exhaustion per charged projectile.")
                .defineInRange("chargedAmmoExhaustion", 0.6D, 0.0D, 40.0D);
        CHARGE_GUN_PROJECTILES = builder.comment("Charged Ammo also charges Scorched Guns bullets.")
                .define("chargeGunProjectiles", false);
        CAJUN_CHARM = builder.comment("Gambit has Hero of the Village while injected.")
                .define("cajunCharm", true);
        builder.pop();

        builder.push("absorptionAnnaMarie");
        ROGUE = new HeroStats(builder, 40.0D, 14.0D, 0.8D, 0.0D, 0.0D, 28.0D, 1);
        MEMORY_SLOTS = builder.defineInRange("memorySlots", 3, 1, 3);
        MEMORY_SECONDS = builder.defineInRange("memorySeconds", 180.0D, 1.0D, 36000.0D);
        BOSS_MEMORY_SECONDS = builder.defineInRange("bossMemorySeconds", 60.0D, 1.0D, 36000.0D);
        MEMORY_DAMAGE_MULTIPLIER = builder.comment("Scales the damage of every absorbed ability.")
                .defineInRange("memoryDamageMultiplier", 1.0D, 0.0D, 100.0D);
        MEMORY_COOLDOWN_MULTIPLIER = builder.comment("Scales the cooldown of every absorbed ability.")
                .defineInRange("memoryCooldownMultiplier", 1.0D, 0.0D, 100.0D);
        DRAIN_DAMAGE = builder.defineInRange("drainDamage", 8.0D, 0.0D, 10000.0D);
        DRAIN_HEAL = builder.defineInRange("drainHeal", 4.0D, 0.0D, 10000.0D);
        DRAIN_WEAKEN_TICKS = builder.defineInRange("drainWeakenTicks", 100, 0, 6000);
        INVOLUNTARY_DRAIN_DAMAGE = builder.comment("Damage dealt to mobs that hit Rogue in melee while her gloves are off.")
                .defineInRange("involuntaryDrainDamage", 3.0D, 0.0D, 10000.0D);
        SIPHON_RANGE = builder.defineInRange("siphonTouchRange", 4.0D, 1.0D, 16.0D);
        SIPHON_DAMAGE = builder.defineInRange("siphonTouchDamage", 16.0D, 0.0D, 10000.0D);
        SIPHON_COOLDOWN = builder.defineInRange("siphonTouchCooldownSeconds", 8.0D, 0.0D, 600.0D);
        ABSORB_PLAYERS = builder.define("absorbPlayers", true);
        BORROW_SECONDS = builder.comment("How long Rogue keeps powers borrowed from another player.")
                .defineInRange("borrowSeconds", 60.0D, 1.0D, 36000.0D);
        VICTIM_SHUTDOWN_SECONDS = builder.comment("How long an absorbed player's abilities stay on cooldown.")
                .defineInRange("victimShutdownSeconds", 15.0D, 0.0D, 600.0D);
        PETRIFY_MOBS = builder.comment("Gorgon and Cockatrice memories turn non-boss mobs to stone (needs Ice and Fire).")
                .define("petrifyMobs", true);
        builder.pop();

        builder.push("pyrotechnicsJubilationLee");
        JUBILEE = new HeroStats(builder, 18.0D, 6.0D, 0.2D, 0.0D, 0.02D, 7.0D, 0,
                List.of("minecraft:generic.safe_fall_distance;4.0;ADD_VALUE"));
        PAFF_DAMAGE = builder.defineInRange("paffDamage", 14.0D, 0.0D, 10000.0D);
        PAFF_SPEED = builder.defineInRange("paffSpeed", 2.2D, 0.1D, 10.0D);
        PAFF_COOLDOWN = builder.defineInRange("paffCooldownSeconds", 0.5D, 0.0D, 600.0D);
        VOLLEY_ROCKETS = builder.defineInRange("fireworkVolleyRockets", 5, 1, 32);
        VOLLEY_DAMAGE = builder.defineInRange("fireworkVolleyDamage", 18.0D, 0.0D, 10000.0D);
        VOLLEY_RANGE = builder.defineInRange("fireworkVolleyRange", 32.0D, 4.0D, 128.0D);
        VOLLEY_COOLDOWN = builder.defineInRange("fireworkVolleyCooldownSeconds", 6.0D, 0.0D, 600.0D);
        FLASHBANG_RADIUS = builder.defineInRange("flashbangRadius", 8.0D, 1.0D, 64.0D);
        FLASHBANG_TICKS = builder.defineInRange("flashbangTicks", 80, 1, 1200);
        FLASHBANG_DAMAGE = builder.defineInRange("flashbangDamage", 6.0D, 0.0D, 10000.0D);
        FLASHBANG_COOLDOWN = builder.defineInRange("flashbangCooldownSeconds", 12.0D, 0.0D, 600.0D);
        PYRO_JUMP_VERTICAL = builder.defineInRange("pyroJumpVertical", 1.2D, 0.0D, 10.0D);
        PYRO_JUMP_HORIZONTAL = builder.defineInRange("pyroJumpHorizontal", 1.3D, 0.0D, 10.0D);
        PYRO_JUMP_ELYTRA_FLIGHT = builder.comment("Flight duration of the rocket Pyro Jump fires while gliding.")
                .defineInRange("pyroJumpElytraFlight", 3, 1, 10);
        PYRO_JUMP_COOLDOWN = builder.defineInRange("pyroJumpCooldownSeconds", 4.0D, 0.0D, 600.0D);
        FINALE_ROCKETS = builder.defineInRange("grandFinaleRockets", 16, 1, 128);
        FINALE_RADIUS = builder.defineInRange("grandFinaleRadius", 10.0D, 2.0D, 64.0D);
        FINALE_DAMAGE = builder.defineInRange("grandFinaleDamage", 30.0D, 0.0D, 10000.0D);
        FINALE_TICKS = builder.defineInRange("grandFinaleTicks", 80, 1, 1200);
        FINALE_COOLDOWN = builder.defineInRange("grandFinaleCooldownSeconds", 60.0D, 0.0D, 3600.0D);
        ELYTRA_BOOST_MULTIPLIER = builder.comment("Firework rockets Jubilee boosts an elytra with last this much longer.")
                .defineInRange("elytraBoostMultiplier", 1.5D, 1.0D, 10.0D);
        LIGHT_SHOW_LEVEL = builder.defineInRange("lightShowLightLevel", 12, 1, 15);
        builder.pop();

        builder.comment("Only used when the Viltrumite mod is installed.").push("viltrumite");
        VILTRUMITE = new HeroStats(builder, 50.0D, 16.0D, 2.0D, 0.5D, 0.0D, 0.0D, 1,
                List.of("minecraft:generic.knockback_resistance;0.8;ADD_VALUE", "minecraft:generic.safe_fall_distance;100.0;ADD_VALUE"));
        VILTRUMITE_BASE_DAMAGE = builder.comment("Viltrumite base damage. Their abilities deal a multiple of it (Sonic Punch 200-500%, Sky Breaker 300-800%).")
                .defineInRange("baseDamage", 45.0D, 0.0D, 10000.0D);
        VILTRUMITE_DAMAGE_REDUCTION = builder.comment("Percent of incoming damage ignored. The Viltrumite mod defaults to 97.")
                .defineInRange("damageReductionPercent", 55.0D, 0.0D, 100.0D);
        VILTRUMITE_DAMAGE_IGNORE = builder.comment("Hits that deal less than this after reduction are ignored completely.")
                .defineInRange("damageIgnoreThreshold", 2.0D, 0.0D, 10000.0D);
        VILTRUMITE_HEAL_FACTOR = builder.comment("Health healed every 2 seconds while hurt.")
                .defineInRange("healFactor", 2.0D, 0.0D, 10000.0D);
        VILTRUMITE_MAX_FLIGHT_SPEED = builder.defineInRange("maxFlightSpeed", 6.0D, 0.5D, 50.0D);
        VILTRUMITE_THROTTLE = builder.defineInRange("flightThrottleAcceleration", 0.012D, 0.001D, 1.0D);
        VILTRUMITE_SYRINGE_ONLY = builder.comment("Turns off the Viltrumite race prompt and default race so the serum is the only way to become a Viltrumite.")
                .define("serumOnly", true);
        VILTRUMITE_TAKEOFF_BREAKS_BLOCKS = builder.define("takeoffBreaksBlocks", true);
        VILTRUMITE_HUMANS_BLEED = builder.comment("Lets players who aren't Viltrumites spill human blood when hurt. Viltrumites always bleed Viltrumite blood.")
                .define("humansBleed", false);
        VILTRUMITE_NPC_MAX_REDUCTION = builder.comment("Caps the damage reduction of Viltrumite NPCs like Conquest (98.5 by default in their mod) so heroes can hurt them.")
                .defineInRange("npcMaxDamageReductionPercent", 85.0D, 0.0D, 100.0D);
        VILTRUMITE_NPC_BLOOD_DROPS = builder.defineInRange("npcBloodSampleDrops", 3, 0, 64);
        VILTRUMITE_NPC_DAMAGE_MULTIPLIER = builder.comment("Scales the base damage of Viltrumite NPCs. Conquest has 32 by default in their mod and his abilities deal up to 8 times that.")
                .defineInRange("npcBaseDamageMultiplier", 0.35D, 0.0D, 10.0D);
        VILTRUMITE_NPC_PER_HIT_CAP = builder.comment("Most damage one hit from a Viltrumite NPC can deal, like the per-hit cap on Hydra villains.")
                .defineInRange("npcPerHitCap", 35.0D, 1.0D, 100000.0D);
        VILTRUMITE_NPC_SQUAD_HEALTH = builder.comment("Max health of Lucan, Vidor and Thula. Their mod leaves them at 20 like a normal player. 0 keeps that.")
                .defineInRange("npcSquadHealth", 300.0D, 0.0D, 100000.0D);
        VILTRUMITE_NPC_CONQUEST_HEALTH = builder.comment("Max health of Conquest. 0 keeps the default 20.")
                .defineInRange("npcConquestHealth", 600.0D, 0.0D, 100000.0D);
        VILTRUMITE_NPC_ONLY_PLAYER_DAMAGE = builder.comment("Viltrumite NPCs only take damage from players, their projectiles and their pets. Mobs, falls, fire and drowning can't hurt them.")
                .define("npcOnlyPlayerDamage", true);
        VILTRUMITE_NPC_PICKUP = builder.comment("Lets Viltrumite NPCs pick up dropped items. Anything they pick up is gone when they leave.")
                .define("npcItemPickup", false);
        VILTRUMITE_NPC_BREAK_BLOCK_ENTITIES = builder.comment("Lets Viltrumite NPC attacks break blocks with block entities, like chests, graves and machines.")
                .define("npcBreakBlockEntities", false);
        VILTRUMITE_INVASION_NEEDS_DRAGON = builder.comment("Random Viltrumite invasions only start once the Ender Dragon has been killed. Rituals and commands still work.")
                .define("randomInvasionsNeedDragon", true);
        VILTRUMITE_INVASION_ADVANCEMENT = builder.comment("Random Viltrumite invasions only target players with this advancement. Leave empty for any player.")
                .define("randomInvasionsNeedAdvancement", "");
        VILTRUMITE_SUIT = builder.comment("Gives serum Viltrumites a Viltrumite suit skin and cape while the power is injected.")
                .define("suitCosmetics", true);
        VILTRUMITE_SUIT_SKIN = builder.define("suitSkin", "skin_omniman_1");
        VILTRUMITE_SUIT_CAPE = builder.define("suitCape", "red");
        VILTRUMITE_ANIMATIONS = builder.comment("Plays Viltrumite arm and body animations for Hulk's Thunderclap, Thing's Clobberin' Time and Black Panther's Claw Swipe. Purely cosmetic.")
                .define("heroAnimations", true);
        VILTRUMITE_CAPTAIN_MARVEL_FLIGHT = builder.comment("Lets Captain Marvel double-tap jump into Viltrumite flight, with its speed and block-breaking takeoff. Other flying heroes never can.")
                .define("captainMarvelViltrumiteFlight", true);
        builder.push("rogue");
        VILTRUMITE_ROGUE_BASE_DAMAGE = builder.defineInRange("borrowedBaseDamage", 25.0D, 0.0D, 10000.0D);
        VILTRUMITE_ROGUE_DAMAGE_REDUCTION = builder.defineInRange("borrowedDamageReductionPercent", 35.0D, 0.0D, 100.0D);
        VILTRUMITE_ROGUE_DASH_DAMAGE = builder.defineInRange("viltrumiteRushDamage", 60.0D, 0.0D, 10000.0D);
        VILTRUMITE_ROGUE_DASH_COOLDOWN = builder.defineInRange("viltrumiteRushCooldownSeconds", 6.0D, 0.0D, 600.0D);
        builder.pop();
        builder.comment("Keeps the Viltrumite mod from interfering with other mods.").push("safeguards");
        VILTRUMITE_GUARD_FLIGHT = builder.comment("Only takes away flight the Viltrumite mod gave. Without this, their mod removes flight from every player who isn't a Viltrumite, every tick, breaking flight from other mods. Grabbed players get their own flight back when released.")
                .define("protectOtherFlight", true);
        VILTRUMITE_GUARD_TELEPORTS = builder.comment("Stops the Viltrumite mod from cancelling teleports from commands and other mods during Super Speed and dashes, and limits its anti-cheat bypass to movement checks.")
                .define("protectTeleports", true);
        VILTRUMITE_GUARD_DAMAGE = builder.comment("Damage tagged to bypass invulnerability or resistance ignores Viltrumite damage reduction and blocking.")
                .define("respectBypassDamage", true);
        VILTRUMITE_GUARD_EFFECTS = builder.comment("Effects in the integrated_superheroes:viltrumite_bypass tag can still be given to Viltrumites, who otherwise reject every harmful effect.")
                .define("allowBypassEffects", true);
        VILTRUMITE_GUARD_VISUALS = builder.comment("Limits Viltrumite pose, hand, FOV and screen shader changes to Viltrumites and fliers so they don't override other mods' animations and effects.")
                .define("limitVisuals", true);
        VILTRUMITE_GUARD_PERFORMANCE = builder.comment("Runs Viltrumite punch, chop and Thunderclap physics once per tick instead of once per player, skips their grab checks for entities nobody is holding, and sends grab hand positions once per tick instead of every frame.")
                .define("performanceFixes", true);
        builder.pop();
        builder.pop();

        builder.push("xGenes");
        X_GENES_HYDRA_BASE_CHANCE = builder.comment("Chance a Hydra base chest holds X-Genes.")
                .defineInRange("hydraBaseChestChance", 0.25D, 0.0D, 1.0D);
        X_GENES_HYDRA_OUTPOST_CHANCE = builder.comment("Chance a Hydra outpost chest holds X-Genes.")
                .defineInRange("hydraOutpostChestChance", 0.1D, 0.0D, 1.0D);
        X_GENES_WITCH_CHANCE = builder.comment("Chance a witch killed by a player drops X-Genes.")
                .defineInRange("witchDropChance", 0.04D, 0.0D, 1.0D);
        builder.pop();

        IronManSuits.define(builder);

        SPEC = builder.build();
    }

    private MarvelBalanceConfig() {
    }

    public static final class HeroStats {
        public final ModConfigSpec.DoubleValue health;
        public final ModConfigSpec.DoubleValue armor;
        public final ModConfigSpec.DoubleValue attackKnockback;
        public final ModConfigSpec.DoubleValue stepHeight;
        public final ModConfigSpec.DoubleValue movementSpeed;
        public final ModConfigSpec.DoubleValue unarmedDamage;
        public final ModConfigSpec.IntValue strength;
        public final ModConfigSpec.ConfigValue<List<? extends String>> extraAttributes;

        HeroStats(ModConfigSpec.Builder builder, double health, double armor, double attackKnockback,
                  double stepHeight, double movementSpeed, double unarmedDamage, int strength) {
            this(builder, health, armor, attackKnockback, stepHeight, movementSpeed, unarmedDamage, strength, List.of());
        }

        HeroStats(ModConfigSpec.Builder builder, double health, double armor, double attackKnockback,
                  double stepHeight, double movementSpeed, double unarmedDamage, int strength, List<String> extraAttributes) {
            this.health = builder.defineInRange("bonusHealth", health, 0.0D, 10000.0D);
            this.armor = builder.defineInRange("bonusArmor", armor, 0.0D, 1000.0D);
            this.attackKnockback = builder.defineInRange("bonusAttackKnockback", attackKnockback, 0.0D, 100.0D);
            this.stepHeight = builder.defineInRange("bonusStepHeight", stepHeight, 0.0D, 10.0D);
            this.movementSpeed = builder.defineInRange("bonusMovementSpeed", movementSpeed, 0.0D, 1.0D);
            this.unarmedDamage = builder.defineInRange("bonusUnarmedDamage", unarmedDamage, 0.0D, 10000.0D);
            this.strength = builder.defineInRange("strengthLevel", strength, 0, 255);
            this.extraAttributes = builder
                    .comment("Extra permanent attribute modifiers while this power is injected.",
                            "Format: \"namespace:attribute;amount;OPERATION\", OPERATION = ADD_VALUE, ADD_MULTIPLIED_BASE or ADD_MULTIPLIED_TOTAL.")
                    .defineListAllowEmpty("extraAttributes", extraAttributes, () -> "", entry -> entry instanceof String);
            ALL_HERO_STATS.add(this);
        }
    }
}
