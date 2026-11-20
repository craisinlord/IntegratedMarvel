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
    public static final ModConfigSpec.BooleanValue THEMATIC_MERGES;
    public static final ModConfigSpec.BooleanValue THEMATIC_EXTRAS;
    public static final ModConfigSpec.BooleanValue THEMATIC_SUIT_TABLE;
    public static final ModConfigSpec.BooleanValue THEMATIC_MONEY;
    public static final ModConfigSpec.BooleanValue THEMATIC_DUPLICATES;
    public static final ModConfigSpec.BooleanValue THEMATIC_TECH;
    public static final ModConfigSpec.BooleanValue THEMATIC_PACK_SWAPS;
    public static final ModConfigSpec.BooleanValue THEMATIC_DECOR;
    public static final ModConfigSpec.BooleanValue THEMATIC_BLOAT;
    public static final ModConfigSpec.BooleanValue UNIFIED_TAB;
    public static final ModConfigSpec.BooleanValue HIDE_VARIANTS;
    public static final ModConfigSpec.BooleanValue HIDE_SERUM_COSTUMES;

    public static final ModConfigSpec.BooleanValue CATALOG_ENABLED;
    public static final ModConfigSpec.BooleanValue HIDE_INERT_COSTUMES;
    public static final ModConfigSpec.BooleanValue HIDE_GENERATED_EQUIPMENT;
    public static final ModConfigSpec.BooleanValue HIDE_COMIC_COVERS;
    public static final ModConfigSpec.BooleanValue HIDE_UNCRAFTABLE_SUITS;
    public static final ModConfigSpec.BooleanValue HIDE_UNCRAFTABLE_WEAPONS;
    public static final ModConfigSpec.BooleanValue RETIRE_LEGACY_GADGETS;
    public static final ModConfigSpec.BooleanValue COSMETIC_MERGES;
    public static final ModConfigSpec.BooleanValue HIDE_UNUSED_STATIONS;
    public static final ModConfigSpec.BooleanValue ULTRON_REQUIRES_IRON_MAN;
    public static final ModConfigSpec.BooleanValue DISABLE_COSTUME_RECIPES;
    public static final ModConfigSpec.BooleanValue DISABLE_GENERATED_RECIPES;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> KEEP_SUITS;
    public static final ModConfigSpec.ConfigValue<java.util.List<? extends String>> EXTRA_RETIRED_SUITS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Removed items are hidden from JEI and creative tabs, their recipes are dropped, they turn into their replacement",
                "(or vanish) in loot and inventories, and their ores stop generating. Changes apply after a restart.").push("removals");
        GAUNTLET_DUPLICATES = builder.comment("Infinity Gauntlet's own gauntlets and stones become this mod's.")
                .define("infinityGauntletDuplicates", true);
        URU_MATERIALS = builder.comment("Infinity Gauntlet's uru becomes Marvel's uru ingot; raw uru, uru ore, the uru star, the smithing template and the netherite stick are removed.")
                .define("uruMaterials", true);
        GAUNTLET_EXTRAS = builder.comment("Infinity Gauntlet's Mjolnir becomes Marvel's and its Sceptre becomes the Mind Stone; the Cosmi Rods, Stone Tracker and Suit-Case are removed.")
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
        THEMATIC_MERGES = builder.comment("Thematic only: its vibranium, adamantium, carbonadium, gold-titanium, titanium, fabrics, reinforced leather, vials,",
                        "mutant genes and pym tech become the Marvel, Marvel Legacy or Antarchy versions. Silver becomes Ice and Fire silver",
                        "(iron without Ice and Fire) and electrum becomes Oritech electrum (gold without Oritech). Their Thematic ores stop generating.")
                .define("thematicMerges", true);
        THEMATIC_EXTRAS = builder.comment("Thematic only: cut its extra materials. Tin and crimson iron become copper; magnesium, dark iron and steel become iron;",
                        "amazonium becomes gold; promethium becomes Oritech prometheum (netherite without Oritech); krypt steel becomes netherite;",
                        "hardened steel becomes titanium; kevlar becomes black cloth; peridot becomes emerald; ruby, scarlet emerald, blood diamond",
                        "and ruby quartz become Antarchy blood crystal (redstone without Antarchy, and their ores stop); sapphire becomes Antarchy",
                        "bluestone (lapis without Antarchy); nth metal becomes Antarchy antimetal (iron without Antarchy); infernal fossil becomes",
                        "Nether Expansion fossil fuel (blaze powder without it); thunder-infused rock and electrolytic solution become Lightning Shards;",
                        "concentrated carbon becomes a coal block; white dwarf star alloy becomes uru; dark quartz becomes quartz; fibers become string;",
                        "plates become the ingot they're pressed from (the Plate Press, Alloy Smelter and Centrifuge are retired).",
                        "The Leather Jacket becomes a black leather chestplate, dog tags a name tag, the magnifying glass and batnoculars a spyglass,",
                        "the voice changer a note block, the blueprint paper, and alien blood vials and splatters become Alien Blood.",
                        "Limestone becomes diorite and slate becomes deepslate. The diamond cube, bat wing, mysterious acid, gamma sample, Cyclops",
                        "visors, ancient relic, plasma ball, lobster thermidor, cosmic debris, the unused emotion shards, the blue, rose and smoky",
                        "quartz families, fiber clusters and the shapes of every cut material are removed. Compound V is made with diamond blocks.",
                        "Thematic's starter suits still charge diamond cubes and reinforced leather in their tier costs, so they stay off.")
                .define("thematicExtras", true);
        THEMATIC_SUIT_TABLE = builder.comment("Thematic only: its suits are made at Marvel's Suit Table and reskinned in the table's Variants tab.",
                        "Thematic's Suit Bench and its recipes are retired, and the Book of Champions is removed.")
                .define("thematicSuitTable", true);
        THEMATIC_MONEY = builder.comment("Thematic only: its cash (bills, money stacks, wallets, the cash register and the Lexcoin, Queencoin and Waynecoin) is removed.",
                        "Criminals stop dropping cash.")
                .define("thematicMoney", true);
        THEMATIC_DUPLICATES = builder.comment("Thematic only: its copies of things Marvel already has are removed. Its two Mjolnirs become Marvel's, its",
                        "Captain America shield becomes the vibranium shield and Deadpool's katana becomes Marvel's. The Entombed Mjolnir,",
                        "the Iron Man helmet, the Hydra spawn eggs and its Thor, Iron Man, War Machine, Ant-Man, Wasp, Winter Soldier and",
                        "Killmonger suits are removed, since Marvel has its own.")
                .define("thematicDuplicates", true);
        THEMATIC_TECH = builder.comment("Thematic and Oritech only: Thematic's tech parts become Oritech components. Technology becomes the Processing Unit;",
                        "Advanced Technology, the Basic A.I. Chip and Wayne, Queen and Stark Tech become the Advanced Computing Engine;",
                        "Luthor Tech and Alien Tech become the Super A.I. Chip; carbon fiber becomes Carbon Fibre Strands.")
                .define("thematicTech", true);
        THEMATIC_PACK_SWAPS = builder.comment("Thematic only, off by default: flavour swaps onto other mods' materials. With Alex's Caves, kryptonite becomes uranium",
                        "(its ores stop), filled ooze canisters become toxic paste (empty canisters are removed and ooze lakes stop) and the radioactive",
                        "barrel becomes a waste drum. With the Immersive Vehicles official pack, the car wheel becomes a medium wheel. With Ice and Fire,",
                        "the Atlantean plate becomes blue sea serpent scales and the nth feather becomes a stymphalian bird feather. With Pastel, cosmic",
                        "debris becomes star fragments and its block becomes a stardust block. The chili pot and the storage network's data cores,",
                        "upgrade cards, terminal link and construction projector are removed.")
                .define("thematicPackSwaps", false);
        THEMATIC_DECOR = builder.comment("Thematic only, off by default: its decor that another installed mod already has becomes that mod's block. Tuff blocks become",
                        "vanilla's; basalt and calcite bricks, tiles and polished blocks become Pastel's; soul sandstone, the nether brick pillar and",
                        "netherite slabs and stairs become Nether Expansion's; plain terracotta slabs and stairs and the driftwood set become Dimensional",
                        "Doors'; plain wallpapers, frosted and dirty glass, barbed wire, the desk bell and life preserver become Abyssal Decor's; steel",
                        "doors and trapdoors become Malisis Doors'; candelabras, candlesticks, the blackboard, globe and clock become Supplementaries';",
                        "kitchen, bathroom, lamps, ceiling lights, couches and wooden chairs, tables and crates become Refurbished Furniture's; TVs become",
                        "Vista's; the willow set becomes Nature's Spirit's; concrete and wool slabs and stairs become VanillaBackport's. The soul brazier,",
                        "soul lamp post, creeping schist, ammo bench and arcane workbench are removed, and criminal spawn eggs and technical blocks are hidden.")
                .define("thematicDecor", false);
        THEMATIC_BLOAT = builder.comment("Thematic only: removes its filler blocks. Its storage network (Batcomputer, Holo Console, Core Array, Satellite Uplink,",
                        "Signal Relay, drives, ports, conduits and cards), the Quantum Tunnel, the Police Box, the Ghostbusters sign and soul, the Jedi and",
                        "Sith archives and holocrons, the Star Wars logo, every movie poster, neon lights and pillars, cement, frosted, dirty and borderless",
                        "glass, the coloured quartz families, ore and metal slabs, stairs and walls, concrete, wool, terracotta and shingle slabs and stairs,",
                        "shingles, plank panels and pillars, the charred and driftwood sets, brick, sandy brick, obsidian, crying obsidian and dripstone",
                        "variants, and the holiday and horror decorations. Swaps from the other cleanup groups still win over these removals.")
                .define("thematicBloat", true);
        builder.pop();
        builder.push("creative");
        UNIFIED_TAB = builder.comment("Show Marvel, Marvel Legacy, Infinity Gauntlet and this mod in one creative tab, grouped by kind. JEI follows the same order.")
                .define("unifiedTab", true);
        HIDE_VARIANTS = builder.comment("Hide suits that can only be made by swapping looks at the Suit Table, and their pieces, from JEI and EMI.",
                        "They stay in the creative tab and in the Suit Table's Variants tab.")
                .define("hideVariants", true);
        HIDE_SERUM_COSTUMES = builder.comment("Hide serum heroes' costume suits from JEI and EMI. Every one of them is a look you can pick with /heroskin.",
                        "They still exist and can still be crafted.")
                .define("hideSerumCostumes", true);
        builder.pop();
        builder.comment("Catalog cleanup only hides entries and disables recipes; existing inventory items are never removed. Restart after changes.").push("catalog");
        CATALOG_ENABLED = builder.comment("Master switch for catalog cleanup, including the cosmetic/serum viewer filters below creative. False restores the previous recipes and entries.")
                .define("enabled", true);
        HIDE_INERT_COSTUMES = builder.comment("Hide reviewed cosmetic or unsupported suits, including every component-based armor piece.")
                .define("hideInertCostumes", true);
        HIDE_GENERATED_EQUIPMENT = builder.comment("Hide Sub-Zero's axe, hammer and sword, and the reviewed Lantern constructs. Crafted gadgets and arrows stay visible.")
                .define("hideGeneratedEquipment", true);
        ULTRON_REQUIRES_IRON_MAN = builder.define("ultronRequiresIronMan", true);
        COSMETIC_MERGES = builder.define("cosmeticMerges", true);
        HIDE_UNUSED_STATIONS = builder.define("hideUnusedStations", true);
        RETIRE_LEGACY_GADGETS = builder
                .define("retireLegacyGadgets", true);
        HIDE_UNCRAFTABLE_WEAPONS = builder
                .define("hideUncraftableWeapons", true);
        HIDE_COMIC_COVERS = builder.comment("Hide decorative comic cover block items. Does not affect guides or their recipes.")
                .define("hideComicCovers", true);
        HIDE_UNCRAFTABLE_SUITS = builder.comment("Hide every Thematic suit that isn't in the integrated_superheroes:craftable_suits tag. Looks, skins and retired suits stay wearable through their commands.")
                .define("hideUncraftableSuits", true);
        DISABLE_COSTUME_RECIPES = builder.comment("Disable recipes producing reviewed cosmetic/inert suits and their armor pieces. False restores these recipes independently of JEI visibility.")
                .define("disableCostumeRecipes", true);
        DISABLE_GENERATED_RECIPES = builder.comment("Disable direct crafting of ability-generated equipment. Powers can still create it.")
                .define("disableGeneratedEquipmentRecipes", true);
        KEEP_SUITS = builder.comment("Exact suit IDs excluded from catalog retirement and cosmetic viewer filtering. Add suits here when developing abilities. Also protects their armor pieces.")
                .defineListAllowEmpty("keepSuits", java.util.List.of(),
                        value -> value instanceof String text && net.minecraft.resources.ResourceLocation.tryParse(text) != null);
        EXTRA_RETIRED_SUITS = builder.comment("Additional exact suit IDs to hide and disable recipes for; keepSuits takes priority. Functional Steel and Talia are not retired by default.")
                .defineListAllowEmpty("extraRetiredSuits", java.util.List.<String>of(),
                        value -> value instanceof String text && net.minecraft.resources.ResourceLocation.tryParse(text) != null);
        builder.pop();
        SPEC = builder.build();
    }

    private CleanupConfig() {
    }

    public static boolean hideVariants() {
        return CatalogCleanup.enabled() && on(HIDE_VARIANTS);
    }

    public static boolean hideSerumCostumes() {
        return CatalogCleanup.enabled() && on(HIDE_SERUM_COSTUMES);
    }

    public static java.util.List<? extends String> strings(ModConfigSpec.ConfigValue<java.util.List<? extends String>> value) {
        return !SPEC.isLoaded() ? value.getDefault() : value.get();
    }

    static boolean on(ModConfigSpec.BooleanValue value) {
        return !SPEC.isLoaded() ? value.getDefault() : value.get();
    }
}
