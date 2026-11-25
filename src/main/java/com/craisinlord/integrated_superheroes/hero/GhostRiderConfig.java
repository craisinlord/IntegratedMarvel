package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GhostRiderConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue CHAIN_RANGE;
    public static ModConfigSpec.DoubleValue CHAIN_DAMAGE_SCALE;
    public static ModConfigSpec.IntValue CHAIN_TETHER_TICKS;
    public static ModConfigSpec.DoubleValue CHAIN_BREAK_DISTANCE;
    public static ModConfigSpec.DoubleValue CHAIN_YANK_SPEED;
    public static ModConfigSpec.DoubleValue CHAIN_COOLDOWN;
    public static ModConfigSpec.DoubleValue CHAIN_MISS_COOLDOWN;
    public static ModConfigSpec.DoubleValue STARE_RANGE;
    public static ModConfigSpec.IntValue STARE_CHANNEL_TICKS;
    public static ModConfigSpec.DoubleValue STARE_BASE_SCALE;
    public static ModConfigSpec.DoubleValue STARE_STACK_SCALE;
    public static ModConfigSpec.DoubleValue STARE_MAX_SCALE;
    public static ModConfigSpec.DoubleValue STARE_CANCEL_DAMAGE;
    public static ModConfigSpec.IntValue STARE_RECOIL_TICKS;
    public static ModConfigSpec.DoubleValue STARE_COOLDOWN;
    public static ModConfigSpec.DoubleValue STARE_FAIL_COOLDOWN;
    public static ModConfigSpec.IntValue JUDGMENT_MAX;
    public static ModConfigSpec.IntValue JUDGMENT_AGGRESSION_TICKS;
    public static ModConfigSpec.IntValue JUDGMENT_EXPIRE_TICKS;
    public static ModConfigSpec.DoubleValue JUDGMENT_RADIUS;
    public static ModConfigSpec.DoubleValue BURN_DAMAGE;
    public static ModConfigSpec.IntValue BURN_TICKS;
    public static ModConfigSpec.IntValue HELLFIRE_INTERVAL_TICKS;
    public static ModConfigSpec.DoubleValue FIRE_RESISTANCE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue FIRE_IMMUNE_MULTIPLIER;
    public static ModConfigSpec.DoubleValue TRAIL_DAMAGE;
    public static ModConfigSpec.IntValue TRAIL_ZONE_TICKS;
    public static ModConfigSpec.DoubleValue TRAIL_RADIUS;
    public static ModConfigSpec.IntValue TRAIL_MAX_ZONES;
    public static ModConfigSpec.DoubleValue HORSE_HEALTH;
    public static ModConfigSpec.DoubleValue HORSE_SPEED;
    public static ModConfigSpec.DoubleValue HORSE_JUMP;
    public static ModConfigSpec.DoubleValue HORSE_LOST_COOLDOWN;
    public static ModConfigSpec.DoubleValue HORSE_DISMISS_COOLDOWN;

    private GhostRiderConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("ghostRider");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 42.0D, 6.0D, 0.5D, 0.0D, 0.0D, 35.0D, 1);
        CHAIN_RANGE = builder.defineInRange("hellchainRange", 10.0D, 2.0D, 32.0D);
        CHAIN_DAMAGE_SCALE = builder.comment("Hellchain damage as a share of Black Panther's Claw Swipe damage.")
                .defineInRange("hellchainDamageScale", 0.6D, 0.0D, 10.0D);
        CHAIN_TETHER_TICKS = builder.comment("How long an ordinary mob stays chained. Bosses, allies and players are never chained.")
                .defineInRange("hellchainTetherTicks", 40, 0, 400);
        CHAIN_BREAK_DISTANCE = builder.comment("The chain snaps past this distance or when anything solid gets in the way.")
                .defineInRange("hellchainBreakDistance", 14.0D, 2.0D, 64.0D);
        CHAIN_YANK_SPEED = builder.comment("Top speed of a chained mob when you press Hellchain again to yank it in.")
                .defineInRange("hellchainYankSpeed", 1.6D, 0.1D, 5.0D);
        CHAIN_COOLDOWN = builder.comment("Cooldown once the chain is yanked, snaps or runs out.")
                .defineInRange("hellchainCooldownSeconds", 8.0D, 0.0D, 600.0D);
        CHAIN_MISS_COOLDOWN = builder.defineInRange("hellchainMissCooldownSeconds", 2.0D, 0.0D, 600.0D);
        STARE_RANGE = builder.defineInRange("penanceStareRange", 8.0D, 2.0D, 32.0D);
        STARE_CHANNEL_TICKS = builder.comment("How long you have to hold the stare on your target.")
                .defineInRange("penanceStareChannelTicks", 20, 1, 200);
        STARE_BASE_SCALE = builder.comment("Penance Stare damage as a share of Black Panther's Pounce damage, before Judgment.")
                .defineInRange("penanceStareBaseScale", 0.4D, 0.0D, 10.0D);
        STARE_STACK_SCALE = builder.comment("Added share of Pounce damage for each Judgment stack consumed.")
                .defineInRange("penanceStareStackScale", 0.15D, 0.0D, 10.0D);
        STARE_MAX_SCALE = builder.defineInRange("penanceStareMaxScale", 1.15D, 0.0D, 10.0D);
        STARE_CANCEL_DAMAGE = builder.comment("Taking a single hit at least this big breaks the stare.")
                .defineInRange("penanceStareCancelDamage", 8.0D, 0.0D, 10000.0D);
        STARE_RECOIL_TICKS = builder.comment("Ordinary mobs reel for this long after a Penance Stare. Players and bosses never do.")
                .defineInRange("penanceStareRecoilTicks", 12, 0, 200);
        STARE_COOLDOWN = builder.defineInRange("penanceStareCooldownSeconds", 25.0D, 0.0D, 600.0D);
        STARE_FAIL_COOLDOWN = builder.comment("Cooldown when the stare is broken before it finishes.")
                .defineInRange("penanceStareFailCooldownSeconds", 4.0D, 0.0D, 600.0D);
        JUDGMENT_MAX = builder.defineInRange("judgmentMaxStacks", 5, 1, 20);
        JUDGMENT_AGGRESSION_TICKS = builder.comment("An enemy that hurts you or a nearby ally earns at most one Judgment stack this often.")
                .defineInRange("judgmentAggressionTicks", 40, 1, 1200);
        JUDGMENT_EXPIRE_TICKS = builder.comment("Judgment on an enemy fades after this long without new stacks.")
                .defineInRange("judgmentExpireTicks", 240, 20, 12000);
        JUDGMENT_RADIUS = builder.comment("Allies inside this radius count for Judgment when an enemy hurts them.")
                .defineInRange("judgmentAllyRadius", 12.0D, 0.0D, 64.0D);
        BURN_DAMAGE = builder.comment("Damage of each hellfire pulse on something you punched.")
                .defineInRange("hellfireBurnDamage", 5.0D, 0.0D, 10000.0D);
        BURN_TICKS = builder.comment("How long a punch keeps hellfire burning on the target.")
                .defineInRange("hellfireBurnTicks", 60, 0, 1200);
        HELLFIRE_INTERVAL_TICKS = builder.comment("Each victim takes at most one hellfire hit from the same rider this often, however many flames overlap.")
                .defineInRange("hellfireIntervalTicks", 10, 1, 200);
        FIRE_RESISTANCE_MULTIPLIER = builder.comment("Hellfire damage against something with Fire Resistance.")
                .defineInRange("hellfireFireResistanceMultiplier", 0.5D, 0.0D, 1.0D);
        FIRE_IMMUNE_MULTIPLIER = builder.comment("Hellfire damage against fire-immune creatures.")
                .defineInRange("hellfireFireImmuneMultiplier", 0.5D, 0.0D, 1.0D);
        TRAIL_DAMAGE = builder.comment("Damage of the hellfire trail left while sprinting or riding.")
                .defineInRange("hellfireTrailDamage", 6.0D, 0.0D, 10000.0D);
        TRAIL_ZONE_TICKS = builder.defineInRange("hellfireTrailTicks", 60, 1, 1200);
        TRAIL_RADIUS = builder.defineInRange("hellfireTrailRadius", 1.2D, 0.2D, 8.0D);
        TRAIL_MAX_ZONES = builder.defineInRange("hellfireTrailMaxPatches", 24, 1, 256);
        HORSE_HEALTH = builder.defineInRange("hellrideHorseHealth", 40.0D, 1.0D, 1024.0D);
        HORSE_SPEED = builder.comment("Movement speed of the Hellride horse. A fast vanilla horse is about 0.34.")
                .defineInRange("hellrideHorseSpeed", 0.42D, 0.05D, 2.0D);
        HORSE_JUMP = builder.defineInRange("hellrideHorseJump", 0.9D, 0.1D, 2.0D);
        HORSE_LOST_COOLDOWN = builder.comment("Cooldown when the horse is destroyed.")
                .defineInRange("hellrideLostCooldownSeconds", 45.0D, 0.0D, 3600.0D);
        HORSE_DISMISS_COOLDOWN = builder.comment("Cooldown when you dismount or dismiss the horse.")
                .defineInRange("hellrideDismissCooldownSeconds", 10.0D, 0.0D, 3600.0D);
        builder.pop();
    }
}
