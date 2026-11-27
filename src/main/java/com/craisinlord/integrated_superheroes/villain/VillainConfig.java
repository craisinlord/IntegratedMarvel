package com.craisinlord.integrated_superheroes.villain;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class VillainConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.DoubleValue HEALTH_PER_EXTRA_PLAYER;
    public static final ModConfigSpec.DoubleValue LEASH_RADIUS;
    public static final Boss ZEMO;
    public static final Boss WINTER_SOLDIER;
    public static final Boss RED_SKULL;
    public static final ModConfigSpec.IntValue RED_BOOK_ALLY_SECONDS;
    public static final ModConfigSpec.DoubleValue TESSERACT_CHARGE_MULTIPLIER;
    public static final ModConfigSpec.DoubleValue AGENT_TESSERACT_CHARGE_MULTIPLIER;
    public static final ModConfigSpec.BooleanValue ARM_AGENTS;
    public static final ModConfigSpec.DoubleValue ALERT_RADIUS;
    public static final ModConfigSpec.DoubleValue ALARM_RADIUS;
    public static final ModConfigSpec.IntValue REINFORCEMENTS_PER_ALARM;
    public static final ModConfigSpec.DoubleValue GUN_DROP_CHANCE;
    public static final ModConfigSpec.BooleanValue HAIL_HYDRA_ENABLED;
    public static final ModConfigSpec.IntValue HAIL_HYDRA_SECONDS;
    public static final ModConfigSpec.IntValue HAIL_HYDRA_COOLDOWN_SECONDS;
    public static final ModConfigSpec.DoubleValue ROCKETEER_SAFETY_RADIUS;
    public static final ModConfigSpec.BooleanValue SNIPERS_HOLD;
    public static final ModConfigSpec.BooleanValue THEMATIC_VILLAINS;
    public static final ModConfigSpec.DoubleValue THEMATIC_LEASH_RADIUS;
    public static final ModConfigSpec.DoubleValue THEMATIC_PER_HIT_CAP;
    public static final ModConfigSpec.DoubleValue THEMATIC_DPS_CAP;
    public static final ModConfigSpec.DoubleValue THEMATIC_HEADLINER_DPS_CAP;
    public static final ModConfigSpec.DoubleValue THEMATIC_CAP_PER_EXTRA_PLAYER;
    public static final ModConfigSpec.BooleanValue ULTRON_CRADLE;
    public static final ModConfigSpec.BooleanValue SHAZAM_THRONE;
    public static final ModConfigSpec.BooleanValue FEAR_SHARD_DROPS;
    public static final ModConfigSpec.DoubleValue ULTRON_AFTER_PRIME_CHANCE;
    public static final ModConfigSpec.DoubleValue BLACK_ADAM_CHANCE;
    public static final ModConfigSpec.DoubleValue REVERSE_FLASH_CHANCE;
    public static final ModConfigSpec.IntValue LIVING_VILLAIN_COOLDOWN_MINUTES;
    public static final ModConfigSpec.IntValue BLACK_ADAM_COOLDOWN_MINUTES;
    public static final ModConfigSpec.IntValue ESCAPEE_COOLDOWN_MINUTES;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ESCAPEE_CAVE_DIMENSIONS;
    public static final ModConfigSpec.DoubleValue VILLAGE_CREW_CHANCE;
    public static final ModConfigSpec.DoubleValue STREET_CREW_CHANCE;
    public static final ModConfigSpec.DoubleValue JOKER_CHANCE;
    public static final ModConfigSpec.BooleanValue ESCAPEE_SPAWNS;
    public static final ModConfigSpec.DoubleValue ESCAPEE_CHANCE;
    public static final ModConfigSpec.DoubleValue KRYPTONIAN_BLOOD_CHANCE;
    public static final ModConfigSpec.DoubleValue MARTIAN_BLOOD_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALIEN_BLOOD_MOBS;
    public static final ModConfigSpec.DoubleValue ALIEN_BLOOD_CHANCE;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> ALIEN_BLOOD_SURE_MOBS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("villains");
        ENABLED = builder.comment("Fight logic for Baron Zemo, the Winter Soldier, Red Skull and Hydra agents.")
                .define("enabled", true);
        HEALTH_PER_EXTRA_PLAYER = builder.comment("Extra boss health per additional player nearby when the fight starts (0.6 = +60%).")
                .defineInRange("healthPerExtraPlayer", 0.6D, 0.0D, 10.0D);
        LEASH_RADIUS = builder.comment("Bosses stay within this many blocks of where their fight started.")
                .defineInRange("leashRadius", 18.0D, 6.0D, 128.0D);
        builder.pop();
        ZEMO = new Boss(builder, "zemo", 400.0D, 12.0D, 30.0D, 8.0D);
        WINTER_SOLDIER = new Boss(builder, "winter_soldier", 500.0D, 12.0D, 35.0D, 9.0D);
        builder.push("winter_soldier");
        RED_BOOK_ALLY_SECONDS = builder.comment("How long the Winter Soldier fights for the Red Book's reader before he breaks free.")
                .defineInRange("redBookAllySeconds", 300, 10, 3600);
        builder.pop();
        RED_SKULL = new Boss(builder, "red_skull", 600.0D, 14.0D, 40.0D, 9.0D);
        builder.push("red_skull");
        TESSERACT_CHARGE_MULTIPLIER = builder.comment("Damage multiplier for Red Skull's Tesseract charges (Marvel's base is 4).")
                .defineInRange("tesseractChargeMultiplier", 3.5D, 0.0D, 100.0D);
        AGENT_TESSERACT_CHARGE_MULTIPLIER = builder.comment("Damage multiplier for Tesseract charges fired by Hydra agents.")
                .defineInRange("agentTesseractChargeMultiplier", 2.0D, 0.0D, 100.0D);
        builder.pop();
        builder.push("hydra");
        ARM_AGENTS = builder.comment("Hydra agents get a squad role and a matching weapon (guns when Scorched Guns is installed).")
                .define("armWithGuns", true);
        ALERT_RADIUS = builder.comment("Agents within this range join in when one is hurt or spots a player.")
                .defineInRange("alertRadius", 24.0D, 4.0D, 128.0D);
        ALARM_RADIUS = builder.comment("Radius of an alarm: every agent and boss inside it converges on the intruder.")
                .defineInRange("alarmRadius", 48.0D, 8.0D, 256.0D);
        REINFORCEMENTS_PER_ALARM = builder.comment("Most agents officers can call in during one alarm.")
                .defineInRange("reinforcementsPerAlarm", 6, 0, 64);
        GUN_DROP_CHANCE = builder.comment("Chance an armed agent drops its weapon.")
                .defineInRange("gunDropChance", 0.03D, 0.0D, 1.0D);
        HAIL_HYDRA_ENABLED = builder.comment("Saying \"Hail Hydra\" in chat makes Hydra agents ignore you for a while. Bosses aren't fooled.")
                .define("hailHydra", true);
        HAIL_HYDRA_SECONDS = builder.comment("How long Hydra agents ignore you after you say it. Attacking a Hydra fighter ends it early.")
                .defineInRange("hailHydraSeconds", 300, 1, 86400);
        HAIL_HYDRA_COOLDOWN_SECONDS = builder.comment("Seconds after saying it before it works again.")
                .defineInRange("hailHydraCooldownSeconds", 900, 0, 86400);
        ROCKETEER_SAFETY_RADIUS = builder.comment("Rocketeers hold fire while another agent stands this close to their target.")
                .defineInRange("rocketeerSafetyRadius", 7.0D, 0.0D, 32.0D);
        SNIPERS_HOLD = builder.comment("Snipers hold their position instead of backing away while their target is far off.")
                .define("snipersHoldPosition", true);
        builder.pop();
        builder.comment("Living villains built on Thematic's mobs. Only used when Thematic is installed.").push("thematic_villains");
        THEMATIC_VILLAINS = builder.comment("Boss bars, damage caps, leashes, title cards, spawns and drops for Ultron, Black Adam, Reverse Flash,",
                        "the Phantom Zone Escapee, the Manhunter and the village crews of the Penguin and Two-Face.")
                .define("enabled", true);
        THEMATIC_LEASH_RADIUS = builder.comment("Living villains are pulled back if they get this far from where they appeared.")
                .defineInRange("leashRadius", 40.0D, 8.0D, 256.0D);
        THEMATIC_PER_HIT_CAP = builder.comment("Most damage one hit can deal to a living villain.")
                .defineInRange("perHitCap", 40.0D, 1.0D, 100000.0D);
        THEMATIC_DPS_CAP = builder.comment("Damage per second allowed over a rolling 5 seconds before further damage is cut to 10%.")
                .defineInRange("dpsCap", 10.0D, 0.5D, 100000.0D);
        THEMATIC_HEADLINER_DPS_CAP = builder.comment("dpsCap for Ultron and Black Adam.")
                .defineInRange("headlinerDpsCap", 25.0D, 0.5D, 100000.0D);
        THEMATIC_CAP_PER_EXTRA_PLAYER = builder.comment("Each extra player within 32 blocks raises the damage per second cap by this fraction.")
                .defineInRange("capPerExtraPlayer", 0.35D, 0.0D, 10.0D);
        ULTRON_CRADLE = builder.comment("Using the Mind Stone on an Ultron Regeneration Cradle wakes Ultron. He keeps the stone until he dies.")
                .define("ultronFromCradle", true);
        SHAZAM_THRONE = builder.comment("Using Shazam's Throne on the Engraved Lodestone in the Rock of Eternity gives a Shazam serum instead of the Book of Champions.",
                        "Each player can be chosen once per lodestone.")
                .define("shazamFromThrone", true);
        FEAR_SHARD_DROPS = builder.comment("A Warden killed by a player drops 2 to 4 Fear Shards, and Arkillo drops 3 to 5 alongside his arena reward.")
                .define("fearShardDrops", true);
        ULTRON_AFTER_PRIME_CHANCE = builder.comment("Chance Ultron also turns up when a player kills Sentinel Prime. 0 turns it off.")
                .defineInRange("ultronAfterPrimeChance", 0.0D, 0.0D, 1.0D);
        BLACK_ADAM_CHANCE = builder.comment("Chance per check (every 30 seconds, per player on the surface of an Overworld desert) that Black Adam appears.")
                .defineInRange("blackAdamChance", 0.004D, 0.0D, 1.0D);
        REVERSE_FLASH_CHANCE = builder.comment("Chance per check (every 30 seconds, per player outdoors in an Overworld thunderstorm) that Reverse Flash appears.")
                .defineInRange("reverseFlashChance", 0.02D, 0.0D, 1.0D);
        LIVING_VILLAIN_COOLDOWN_MINUTES = builder.comment("Real minutes before the same player can meet Reverse Flash or the Joker again.")
                .defineInRange("livingVillainCooldownMinutes", 120, 0, 100000);
        BLACK_ADAM_COOLDOWN_MINUTES = builder.comment("Real minutes before the same player can meet Black Adam again. Separate from the other living villains.")
                .defineInRange("blackAdamCooldownMinutes", 30, 0, 100000);
        ESCAPEE_COOLDOWN_MINUTES = builder.comment("Real minutes before the same player can meet another Phantom Zone Escapee. Separate from the other living villains.")
                .defineInRange("escapeeCooldownMinutes", 30, 0, 100000);
        VILLAGE_CREW_CHANCE = builder.comment("Chance per check (every 30 seconds, per player in an Overworld village at night) that the Penguin's or Two-Face's crew",
                        "starts a raid. Each player can only run each raid once every two hours.")
                .defineInRange("villageCrewChance", 0.01D, 0.0D, 1.0D);
        STREET_CREW_CHANCE = builder.comment("Chance per check (every 30 seconds, per player inside an Abandoned Street at night) that the Penguin's or Two-Face's crew",
                        "starts a raid there. Shares the per-raid two hour limit with village raids.")
                .defineInRange("streetCrewChance", 0.05D, 0.0D, 1.0D);
        JOKER_CHANCE = builder.comment("Chance per check (every 30 seconds, per player inside an Abandoned Street at night) that the Joker shows up.",
                        "Only for players who have beaten both the Penguin and Two-Face. Shares the living villain cooldown.")
                .defineInRange("jokerChance", 0.2D, 0.0D, 1.0D);
        ESCAPEE_SPAWNS = builder.comment("A Phantom Zone Escapee can crash down near players outdoors on clear Overworld nights, or in the escapeeCaveDimensions.")
                .define("escapeeSpawns", true);
        ESCAPEE_CHANCE = builder.comment("Chance per check (every 30 seconds, per player outdoors on a clear Overworld night) that a Phantom Zone Escapee appears.",
                        "Uses the Escapee cooldown.")
                .defineInRange("escapeeChance", 0.015D, 0.0D, 1.0D);
        ESCAPEE_CAVE_DIMENSIONS = builder.comment("Dimensions without open sky where a Phantom Zone Escapee can break through at any time, as dimension=chance per check",
                        "(every 30 seconds, per player). They land on solid ground near the player. Uses the Escapee cooldown.")
                .defineListAllowEmpty("escapeeCaveDimensions", List.of("antarchy:thoraxis=0.04"), () -> "", entry -> entry instanceof String);
        KRYPTONIAN_BLOOD_CHANCE = builder.comment("Chance a Phantom Zone Escapee killed by a player drops Kryptonian Blood.")
                .defineInRange("kryptonianBloodChance", 1.0D, 0.0D, 1.0D);
        MARTIAN_BLOOD_CHANCE = builder.comment("Chance a Manhunter killed by a player drops Martian Blood.")
                .defineInRange("martianBloodChance", 0.5D, 0.0D, 1.0D);
        ALIEN_BLOOD_MOBS = builder.comment("Gigeresque aliens that can drop Alien Blood when killed by a player.")
                .defineListAllowEmpty("alienBloodMobs", List.of("gigeresque:alien", "gigeresque:runner_alien", "gigeresque:aquatic_alien",
                        "gigeresque:hellmorph_runner", "gigeresque:spitter"), () -> "", entry -> entry instanceof String);
        ALIEN_BLOOD_CHANCE = builder.comment("Chance one of the alienBloodMobs drops Alien Blood.")
                .defineInRange("alienBloodChance", 0.25D, 0.0D, 1.0D);
        ALIEN_BLOOD_SURE_MOBS = builder.comment("Gigeresque aliens that always drop 1 or 2 Alien Blood when killed by a player.")
                .defineListAllowEmpty("alienBloodSureMobs", List.of("gigeresque:draconictemplebeast", "gigeresque:moonlighthorrortemplebeast",
                        "gigeresque:ravenoustemplebeast", "gigeresque:baphomorph"), () -> "", entry -> entry instanceof String);
        builder.pop();
        SPEC = builder.build();
    }

    private VillainConfig() {
    }

    public static boolean enabled() {
        return SPEC.isLoaded() && ENABLED.get();
    }

    public static final class Boss {
        public final ModConfigSpec.BooleanValue enabled;
        public final ModConfigSpec.DoubleValue health;
        public final ModConfigSpec.DoubleValue armor;
        public final ModConfigSpec.DoubleValue perHitCap;
        public final ModConfigSpec.DoubleValue dpsCap;

        Boss(ModConfigSpec.Builder builder, String name, double health, double armor, double perHitCap, double dpsCap) {
            builder.push(name);
            this.enabled = builder.define("enabled", true);
            this.health = builder.defineInRange("health", health, 1.0D, 100000.0D);
            this.armor = builder.defineInRange("armor", armor, 0.0D, 30.0D);
            this.perHitCap = builder.comment("Most damage one hit can deal.").defineInRange("perHitCap", perHitCap, 1.0D, 100000.0D);
            this.dpsCap = builder.comment("Damage per second allowed over a rolling 5 seconds before further damage is cut to 10%.")
                    .defineInRange("dpsCap", dpsCap, 0.5D, 100000.0D);
            builder.pop();
        }
    }
}
