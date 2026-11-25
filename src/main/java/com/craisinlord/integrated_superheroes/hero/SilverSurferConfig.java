package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SilverSurferConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue CRUISE_MAX_SPEED;
    public static ModConfigSpec.DoubleValue PRECISION_MAX_SPEED;
    public static ModConfigSpec.DoubleValue CRUISE_ACCELERATION;
    public static ModConfigSpec.DoubleValue PRECISION_ACCELERATION;
    public static ModConfigSpec.DoubleValue CRUISE_TURN_DEGREES;
    public static ModConfigSpec.DoubleValue PRECISION_TURN_DEGREES;
    public static ModConfigSpec.DoubleValue CRUISE_COAST_DRAG;
    public static ModConfigSpec.DoubleValue PRECISION_COAST_DRAG;
    public static ModConfigSpec.DoubleValue MAX_PITCH_DEGREES;
    public static ModConfigSpec.DoubleValue MAX_CLIMB;
    public static ModConfigSpec.DoubleValue MAX_DESCENT;
    public static ModConfigSpec.DoubleValue SUMMON_COOLDOWN;
    public static ModConfigSpec.DoubleValue FORCED_COOLDOWN;
    public static ModConfigSpec.IntValue LANDING_GRACE_TICKS;
    public static ModConfigSpec.IntValue OVERSPEED_TICKS;
    public static ModConfigSpec.DoubleValue LANCE_PHOTON_MULTIPLIER;
    public static ModConfigSpec.DoubleValue LANCE_RANGE;
    public static ModConfigSpec.IntValue LANCE_TARGETS;
    public static ModConfigSpec.DoubleValue LANCE_KNOCKBACK;
    public static ModConfigSpec.DoubleValue LANCE_COOLDOWN;
    public static ModConfigSpec.DoubleValue ROGUE_LANCE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue ROGUE_LANCE_COOLDOWN;
    public static ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_DIMENSIONS;

    private SilverSurferConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("silverSurfer");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 47.0D, 6.0D, 1.0D, 0.0D, 0.0D, 35.0D, 1, List.of());
        CRUISE_MAX_SPEED = builder.comment("Top board speed in Cruise handling, in blocks per tick.")
                .defineInRange("cruiseMaxSpeed", 1.4D, 0.1D, 3.0D);
        PRECISION_MAX_SPEED = builder.comment("Top board speed with Precision Handling on, in blocks per tick.")
                .defineInRange("precisionMaxSpeed", 0.8D, 0.1D, 3.0D);
        CRUISE_ACCELERATION = builder.comment("Speed gained per tick while pushing forward in Cruise handling.")
                .defineInRange("cruiseAcceleration", 0.07D, 0.005D, 1.0D);
        PRECISION_ACCELERATION = builder.defineInRange("precisionAcceleration", 0.06D, 0.005D, 1.0D);
        CRUISE_TURN_DEGREES = builder.comment("How far the board's momentum can swing toward where you look each tick in Cruise handling.")
                .defineInRange("cruiseTurnDegreesPerTick", 4.5D, 0.5D, 90.0D);
        PRECISION_TURN_DEGREES = builder.defineInRange("precisionTurnDegreesPerTick", 11.0D, 0.5D, 90.0D);
        CRUISE_COAST_DRAG = builder.comment("Share of speed kept each tick while coasting in Cruise handling.")
                .defineInRange("cruiseCoastDrag", 0.975D, 0.5D, 1.0D);
        PRECISION_COAST_DRAG = builder.defineInRange("precisionCoastDrag", 0.88D, 0.5D, 1.0D);
        MAX_PITCH_DEGREES = builder.comment("Steepest climb or dive angle the board follows from where you look.")
                .defineInRange("maxPitchDegrees", 35.0D, 0.0D, 89.0D);
        MAX_CLIMB = builder.comment("Fastest the board can rise, in blocks per tick.")
                .defineInRange("maxClimb", 0.45D, 0.0D, 3.0D);
        MAX_DESCENT = builder.comment("Fastest the board can sink, in blocks per tick.")
                .defineInRange("maxDescent", 0.7D, 0.0D, 3.0D);
        SUMMON_COOLDOWN = builder.comment("Seconds before the board can be called again after stepping off.")
                .defineInRange("boardCooldownSeconds", 1.0D, 0.0D, 600.0D);
        FORCED_COOLDOWN = builder.comment("Seconds before the board can be called again after being knocked off it.")
                .defineInRange("forcedDismountCooldownSeconds", 4.0D, 0.0D, 600.0D);
        LANDING_GRACE_TICKS = builder.comment("After being knocked off the board, the first fall within this many ticks does no damage.")
                .defineInRange("landingGraceTicks", 40, 0, 400);
        OVERSPEED_TICKS = builder.comment("Moving much faster than the board allows for this many ticks in a row ends the ride.")
                .defineInRange("overspeedTicks", 10, 1, 200);
        LANCE_PHOTON_MULTIPLIER = builder.comment("Cosmic Lance damage per target as a multiple of Binary's photonBlastDamagePerHit.")
                .defineInRange("lancePhotonMultiplier", 0.9D, 0.0D, 10.0D);
        LANCE_RANGE = builder.defineInRange("lanceRange", 48.0D, 4.0D, 128.0D);
        LANCE_TARGETS = builder.comment("Most creatures one Cosmic Lance can pierce.")
                .defineInRange("lanceTargets", 3, 1, 16);
        LANCE_KNOCKBACK = builder.defineInRange("lanceKnockback", 0.6D, 0.0D, 5.0D);
        LANCE_COOLDOWN = builder.defineInRange("lanceCooldownSeconds", 7.0D, 0.0D, 600.0D);
        ROGUE_LANCE_MULTIPLIER = builder.comment("Damage of the Cosmic Lance Rogue borrows, as a share of the full Lance before her memory multiplier.")
                .defineInRange("rogueLanceMultiplier", 0.6D, 0.0D, 1.0D);
        ROGUE_LANCE_COOLDOWN = builder.defineInRange("rogueLanceCooldownSeconds", 10.0D, 0.0D, 600.0D);
        BLOCKED_DIMENSIONS = builder.comment("Dimensions where the board can't be called, like \"minecraft:the_end\".")
                .defineListAllowEmpty("blockedDimensions", List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
    }
}
