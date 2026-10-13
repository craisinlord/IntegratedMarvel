package com.craisinlord.integrated_superheroes.suit;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class PymConfig {
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.DoubleValue COMMAND_RANGE;
    public static ModConfigSpec.DoubleValue LEASH_RANGE;

    public static ModConfigSpec.IntValue ANT_SUMMON_CAP;
    public static ModConfigSpec.IntValue ANT_RECRUIT_CAP;
    public static ModConfigSpec.IntValue BROWN_WEIGHT;
    public static ModConfigSpec.IntValue RED_WEIGHT;
    public static ModConfigSpec.IntValue TERMITE_WEIGHT;
    public static ModConfigSpec.DoubleValue ANT_HEALTH;
    public static ModConfigSpec.DoubleValue ANT_DAMAGE;
    public static ModConfigSpec.IntValue RED_ANT_FIRE_SECONDS;

    public static ModConfigSpec.IntValue WASP_SUMMON_CAP;
    public static ModConfigSpec.IntValue WASP_RECRUIT_CAP;
    public static ModConfigSpec.DoubleValue WASP_HEALTH;
    public static ModConfigSpec.DoubleValue WASP_DAMAGE;

    public static ModConfigSpec.DoubleValue SUMMON_COOLDOWN;
    public static ModConfigSpec.DoubleValue RESPAWN_SECONDS;
    public static ModConfigSpec.DoubleValue RECRUIT_RADIUS;
    public static ModConfigSpec.DoubleValue RECRUIT_COOLDOWN;
    public static ModConfigSpec.DoubleValue SIC_EM_COOLDOWN;

    public static ModConfigSpec.DoubleValue OVERRUN_COOLDOWN;
    public static ModConfigSpec.IntValue OVERRUN_TICKS;
    public static ModConfigSpec.DoubleValue OVERRUN_BITE_DAMAGE;

    public static ModConfigSpec.DoubleValue STING_STORM_COOLDOWN;
    public static ModConfigSpec.DoubleValue STING_STORM_DAMAGE;
    public static ModConfigSpec.IntValue STING_STORM_STAGGER_TICKS;

    public static ModConfigSpec.DoubleValue FORAGE_RANGE;
    public static ModConfigSpec.DoubleValue CHEW_TICKS_PER_HARDNESS;
    public static ModConfigSpec.IntValue CHEW_MAX_BLOCKS;
    public static ModConfigSpec.BooleanValue CHEW_DROPS;
    public static ModConfigSpec.IntValue SCREEN_RECHARGE_TICKS;

    public static ModConfigSpec.BooleanValue ANTHONY;
    public static ModConfigSpec.DoubleValue ANTHONY_HEALTH;
    public static ModConfigSpec.DoubleValue ANTHONY_DAMAGE;
    public static ModConfigSpec.DoubleValue ANTHONY_SCALE;
    public static ModConfigSpec.DoubleValue ANTHONY_RIDE_SPEED;
    public static ModConfigSpec.DoubleValue ANTHONY_RIDE_MAX_SCALE;
    public static ModConfigSpec.DoubleValue ANTHONY_RESPAWN_SECONDS;

    private PymConfig() {
    }

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment("Ant-Man and Wasp helmet colony control. Requires Antarchy; granted while an Ant-Man or Wasp helmet is worn.")
                .push("pymColony");
        ENABLED = builder.comment("Master switch for Pym helmet colony abilities.").define("enabled", true);
        COMMAND_RANGE = builder.comment("How far Sic 'Em, Overrun, Sting Storm and Chew reach.").defineInRange("commandRange", 24.0D, 1.0D, 128.0D);
        LEASH_RANGE = builder.comment("Colony members further than this teleport back to you.").defineInRange("leashRange", 28.0D, 4.0D, 128.0D);

        builder.push("ants");
        ANT_SUMMON_CAP = builder.defineInRange("summonCap", 8, 0, 64);
        ANT_RECRUIT_CAP = builder.defineInRange("recruitCap", 12, 0, 64);
        BROWN_WEIGHT = builder.comment("Relative odds of each summoned ant type.").defineInRange("brownWeight", 60, 0, 1000);
        RED_WEIGHT = builder.defineInRange("redWeight", 25, 0, 1000);
        TERMITE_WEIGHT = builder.defineInRange("termiteWeight", 15, 0, 1000);
        ANT_HEALTH = builder.comment("Health of summoned ants.").defineInRange("health", 10.0D, 1.0D, 1000.0D);
        ANT_DAMAGE = builder.comment("Bite damage of summoned ants.").defineInRange("damage", 3.0D, 0.0D, 1000.0D);
        RED_ANT_FIRE_SECONDS = builder.comment("Red ant bites from your colony set targets alight for this long.").defineInRange("redAntFireSeconds", 3, 0, 60);
        builder.pop();

        builder.push("wasps");
        WASP_SUMMON_CAP = builder.defineInRange("summonCap", 6, 0, 64);
        WASP_RECRUIT_CAP = builder.defineInRange("recruitCap", 8, 0, 64);
        WASP_HEALTH = builder.comment("Health of summoned wasps.").defineInRange("health", 8.0D, 1.0D, 1000.0D);
        WASP_DAMAGE = builder.comment("Sting damage of summoned wasps.").defineInRange("damage", 3.0D, 0.0D, 1000.0D);
        builder.pop();

        builder.push("abilities");
        SUMMON_COOLDOWN = builder.defineInRange("callCooldownSeconds", 3.0D, 0.0D, 3600.0D);
        RESPAWN_SECONDS = builder.comment("While the colony is out, one fallen summon is replaced this often.").defineInRange("respawnSeconds", 4.0D, 0.0D, 600.0D);
        RECRUIT_RADIUS = builder.defineInRange("pheromoneRadius", 16.0D, 1.0D, 64.0D);
        RECRUIT_COOLDOWN = builder.defineInRange("pheromoneCooldownSeconds", 6.0D, 0.0D, 3600.0D);
        SIC_EM_COOLDOWN = builder.defineInRange("sicEmCooldownSeconds", 2.0D, 0.0D, 3600.0D);
        OVERRUN_COOLDOWN = builder.defineInRange("overrunCooldownSeconds", 14.0D, 0.0D, 3600.0D);
        OVERRUN_TICKS = builder.comment("How long Overrun pins its target.").defineInRange("overrunTicks", 60, 1, 1200);
        OVERRUN_BITE_DAMAGE = builder.comment("Damage per ant bite during Overrun.").defineInRange("overrunBiteDamage", 2.0D, 0.0D, 1000.0D);
        STING_STORM_COOLDOWN = builder.defineInRange("stingStormCooldownSeconds", 12.0D, 0.0D, 3600.0D);
        STING_STORM_DAMAGE = builder.comment("Bonus damage per wasp dive.").defineInRange("stingStormDamage", 4.0D, 0.0D, 1000.0D);
        STING_STORM_STAGGER_TICKS = builder.comment("Delay between each wasp's dive.").defineInRange("stingStormStaggerTicks", 4, 0, 100);
        builder.pop();

        builder.push("orders");
        FORAGE_RANGE = builder.defineInRange("forageRange", 16.0D, 1.0D, 64.0D);
        CHEW_TICKS_PER_HARDNESS = builder.comment("Termite-ticks needed per point of block hardness.").defineInRange("chewTicksPerHardness", 30.0D, 1.0D, 1000.0D);
        CHEW_MAX_BLOCKS = builder.defineInRange("chewMaxBlocks", 64, 1, 4096);
        CHEW_DROPS = builder.define("chewDrops", true);
        SCREEN_RECHARGE_TICKS = builder.comment("How long a screening wasp needs between blocked projectiles.").defineInRange("screenRechargeTicks", 30, 1, 1200);
        builder.pop();

        builder.push("anthony");
        ANTHONY = builder.comment("Ant-Man helmets bring Anthony along.").define("enabled", true);
        ANTHONY_HEALTH = builder.defineInRange("health", 40.0D, 1.0D, 1000.0D);
        ANTHONY_DAMAGE = builder.defineInRange("damage", 6.0D, 0.0D, 1000.0D);
        ANTHONY_SCALE = builder.defineInRange("scale", 1.4D, 0.1D, 8.0D);
        ANTHONY_RIDE_SPEED = builder.comment("Multiplier on Anthony's speed while ridden.").defineInRange("rideSpeed", 1.6D, 0.1D, 10.0D);
        ANTHONY_RIDE_MAX_SCALE = builder.comment("You must be at most this scale (shrunk) to ride Anthony.").defineInRange("rideMaxScale", 0.5D, 0.0D, 16.0D);
        ANTHONY_RESPAWN_SECONDS = builder.defineInRange("respawnSeconds", 60.0D, 0.0D, 3600.0D);
        builder.pop();

        builder.pop();
    }
}
