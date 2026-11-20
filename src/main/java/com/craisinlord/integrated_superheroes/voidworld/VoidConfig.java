package com.craisinlord.integrated_superheroes.voidworld;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class VoidConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> RING_EDGES;
    public static final ModConfigSpec.IntValue MINUTES_PER_TIME_TIER;
    public static final ModConfigSpec.IntValue MAX_TIME_TIERS;
    public static final ModConfigSpec.IntValue ARRIVAL_MIN_RADIUS;
    public static final ModConfigSpec.IntValue ARRIVAL_MAX_RADIUS;
    public static final ModConfigSpec.IntValue CATCH_Y;
    public static final ModConfigSpec.IntValue ARRIVAL_SLOW_FALL_TICKS;
    public static final ModConfigSpec.DoubleValue NATURAL_SPAWN_CHANCE;
    public static final ModConfigSpec.BooleanValue STORM_ENABLED;
    public static final ModConfigSpec.IntValue STORM_START_STAGE;
    public static final ModConfigSpec.IntValue STORM_SPAWN_DISTANCE;
    public static final ModConfigSpec.IntValue STORM_CLOSEST_DISTANCE;
    public static final ModConfigSpec.DoubleValue STORM_APPROACH;
    public static final ModConfigSpec.IntValue STORM_BLOCKS_PER_TICK;
    public static final ModConfigSpec.IntValue STORM_MAX_FLYING;
    public static final ModConfigSpec.IntValue STORM_RESPAWN_MINUTES;
    public static final ModConfigSpec.IntValue STORM_GROWTH;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> STORM_COMMAND_BLOCK_DROPS;
    public static final ModConfigSpec.BooleanValue PRUNE_ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends Integer>> PRUNE_INTERVALS;
    public static final ModConfigSpec.DoubleValue PRUNE_JITTER;
    public static final ModConfigSpec.IntValue PRUNE_WARN_TICKS;
    public static final ModConfigSpec.IntValue PRUNE_TEAR_HEIGHT;
    public static final ModConfigSpec.IntValue PRUNE_MIN_DISTANCE;
    public static final ModConfigSpec.IntValue PRUNE_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue PRUNE_MAX_CONCURRENT;
    public static final ModConfigSpec.DoubleValue PRUNE_RIDER_CHANCE;
    public static final ModConfigSpec.DoubleValue PRUNE_SHOCKWAVE_DAMAGE;
    public static final ModConfigSpec.IntValue PRUNE_SHOCKWAVE_RADIUS;
    public static final ModConfigSpec.BooleanValue VEHICLES_ENABLED;
    public static final ModConfigSpec.IntValue VEHICLE_CHECK_SECONDS;
    public static final ModConfigSpec.DoubleValue VEHICLE_CHANCE;
    public static final ModConfigSpec.DoubleValue VEHICLE_CHANCE_PER_TIER;
    public static final ModConfigSpec.DoubleValue VEHICLE_TEAR_CHANCE;
    public static final ModConfigSpec.IntValue VEHICLE_MAX_LOADED;
    public static final ModConfigSpec.IntValue VEHICLE_MAX_NEAR_PLAYER;
    public static final ModConfigSpec.IntValue VEHICLE_MIN_DISTANCE;
    public static final ModConfigSpec.IntValue VEHICLE_MAX_DISTANCE;
    public static final ModConfigSpec.DoubleValue VEHICLE_FUEL_MIN;
    public static final ModConfigSpec.DoubleValue VEHICLE_FUEL_MAX;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VEHICLE_ALLOWED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> VEHICLE_DENIED;
    public static final ModConfigSpec.IntValue VEHICLE_AIRCRAFT_MIN_TIER;
    public static final ModConfigSpec.DoubleValue VEHICLE_AIRCRAFT_CHANCE;
    public static final ModConfigSpec.IntValue VEHICLE_DESPAWN_DISTANCE;
    public static final ModConfigSpec.IntValue VEHICLE_DESPAWN_SECONDS;
    public static final ModConfigSpec.DoubleValue VEHICLE_DRIVER_CHANCE;
    public static final ModConfigSpec.DoubleValue VEHICLE_LAUNCH_CHANCE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("tier");
        RING_EDGES = builder
                .comment("Distance from 0,0 where each Void tier starts, tier 0 first. Six values for tiers 0 to 5.")
                .defineList("ringEdges", List.of(0, 600, 1800, 3600, 6000, 9000), () -> 0, value -> value instanceof Integer i && i >= 0);
        MINUTES_PER_TIME_TIER = builder
                .comment("Minutes spent in the Void on one visit that add one tier for a player (spawns and loot only). 0 turns the time part off.")
                .defineInRange("minutesPerTimeTier", 10, 0, 1440);
        MAX_TIME_TIERS = builder
                .comment("Most tiers time in the Void can add on top of distance.")
                .defineInRange("maxTimeTiers", 5, 0, 5);
        builder.pop();
        builder.push("travel");
        ARRIVAL_MIN_RADIUS = builder.defineInRange("arrivalMinRadius", 64, 0, 100000);
        ARRIVAL_MAX_RADIUS = builder.defineInRange("arrivalMaxRadius", 256, 0, 100000);
        ARRIVAL_SLOW_FALL_TICKS = builder.defineInRange("arrivalSlowFallTicks", 100, 0, 1200);
        CATCH_Y = builder
                .comment("Players below this height in the Void have fallen through and get caught.")
                .defineInRange("catchY", 16, -64, 255);
        builder.pop();
        builder.push("spawns");
        NATURAL_SPAWN_CHANCE = builder
                .comment("Share of natural mob spawns in the Void that go through. Variants come from their own spawner and ignore this.")
                .defineInRange("naturalSpawnChance", 0.1D, 0.0D, 1.0D);
        builder.pop();
        builder.push("storm");
        STORM_ENABLED = builder
                .comment("A roaming Witherstorm lives in the Void whenever a player is there.")
                .define("enabled", true);
        STORM_START_STAGE = builder
                .comment("Stage the roaming storm starts at.")
                .defineInRange("startStage", 3, 2, 5);
        STORM_SPAWN_DISTANCE = builder
                .comment("Blocks from a player where the storm appears. Capped to the server view distance so it can be seen.")
                .defineInRange("spawnDistance", 160, 48, 512);
        STORM_CLOSEST_DISTANCE = builder
                .comment("The storm creeps in over time until it is this many blocks from the nearest player.")
                .defineInRange("closestDistance", 48, 0, 512);
        STORM_APPROACH = builder
                .comment("Blocks per minute the storm closes in while players stay in the Void.")
                .defineInRange("approachBlocksPerMinute", 6.0D, 0.0D, 600.0D);
        STORM_BLOCKS_PER_TICK = builder
                .comment("Most terrain blocks the storm eats per tick.")
                .defineInRange("blocksPerTick", 24, 0, 256);
        STORM_MAX_FLYING = builder
                .comment("Most eaten blocks flying into its mouth at once.")
                .defineInRange("maxFlyingBlocks", 24, 0, 128);
        STORM_RESPAWN_MINUTES = builder
                .comment("Minutes before a new storm appears after one is killed or removed.")
                .defineInRange("respawnMinutes", 60, 0, 10080);
        STORM_GROWTH = builder
                .comment("How much the storm has to eat to grow a stage, times its current stage. A block counts 1, a mob 200, a Variant 1000. 0 turns growth off.")
                .defineInRange("growthPerStage", 150000, 0, 100000000);
        STORM_COMMAND_BLOCK_DROPS = builder
                .comment("Storm Command Blocks a Witherstorm gives the player who kills it, by the stage it died at. Stage 1 first, five values.")
                .defineList("commandBlockDrops", List.of(0, 0, 1, 2, 4), () -> 0, value -> value instanceof Integer i && i >= 0 && i <= 64);
        builder.pop();
        builder.push("pruning");
        PRUNE_ENABLED = builder
                .comment("Pieces of other realities get pruned and fall into the Void near players.")
                .define("enabled", true);
        PRUNE_INTERVALS = builder
                .comment("Average seconds between pruned pieces falling near each player, by that player's tier, tier 0 first.")
                .defineList("intervalSeconds", List.of(420, 330, 250, 180, 130, 90), () -> 300, value -> value instanceof Integer i && i >= 10);
        PRUNE_JITTER = builder
                .comment("Random spread on each interval, as a share of it.")
                .defineInRange("intervalJitter", 0.25D, 0.0D, 0.9D);
        PRUNE_WARN_TICKS = builder
                .comment("Ticks of warning (tear, ground marker, sound) before a piece starts falling.")
                .defineInRange("warnTicks", 100, 20, 1200);
        PRUNE_TEAR_HEIGHT = builder
                .comment("Blocks above the landing spot where the tear opens. Each drop varies this by up to 15.")
                .defineInRange("tearHeight", 135, 40, 300);
        PRUNE_MIN_DISTANCE = builder
                .comment("Closest a piece lands to the player it was rolled for.")
                .defineInRange("minDistance", 24, 4, 256);
        PRUNE_MAX_DISTANCE = builder
                .comment("Farthest a piece lands from the player it was rolled for.")
                .defineInRange("maxDistance", 80, 8, 256);
        PRUNE_MAX_CONCURRENT = builder
                .comment("Most pieces warning or falling at once in the Void.")
                .defineInRange("maxConcurrent", 3, 1, 32);
        PRUNE_RIDER_CHANCE = builder
                .comment("Chance for each Variant rider slot on a landed piece. Higher tiers get more slots, up to three.")
                .defineInRange("riderChance", 0.35D, 0.0D, 1.0D);
        PRUNE_SHOCKWAVE_DAMAGE = builder
                .comment("Damage at the centre of the landing shockwave, falling off to nothing at its edge.")
                .defineInRange("shockwaveDamage", 8.0D, 0.0D, 200.0D);
        PRUNE_SHOCKWAVE_RADIUS = builder
                .comment("Blocks past the edge of the landed piece that the shockwave reaches.")
                .defineInRange("shockwaveRadius", 8, 0, 48);
        builder.pop();
        builder.push("vehicles");
        VEHICLES_ENABLED = builder
                .comment("Abandoned Immersive Vehicles turn up around the Void near players, fitted out and fuelled so they can be driven off. Needs Immersive Vehicles.")
                .define("enabled", true);
        VEHICLE_CHECK_SECONDS = builder
                .comment("Seconds between vehicle checks for each player.")
                .defineInRange("checkSeconds", 30, 1, 3600);
        VEHICLE_CHANCE = builder
                .comment("Chance a check places a vehicle near that player.")
                .defineInRange("chancePerCheck", 0.06D, 0.0D, 1.0D);
        VEHICLE_CHANCE_PER_TIER = builder
                .comment("Extra chance per check for each Void tier the player is at.")
                .defineInRange("chancePerTier", 0.01D, 0.0D, 1.0D);
        VEHICLE_TEAR_CHANCE = builder
                .comment("Share of vehicles that arrive through a tear, already rolling, instead of sitting on the ground.")
                .defineInRange("tearChance", 0.3D, 0.0D, 1.0D);
        VEHICLE_MAX_LOADED = builder
                .comment("Most unclaimed vehicles loaded in the Void at once. Vehicles a player has driven no longer count.")
                .defineInRange("maxLoaded", 6, 0, 64);
        VEHICLE_MAX_NEAR_PLAYER = builder
                .comment("Most unclaimed vehicles within despawnDistance of one player before no more are placed near them.")
                .defineInRange("maxNearPlayer", 2, 0, 64);
        VEHICLE_MIN_DISTANCE = builder
                .comment("Closest a vehicle is placed to the player it was rolled for.")
                .defineInRange("minDistance", 24, 4, 256);
        VEHICLE_MAX_DISTANCE = builder
                .comment("Farthest a vehicle is placed from the player it was rolled for.")
                .defineInRange("maxDistance", 64, 8, 256);
        VEHICLE_FUEL_MIN = builder
                .comment("Lowest share of a full tank a vehicle turns up with.")
                .defineInRange("fuelMin", 0.15D, 0.0D, 1.0D);
        VEHICLE_FUEL_MAX = builder
                .comment("Highest share of a full tank a vehicle turns up with.")
                .defineInRange("fuelMax", 0.6D, 0.0D, 1.0D);
        VEHICLE_ALLOWED = builder
                .comment("Vehicles that can turn up, as globs matched against pack:vehicle (mtsofficialpack:scout) or the item id (mts:mtsofficialpack.scout_red).")
                .defineListAllowEmpty("allowed", List.of("*"), () -> "*", value -> value instanceof String);
        VEHICLE_DENIED = builder
                .comment("Vehicles that never turn up, same format as allowed. Wins over allowed.")
                .defineListAllowEmpty("denied", List.of(), () -> "", value -> value instanceof String);
        VEHICLE_AIRCRAFT_MIN_TIER = builder
                .comment("Lowest player tier where an aircraft can turn up instead of a land vehicle.")
                .defineInRange("aircraftMinTier", 3, 0, 6);
        VEHICLE_AIRCRAFT_CHANCE = builder
                .comment("Chance a vehicle is an aircraft once the tier allows it.")
                .defineInRange("aircraftChance", 0.15D, 0.0D, 1.0D);
        VEHICLE_DESPAWN_DISTANCE = builder
                .comment("Unclaimed vehicles farther than this from every player start counting down to despawn.")
                .defineInRange("despawnDistance", 96, 16, 1024);
        VEHICLE_DESPAWN_SECONDS = builder
                .comment("Seconds an unclaimed vehicle has to stay far from every player before it despawns. 0 keeps them. Vehicles a player has ridden never despawn.")
                .defineInRange("despawnSeconds", 180, 0, 86400);
        VEHICLE_DRIVER_CHANCE = builder
                .comment("Chance a vehicle coming through a tear has a hostile Variant at the wheel, who gets out once it stops.")
                .defineInRange("driverChance", 0.15D, 0.0D, 1.0D);
        VEHICLE_LAUNCH_CHANCE = builder
                .comment("Chance a vehicle coming through a tear is thrown out of it from above the ground instead of rolling out.")
                .defineInRange("launchChance", 0.25D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private VoidConfig() {
    }
}
