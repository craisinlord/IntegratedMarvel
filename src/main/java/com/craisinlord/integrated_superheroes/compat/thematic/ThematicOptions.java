package com.craisinlord.integrated_superheroes.compat.thematic;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ThematicOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue DISABLE_STARTERS;
    public static final ModConfigSpec.BooleanValue NO_START_ITEMS;
    public static final ModConfigSpec.BooleanValue VANILLA_DEATH_MESSAGES;
    public static final ModConfigSpec.BooleanValue SHOW_NAME_TAGS;
    public static final ModConfigSpec.BooleanValue NO_CONSUMABLE_COOLDOWNS;
    public static final ModConfigSpec.BooleanValue NO_RESISTANCE_CAP;
    public static final ModConfigSpec.BooleanValue ALLOW_TRIPWIRE;
    public static final ModConfigSpec.BooleanValue VISUALS;
    public static final ModConfigSpec.IntValue FIXED_RARITY;
    public static final ModConfigSpec.BooleanValue QUANTUM_DIVE;
    public static final ModConfigSpec.IntValue QUANTUM_DIVE_TICKS;
    public static final ModConfigSpec.BooleanValue KEEP_OTHER_FLIGHT;
    public static final ModConfigSpec.BooleanValue QUIET_DEBUG_LOG;
    public static final ModConfigSpec.BooleanValue TRIM_MOB_SYNC;
    public static final ModConfigSpec.BooleanValue VANILLA_ARMOR;
    public static final ModConfigSpec.BooleanValue OTHER_ELYTRAS;
    public static final ModConfigSpec.BooleanValue VANILLA_EXPLOSION_KNOCKBACK;
    public static final ModConfigSpec.BooleanValue ATTACK_EVENTS;
    public static final ModConfigSpec.BooleanValue VANILLA_STATUS_EFFECTS;
    public static final ModConfigSpec.BooleanValue OTHER_SHIELDS;
    public static final ModConfigSpec.BooleanValue TRIM_EFFECT_BROADCASTS;
    public static final ModConfigSpec.BooleanValue OWN_CONTENT_ONLY;
    public static final ModConfigSpec.BooleanValue LEGACY_GUN_MODELS;
    public static final ModConfigSpec.BooleanValue LANTERN_CHARGE;
    public static final ModConfigSpec.BooleanValue CRIMINAL_SCORCHED_GUNS;
    public static final ModConfigSpec.IntValue DRACULA_VAMPIRE_LEVEL;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Only used when Thematic is installed.").push("onboarding");
        DISABLE_STARTERS = builder.comment("Turn off Thematic's starter character picker (Steve, Alex, Toby, Herobrine) and its /thematic starter command.",
                        "Operators can still use the command.")
                .define("disableStarters", true);
        NO_START_ITEMS = builder.comment("Don't hand out Thematic's first-join items (the Tablet and the Wallet).")
                .define("noStartItems", true);
        builder.pop();

        builder.push("rules");
        VANILLA_DEATH_MESSAGES = builder.comment("Keep vanilla death messages instead of Thematic's rewritten ones.")
                .define("vanillaDeathMessages", true);
        SHOW_NAME_TAGS = builder.comment("Show name tags at the normal distance instead of hiding them past a few blocks.")
                .define("showNameTags", true);
        NO_CONSUMABLE_COOLDOWNS = builder.comment("Turn off Thematic's potion and golden apple cooldowns (gamerule disableConsumableCooldowns).",
                        "The gamerules below are only changed while they still hold Thematic's default, so an admin's own setting sticks.")
                .define("noConsumableCooldowns", true);
        NO_RESISTANCE_CAP = builder.comment("Turn off Thematic's cap of Resistance III on every entity (gamerule maxResistanceAmplifier -1).")
                .define("noResistanceCap", false);
        ALLOW_TRIPWIRE = builder.comment("Let players place tripwire again (gamerule disableTripWire false).")
                .define("allowTripwire", true);
        KEEP_OTHER_FLIGHT = builder.comment("Stop Thematic from cancelling flight that comes from somewhere else (serum heroes, Iron Man, Viltrumite, creative-flight items).",
                        "Without this, Thematic takes away flight and resets fly speed every tick for any player who isn't wearing one of its flying suits.",
                        "Thematic's own grounding effects (Grounded, Frozen, Speed Stun, Reflex Slow) and Offline still apply.")
                .define("keepOtherFlight", true);
        OTHER_ELYTRAS = builder.comment("Let elytras from other mods work once a Thematic suit with gliding is loaded.",
                        "Also stops Thematic's elytra tick from running for anyone without a Thematic glider or flyer, which wore vanilla elytras out twice as fast.")
                .define("otherElytras", true);
        VANILLA_ARMOR = builder.comment("Apply vanilla armour, toughness and armour durability loss again. Thematic skips them for every entity, suited or not.",
                        "A Thematic suit's defence still divides the damage on top.")
                .define("vanillaArmor", true);
        VANILLA_EXPLOSION_KNOCKBACK = builder.comment("Keep vanilla explosion knockback. Thematic cuts the upward push of every explosion to about a tenth;",
                        "with this on that only happens for explosions caused by someone wearing a Thematic suit.")
                .define("vanillaExplosionKnockback", true);
        ATTACK_EVENTS = builder.comment("Fire the normal attack event before a suited player's punch, so protection, claims and frozen time can stop it.",
                        "Without this, Thematic's suit attacks skip every other mod's attack checks.")
                .define("attackEvents", true);
        VANILLA_STATUS_EFFECTS = builder.comment("Keep vanilla Regeneration, Poison, Wither and Instant Health for suited entities (gamerule oldStatusEffects true).",
                        "Thematic otherwise rewrites how they heal and hurt anyone in a suit.")
                .define("vanillaStatusEffects", true);
        OTHER_SHIELDS = builder.comment("Only let Thematic handle shield damage for its own shields. Other mods' shields take damage their own way again.")
                .define("otherShields", true);
        builder.pop();

        builder.push("suits");
        FIXED_RARITY = builder.comment("Every new suit gets this rarity instead of a random roll: 1 Common, 2 Uncommon, 3 Rare, 4 Epic, 5 Legendary.",
                        "Each step adds 2 to the suit's stats. Combining two identical suits in an anvil still upgrades them. 0 keeps Thematic's random roll.")
                .defineInRange("fixedRarity", 3, 0, 5);
        QUANTUM_DIVE = builder.comment("Players fully shrunk in a suit tagged integrated_superheroes:quantum_divers (Ant-Man, Wasp) who keep crouching",
                        "shrink past the subatomic and land in the Quantum Realm.")
                .define("quantumDive", true);
        QUANTUM_DIVE_TICKS = builder.comment("Ticks of crouching while fully shrunk before the dive.")
                .defineInRange("quantumDiveTicks", 100, 20, 1200);
        DRACULA_VAMPIRE_LEVEL = builder.comment("Witchery vampire level a player needs to wear Dracula and his variants. Needs Witchery; without it anyone can wear them.",
                        "0 lets anyone wear them.")
                .defineInRange("draculaVampireLevel", 5, 0, 10);
        builder.pop();

        builder.push("performance");
        QUIET_DEBUG_LOG = builder.comment("Raise Thematic's logger to INFO. Its debug lines (every ability tick, every entity created or saved) were most of debug.log.")
                .define("quietDebugLog", true);
        TRIM_MOB_SYNC = builder.comment("Only sync Thematic's speed and component data for players, their copies, Thematic's own mobs and mobs wearing a suit.",
                        "Plain mobs no longer send packets whenever their speed changes or a player starts tracking them.")
                .define("trimMobSync", true);
        TRIM_EFFECT_BROADCASTS = builder.comment("Only send Thematic's own status effects to every nearby player. Vanilla and other mods' effects go to the affected player only, as in vanilla.")
                .define("trimEffectBroadcasts", true);
        builder.pop();

        builder.push("safety");
        OWN_CONTENT_ONLY = builder.comment("Only let Thematic load items, blocks, entities, guns, suits, effects and professions from the thematic and integrated_superheroes data folders.",
                        "Thematic reads those folders from every namespace, so other mods' files with the same folder name (Scorched Guns' guns, for one) were read as Thematic content.")
                .define("ownContentOnly", true);
        builder.pop();

        builder.push("lanterns");
        LANTERN_CHARGE = builder.comment("Green and Yellow Lantern rings run on charge: moves, flight and the construct in hand spend it, each color refills its own way, and an empty ring loses its constructs and flight.",
                        "Off: rings refill on their own as Thematic made them.")
                .define("lanternCharge", true);
        builder.pop();

        builder.push("criminals");
        CRIMINAL_SCORCHED_GUNS = builder.comment("With Scorched Guns installed, Thematic's gun-carrying criminals (street crews, the strong tier, Penguin, Two-Face and HYDRA crews)",
                        "carry and fire a Scorched Guns gun that fits their role instead of a Thematic gun: pistols, SMGs, rifles, shotguns, a gatling, launchers.",
                        "Only affects criminals that spawn after the change.")
                .define("scorchedGuns", true);
        builder.pop();

        builder.push("visuals");
        VISUALS = builder.comment("Hero and suit abilities use Thematic's effects where one fits, instead of their own particles.")
                .define("thematicVisuals", true);
        LEGACY_GUN_MODELS = builder.comment("Load Thematic gun models that still use the old forge:separate_transforms model loader, through NeoForge's own loader. Needs a game restart.")
                .define("legacyGunModels", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ThematicOptions() {
    }

    public static int fixedRarity() {
        return !SPEC.isLoaded() ? FIXED_RARITY.getDefault() : FIXED_RARITY.get();
    }

    public static boolean on(ModConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() ? value.getDefault() : value.get();
    }
}
