package com.craisinlord.integrated_superheroes.suit;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class SuitAbilityConfig {
    public static ModConfigSpec.BooleanValue ENABLED;
    public static ModConfigSpec.BooleanValue MISSILES_TARGET_PLAYERS;
    public static ModConfigSpec.DoubleValue MISSILE_LOCK_RANGE;
    public static ModConfigSpec.DoubleValue MISSILE_CONE_RANGE;
    public static ModConfigSpec.DoubleValue MISSILE_CONE_DEGREES;
    public static ModConfigSpec.DoubleValue MISSILE_TURN_DEGREES;
    public static ModConfigSpec.DoubleValue MISSILE_SPLASH_RADIUS;

    public static Settings AFTERBURNER;
    public static ModConfigSpec.DoubleValue AFTERBURNER_SPEED;
    public static ModConfigSpec.IntValue AFTERBURNER_TICKS;

    public static Settings MICRO_MISSILES;
    public static ModConfigSpec.IntValue MICRO_MISSILES_MIN;
    public static ModConfigSpec.IntValue MICRO_MISSILES_MAX;

    public static Settings LANDING;
    public static ModConfigSpec.DoubleValue LANDING_PER_BLOCK;
    public static ModConfigSpec.DoubleValue LANDING_MAX_MULTIPLIER;
    public static ModConfigSpec.DoubleValue LANDING_RADIUS;

    public static Settings FLARES;
    public static ModConfigSpec.DoubleValue FLARES_RADIUS;

    public static Settings SHOULDER_ROCKET;
    public static Settings LASER_RING;
    public static ModConfigSpec.DoubleValue LASER_RING_RADIUS;

    public static Settings STEALTH;
    public static ModConfigSpec.IntValue STEALTH_TICKS;

    public static Settings OVERCHARGE;
    public static ModConfigSpec.DoubleValue OVERCHARGE_MAX_BONUS;

    public static Settings BRACE;
    public static ModConfigSpec.IntValue BRACE_TICKS;

    public static Settings HYDRAULIC_PUNCH;
    public static ModConfigSpec.IntValue HYDRAULIC_PUNCH_STUN_TICKS;
    public static ModConfigSpec.DoubleValue HYDRAULIC_PUNCH_MAX_HARDNESS;

    public static Settings GRAPPLE;
    public static ModConfigSpec.DoubleValue GRAPPLE_RANGE;

    public static Settings HEAVY_THROW;
    public static ModConfigSpec.IntValue HEAVY_THROW_HOLD_TICKS;

    public static Settings STARBOOST;
    public static ModConfigSpec.IntValue STARBOOST_TICKS;

    public static Settings ROCKET_PUNCH;

    public static Settings PROTON_CANNON;
    public static ModConfigSpec.DoubleValue PROTON_CANNON_RANGE;
    public static ModConfigSpec.IntValue PROTON_CANNON_TICKS;
    public static ModConfigSpec.IntValue PROTON_CANNON_HIT_INTERVAL;

    public static Settings GRENADE_LAUNCHER;
    public static ModConfigSpec.IntValue GRENADE_LAUNCHER_GRENADES;
    public static ModConfigSpec.DoubleValue GRENADE_LAUNCHER_RADIUS;

    public static Settings HELLFIRE_BOMB;

    public static ModConfigSpec.BooleanValue FLARES_FLASHBANG;

    public static Settings ORDNANCE_DROP;
    public static ModConfigSpec.IntValue ORDNANCE_DROP_MISSILES;
    public static ModConfigSpec.DoubleValue ORDNANCE_DROP_RADIUS;

    public static Settings RALLY;
    public static ModConfigSpec.DoubleValue RALLY_RADIUS;
    public static ModConfigSpec.IntValue RALLY_TICKS;

    public static Settings NUCLEAR_MISSILE;
    public static ModConfigSpec.IntValue NUCLEAR_IMMUNITY_TICKS;

    public static Settings CRYO_CHARGE;
    public static Settings FIRE_CHARGE;
    public static Settings ARC_CHARGE;
    public static ModConfigSpec.DoubleValue ARC_CHAIN_RANGE;

    public static Settings DEPTH_CHARGE;

    public static Settings MAGNET_PULSE;
    public static ModConfigSpec.DoubleValue MAGNET_PULSE_RADIUS;

    private SuitAbilityConfig() {
    }

    public static void define(ModConfigSpec.Builder builder) {
        builder.comment("Iron Man / War Machine suit abilities on Marvel Legacy's ability hotkeys, granted while a full suit is worn.",
                        "damageMultiplier is a multiple of the worn mark's repulsorDamage (Proton Cannon: of its unibeamDamage, per hit).",
                        "energyCost is out of the chestplate's 100.")
                .push("abilities");
        ENABLED = builder.comment("Master switch for suit abilities.").define("enabled", true);

        builder.push("missiles");
        MISSILES_TARGET_PLAYERS = builder.comment("Missiles may target other players (only when the server has PvP on, never teammates).")
                .define("targetPlayers", true);
        MISSILE_LOCK_RANGE = builder.comment("How far the crosshair lock-on reaches.").defineInRange("lockRange", 48.0D, 1.0D, 256.0D);
        MISSILE_CONE_RANGE = builder.comment("How far auto-targeting looks when nothing is under the crosshair.").defineInRange("coneRange", 32.0D, 1.0D, 256.0D);
        MISSILE_CONE_DEGREES = builder.comment("Half-angle of the auto-targeting cone.").defineInRange("coneDegrees", 35.0D, 1.0D, 180.0D);
        MISSILE_TURN_DEGREES = builder.comment("How sharply a micro-missile can turn per tick.").defineInRange("turnDegreesPerTick", 10.0D, 0.0D, 180.0D);
        MISSILE_SPLASH_RADIUS = builder.comment("Micro-missiles deal half damage to other valid targets within this radius.").defineInRange("splashRadius", 2.0D, 0.0D, 16.0D);
        builder.pop();

        builder.push("afterburner");
        AFTERBURNER = settings(builder, 4.0D, 5.0D, 1.0D);
        AFTERBURNER_SPEED = builder.defineInRange("speed", 2.2D, 0.0D, 10.0D);
        AFTERBURNER_TICKS = builder.comment("How long the dash damages mobs it passes through.").defineInRange("damageTicks", 8, 1, 100);
        builder.pop();

        builder.push("microMissiles");
        MICRO_MISSILES = settings(builder, 8.0D, 10.0D, 1.0D);
        MICRO_MISSILES_MIN = builder.comment("Missiles per volley on the first missile suit (Mk7).").defineInRange("minMissiles", 3, 1, 32);
        MICRO_MISSILES_MAX = builder.comment("Missiles per volley on the strongest suit (Mk43).").defineInRange("maxMissiles", 8, 1, 32);
        builder.pop();

        builder.push("superheroLanding");
        LANDING = settings(builder, 6.0D, 5.0D, 1.0D);
        LANDING_PER_BLOCK = builder.comment("Extra damage multiplier per block fallen.").defineInRange("multiplierPerBlock", 0.25D, 0.0D, 10.0D);
        LANDING_MAX_MULTIPLIER = builder.defineInRange("maxMultiplier", 4.0D, 0.0D, 100.0D);
        LANDING_RADIUS = builder.defineInRange("radius", 4.0D, 0.0D, 32.0D);
        builder.pop();

        builder.push("flares");
        FLARES = settings(builder, 12.0D, 5.0D, 0.0D);
        FLARES_RADIUS = builder.defineInRange("radius", 8.0D, 0.0D, 64.0D);
        FLARES_FLASHBANG = builder.comment("Also lob a Scorched Guns stun grenade ahead (suit helmets from Mk2 on are flash-proof).")
                .define("flashbang", true);
        builder.pop();

        builder.push("shoulderRocket");
        SHOULDER_ROCKET = settings(builder, 10.0D, 0.0D, 3.0D);
        builder.pop();

        builder.push("laserRing");
        LASER_RING = settings(builder, 25.0D, 15.0D, 4.0D);
        LASER_RING_RADIUS = builder.defineInRange("radius", 6.0D, 0.0D, 32.0D);
        builder.pop();

        builder.push("stealth");
        STEALTH = settings(builder, 30.0D, 10.0D, 0.0D);
        STEALTH_TICKS = builder.defineInRange("durationTicks", 160, 1, 6000);
        builder.pop();

        builder.push("overcharge");
        OVERCHARGE = settings(builder, 45.0D, 0.0D, 0.0D);
        OVERCHARGE_MAX_BONUS = builder.comment("Extra unibeam damage multiplier at a full 100 energy (scales down with less).")
                .defineInRange("maxBonusMultiplier", 2.0D, 0.0D, 100.0D);
        builder.pop();

        builder.push("brace");
        BRACE = settings(builder, 20.0D, 10.0D, 0.0D);
        BRACE_TICKS = builder.defineInRange("durationTicks", 80, 1, 6000);
        builder.pop();

        builder.push("hydraulicPunch");
        HYDRAULIC_PUNCH = settings(builder, 10.0D, 10.0D, 3.0D);
        HYDRAULIC_PUNCH_STUN_TICKS = builder.comment("Alex's Caves Stunned duration.").defineInRange("stunTicks", 40, 0, 1200);
        HYDRAULIC_PUNCH_MAX_HARDNESS = builder.comment("Blocks up to this hardness break when the superpowerGriefing gamerule is on.")
                .defineInRange("maxBlockHardness", 3.0D, 0.0D, 100.0D);
        builder.pop();

        builder.push("grapple");
        GRAPPLE = settings(builder, 15.0D, 5.0D, 0.0D);
        GRAPPLE_RANGE = builder.defineInRange("range", 20.0D, 1.0D, 64.0D);
        builder.pop();

        builder.push("heavyThrow");
        HEAVY_THROW = settings(builder, 12.0D, 10.0D, 2.0D);
        HEAVY_THROW_HOLD_TICKS = builder.comment("How long a grabbed mob is held before it's thrown automatically.").defineInRange("holdTicks", 60, 1, 600);
        builder.pop();

        builder.push("starboost");
        STARBOOST = settings(builder, 10.0D, 10.0D, 0.0D);
        STARBOOST_TICKS = builder.defineInRange("durationTicks", 40, 1, 600);
        builder.pop();

        builder.push("rocketPunch");
        ROCKET_PUNCH = settings(builder, 8.0D, 10.0D, 2.0D);
        builder.pop();

        builder.push("protonCannon");
        PROTON_CANNON = settings(builder, 30.0D, 30.0D, 0.4D);
        PROTON_CANNON_RANGE = builder.defineInRange("range", 48.0D, 1.0D, 256.0D);
        PROTON_CANNON_TICKS = builder.comment("How long the beam fires.").defineInRange("durationTicks", 50, 1, 600);
        PROTON_CANNON_HIT_INTERVAL = builder.comment("Ticks between damage pulses along the beam.").defineInRange("hitIntervalTicks", 5, 1, 100);
        builder.pop();

        builder.push("grenadeLauncher");
        GRENADE_LAUNCHER = settings(builder, 12.0D, 10.0D, 1.5D);
        GRENADE_LAUNCHER_GRENADES = builder.defineInRange("grenades", 3, 1, 16);
        GRENADE_LAUNCHER_RADIUS = builder.comment("Each Scorched Guns grenade's blast radius.").defineInRange("radius", 2.5D, 0.0D, 16.0D);
        builder.pop();

        builder.push("hellfireBomb");
        HELLFIRE_BOMB = settings(builder, 15.0D, 10.0D, 0.0D);
        builder.pop();

        builder.push("ordnanceDrop");
        ORDNANCE_DROP = settings(builder, 30.0D, 20.0D, 1.0D);
        ORDNANCE_DROP_MISSILES = builder.defineInRange("missiles", 6, 1, 32);
        ORDNANCE_DROP_RADIUS = builder.comment("Each missile's blast radius.").defineInRange("radius", 3.0D, 0.0D, 16.0D);
        builder.pop();

        builder.push("rally");
        RALLY = settings(builder, 40.0D, 15.0D, 0.0D);
        RALLY_RADIUS = builder.defineInRange("radius", 10.0D, 0.0D, 64.0D);
        RALLY_TICKS = builder.defineInRange("durationTicks", 200, 1, 6000);
        builder.pop();

        builder.push("nuclearMissile");
        NUCLEAR_MISSILE = settings(builder, 600.0D, 60.0D, 0.0D);
        NUCLEAR_IMMUNITY_TICKS = builder.comment("Irradiated immunity for whoever fires it.").defineInRange("firerImmunityTicks", 1200, 0, 72000);
        builder.pop();

        builder.push("cryoCharge");
        CRYO_CHARGE = settings(builder, 8.0D, 10.0D, 2.0D);
        builder.pop();
        builder.push("fireCharge");
        FIRE_CHARGE = settings(builder, 8.0D, 10.0D, 2.0D);
        builder.pop();
        builder.push("arcCharge");
        ARC_CHARGE = settings(builder, 8.0D, 10.0D, 2.0D);
        ARC_CHAIN_RANGE = builder.defineInRange("chainRange", 6.0D, 0.0D, 32.0D);
        builder.pop();

        builder.push("depthCharge");
        DEPTH_CHARGE = settings(builder, 10.0D, 10.0D, 0.0D);
        builder.pop();

        builder.push("magnetPulse");
        MAGNET_PULSE = settings(builder, 15.0D, 10.0D, 0.0D);
        MAGNET_PULSE_RADIUS = builder.defineInRange("radius", 12.0D, 0.0D, 64.0D);
        builder.pop();

        builder.pop();
    }

    private static Settings settings(ModConfigSpec.Builder builder, double cooldown, double energy, double damage) {
        return new Settings(
                builder.defineInRange("cooldownSeconds", cooldown, 0.0D, 3600.0D),
                builder.defineInRange("energyCost", energy, 0.0D, 100.0D),
                builder.defineInRange("damageMultiplier", damage, 0.0D, 1000.0D));
    }

    public record Settings(ModConfigSpec.DoubleValue cooldownSeconds, ModConfigSpec.DoubleValue energyCost,
                           ModConfigSpec.DoubleValue damageMultiplier) {
    }
}
