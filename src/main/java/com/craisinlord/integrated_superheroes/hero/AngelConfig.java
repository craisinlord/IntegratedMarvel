package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class AngelConfig {
    public static MarvelBalanceConfig.HeroStats ANGEL;
    public static ModConfigSpec.DoubleValue FLY_SPEED;
    public static ModConfigSpec.DoubleValue LIFT_MAX;
    public static ModConfigSpec.DoubleValue TAKEOFF_COST;
    public static ModConfigSpec.DoubleValue HOVER_DRAIN;
    public static ModConfigSpec.DoubleValue SPRINT_CLIMB_DRAIN;
    public static ModConfigSpec.DoubleValue SPRINT_DRAIN;
    public static ModConfigSpec.DoubleValue GLIDE_DRAIN;
    public static ModConfigSpec.DoubleValue GROUND_REGAIN;
    public static ModConfigSpec.IntValue GROUND_SETTLE_TICKS;
    public static ModConfigSpec.DoubleValue WET_MULTIPLIER;
    public static ModConfigSpec.DoubleValue GLIDE_SPEED;
    public static ModConfigSpec.DoubleValue GLIDE_SINK;
    public static ModConfigSpec.DoubleValue ASSIST_FALL_SPEED;
    public static ModConfigSpec.DoubleValue LANDING_AID_MULTIPLIER;
    public static ModConfigSpec.DoubleValue SKYFALL_POUNCE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue SKYFALL_MIN_DAMAGE_FRACTION;
    public static ModConfigSpec.DoubleValue SKYFALL_START_SPEED;
    public static ModConfigSpec.DoubleValue SKYFALL_MAX_SPEED;
    public static ModConfigSpec.DoubleValue SKYFALL_ACCELERATION;
    public static ModConfigSpec.IntValue SKYFALL_MAX_TICKS;
    public static ModConfigSpec.IntValue SKYFALL_PULL_OUT_TICKS;
    public static ModConfigSpec.DoubleValue SKYFALL_CUSHION;
    public static ModConfigSpec.DoubleValue SKYFALL_IMPACT_RADIUS;
    public static ModConfigSpec.DoubleValue SKYFALL_KNOCKBACK;
    public static ModConfigSpec.DoubleValue SKYFALL_COOLDOWN;
    public static ModConfigSpec.DoubleValue WING_LEAP_UP;
    public static ModConfigSpec.DoubleValue WING_LEAP_FORWARD;
    public static ModConfigSpec.DoubleValue WING_LEAP_COOLDOWN;

    private AngelConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("angel");
        ANGEL = new MarvelBalanceConfig.HeroStats(builder, 20.0D, 6.0D, 0.5D, 0.0D, 0.0D, 16.0D, 1, List.of());
        FLY_SPEED = builder.comment("Flying speed while Wingbeat is on. Sprinting doubles it like normal flight.")
                .defineInRange("flySpeed", 0.07D, 0.01D, 1.0D);
        LIFT_MAX = builder.defineInRange("liftMax", 100.0D, 1.0D, 10000.0D);
        TAKEOFF_COST = builder.comment("Lift spent to take off. Touching the ground briefly does not refund it.")
                .defineInRange("takeoffCost", 10.0D, 0.0D, 10000.0D);
        HOVER_DRAIN = builder.comment("Lift per second while hovering or climbing.")
                .defineInRange("hoverDrainPerSecond", 5.0D, 0.0D, 10000.0D);
        SPRINT_CLIMB_DRAIN = builder.comment("Lift per second while climbing and sprinting.")
                .defineInRange("sprintClimbDrainPerSecond", 9.0D, 0.0D, 10000.0D);
        SPRINT_DRAIN = builder.comment("Lift per second while sprinting level or downhill.")
                .defineInRange("sprintDrainPerSecond", 3.0D, 0.0D, 10000.0D);
        GLIDE_DRAIN = builder.comment("Lift per second while flying level or downhill.")
                .defineInRange("glideDrainPerSecond", 1.0D, 0.0D, 10000.0D);
        GROUND_REGAIN = builder.comment("Lift per second regained while standing on the ground.")
                .defineInRange("groundRegainPerSecond", 8.0D, 0.0D, 10000.0D);
        GROUND_SETTLE_TICKS = builder.comment("Ticks you must stay on the ground before Lift starts coming back.")
                .defineInRange("groundSettleTicks", 10, 0, 1200);
        WET_MULTIPLIER = builder.comment("Lift costs are multiplied by this in rain or water.")
                .defineInRange("wetDrainMultiplier", 1.25D, 1.0D, 10.0D);
        GLIDE_SPEED = builder.comment("Forward speed of an unpowered glide, in blocks per tick.")
                .defineInRange("glideSpeed", 0.55D, 0.05D, 3.0D);
        GLIDE_SINK = builder.comment("How fast an unpowered glide sinks, in blocks per tick.")
                .defineInRange("glideSink", 0.08D, 0.04D, 1.0D);
        ASSIST_FALL_SPEED = builder.comment("Glide Assist opens the wings once you fall faster than this, in blocks per tick.")
                .defineInRange("glideAssistFallSpeed", 0.6D, 0.1D, 5.0D);
        LANDING_AID_MULTIPLIER = builder.comment("Fall damage is multiplied by this while Landing Aid is on.")
                .defineInRange("landingAidMultiplier", 0.5D, 0.0D, 1.0D);
        SKYFALL_POUNCE_MULTIPLIER = builder.comment("Skyfall's top damage as a multiple of Panther Pounce damage.")
                .defineInRange("skyfallPounceMultiplier", 1.2D, 0.0D, 100.0D);
        SKYFALL_MIN_DAMAGE_FRACTION = builder.comment("Share of top damage dealt at the slowest dive speed.")
                .defineInRange("skyfallMinDamageFraction", 0.25D, 0.0D, 1.0D);
        SKYFALL_START_SPEED = builder.defineInRange("skyfallStartSpeed", 0.8D, 0.1D, 10.0D);
        SKYFALL_MAX_SPEED = builder.comment("Top dive speed in blocks per tick. Damage stops growing here.")
                .defineInRange("skyfallMaxSpeed", 2.4D, 0.2D, 10.0D);
        SKYFALL_ACCELERATION = builder.defineInRange("skyfallAcceleration", 0.12D, 0.0D, 5.0D);
        SKYFALL_MAX_TICKS = builder.comment("The dive ends on its own after this many ticks.")
                .defineInRange("skyfallMaxTicks", 60, 5, 600);
        SKYFALL_PULL_OUT_TICKS = builder.comment("Ticks before pressing Skyfall again pulls out of the dive.")
                .defineInRange("skyfallPullOutTicks", 10, 0, 600);
        SKYFALL_CUSHION = builder.comment("Blocks of the dive that do not count toward fall damage if you hit the ground.")
                .defineInRange("skyfallCushionBlocks", 16.0D, 0.0D, 1000.0D);
        SKYFALL_IMPACT_RADIUS = builder.comment("A ground impact hits the closest creature in this radius.")
                .defineInRange("skyfallImpactRadius", 2.5D, 0.0D, 16.0D);
        SKYFALL_KNOCKBACK = builder.defineInRange("skyfallKnockback", 1.2D, 0.0D, 10.0D);
        SKYFALL_COOLDOWN = builder.defineInRange("skyfallCooldownSeconds", 12.0D, 0.0D, 600.0D);
        WING_LEAP_UP = builder.comment("Upward push of the Wing Leap memory Rogue can borrow.")
                .defineInRange("rogueWingLeapUp", 0.9D, 0.0D, 5.0D);
        WING_LEAP_FORWARD = builder.defineInRange("rogueWingLeapForward", 1.1D, 0.0D, 5.0D);
        WING_LEAP_COOLDOWN = builder.defineInRange("rogueWingLeapCooldownSeconds", 8.0D, 0.0D, 600.0D);
        builder.pop();
    }
}
