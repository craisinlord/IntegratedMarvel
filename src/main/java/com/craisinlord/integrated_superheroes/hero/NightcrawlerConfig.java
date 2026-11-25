package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class NightcrawlerConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue BAMF_RANGE;
    public static ModConfigSpec.IntValue BAMF_CHARGES;
    public static ModConfigSpec.DoubleValue BAMF_RECHARGE_SECONDS;
    public static ModConfigSpec.IntValue LANDING_GRACE_TICKS;
    public static ModConfigSpec.DoubleValue LANDING_GRACE_DAMAGE;
    public static ModConfigSpec.DoubleValue BALLET_RANGE;
    public static ModConfigSpec.IntValue BALLET_TARGETS;
    public static ModConfigSpec.DoubleValue BALLET_POUNCE_MULTIPLIER;
    public static ModConfigSpec.IntValue BALLET_STEP_TICKS;
    public static ModConfigSpec.DoubleValue BALLET_COOLDOWN;
    public static ModConfigSpec.DoubleValue ROGUE_BAMF_RANGE;
    public static ModConfigSpec.DoubleValue ROGUE_BAMF_COOLDOWN;
    public static ModConfigSpec.IntValue LEAP_LEVEL;
    public static ModConfigSpec.DoubleValue PERCH_SHUFFLE;
    public static ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_DIMENSIONS;

    private NightcrawlerConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("nightcrawler");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 22.0D, 2.0D, 0.0D, 0.0D, 0.01D, 18.0D, 0,
                List.of("minecraft:generic.safe_fall_distance;6.0;ADD_VALUE"));
        BAMF_RANGE = builder.comment("Farthest a Bamf can take you, in blocks.")
                .defineInRange("bamfRange", 12.0D, 2.0D, 32.0D);
        BAMF_CHARGES = builder.comment("How many Bamf charges you can bank. Bamf Ballet needs all of them.")
                .defineInRange("bamfCharges", 3, 1, 10);
        BAMF_RECHARGE_SECONDS = builder.comment("Seconds for one charge to come back. Charges come back one at a time.")
                .defineInRange("bamfRechargeSeconds", 6.0D, 0.0D, 600.0D);
        LANDING_GRACE_TICKS = builder.comment("After a Bamf that leaves you in the air, a fall within this many ticks hurts less.")
                .defineInRange("landingGraceTicks", 30, 0, 200);
        LANDING_GRACE_DAMAGE = builder.comment("How much fall damage that landing grace soaks up, once.")
                .defineInRange("landingGraceDamage", 4.0D, 0.0D, 100.0D);
        BALLET_RANGE = builder.comment("Bamf Ballet picks targets you can see within this many blocks.")
                .defineInRange("balletRange", 10.0D, 2.0D, 32.0D);
        BALLET_TARGETS = builder.defineInRange("balletTargets", 3, 1, 6);
        BALLET_POUNCE_MULTIPLIER = builder.comment("Total Bamf Ballet damage is Panther Pounce damage times this, split evenly across the targets it picks.")
                .defineInRange("balletPounceMultiplier", 1.5D, 0.0D, 10.0D);
        BALLET_STEP_TICKS = builder.comment("Ticks between each strike of Bamf Ballet.")
                .defineInRange("balletStepTicks", 5, 1, 40);
        BALLET_COOLDOWN = builder.defineInRange("balletCooldownSeconds", 20.0D, 0.0D, 600.0D);
        ROGUE_BAMF_RANGE = builder.comment("Range of the single Bamf Rogue gets from absorbing Nightcrawler.")
                .defineInRange("rogueBamfRange", 6.0D, 2.0D, 32.0D);
        ROGUE_BAMF_COOLDOWN = builder.defineInRange("rogueBamfCooldownSeconds", 8.0D, 0.0D, 600.0D);
        LEAP_LEVEL = builder.comment("Jump Boost level while the Super Leap toggle is on.")
                .defineInRange("leapJumpLevel", 2, 1, 10);
        PERCH_SHUFFLE = builder.comment("How much sideways movement you keep while perched on a wall. 0 holds you still, 1 lets you move freely.")
                .defineInRange("perchShuffle", 0.5D, 0.0D, 1.0D);
        BLOCKED_DIMENSIONS = builder.comment("Dimensions where Bamf and Bamf Ballet don't work, like \"minecraft:the_end\".")
                .defineListAllowEmpty("blockedDimensions", List.of(), () -> "", entry -> entry instanceof String);
        builder.pop();
    }
}
