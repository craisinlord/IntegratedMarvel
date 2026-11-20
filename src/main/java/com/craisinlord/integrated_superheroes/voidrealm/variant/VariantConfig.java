package com.craisinlord.integrated_superheroes.voidrealm.variant;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class VariantConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> TEST_DIMENSIONS;
    public static final ModConfigSpec.IntValue TEST_TIER;
    public static final ModConfigSpec.IntValue CHECK_TICKS;
    public static final ModConfigSpec.DoubleValue SPAWN_CHANCE;
    public static final ModConfigSpec.DoubleValue SPAWN_CHANCE_PER_TIER;
    public static final ModConfigSpec.DoubleValue SPAWN_MIN_DISTANCE;
    public static final ModConfigSpec.DoubleValue SPAWN_MAX_DISTANCE;
    public static final ModConfigSpec.IntValue MAX_LOADED;
    public static final ModConfigSpec.IntValue MAX_NEAR_PLAYER;
    public static final ModConfigSpec.DoubleValue NEAR_PLAYER_RADIUS;
    public static final ModConfigSpec.IntValue BROOD_EXTRA;
    public static final ModConfigSpec.IntValue LONELY_SECONDS;
    public static final ModConfigSpec.DoubleValue HEALTH_PER_TIER;
    public static final ModConfigSpec.DoubleValue DAMAGE_PER_TIER;
    public static final ModConfigSpec.DoubleValue DROP_CHANCE;
    public static final ModConfigSpec.DoubleValue DROP_CHANCE_PER_TIER;
    public static final ModConfigSpec.DoubleValue LOOTING_BONUS;
    public static final ModConfigSpec.BooleanValue FIGHT_EACH_OTHER;
    public static final ModConfigSpec.DoubleValue RIVAL_RADIUS;
    public static final ModConfigSpec.DoubleValue PLAYER_FOCUS_RADIUS;
    public static final ModConfigSpec.IntValue FALLBACK_GAP_TICKS;
    public static final ModConfigSpec.DoubleValue TEAR_CHANCE;
    public static final ModConfigSpec.DoubleValue NEXUS_CHANCE;
    public static final ModConfigSpec.IntValue NEXUS_MIN_TIER;
    public static final ModConfigSpec.IntValue NEXUS_COOLDOWN_MINUTES;
    public static final ModConfigSpec.IntValue NEXUS_DURATION_SECONDS;
    public static final ModConfigSpec.DoubleValue ESCAPE_CHANCE;
    public static final ModConfigSpec.BooleanValue TIER_LOCKS;
    public static final ModConfigSpec.ConfigValue<List<? extends Double>> RARITY_BY_GATE;
    public static final ModConfigSpec.BooleanValue FULL_POWER;
    public static final ModConfigSpec.BooleanValue NEUTRAL_HEROES;
    public static final ModConfigSpec.IntValue GRUDGE_SECONDS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("variants");
        ENABLED = builder.comment("Void Variants spawn around players in the Void and in the test dimensions below.")
                .define("enabled", true);
        TEST_DIMENSIONS = builder.comment("Extra dimensions where Variants spawn, for testing before the Void exists (e.g. minecraft:overworld).")
                .defineListAllowEmpty("testDimensions", List.of(), () -> "minecraft:overworld", value -> value instanceof String);
        TEST_TIER = builder.comment("Tier used in the test dimensions, and anywhere the Void reports no tier.")
                .defineInRange("testTier", 0, 0, 5);
        CHECK_TICKS = builder.comment("Ticks between spawn checks per player.")
                .defineInRange("checkTicks", 40, 20, 12000);
        SPAWN_CHANCE = builder.defineInRange("spawnChance", 0.6D, 0.0D, 1.0D);
        SPAWN_CHANCE_PER_TIER = builder.defineInRange("spawnChancePerTier", 0.05D, 0.0D, 1.0D);
        SPAWN_MIN_DISTANCE = builder.defineInRange("spawnMinDistance", 24.0D, 4.0D, 128.0D);
        SPAWN_MAX_DISTANCE = builder.defineInRange("spawnMaxDistance", 48.0D, 8.0D, 160.0D);
        MAX_LOADED = builder.comment("Most Variants loaded in one dimension.")
                .defineInRange("maxLoadedVariants", 24, 0, 512);
        MAX_NEAR_PLAYER = builder.comment("Most Variants near one player before the spawner stops adding more.")
                .defineInRange("maxVariantsNearPlayer", 7, 0, 64);
        NEAR_PLAYER_RADIUS = builder.defineInRange("nearPlayerRadius", 64.0D, 8.0D, 256.0D);
        BROOD_EXTRA = builder.comment("A storm's brood may go this far over maxLoadedVariants.")
                .defineInRange("broodExtra", 8, 0, 128);
        LONELY_SECONDS = builder.comment("A Variant with no player within 128 blocks for this long despawns.")
                .defineInRange("lonelySeconds", 120, 10, 3600);
        TIER_LOCKS = builder.comment("Families only spawn once the Void tier reaches their gate. Off: every family spawns everywhere, stronger ones rarer.")
                .define("tierLocks", false);
        RARITY_BY_GATE = builder.comment("With tierLocks off, a family's spawn weight is multiplied by the entry for its gate (0 to 3).")
                .defineList("rarityByGate", List.of(1.0D, 0.6D, 0.35D, 0.2D), () -> 1.0D, value -> value instanceof Double d && d >= 0.0D);
        FULL_POWER = builder.comment("Every Variant gets the top tier's health and damage. Drop chances still follow the Void tier.")
                .define("fullPower", true);
        NEUTRAL_HEROES = builder.comment("Hero Variants leave players alone until a player hurts them. Villains always attack.")
                .define("neutralHeroes", true);
        GRUDGE_SECONDS = builder.comment("How long a hurt hero Variant keeps going after that player.")
                .defineInRange("grudgeSeconds", 60, 5, 3600);
        HEALTH_PER_TIER = builder.defineInRange("healthPerTier", 0.35D, 0.0D, 10.0D);
        DAMAGE_PER_TIER = builder.defineInRange("damagePerTier", 0.2D, 0.0D, 10.0D);
        DROP_CHANCE = builder.comment("Chance a Variant killed by a player drops its suit, piece or serum.")
                .defineInRange("dropChance", 0.03D, 0.0D, 1.0D);
        DROP_CHANCE_PER_TIER = builder.defineInRange("dropChancePerTier", 0.014D, 0.0D, 1.0D);
        LOOTING_BONUS = builder.comment("Extra drop chance per Looting level.")
                .defineInRange("lootingBonus", 0.0D, 0.0D, 1.0D);
        FIGHT_EACH_OTHER = builder.comment("Hero and villain Variants attack each other.")
                .define("fightEachOther", true);
        RIVAL_RADIUS = builder.defineInRange("rivalRadius", 16.0D, 2.0D, 64.0D);
        PLAYER_FOCUS_RADIUS = builder.comment("A Variant keeps attacking a player closer than this even with a rival nearby.")
                .defineInRange("playerFocusRadius", 8.0D, 0.0D, 64.0D);
        FALLBACK_GAP_TICKS = builder.comment("Ticks between a Variant's own fallback moves.")
                .defineInRange("fallbackGapTicks", 60, 10, 1200);
        TEAR_CHANCE = builder.comment("Chance a spawned Variant arrives through a dimensional tear instead of just appearing.")
                .defineInRange("tearChance", 0.85D, 0.0D, 1.0D);
        NEXUS_CHANCE = builder.comment("Chance per spawn check that a nexus tear opens near the player and lets out a brood in waves.")
                .defineInRange("nexusChance", 0.0015D, 0.0D, 1.0D);
        NEXUS_MIN_TIER = builder.comment("Lowest tier where nexus tears can open.")
                .defineInRange("nexusMinTier", 3, 0, 5);
        NEXUS_COOLDOWN_MINUTES = builder.comment("Minutes before another nexus can open near the same player.")
                .defineInRange("nexusCooldownMinutes", 10, 0, 1440);
        NEXUS_DURATION_SECONDS = builder.comment("Seconds a nexus tear stays open.")
                .defineInRange("nexusDurationSeconds", 60, 15, 600);
        ESCAPE_CHANCE = builder.comment("Chance a Variant below 20% health, recently hurt by a player, runs for a tear and escapes without drops. Rolled once per Variant.")
                .defineInRange("escapeChance", 0.35D, 0.0D, 1.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private VariantConfig() {
    }

    public static boolean ready() {
        return SPEC.isLoaded();
    }
}
