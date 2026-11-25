package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class JeanGreyConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue FLY_SPEED;
    public static ModConfigSpec.DoubleValue GRAB_RANGE;
    public static ModConfigSpec.IntValue MAX_HELD;
    public static ModConfigSpec.IntValue HOSTILE_HOLD_TICKS;
    public static ModConfigSpec.IntValue RESCUE_HOLD_TICKS;
    public static ModConfigSpec.IntValue CONTROL_IMMUNITY_TICKS;
    public static ModConfigSpec.IntValue FALL_GRACE_TICKS;
    public static ModConfigSpec.DoubleValue GRAB_COOLDOWN;
    public static ModConfigSpec.DoubleValue THROW_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue THROW_CAST_CAP_MULTIPLIER;
    public static ModConfigSpec.DoubleValue THROW_SPEED_MULTIPLIER;
    public static ModConfigSpec.DoubleValue THROW_COOLDOWN;
    public static ModConfigSpec.DoubleValue SET_DOWN_COOLDOWN;
    public static ModConfigSpec.DoubleValue CRUSH_MULTIPLIER;
    public static MarvelBalanceConfig.HeroStats PHOENIX_STATS;
    public static ModConfigSpec.DoubleValue DAMAGE_DEALT_PER_PERCENT;
    public static ModConfigSpec.DoubleValue DAMAGE_TAKEN_PER_PERCENT;
    public static ModConfigSpec.IntValue METER_DECAY_DELAY_TICKS;
    public static ModConfigSpec.DoubleValue METER_DECAY_PER_SECOND;
    public static ModConfigSpec.IntValue PHOENIX_TICKS;
    public static ModConfigSpec.DoubleValue PHOENIX_FLY_SPEED;
    public static ModConfigSpec.DoubleValue AURA_RADIUS;
    public static ModConfigSpec.DoubleValue BURST_RADIUS;
    public static ModConfigSpec.DoubleValue BURST_MULTIPLIER;
    public static ModConfigSpec.DoubleValue SUPERNOVA_RADIUS;
    public static ModConfigSpec.DoubleValue SUPERNOVA_DAMAGE;
    public static ModConfigSpec.DoubleValue SUPERNOVA_COOLDOWN;
    public static ModConfigSpec.DoubleValue FADE_COOLDOWN;
    public static ModConfigSpec.BooleanValue REBIRTH;
    public static ModConfigSpec.IntValue ROGUE_HOLD_TICKS;
    public static ModConfigSpec.DoubleValue ROGUE_RANGE;
    public static ModConfigSpec.DoubleValue ROGUE_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue ROGUE_COOLDOWN;

    private JeanGreyConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("jeanGrey");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 42.0D, 5.0D, 0.2D, 0.0D, 0.0D, 3.0D, 0, List.of());
        FLY_SPEED = builder.comment("Flying speed while Levitation is on.")
                .defineInRange("levitationFlySpeed", 0.045D, 0.01D, 1.0D);
        GRAB_RANGE = builder.comment("How far away Telekinetic Grab can reach a creature.")
                .defineInRange("grabRange", 16.0D, 1.0D, 64.0D);
        MAX_HELD = builder.comment("How many creatures can be held at once.")
                .defineInRange("maxHeld", 3, 1, 3);
        HOSTILE_HOLD_TICKS = builder.comment("Longest time one creature is held before it is set down on its own.")
                .defineInRange("hostileHoldTicks", 100, 1, 1200);
        RESCUE_HOLD_TICKS = builder.comment("Longest time a willing player or your own pet is carried before being set down.")
                .defineInRange("rescueHoldTicks", 200, 1, 2400);
        CONTROL_IMMUNITY_TICKS = builder.comment("After a creature is let go, nobody's Telekinetic Grab can lift it again for this long.")
                .defineInRange("controlImmunityTicks", 80, 0, 1200);
        FALL_GRACE_TICKS = builder.comment("Anything set down or dropped is spared fall damage for this long.")
                .defineInRange("fallGraceTicks", 60, 0, 600);
        GRAB_COOLDOWN = builder.comment("Short cooldown after each successful grab.")
                .defineInRange("grabCooldownSeconds", 0.5D, 0.0D, 600.0D);
        THROW_DAMAGE_MULTIPLIER = builder.comment("Throw damage per creature as a fraction of Magneto's magneticGripThrowDamage.")
                .defineInRange("throwDamageMultiplier", 0.7D, 0.0D, 10.0D);
        THROW_CAST_CAP_MULTIPLIER = builder.comment("Total damage one Throw can deal across every creature, as a fraction of Magneto's magneticGripThrowDamage.")
                .defineInRange("throwCastCapMultiplier", 1.5D, 0.0D, 30.0D);
        THROW_SPEED_MULTIPLIER = builder.comment("Throw speed as a fraction of Magneto's magneticGripThrowSpeed.")
                .defineInRange("throwSpeedMultiplier", 0.8D, 0.0D, 5.0D);
        THROW_COOLDOWN = builder.defineInRange("throwCooldownSeconds", 4.0D, 0.0D, 600.0D);
        SET_DOWN_COOLDOWN = builder.comment("Cooldown after crouching to set everything down.")
                .defineInRange("setDownCooldownSeconds", 1.0D, 0.0D, 600.0D);
        CRUSH_MULTIPLIER = builder.comment("Damage each held hostile takes every second, as a fraction of Magneto's magneticGripThrowDamage. Doubled while the Phoenix burns.")
                .defineInRange("crushDamageMultiplier", 0.1D, 0.0D, 10.0D);
        builder.push("phoenix");
        PHOENIX_STATS = new MarvelBalanceConfig.HeroStats(builder, 89.0D, 9.0D, 1.0D, 0.5D, 0.02D, 60.0D, 1, List.of());
        DAMAGE_DEALT_PER_PERCENT = builder.comment("Damage you deal for each 1% of the Phoenix meter.")
                .defineInRange("damageDealtPerPercent", 6.0D, 0.01D, 10000.0D);
        DAMAGE_TAKEN_PER_PERCENT = builder.comment("Damage you take for each 1% of the Phoenix meter.")
                .defineInRange("damageTakenPerPercent", 0.6D, 0.01D, 10000.0D);
        METER_DECAY_DELAY_TICKS = builder.comment("Ticks without dealing or taking damage before the meter starts to drain.")
                .defineInRange("meterDecayDelayTicks", 300, 0, 72000);
        METER_DECAY_PER_SECOND = builder.defineInRange("meterDecayPercentPerSecond", 2.0D, 0.0D, 100.0D);
        PHOENIX_TICKS = builder.comment("How long the Phoenix burns once ignited.")
                .defineInRange("phoenixTicks", 600, 20, 12000);
        PHOENIX_FLY_SPEED = builder.defineInRange("phoenixFlySpeed", 0.09D, 0.01D, 1.0D);
        AURA_RADIUS = builder.comment("Hostiles this close are set alight while the Phoenix burns. 0 turns it off.")
                .defineInRange("auraRadius", 4.0D, 0.0D, 16.0D);
        BURST_RADIUS = builder.comment("Radius of the fire burst when something you threw lands while the Phoenix burns.")
                .defineInRange("throwBurstRadius", 3.0D, 0.0D, 16.0D);
        BURST_MULTIPLIER = builder.comment("Burst damage as a fraction of that throw's damage.")
                .defineInRange("throwBurstMultiplier", 0.6D, 0.0D, 10.0D);
        SUPERNOVA_RADIUS = builder.defineInRange("supernovaRadius", 10.0D, 1.0D, 64.0D);
        SUPERNOVA_DAMAGE = builder.defineInRange("supernovaDamage", 150.0D, 0.0D, 10000.0D);
        SUPERNOVA_COOLDOWN = builder.comment("Cooldown on Phoenix after Supernova ends the form.")
                .defineInRange("supernovaCooldownSeconds", 60.0D, 0.0D, 3600.0D);
        FADE_COOLDOWN = builder.comment("Cooldown on Phoenix after the fire runs out on its own.")
                .defineInRange("fadeCooldownSeconds", 30.0D, 0.0D, 3600.0D);
        REBIRTH = builder.comment("Dying with a full Phoenix meter brings you back already burning. Shares the cheat-death cooldown.")
                .define("rebirth", true);
        builder.pop();
        builder.push("rogue");
        ROGUE_HOLD_TICKS = builder.defineInRange("borrowedLiftTicks", 40, 1, 400);
        ROGUE_RANGE = builder.defineInRange("borrowedLiftRange", 12.0D, 1.0D, 64.0D);
        ROGUE_DAMAGE_MULTIPLIER = builder.comment("Damage when the borrowed lift ends, as a fraction of Magneto's magneticGripThrowDamage.")
                .defineInRange("borrowedLiftDamageMultiplier", 0.35D, 0.0D, 10.0D);
        ROGUE_COOLDOWN = builder.defineInRange("borrowedLiftCooldownSeconds", 10.0D, 0.0D, 600.0D);
        builder.pop();
        builder.pop();
    }
}
