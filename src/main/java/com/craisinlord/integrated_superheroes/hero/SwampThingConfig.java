package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class SwampThingConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue ENTANGLE_RANGE;
    public static ModConfigSpec.DoubleValue ENTANGLE_RADIUS;
    public static ModConfigSpec.DoubleValue ENTANGLE_DAMAGE;
    public static ModConfigSpec.IntValue ENTANGLE_BIND_TICKS;
    public static ModConfigSpec.IntValue ENTANGLE_BIND_IMMUNITY_TICKS;
    public static ModConfigSpec.IntValue ENTANGLE_SLOW_TICKS;
    public static ModConfigSpec.IntValue ENTANGLE_SLOW_LEVEL;
    public static ModConfigSpec.DoubleValue ENTANGLE_COOLDOWN;
    public static ModConfigSpec.DoubleValue ROOT_STEP_RANGE;
    public static ModConfigSpec.DoubleValue ROOT_STEP_COOLDOWN;
    public static ModConfigSpec.ConfigValue<List<? extends String>> ROOT_STEP_BLOCKED_DIMENSIONS;
    public static ModConfigSpec.DoubleValue MENDING_HEAL;
    public static ModConfigSpec.IntValue MENDING_INTERVAL_TICKS;
    public static ModConfigSpec.IntValue MENDING_COMBAT_PAUSE_TICKS;
    public static ModConfigSpec.DoubleValue ROGUE_RADIUS;
    public static ModConfigSpec.DoubleValue ROGUE_DAMAGE;
    public static ModConfigSpec.DoubleValue ROGUE_COOLDOWN;

    private SwampThingConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("swampThing");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 24.0D, 3.0D, 0.3D, 0.0D, 0.0D, 14.0D, 0,
                List.of("minecraft:generic.knockback_resistance;0.3;ADD_VALUE", "minecraft:generic.movement_speed;-0.05;ADD_MULTIPLIED_TOTAL"));
        ENTANGLE_RANGE = builder.comment("How far away you can aim Entangle at the ground.")
                .defineInRange("entangleRange", 16.0D, 2.0D, 48.0D);
        ENTANGLE_RADIUS = builder.comment("Radius of the patch of roots Entangle raises.")
                .defineInRange("entangleRadius", 3.5D, 1.0D, 12.0D);
        ENTANGLE_DAMAGE = builder.comment("Entangle damage per creature.")
                .defineInRange("entangleDamage", 55.0D, 0.0D, 10000.0D);
        ENTANGLE_BIND_TICKS = builder.comment("How long ordinary creatures are held in place by the roots.")
                .defineInRange("entangleBindTicks", 20, 0, 200);
        ENTANGLE_BIND_IMMUNITY_TICKS = builder.comment("After being bound, no Swamp Thing roots can bind that creature again for this long. Slowness still applies.")
                .defineInRange("entangleBindImmunityTicks", 80, 0, 1200);
        ENTANGLE_SLOW_TICKS = builder.defineInRange("entangleSlowTicks", 60, 0, 600);
        ENTANGLE_SLOW_LEVEL = builder.comment("Slowness level on ordinary creatures. Bosses get one level less.")
                .defineInRange("entangleSlowLevel", 2, 1, 10);
        ENTANGLE_COOLDOWN = builder.defineInRange("entangleCooldownSeconds", 12.0D, 0.0D, 600.0D);
        ROOT_STEP_RANGE = builder.comment("Farthest Root Step can carry you, in blocks.")
                .defineInRange("rootStepRange", 16.0D, 2.0D, 48.0D);
        ROOT_STEP_COOLDOWN = builder.defineInRange("rootStepCooldownSeconds", 12.0D, 0.0D, 600.0D);
        ROOT_STEP_BLOCKED_DIMENSIONS = builder.comment("Dimensions where Root Step doesn't work, like \"minecraft:the_end\".")
                .defineListAllowEmpty("rootStepBlockedDimensions", List.of(), () -> "", entry -> entry instanceof String);
        MENDING_HEAL = builder.comment("Health restored each pulse while standing on natural ground with Rooted Mending on.")
                .defineInRange("mendingHeal", 1.0D, 0.0D, 100.0D);
        MENDING_INTERVAL_TICKS = builder.defineInRange("mendingIntervalTicks", 40, 1, 1200);
        MENDING_COMBAT_PAUSE_TICKS = builder.comment("Rooted Mending stops for this long after you take damage.")
                .defineInRange("mendingCombatPauseTicks", 100, 0, 1200);
        builder.push("rogue");
        ROGUE_RADIUS = builder.defineInRange("borrowedRootPulseRadius", 3.0D, 1.0D, 12.0D);
        ROGUE_DAMAGE = builder.comment("Borrowed root pulse damage per creature, before Rogue's memory multiplier.")
                .defineInRange("borrowedRootPulseDamage", 40.0D, 0.0D, 10000.0D);
        ROGUE_COOLDOWN = builder.defineInRange("borrowedRootPulseCooldownSeconds", 14.0D, 0.0D, 600.0D);
        builder.pop();
        builder.pop();
    }
}
