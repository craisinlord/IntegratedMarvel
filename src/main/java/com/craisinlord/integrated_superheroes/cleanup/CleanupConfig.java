package com.craisinlord.integrated_superheroes.cleanup;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class CleanupConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GAUNTLET_DUPLICATES;
    public static final ModConfigSpec.BooleanValue URU_MATERIALS;
    public static final ModConfigSpec.BooleanValue GAUNTLET_EXTRAS;
    public static final ModConfigSpec.BooleanValue TITANIUM;
    public static final ModConfigSpec.BooleanValue PALLADIUM;
    public static final ModConfigSpec.BooleanValue PROTO_ADAMANTIUM;
    public static final ModConfigSpec.BooleanValue HYDRA_BRICKS;
    public static final ModConfigSpec.BooleanValue LEGACY_MERGE;
    public static final ModConfigSpec.BooleanValue SUIT_ABILITY_ITEMS;
    public static final ModConfigSpec.BooleanValue UNIFIED_TAB;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Removed items are hidden from JEI and creative tabs, their recipes are dropped, they turn into their replacement",
                "(or vanish) in loot and inventories, and their ores stop generating. Changes apply after a restart.").push("removals");
        GAUNTLET_DUPLICATES = builder.comment("Infinity Gauntlet's own gauntlets and stones become this mod's.")
                .define("infinityGauntletDuplicates", true);
        URU_MATERIALS = builder.comment("Infinity Gauntlet's uru becomes Marvel's uru ingot; raw uru, uru ore, the uru star, the smithing template and the netherite stick are removed.")
                .define("uruMaterials", true);
        GAUNTLET_EXTRAS = builder.comment("Infinity Gauntlet's Mjolnir becomes Marvel's; the Cosmi Rods, Sceptre, Stone Tracker and Suit-Case are removed.")
                .define("infinityGauntletExtras", true);
        TITANIUM = builder.comment("Marvel's titanium becomes Antarchy titanium.")
                .define("titanium", true);
        PALLADIUM = builder.comment("Palladium ingots and blocks become netherite; the rest of palladium is removed. The Palladium Arc Reactor becomes the Netherite Arc Reactor.")
                .define("palladium", true);
        PROTO_ADAMANTIUM = builder.comment("Proto-adamantium becomes adamantium.")
                .define("protoAdamantium", true);
        HYDRA_BRICKS = builder.comment("Hydra bricks, slabs, stairs, walls and lamps are removed.")
                .define("hydraBricks", true);
        LEGACY_MERGE = builder.comment("Marvel's syringe, reinforced leather, web shooter and web fluid give way to Marvel Legacy's. Legacy's empty syringe draws villager blood.")
                .define("legacyMerge", true);
        SUIT_ABILITY_ITEMS = builder.comment("Hide the items suits hand out for their abilities (claws, blasts, repulsors, the drill and so on) from JEI and creative tabs.")
                .define("hideSuitAbilityItems", true);
        builder.pop();
        builder.push("creative");
        UNIFIED_TAB = builder.comment("Show Marvel, Marvel Legacy, Infinity Gauntlet and this mod in one creative tab, grouped by kind. JEI follows the same order.")
                .define("unifiedTab", true);
        builder.pop();
        SPEC = builder.build();
    }

    private CleanupConfig() {
    }

    static boolean on(ModConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() ? value.getDefault() : value.get();
    }
}
