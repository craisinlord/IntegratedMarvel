package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class GrootConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue ABILITY_DAMAGE;
    public static ModConfigSpec.DoubleValue BRANCH_RANGE;
    public static ModConfigSpec.DoubleValue BRANCH_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue BRANCH_COOLDOWN;
    public static ModConfigSpec.IntValue PULL_WINDOW_TICKS;
    public static ModConfigSpec.DoubleValue PULL_SPEED;
    public static ModConfigSpec.IntValue PULL_STUN_TICKS;
    public static ModConfigSpec.IntValue TAKE_ROOT_TICKS;
    public static ModConfigSpec.DoubleValue TAKE_ROOT_DAMAGE_TAKEN;
    public static ModConfigSpec.DoubleValue TAKE_ROOT_HEAL;
    public static ModConfigSpec.DoubleValue TAKE_ROOT_COOLDOWN;
    public static ModConfigSpec.DoubleValue TAKE_ROOT_LASH_RADIUS;
    public static ModConfigSpec.IntValue TAKE_ROOT_LASH_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue TAKE_ROOT_LASH_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue TAKE_ROOT_BRANCH_RANGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue THORN_BURST_RADIUS;
    public static ModConfigSpec.DoubleValue THORN_BURST_ROOTED_RADIUS;
    public static ModConfigSpec.DoubleValue THORN_BURST_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue THORN_BURST_KNOCKBACK;
    public static ModConfigSpec.DoubleValue THORN_BURST_COOLDOWN;
    public static ModConfigSpec.DoubleValue SKEWER_RANGE;
    public static ModConfigSpec.IntValue SKEWER_MAX_TARGETS;
    public static ModConfigSpec.IntValue SKEWER_PIN_TICKS;
    public static ModConfigSpec.DoubleValue SKEWER_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue SKEWER_COOLDOWN;
    public static ModConfigSpec.DoubleValue SKEWER_MISS_COOLDOWN;
    public static ModConfigSpec.DoubleValue GRAPPLE_RANGE;
    public static ModConfigSpec.IntValue GRAPPLE_TICKS;
    public static ModConfigSpec.DoubleValue GRAPPLE_SPEED;
    public static ModConfigSpec.DoubleValue GRAPPLE_COOLDOWN;
    public static ModConfigSpec.DoubleValue TRUNK_RANGE;
    public static ModConfigSpec.IntValue TRUNK_HEIGHT;
    public static ModConfigSpec.IntValue TRUNK_SECONDS;
    public static ModConfigSpec.DoubleValue TRUNK_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue TRUNK_LAUNCH;
    public static ModConfigSpec.DoubleValue TRUNK_COOLDOWN;
    public static ModConfigSpec.DoubleValue WALL_DISTANCE;
    public static ModConfigSpec.IntValue WALL_WIDTH;
    public static ModConfigSpec.IntValue WALL_HEIGHT;
    public static ModConfigSpec.IntValue WALL_SECONDS;
    public static ModConfigSpec.DoubleValue WALL_COOLDOWN;
    public static ModConfigSpec.DoubleValue SHELTER_RADIUS;
    public static ModConfigSpec.IntValue SHELTER_SECONDS;
    public static ModConfigSpec.DoubleValue SHELTER_COOLDOWN;
    public static ModConfigSpec.DoubleValue SPORE_RADIUS;
    public static ModConfigSpec.IntValue SPORE_GLOW_TICKS;
    public static ModConfigSpec.IntValue SPORE_LIGHTS;
    public static ModConfigSpec.IntValue SPORE_LIGHT_LEVEL;
    public static ModConfigSpec.IntValue SPORE_LIGHT_SECONDS;
    public static ModConfigSpec.DoubleValue SPORE_MARK_BONUS;
    public static ModConfigSpec.DoubleValue SPORE_COOLDOWN;
    public static ModConfigSpec.IntValue SUN_CALM_TICKS;
    public static ModConfigSpec.DoubleValue SUN_HEAL;
    public static ModConfigSpec.DoubleValue FIRE_DAMAGE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue ROGUE_DAMAGE_MULTIPLIER;

    private GrootConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("floraColossusGroot");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 24.0D, 3.0D, 0.3D, 0.5D, 0.0D, 14.0D, 0,
                List.of("minecraft:generic.scale;0.2;ADD_MULTIPLIED_BASE", "minecraft:generic.armor_toughness;2.0;ADD_VALUE"));
        ABILITY_DAMAGE = builder.comment("Base damage every Groot ability is a fraction of.")
                .defineInRange("abilityDamage", 50.0D, 0.0D, 10000.0D);
        BRANCH_RANGE = builder.comment("How far Branch Arm reaches.")
                .defineInRange("branchArmRange", 10.0D, 2.0D, 32.0D);
        BRANCH_DAMAGE_MULTIPLIER = builder.comment("Branch Arm damage as a fraction of Groot's abilityDamage.")
                .defineInRange("branchArmDamageMultiplier", 0.5D, 0.0D, 10.0D);
        BRANCH_COOLDOWN = builder.comment("Cooldown of Branch Arm. Starts after the pull, or when the pull window runs out.")
                .defineInRange("branchArmCooldownSeconds", 3.0D, 0.0D, 600.0D);
        PULL_WINDOW_TICKS = builder.comment("After Branch Arm hits an ordinary mob, pressing it again within this many ticks pulls the mob in.")
                .defineInRange("pullWindowTicks", 30, 1, 200);
        PULL_SPEED = builder.comment("Top speed of the pull. Mobs further away are pulled faster, up to this.")
                .defineInRange("pullSpeed", 1.6D, 0.2D, 4.0D);
        PULL_STUN_TICKS = builder.comment("How long a pulled mob is stunned once it lands near you.")
                .defineInRange("pullStunTicks", 10, 0, 100);
        TAKE_ROOT_TICKS = builder.comment("How long Take Root keeps you rooted.")
                .defineInRange("takeRootTicks", 120, 10, 600);
        TAKE_ROOT_DAMAGE_TAKEN = builder.comment("Fraction of incoming damage you still take while rooted.")
                .defineInRange("takeRootDamageTaken", 0.5D, 0.0D, 1.0D);
        TAKE_ROOT_HEAL = builder.comment("Health restored when Take Root runs its full length. Ending it early gives nothing.")
                .defineInRange("takeRootHeal", 6.0D, 0.0D, 1000.0D);
        TAKE_ROOT_COOLDOWN = builder.comment("Cooldown of Take Root. Starts when it ends.")
                .defineInRange("takeRootCooldownSeconds", 25.0D, 0.0D, 600.0D);
        TAKE_ROOT_LASH_RADIUS = builder.comment("While rooted, roots lash the nearest hostile mob within this many blocks.")
                .defineInRange("takeRootLashRadius", 6.0D, 1.0D, 24.0D);
        TAKE_ROOT_LASH_INTERVAL_TICKS = builder.comment("Ticks between root lashes while rooted.")
                .defineInRange("takeRootLashIntervalTicks", 20, 1, 200);
        TAKE_ROOT_LASH_DAMAGE_MULTIPLIER = builder.comment("Root lash damage as a fraction of Groot's abilityDamage.")
                .defineInRange("takeRootLashDamageMultiplier", 0.3D, 0.0D, 10.0D);
        TAKE_ROOT_BRANCH_RANGE_MULTIPLIER = builder.comment("Branch Arm range multiplier while rooted.")
                .defineInRange("takeRootBranchRangeMultiplier", 1.5D, 1.0D, 4.0D);
        THORN_BURST_RADIUS = builder.comment("Radius of Thorn Burst.")
                .defineInRange("thornBurstRadius", 4.0D, 1.0D, 16.0D);
        THORN_BURST_ROOTED_RADIUS = builder.comment("Radius of Thorn Burst while rooted.")
                .defineInRange("thornBurstRootedRadius", 6.0D, 1.0D, 16.0D);
        THORN_BURST_DAMAGE_MULTIPLIER = builder.comment("Thorn Burst damage as a fraction of Groot's abilityDamage.")
                .defineInRange("thornBurstDamageMultiplier", 0.5D, 0.0D, 10.0D);
        THORN_BURST_KNOCKBACK = builder.comment("Horizontal speed Thorn Burst throws ordinary mobs away at.")
                .defineInRange("thornBurstKnockback", 1.2D, 0.0D, 4.0D);
        THORN_BURST_COOLDOWN = builder.comment("Cooldown of Thorn Burst.")
                .defineInRange("thornBurstCooldownSeconds", 10.0D, 0.0D, 600.0D);
        SKEWER_RANGE = builder.comment("How far Skewer reaches.")
                .defineInRange("skewerRange", 12.0D, 2.0D, 32.0D);
        SKEWER_MAX_TARGETS = builder.comment("How many creatures one Skewer can pierce.")
                .defineInRange("skewerMaxTargets", 5, 1, 20);
        SKEWER_PIN_TICKS = builder.comment("How long ordinary mobs stay pinned before Skewer drags them in.")
                .defineInRange("skewerPinTicks", 30, 1, 200);
        SKEWER_DAMAGE_MULTIPLIER = builder.comment("Skewer damage per creature as a fraction of Groot's abilityDamage.")
                .defineInRange("skewerDamageMultiplier", 1.2D, 0.0D, 10.0D);
        SKEWER_COOLDOWN = builder.comment("Cooldown of Skewer after it hits something.")
                .defineInRange("skewerCooldownSeconds", 12.0D, 0.0D, 600.0D);
        SKEWER_MISS_COOLDOWN = builder.comment("Cooldown of Skewer when it hits nothing.")
                .defineInRange("skewerMissCooldownSeconds", 3.0D, 0.0D, 600.0D);
        GRAPPLE_RANGE = builder.comment("How far Root Grapple reaches.")
                .defineInRange("rootGrappleRange", 24.0D, 2.0D, 64.0D);
        GRAPPLE_TICKS = builder.comment("Longest a Root Grapple pull lasts.")
                .defineInRange("rootGrappleTicks", 30, 1, 200);
        GRAPPLE_SPEED = builder.comment("Speed Root Grapple pulls you at, in blocks per tick.")
                .defineInRange("rootGrappleSpeed", 1.2D, 0.2D, 4.0D);
        GRAPPLE_COOLDOWN = builder.comment("Cooldown of Root Grapple.")
                .defineInRange("rootGrappleCooldownSeconds", 6.0D, 0.0D, 600.0D);
        TRUNK_RANGE = builder.comment("How far away Trunk Surge can grow a trunk.")
                .defineInRange("trunkSurgeRange", 10.0D, 2.0D, 32.0D);
        TRUNK_HEIGHT = builder.comment("Height of a Trunk Surge column in blocks.")
                .defineInRange("trunkSurgeHeight", 4, 1, 12);
        TRUNK_SECONDS = builder.comment("How long Trunk Surge logs stay before they revert.")
                .defineInRange("trunkSurgeSeconds", 10, 1, 120);
        TRUNK_DAMAGE_MULTIPLIER = builder.comment("Trunk Surge damage as a fraction of Groot's abilityDamage.")
                .defineInRange("trunkSurgeDamageMultiplier", 0.4D, 0.0D, 10.0D);
        TRUNK_LAUNCH = builder.comment("Upward speed Trunk Surge launches ordinary mobs with.")
                .defineInRange("trunkSurgeLaunch", 1.1D, 0.0D, 4.0D);
        TRUNK_COOLDOWN = builder.comment("Cooldown of Trunk Surge.")
                .defineInRange("trunkSurgeCooldownSeconds", 8.0D, 0.0D, 600.0D);
        WALL_DISTANCE = builder.comment("How far ahead Branch Wall grows.")
                .defineInRange("branchWallDistance", 3.0D, 1.0D, 10.0D);
        WALL_WIDTH = builder.comment("Width of Branch Wall in blocks.")
                .defineInRange("branchWallWidth", 5, 1, 15);
        WALL_HEIGHT = builder.comment("Height of Branch Wall in blocks.")
                .defineInRange("branchWallHeight", 3, 1, 8);
        WALL_SECONDS = builder.comment("How long Branch Wall stays before it reverts.")
                .defineInRange("branchWallSeconds", 8, 1, 120);
        WALL_COOLDOWN = builder.comment("Cooldown of Branch Wall.")
                .defineInRange("branchWallCooldownSeconds", 15.0D, 0.0D, 600.0D);
        SHELTER_RADIUS = builder.comment("Radius of the Shelter dome.")
                .defineInRange("shelterRadius", 3.0D, 2.0D, 8.0D);
        SHELTER_SECONDS = builder.comment("How long Shelter stays before it reverts.")
                .defineInRange("shelterSeconds", 6, 1, 60);
        SHELTER_COOLDOWN = builder.comment("Cooldown of Shelter.")
                .defineInRange("shelterCooldownSeconds", 45.0D, 0.0D, 600.0D);
        SPORE_RADIUS = builder.comment("Radius of Spore Glow.")
                .defineInRange("sporeGlowRadius", 12.0D, 2.0D, 32.0D);
        SPORE_GLOW_TICKS = builder.comment("How long hostile mobs glow after Spore Glow.")
                .defineInRange("sporeGlowTicks", 200, 1, 1200);
        SPORE_LIGHTS = builder.comment("Most temporary light spots one Spore Glow leaves behind.")
                .defineInRange("sporeGlowLights", 12, 0, 48);
        SPORE_LIGHT_LEVEL = builder.comment("Light level of the spots Spore Glow leaves.")
                .defineInRange("sporeGlowLightLevel", 12, 1, 15);
        SPORE_LIGHT_SECONDS = builder.comment("How long Spore Glow's light spots last.")
                .defineInRange("sporeGlowLightSeconds", 30, 1, 600);
        SPORE_MARK_BONUS = builder.comment("Damage multiplier Groot's abilities deal to glowing targets.")
                .defineInRange("sporeGlowMarkBonus", 1.25D, 1.0D, 4.0D);
        SPORE_COOLDOWN = builder.comment("Cooldown of Spore Glow.")
                .defineInRange("sporeGlowCooldownSeconds", 20.0D, 0.0D, 600.0D);
        SUN_CALM_TICKS = builder.comment("Photosynthesis only heals after this many ticks without taking damage.")
                .defineInRange("photosynthesisCalmTicks", 100, 0, 1200);
        SUN_HEAL = builder.comment("Health Photosynthesis restores every 2 seconds under open sky in daylight or rain.")
                .defineInRange("photosynthesisHeal", 1.0D, 0.0D, 100.0D);
        FIRE_DAMAGE_MULTIPLIER = builder.comment("Multiplier on fire damage Groot takes. 1.0 turns the weakness off.")
                .defineInRange("fireDamageMultiplier", 1.5D, 0.0D, 10.0D);
        ROGUE_DAMAGE_MULTIPLIER = builder.comment("Rogue's borrowed Skewer damage per creature as a fraction of Groot's abilityDamage, before her memory multiplier.")
                .defineInRange("rogueSkewerDamageMultiplier", 0.6D, 0.0D, 10.0D);
        builder.pop();
    }
}
