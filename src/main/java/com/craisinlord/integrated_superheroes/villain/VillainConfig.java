package com.craisinlord.integrated_superheroes.villain;

import net.neoforged.neoforge.common.ModConfigSpec;

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
