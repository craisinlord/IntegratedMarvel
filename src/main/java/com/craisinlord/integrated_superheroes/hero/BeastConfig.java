package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class BeastConfig {
    public static MarvelBalanceConfig.HeroStats BEAST;
    public static ModConfigSpec.DoubleValue POUNCE_SPEED;
    public static ModConfigSpec.DoubleValue POUNCE_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue POUNCE_COOLDOWN;
    public static ModConfigSpec.IntValue REBOUND_WINDOW_TICKS;
    public static ModConfigSpec.DoubleValue REBOUND_RANGE;
    public static ModConfigSpec.DoubleValue REBOUND_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue REBOUND_REFUND;
    public static ModConfigSpec.DoubleValue SWEEP_RANGE;
    public static ModConfigSpec.DoubleValue SWEEP_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.IntValue SWEEP_STUMBLE_TICKS;
    public static ModConfigSpec.IntValue SWEEP_STUMBLE_IMMUNITY_TICKS;
    public static ModConfigSpec.DoubleValue SWEEP_COOLDOWN;
    public static ModConfigSpec.IntValue LEAP_JUMP_LEVEL;
    public static ModConfigSpec.DoubleValue LEAP_MULTIPLIER;
    public static ModConfigSpec.DoubleValue LEAP_HORIZONTAL;
    public static ModConfigSpec.DoubleValue CLING_LATERAL;

    private BeastConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("beast");
        BEAST = new MarvelBalanceConfig.HeroStats(builder, 25.0D, 8.0D, 0.5D, 0.5D, 0.03D, 24.0D, 1,
                List.of("minecraft:generic.safe_fall_distance;8.0;ADD_VALUE"));
        POUNCE_SPEED = builder.comment("Launch speed of Pounce. 1.3 carries you about 12 blocks on flat ground.")
                .defineInRange("pounceSpeed", 1.3D, 0.2D, 3.0D);
        POUNCE_DAMAGE_MULTIPLIER = builder.comment("Pounce damage as a fraction of Black Panther's pounceDamage.")
                .defineInRange("pounceDamageMultiplier", 0.8D, 0.0D, 10.0D);
        POUNCE_COOLDOWN = builder.comment("Cooldown of Pounce. A miss still costs the full cooldown.")
                .defineInRange("pounceCooldownSeconds", 7.0D, 0.0D, 600.0D);
        REBOUND_WINDOW_TICKS = builder.comment("After a Pounce lands on an enemy, pressing Pounce again within this many ticks does a Rebound Kick.")
                .defineInRange("reboundWindowTicks", 24, 1, 200);
        REBOUND_RANGE = builder.comment("How far away the second enemy can be for a Rebound Kick.")
                .defineInRange("reboundRange", 8.0D, 1.0D, 32.0D);
        REBOUND_DAMAGE_MULTIPLIER = builder.comment("Rebound Kick damage as a fraction of Black Panther's pounceDamage.")
                .defineInRange("reboundDamageMultiplier", 0.5D, 0.0D, 10.0D);
        REBOUND_REFUND = builder.comment("Fraction of the Pounce cooldown refunded when the Rebound Kick hits an enemy.")
                .defineInRange("reboundCooldownRefund", 0.5D, 0.0D, 1.0D);
        SWEEP_RANGE = builder.defineInRange("weightedSweepRange", 4.0D, 1.0D, 16.0D);
        SWEEP_DAMAGE_MULTIPLIER = builder.comment("Weighted Sweep damage as a fraction of Black Panther's clawSwipeDamage.")
                .defineInRange("weightedSweepDamageMultiplier", 0.55D, 0.0D, 10.0D);
        SWEEP_STUMBLE_TICKS = builder.comment("How long ordinary mobs stumble after a Weighted Sweep. Bosses never stumble.")
                .defineInRange("weightedSweepStumbleTicks", 16, 0, 200);
        SWEEP_STUMBLE_IMMUNITY_TICKS = builder.comment("After a stumble ends, the same creature can't be made to stumble again for this long.")
                .defineInRange("weightedSweepStumbleImmunityTicks", 80, 0, 1200);
        SWEEP_COOLDOWN = builder.defineInRange("weightedSweepCooldownSeconds", 9.0D, 0.0D, 600.0D);
        LEAP_JUMP_LEVEL = builder.comment("Jump Boost level while the Super Leap toggle is on.")
                .defineInRange("leapJumpLevel", 2, 1, 10);
        LEAP_MULTIPLIER = builder.comment("Crouch-jumping multiplies your jump height by this while Super Leap is on.")
                .defineInRange("crouchJumpMultiplier", 2.0D, 1.0D, 20.0D);
        LEAP_HORIZONTAL = builder.comment("Sprinting crouch-jumps also multiply your forward speed by this.")
                .defineInRange("crouchJumpHorizontal", 1.8D, 1.0D, 20.0D);
        CLING_LATERAL = builder.comment("How much of your sideways speed you keep each tick while clinging to a wall. Lower is slower.")
                .defineInRange("wallClingLateral", 0.5D, 0.0D, 1.0D);
        builder.pop();
    }
}
