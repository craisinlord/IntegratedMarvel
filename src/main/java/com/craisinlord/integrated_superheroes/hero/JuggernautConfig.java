package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class JuggernautConfig {
    public static MarvelBalanceConfig.HeroStats JUGGERNAUT;
    public static ModConfigSpec.DoubleValue CHARGE_MAX_SPEED;
    public static ModConfigSpec.IntValue CHARGE_ACCELERATION_TICKS;
    public static ModConfigSpec.IntValue CHARGE_MAX_TICKS;
    public static ModConfigSpec.DoubleValue CHARGE_TURN_DEGREES;
    public static ModConfigSpec.DoubleValue CHARGE_FULL_MOMENTUM_TURN_DEGREES;
    public static ModConfigSpec.DoubleValue CHARGE_MIN_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue CHARGE_MAX_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.IntValue CHARGE_MAX_TARGETS_PER_TICK;
    public static ModConfigSpec.DoubleValue CHARGE_MAX_DAMAGE_PER_CAST;
    public static ModConfigSpec.DoubleValue CHARGE_KNOCKBACK;
    public static ModConfigSpec.DoubleValue CHARGE_COOLDOWN;
    public static ModConfigSpec.DoubleValue MOMENTUM_PER_BLOCK;
    public static ModConfigSpec.DoubleValue MOMENTUM_PER_HIT;
    public static ModConfigSpec.DoubleValue IMPACT_SPEED_FRACTION;
    public static ModConfigSpec.DoubleValue IMPACT_SHOCKWAVE_DAMAGE;
    public static ModConfigSpec.IntValue DIG_IN_BRAKE_TICKS;
    public static ModConfigSpec.IntValue DIG_IN_TICKS;
    public static ModConfigSpec.DoubleValue DIG_IN_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue DIG_IN_ARC_DEGREES;
    public static ModConfigSpec.DoubleValue SHOCK_MIN_MOMENTUM;
    public static ModConfigSpec.DoubleValue SHOCK_DAMAGE;
    public static ModConfigSpec.IntValue SHOCK_LENGTH;
    public static ModConfigSpec.DoubleValue SHOCK_KNOCKBACK;
    public static ModConfigSpec.DoubleValue SHOCK_COOLDOWN;
    public static ModConfigSpec.BooleanValue BREACH_ALLOWED;
    public static ModConfigSpec.DoubleValue BREACH_MIN_MOMENTUM;
    public static ModConfigSpec.DoubleValue BREACH_MAX_HARDNESS;
    public static ModConfigSpec.IntValue BREACH_BLOCKS_PER_TICK;
    public static ModConfigSpec.IntValue BREACH_BLOCKS_PER_CHARGE;

    private JuggernautConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("juggernaut");
        JUGGERNAUT = new MarvelBalanceConfig.HeroStats(builder, 74.0D, 7.0D, 2.0D, 0.5D, 0.0D, 65.0D, 1,
                List.of("minecraft:generic.scale;0.25;ADD_MULTIPLIED_BASE", "minecraft:generic.armor_toughness;8.0;ADD_VALUE",
                        "minecraft:generic.knockback_resistance;0.5;ADD_VALUE", "minecraft:generic.safe_fall_distance;10.0;ADD_VALUE"));
        CHARGE_MAX_SPEED = builder.comment("Top speed of Unstoppable Charge in blocks per tick.")
                .defineInRange("chargeMaxSpeed", 1.0D, 0.2D, 2.0D);
        CHARGE_ACCELERATION_TICKS = builder.comment("Ticks to reach top speed.")
                .defineInRange("chargeAccelerationTicks", 30, 1, 200);
        CHARGE_MAX_TICKS = builder.comment("Longest a charge can last.")
                .defineInRange("chargeMaxTicks", 100, 10, 600);
        CHARGE_TURN_DEGREES = builder.comment("Most the charge can turn per tick with no momentum.")
                .defineInRange("chargeTurnDegrees", 8.0D, 0.0D, 90.0D);
        CHARGE_FULL_MOMENTUM_TURN_DEGREES = builder.comment("Most the charge can turn per tick at full momentum.")
                .defineInRange("chargeFullMomentumTurnDegrees", 3.0D, 0.0D, 90.0D);
        CHARGE_MIN_DAMAGE_MULTIPLIER = builder.comment("Charge damage at zero momentum, as a multiple of Colossus' steelChargeDamage.")
                .defineInRange("chargeMinDamageMultiplier", 0.5D, 0.0D, 10.0D);
        CHARGE_MAX_DAMAGE_MULTIPLIER = builder.comment("Charge damage at full momentum, as a multiple of Colossus' steelChargeDamage.")
                .defineInRange("chargeMaxDamageMultiplier", 1.8D, 0.0D, 10.0D);
        CHARGE_MAX_TARGETS_PER_TICK = builder.defineInRange("chargeMaxTargetsPerTick", 6, 1, 64);
        CHARGE_MAX_DAMAGE_PER_CAST = builder.comment("Total damage one charge can deal across every creature it hits.")
                .defineInRange("chargeMaxDamagePerCharge", 600.0D, 0.0D, 100000.0D);
        CHARGE_KNOCKBACK = builder.defineInRange("chargeKnockback", 1.6D, 0.0D, 10.0D);
        CHARGE_COOLDOWN = builder.comment("Cooldown after a charge ends.")
                .defineInRange("chargeCooldownSeconds", 10.0D, 0.0D, 600.0D);
        MOMENTUM_PER_BLOCK = builder.comment("Momentum gained per block of grounded travel while charging. Momentum runs 0 to 100.")
                .defineInRange("momentumPerBlock", 7.0D, 0.0D, 100.0D);
        MOMENTUM_PER_HIT = builder.comment("Momentum lost for each creature the charge runs through.")
                .defineInRange("momentumLostPerHit", 4.0D, 0.0D, 100.0D);
        IMPACT_SPEED_FRACTION = builder.comment("Hitting a wall at this fraction of top speed or faster ends the charge with a shockwave.")
                .defineInRange("impactSpeedFraction", 0.6D, 0.0D, 1.0D);
        IMPACT_SHOCKWAVE_DAMAGE = builder.defineInRange("impactShockwaveDamage", 20.0D, 0.0D, 10000.0D);
        DIG_IN_BRAKE_TICKS = builder.comment("Pressing Unstoppable Charge again mid-charge brakes for this long, then digs in.")
                .defineInRange("digInBrakeTicks", 8, 1, 100);
        DIG_IN_TICKS = builder.comment("How long you stay dug in after braking.")
                .defineInRange("digInTicks", 30, 1, 400);
        DIG_IN_DAMAGE_MULTIPLIER = builder.comment("Damage taken from the front while dug in. Attacks that ignore armor or invulnerability are not reduced.")
                .defineInRange("digInFrontDamageMultiplier", 0.5D, 0.0D, 1.0D);
        DIG_IN_ARC_DEGREES = builder.comment("Half-angle of the front covered while dug in.")
                .defineInRange("digInFrontHalfAngleDegrees", 70.0D, 0.0D, 180.0D);
        SHOCK_MIN_MOMENTUM = builder.comment("Momentum needed for Crimson Shock. It spends all of it.")
                .defineInRange("crimsonShockMinMomentum", 50.0D, 0.0D, 100.0D);
        SHOCK_DAMAGE = builder.comment("Crimson Shock damage at full momentum. Scales down with the momentum spent.")
                .defineInRange("crimsonShockDamage", 45.0D, 0.0D, 10000.0D);
        SHOCK_LENGTH = builder.comment("How far the Crimson Shock wave travels at full momentum.")
                .defineInRange("crimsonShockLength", 10, 2, 32);
        SHOCK_KNOCKBACK = builder.defineInRange("crimsonShockKnockback", 1.6D, 0.0D, 10.0D);
        SHOCK_COOLDOWN = builder.defineInRange("crimsonShockCooldownSeconds", 18.0D, 0.0D, 600.0D);
        BREACH_ALLOWED = builder.comment("Lets the Breach Terrain toggle break blocks in the charge's path. Only blocks in the",
                        "integrated_superheroes:juggernaut_breachable tag break, they never drop, and the superpower griefing game rule must also be on.")
                .define("breachTerrainAllowed", true);
        BREACH_MIN_MOMENTUM = builder.comment("Momentum needed before the charge can break through a block.")
                .defineInRange("breachMinMomentum", 50.0D, 0.0D, 100.0D);
        BREACH_MAX_HARDNESS = builder.comment("Tagged blocks harder than this still stop the charge.")
                .defineInRange("breachMaxHardness", 3.0D, 0.0D, 50.0D);
        BREACH_BLOCKS_PER_TICK = builder.defineInRange("breachBlocksPerTick", 8, 0, 64);
        BREACH_BLOCKS_PER_CHARGE = builder.defineInRange("breachBlocksPerCharge", 64, 0, 1024);
        builder.pop();
    }
}
