package com.craisinlord.integrated_superheroes.cleanup;

import com.craisinlord.integrated_superheroes.compat.ModCompat;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class Cleanup {
    private static final List<Group> GROUPS = new ArrayList<>();
    private static volatile Active active;

    static {
        Group defaults = group(null);
        defaults.dropRecipe("marvel:adamantium_ingot");
        defaults.dropRecipe("marvellegacy:syringe_spider_powers_peter_parker");
        defaults.dropRecipe("marvellegacy:syringe_weapon_x_logan");
        for (String hero : List.of("spider_man", "wolverine")) {
            for (String slot : List.of("helmet", "chestplate", "leggings", "boots")) {
                defaults.dropRecipe("marvel:" + hero + "_" + slot);
                defaults.dropRecipe("marvel:" + hero + "_" + slot + "_repairing");
            }
        }

        Group gauntlet = group(CleanupConfig.GAUNTLET_DUPLICATES);
        for (String id : List.of("infinity_gauntlet_item", "g_0", "g_6", "gb_0", "gb_6", "fg_6", "gauntletbase_full")) {
            gauntlet.replace("infinity_gauntlet:" + id, "integrated_superheroes:infinity_gauntlet");
        }
        for (String id : List.of("nano_gauntlet_item", "ng_0", "ng_6", "ngb_0", "ngb_6")) {
            gauntlet.replace("infinity_gauntlet:" + id, "integrated_superheroes:nano_gauntlet");
        }
        for (String stone : List.of("power", "space", "reality", "soul", "time", "mind")) {
            gauntlet.replace("infinity_gauntlet:" + stone, "integrated_superheroes:" + stone + "_stone");
        }
        gauntlet.replace("marvel:space_stone", "integrated_superheroes:space_stone");

        Group uru = group(CleanupConfig.URU_MATERIALS);
        uru.replace("infinity_gauntlet:uru_ingot", "marvel:uru_ingot");
        uru.remove("infinity_gauntlet:raw_uru", "infinity_gauntlet:uru_ore", "infinity_gauntlet:uru_star",
                "infinity_gauntlet:smithinggauntlet", "infinity_gauntlet:smithing_gauntlet", "infinity_gauntlet:netherite_stick");
        uru.feature("infinity_gauntlet:uru_ore");

        Group extras = group(CleanupConfig.GAUNTLET_EXTRAS);
        for (String id : List.of("mjolnir", "mjolnir_hammer", "mjolnir_strike")) {
            extras.replace("infinity_gauntlet:" + id, "marvel:mjolnir");
        }
        extras.replace("infinity_gauntlet:the_sceptre", "integrated_superheroes:mind_stone");
        extras.remove("infinity_gauntlet:cosmi_rod", "infinity_gauntlet:infinity_stone_tracker",
                "infinity_gauntlet:infinity_stone_case");
        for (String stone : List.of("power", "space", "reality", "soul", "time", "mind")) {
            extras.remove("infinity_gauntlet:cosmi_rod_" + stone);
        }
        extras.feature("infinity_gauntlet:mjolnir_structure_feature");

        if (ModCompat.ANTARCHY) {
            Group titanium = group(CleanupConfig.TITANIUM);
            for (String id : List.of("titanium_ingot", "raw_titanium", "titanium_nugget", "titanium_block", "raw_titanium_block",
                    "titanium_ore", "deepslate_titanium_ore")) {
                titanium.replace("marvel:" + id, "antarchy:" + id);
            }
            titanium.feature("marvel:ore_titanium");
        }

        Group palladium = group(CleanupConfig.PALLADIUM);
        palladium.replace("marvel:palladium_ingot", "minecraft:netherite_ingot");
        palladium.replace("marvel:palladium_block", "minecraft:netherite_block");
        palladium.remove("marvel:palladium_nugget", "marvel:raw_palladium", "marvel:raw_palladium_block",
                "marvel:palladium_ore", "marvel:deepslate_palladium_ore");
        palladium.tag("c:ingots/palladium", "c:ingots/netherite");
        palladium.tag("c:storage_blocks/palladium", "c:storage_blocks/netherite");
        palladium.feature("marvel:ore_palladium");
        palladium.rename("marvel:palladium_arc_reactor", "item.integrated_superheroes.netherite_arc_reactor");

        Group proto = group(CleanupConfig.PROTO_ADAMANTIUM);
        for (String form : List.of("ingot", "nugget", "block")) {
            proto.replace("marvel:proto_adamantium_" + form, "marvel:adamantium_" + form);
        }
        proto.tag("c:ingots/proto_adamantium", "c:ingots/adamantium");
        proto.tag("c:nuggets/proto_adamantium", "c:nuggets/adamantium");
        proto.tag("c:storage_blocks/proto_adamantium", "c:storage_blocks/adamantium");

        Group hydra = group(CleanupConfig.HYDRA_BRICKS);
        for (String color : List.of("gray", "light_gray", "green", "yellow")) {
            hydra.remove("marvel:" + color + "_hydra_bricks", "marvel:" + color + "_hydra_brick_slab", "marvel:" + color + "_hydra_brick_stairs",
                    "marvel:" + color + "_hydra_brick_wall", "marvel:" + color + "_hydra_brick_lamp");
        }

        Group legacy = group(CleanupConfig.LEGACY_MERGE);
        legacy.replace("marvel:syringe", "marvellegacy:syringe_empty");
        legacy.replace("marvel:reinforced_leather", "marvellegacy:cloth_black");
        legacy.retire("marvel:web_shooter", "marvel:web_fluid", "marvellegacy:tailoring_bench_block", "marvellegacy:sewing_kit");
        for (String hero : List.of("spider_man", "spider_man_mcu", "miles_morales_suit", "spider_gwen", "spider_man_insomniac",
                "wolverine", "wolverine_brown", "wolverine_x_force")) {
            for (String slot : List.of("helmet", "chestplate", "leggings", "boots", "sleeveless_chestplate")) {
                legacy.dropRecipe("marvel:" + hero + "_" + slot + "_from_variants");
            }
        }

        Group abilities = group(CleanupConfig.SUIT_ABILITY_ITEMS);
        abilities.retire("marvel:adamantium_claws", "marvel:disaster_rescue_claws", "marvel:optic_blast", "marvel:sonic_boom",
                "marvel:photon_blast", "marvel:repulsor", "marvel:unibeam", "marvel:flamethrower", "marvel:grow", "marvel:shrink",
                "marvel:sting", "marvel:shoulder_turret", "marvel:mining_drill", "marvel:centurion_blade");

        thematicBloat(group(CleanupConfig.THEMATIC_BLOAT));
        thematicMerges(group(CleanupConfig.THEMATIC_MERGES));
        thematicExtras(group(CleanupConfig.THEMATIC_EXTRAS));
        Group suitTable = group(CleanupConfig.THEMATIC_SUIT_TABLE);
        suitTable.retire("thematic:suit_bench");
        suitTable.dropType("thematic:suit_bench");
        suitTable.remove("thematic:book_of_champions");
        suitTable.dropRecipe("thematic:joker_venom");
        if (ModCompat.ORITECH) {
            Group tech = group(CleanupConfig.THEMATIC_TECH);
            tech.replace("thematic:technology", "oritech:processing_unit");
            for (String id : List.of("advanced_technology", "future_tech", "basic_ai_chip", "wayne_tech", "queen_tech", "stark_tech")) {
                tech.replace("thematic:" + id, "oritech:advanced_computing_engine");
            }
            tech.replace("thematic:luthor_tech", "oritech:super_ai_chip");
            tech.replace("thematic:alien_tech", "oritech:super_ai_chip");
            tech.replace("thematic:carbon_fiber", "oritech:carbon_fibre_strands");
        }
        Group duplicates = group(CleanupConfig.THEMATIC_DUPLICATES);
        duplicates.dropRecipe("thematic:ultron_chip");
        duplicates.replace("thematic:mjolnir", "marvel:mjolnir");
        duplicates.replace("thematic:thor_hammer", "marvel:mjolnir");
        duplicates.replace("thematic:cap_shield", "marvel:vibranium_shield");
        duplicates.replace("thematic:deadpool_katana", "marvel:katanas");
        duplicates.remove("thematic:mjolnirblock", "thematic:iron_man_helmet", "thematic:thor", "thematic:iron_man", "thematic:war_machine",
                "thematic:ant_man", "thematic:wasp", "thematic:winter_soldier", "thematic:killmonger", "thematic:hydra_soldier_spawn_egg");
        for (String role : List.of("sniper", "pistol", "smg", "rifle", "auto_rifle", "rocket_launcher", "grenade_launcher", "lmg", "shotgun")) {
            duplicates.remove("thematic:hydra_" + role + "_spawn_egg");
        }
        for (String suit : List.of("human_torch", "invisible_woman", "the_thing", "mr_fantastic", "mr_fantastic1", "mr_fantastic2005",
                "mr_fantastic_fant4stic", "mr_fantastic_fof4", "mr_fantastic_future", "mr_fantastic_mcu", "mr_fantastic_rivals",
                "mr_fantastic_ultimate", "mr_fantastic_wgh", "the_maker")) {
            duplicates.remove("thematic:" + suit);
        }
        thematicPackSwaps(group(CleanupConfig.THEMATIC_PACK_SWAPS));
        thematicDecor(group(CleanupConfig.THEMATIC_DECOR));
        group(CleanupConfig.THEMATIC_MONEY).remove("thematic:1", "thematic:5", "thematic:10", "thematic:20", "thematic:50", "thematic:100",
                "thematic:money", "thematic:wallet", "thematic:cash_register", "thematic:lexcoin", "thematic:queencoin", "thematic:waynecoin");
    }

    private static void thematicBloat(Group bloat) {
        for (String id : List.of("core_array", "batcomputer", "holo_console", "field_terminal", "data_conduit", "dispatch_port", "fabricator",
                "intake_hopper", "intake_port", "output_hopper", "satellite_uplink", "signal_relay", "storage_link", "terminal_link",
                "storage_drive_1k", "storage_drive_4k", "storage_drive_16k", "storage_drive_64k", "speed_card", "bulk_card", "tag_filter_card",
                "capacity_card", "range_card", "network_card", "parallel_card", "surface_card", "grid_card", "build_to_me_card", "air_place_card",
                "quantum_tunnel_core", "quantum_tunnel_frame", "tardis", "ghostbusters", "soul", "jedi_archive", "sith_archive", "jedi_holocron",
                "sith_holocron", "star_wars_logo", "popcorn_box", "blacklight", "blacklight_pillar", "glass_pillar", "glass_slab", "glass_stairs",
                "bright_falls_billboard", "derry_billboard", "silent_hill_billboard", "raccoon_city_billboard", "nuka_cola_billboard",
                "aged_nuka_cola_billboard", "shady_sands_billboard", "frankenstein_experiment", "strange_idol", "scooby_snacks", "blood_splatter",
                "christmas_garland", "christmas_lights", "christmas_star", "christmas_tree", "colorful_christmas_lights", "white_christmas_lights",
                "mistletoe", "wreath", "christmas_wallpaper", "halloween_lights", "orange_halloween_lights", "halloween_wallpaper", "sticky_cobweb",
                "terracotta_slab", "terracotta_stairs", "shingles", "shingle_slab", "shingle_stairs", "large_bricks", "stacked_bricks", "sandy_bricks",
                "large_sandy_bricks", "stacked_sandy_bricks", "cement", "cement_brick_slab", "cement_brick_stairs", "cement_brick_wall",
                "cement_bricks", "cement_panel", "cement_pillar", "cement_pillar_wall", "cement_slab", "cement_stairs", "cement_wall",
                "polished_cement", "polished_cement_pillar", "polished_cement_pillar_wall", "polished_cement_slab", "polished_cement_stairs",
                "polished_cement_wall", "dripstone_brick_slab", "dripstone_brick_stairs", "dripstone_brick_wall", "dripstone_bricks",
                "polished_dripstone", "polished_dripstone_slab", "polished_dripstone_stairs", "polished_dripstone_wall")) {
            bloat.remove("thematic:" + id);
        }
        for (String candy : List.of("red", "green")) {
            bloat.remove("thematic:" + candy + "_candy_cane_block", "thematic:" + candy + "_candy_cane_block_slab",
                    "thematic:" + candy + "_candy_cane_block_stairs", "thematic:" + candy + "_candy_cane_block_wall");
        }
        for (String color : colors()) {
            for (String kind : List.of("neon_light", "neon_pillar", "stocking", "stained_glass_slab", "stained_glass_stairs", "concrete_slab",
                    "concrete_stairs", "wool_slab", "wool_stairs", "terracotta_slab", "terracotta_stairs", "shingles", "shingle_slab", "shingle_stairs")) {
                bloat.remove("thematic:" + color + "_" + kind);
            }
        }
        List<String> glasses = new java.util.ArrayList<>();
        colors().forEach(color -> glasses.add(color + "_frosted_glass"));
        for (String glass : List.of("plum", "circular", "neon_cyan", "neon_light_blue", "neon_lime", "neon_purple")) {
            glasses.add(glass + "_frosted_glass");
        }
        glasses.addAll(List.of("frosted_glass", "dirty_glass", "borderless_glass"));
        for (String glass : glasses) {
            bloat.remove("thematic:" + glass, "thematic:" + glass + "_pane", "thematic:" + glass + "_slab", "thematic:" + glass + "_stairs");
        }
        for (String ore : List.of("adamantium", "amazonium", "blood_diamond", "carbonadium", "coal", "crimson_iron", "dark_iron", "diamond",
                "electrum", "emerald", "gold", "gold_titanium_alloy", "iron", "krypt_steel", "lapis", "magnesium", "netherite", "nth_metal", "peridot",
                "promethium", "ruby", "sapphire", "scarlet_emerald", "silver", "smooth_steel", "steel", "thunder_infused", "tin", "titanium",
                "vibranium", "wayne_enterprises_steel", "white_dwarf_star_alloy")) {
            removeShapes(bloat, ore);
        }
        for (String wood : List.of("acacia", "bamboo", "birch", "charred", "cherry", "crimson", "dark_oak", "driftwood", "jungle", "mangrove", "oak",
                "spruce", "warped", "willow")) {
            bloat.remove("thematic:" + wood + "_panel", "thematic:" + wood + "_pillar");
        }
        for (String wood : List.of("charred", "driftwood")) {
            for (String part : List.of("bench", "bookshelf", "button", "chair", "crate", "door", "fence", "fence_gate", "hanging_sign", "log",
                    "picket_fence", "planks", "pressure_plate", "sign", "slab", "stairs", "table", "trapdoor", "wood")) {
                bloat.remove("thematic:" + wood + "_" + part);
            }
        }
        for (String base : List.of("brick", "sandy_brick")) {
            for (String tint : List.of("bleached", "dark", "vibrant", "weathered")) {
                String name = tint + "_" + base;
                bloat.remove("thematic:" + name + "s", "thematic:" + name + "_slab", "thematic:" + name + "_stairs", "thematic:" + name + "_wall");
                if (!tint.equals("weathered")) {
                    bloat.remove("thematic:large_" + name + "s", "thematic:stacked_" + name + "s");
                }
            }
        }
        for (String quartz : List.of("blue", "rose", "smoky", "dark", "ruby")) {
            String name = quartz + "_quartz";
            bloat.remove("thematic:" + name, "thematic:" + name + "_block", "thematic:chiseled_" + name + "_block", "thematic:smooth_" + name);
            for (String shape : List.of("brick_slab", "brick_stairs", "brick_wall", "bricks", "pillar", "pillar_wall", "slab", "stairs", "tile_slab",
                    "tile_stairs", "tile_wall", "tiles", "wall")) {
                bloat.remove("thematic:" + name + "_" + shape);
            }
            removeShapes(bloat, "smooth_" + name);
        }
        for (String stone : List.of("obsidian", "crying_obsidian")) {
            bloat.remove("thematic:" + stone + "_bricks", "thematic:" + stone + "_brick_slab", "thematic:" + stone + "_brick_stairs",
                    "thematic:" + stone + "_brick_wall", "thematic:" + stone + "_pillar", "thematic:" + stone + "_pillar_wall",
                    "thematic:polished_" + stone, "thematic:polished_" + stone + "_slab", "thematic:polished_" + stone + "_stairs",
                    "thematic:polished_" + stone + "_wall");
        }
        for (String poster : List.of("2001", "akira", "alien", "ant_man", "aou", "aou_alt", "army_of_darkness", "atsv", "avatar", "avengers", "avengers_alt",
                "avengers_endgame", "batman", "batman66", "batman_returns", "beetlejuice", "billandted", "black_panther",
                "black_pearl", "blade", "bnd", "bride", "brightburn", "bttf", "casablanca", "chickenlittle", "childs_play",
                "citw", "civil_war", "click", "clones", "crazyeightnights", "crystalskull", "dark", "dark_knight",
                "dark_knight_alt", "darkcrystal", "dialofdestiny", "dofp", "doomsday_alt", "dpw", "empire", "endgame_alt", "et",
                "evangelion", "exorcist", "fellowship", "fight_club", "fnaf", "fnaf2", "frankenstein", "friday_the_13th",
                "ghostbusters", "goodfellas", "gotg", "greenlantern2011", "gremlins", "gremlins2", "grownups", "halloween",
                "harry_potter", "hellraiser", "hill_house", "hulk2003", "hulk2008", "hunger_games", "infinity_war",
                "infinity_war_alt", "invisible_man", "iron_man", "ironlung", "it", "it_follows", "itsv", "jaws", "jurassic_park",
                "killbill", "labyrinth", "lalaland", "lanterns", "lastcrusade", "littleshopofhorrors", "logan", "malignant",
                "mallrats", "metropolis", "midsommar", "minecraft", "mummy", "ncfom", "neverendingstory", "nosferatu", "nwh",
                "obsession", "odyssey", "parasite", "ragnarok", "raiders", "rogue_one", "rotj", "rots", "saw", "scott_pilgrim",
                "scream", "shazam", "shrek", "smilingfriends", "sotl", "spawn", "spiderman", "spiderman3", "spiderman_homecoming",
                "star_wars", "supergirl", "superman", "superman2025_alt", "superman_dcu", "superman_returns", "tasm",
                "templeofdoom", "texas_chainsaw", "tfa", "the_batman", "the_flash", "the_godfather", "the_rocketeer",
                "the_shining", "the_terminator", "the_thing", "thegoonies", "thesopranos", "titanic", "tlj", "tmnt", "tpm",
                "tros", "tros_alt", "truman_show", "tss", "vader", "visionquest", "wandavision", "watchmen", "werewolf_by_night",
                "willy_wonka", "wolfman", "wonder_man", "ww", "xmen", "zsjl")) {
            bloat.remove("thematic:poster_" + poster);
        }
    }

    private static void thematicMerges(Group merges) {
        merges.replace("thematic:vibranium", "marvel:vibranium");
        merges.replace("thematic:vibranium_block", "marvel:vibranium_block");
        merges.replace("thematic:vibranium_ore", "marvel:vibranium_ore");
        merges.feature("thematic:ore_vibranium");

        merges.replace("thematic:adamantium", "marvel:adamantium_ingot");
        merges.replace("thematic:adamantium_nugget", "marvel:adamantium_nugget");
        merges.replace("thematic:adamantium_block", "marvel:adamantium_block");
        merges.replace("thematic:carbonadium_ingot", "marvel:adamantium_ingot");
        merges.replace("thematic:carbonadium_block", "marvel:adamantium_block");

        merges.replace("thematic:gold_titanium_alloy_ingot", "marvel:gold_titanium_ingot");
        merges.replace("thematic:gold_titanium_alloy_nugget", "marvel:gold_titanium_nugget");
        merges.replace("thematic:gold_titanium_alloy_block", "marvel:gold_titanium_block");

        String titanium = ModCompat.ANTARCHY ? "antarchy:" : "marvel:";
        for (String id : List.of("titanium_ingot", "raw_titanium", "titanium_block", "raw_titanium_block", "titanium_ore", "deepslate_titanium_ore")) {
            merges.replace("thematic:" + id, titanium + id);
        }
        merges.feature("thematic:ore_titanium");

        merges.replace("thematic:fabric", "marvellegacy:cloth_white");
        merges.replace("thematic:elastic_fabric", "marvellegacy:cloth_blue");
        merges.replace("thematic:velocity_resistant_fabric", "marvellegacy:cloth_red");
        for (String color : colors()) {
            for (String kind : List.of("_fabric", "_elastic_fabric", "_velocity_resistant_fabric")) {
                merges.replace("thematic:" + color + kind, "marvellegacy:cloth_" + color);
            }
        }
        merges.replace("thematic:reinforced_leather", "marvellegacy:cloth_black");

        merges.replace("thematic:empty_vial", "marvellegacy:vial_empty");
        merges.replace("thematic:mutant_gene", "marvel:x_genes");
        merges.replace("thematic:blood_vial", "marvel:villager_blood_sample");
        merges.replace("thematic:pym_tech", "marvel:pym_particle");

        if (ModCompat.ICE_AND_FIRE) {
            merges.replace("thematic:silver_ingot", "iceandfire:silver_ingot");
            merges.replace("thematic:silver_nugget", "iceandfire:silver_nugget");
            merges.replace("thematic:silver_block", "iceandfire:silver_block");
            merges.replace("thematic:raw_silver", "iceandfire:silver_ingot");
            merges.remove("thematic:raw_silver_block", "thematic:silver_ore", "thematic:deepslate_silver_ore");
        } else {
            merges.replace("thematic:silver_ingot", "minecraft:iron_ingot");
            merges.replace("thematic:silver_nugget", "minecraft:iron_nugget");
            merges.replace("thematic:silver_block", "minecraft:iron_block");
            merges.replace("thematic:raw_silver", "minecraft:raw_iron");
            merges.replace("thematic:raw_silver_block", "minecraft:raw_iron_block");
            merges.replace("thematic:silver_ore", "minecraft:iron_ore");
            merges.replace("thematic:deepslate_silver_ore", "minecraft:deepslate_iron_ore");
        }
        merges.feature("thematic:ore_silver");
        if (ModCompat.ORITECH) {
            merges.replace("thematic:electrum_ingot", "oritech:electrum_ingot");
            merges.replace("thematic:electrum_block", "oritech:electrum_block");
        } else {
            merges.replace("thematic:electrum_ingot", "minecraft:gold_ingot");
            merges.replace("thematic:electrum_block", "minecraft:gold_block");
        }

        for (String material : List.of("vibranium", "adamantium", "carbonadium", "gold_titanium_alloy", "titanium", "silver", "electrum")) {
            removeShapes(merges, material);
        }
    }

    private static void thematicExtras(Group extras) {
        metal(extras, "tin", "copper", null);
        metal(extras, "crimson_iron", "copper", null);
        metal(extras, "magnesium", "iron", "minecraft:iron_nugget");
        metal(extras, "dark_iron", "iron", "minecraft:iron_nugget");
        for (String feature : List.of("tin", "magnesium", "crimson_iron", "dark_iron")) {
            for (String size : List.of("", "_middle", "_upper", "_small")) {
                extras.feature("thematic:ore_" + feature + size);
            }
        }

        extras.replace("thematic:amazonium_ingot", "minecraft:gold_ingot");
        extras.replace("thematic:amazonium_nugget", "minecraft:gold_nugget");
        extras.replace("thematic:amazonium_block", "minecraft:gold_block");
        if (ModCompat.ORITECH) {
            extras.replace("thematic:promethium_ingot", "oritech:prometheum_ingot");
            extras.replace("thematic:promethium_plate", "oritech:prometheum_ingot");
            extras.remove("thematic:promethium_block");
        } else {
            extras.replace("thematic:promethium_ingot", "minecraft:netherite_ingot");
            extras.replace("thematic:promethium_plate", "minecraft:netherite_ingot");
            extras.replace("thematic:promethium_block", "minecraft:netherite_block");
        }
        extras.remove("thematic:promethium_nugget");
        extras.replace("thematic:white_dwarf_star_alloy_ingot", "marvel:uru_ingot");
        extras.replace("thematic:white_dwarf_star_alloy_nugget", "marvel:uru_nugget");
        extras.replace("thematic:white_dwarf_star_alloy_block", "marvel:uru_block");
        extras.replace("thematic:white_dwarf_star_alloy_plate", "marvel:uru_ingot");
        extras.replace("thematic:kevlar", "marvellegacy:cloth_black");

        gem(extras, "peridot", "minecraft:emerald", "minecraft:emerald_block", "minecraft:emerald_ore", "minecraft:deepslate_emerald_ore");
        String redGem = ModCompat.ANTARCHY ? "antarchy:blood_crystal_shard" : "minecraft:redstone";
        String redBlock = ModCompat.ANTARCHY ? "antarchy:blood_crystal_block" : "minecraft:redstone_block";
        for (String red : List.of("ruby", "scarlet_emerald", "blood_diamond", "ruby_quartz")) {
            extras.replace("thematic:" + red, redGem);
            extras.replace("thematic:" + red + "_block", redBlock);
        }
        for (String ore : List.of("ruby_ore", "scarlet_emeralds_ore", "blood_diamonds_ore")) {
            if (ModCompat.ANTARCHY) {
                extras.remove("thematic:" + ore, "thematic:deepslate_" + ore);
            } else {
                extras.replace("thematic:" + ore, "minecraft:redstone_ore");
                extras.replace("thematic:deepslate_" + ore, "minecraft:deepslate_redstone_ore");
            }
        }
        extras.remove("thematic:ruby_tile_block", "thematic:ruby_tile_slab", "thematic:ruby_tile_stairs", "thematic:ruby_tile_wall");
        extras.feature("thematic:ore_ruby");
        extras.feature("thematic:ore_peridot");
        extras.feature("thematic:ore_scarlet_emeralds");
        for (String size : List.of("", "_small", "_large")) {
            extras.feature("thematic:ore_blood_diamonds" + size);
        }
        if (ModCompat.ANTARCHY) {
            gem(extras, "sapphire", "antarchy:bluestone", "antarchy:bluestone_block", "antarchy:bluestone_ore", "antarchy:bluestone_ore");
        } else {
            gem(extras, "sapphire", "minecraft:lapis_lazuli", "minecraft:lapis_block", "minecraft:lapis_ore", "minecraft:deepslate_lapis_ore");
        }
        extras.feature("thematic:ore_sapphire");
        extras.feature("thematic:ore_sapphire_small");

        extras.replace("thematic:thunder_infused_ingot", "thematic:lightning_shard");
        extras.replace("thematic:raw_thunder_infused_rock", "thematic:lightning_shard");
        extras.replace("thematic:electrolytic_solution", "thematic:lightning_shard");
        extras.remove("thematic:thunder_infused_block", "thematic:raw_thunder_infused_block", "thematic:thunder_infused_rock_ore",
                "thematic:deepslate_thunder_infused_rock_ore");
        extras.feature("thematic:ore_thunder_infused_rock");

        extras.replace("thematic:krypt_steel", "minecraft:netherite_ingot");
        extras.replace("thematic:krypt_steel_block", "minecraft:netherite_block");
        extras.replace("thematic:hardened_steel", ModCompat.ANTARCHY ? "antarchy:titanium_ingot" : "marvel:titanium_ingot");
        extras.replace("thematic:concentrated_carbon", "minecraft:coal_block");
        extras.replace("thematic:dark_quartz", "minecraft:quartz");
        extras.replace("thematic:dark_quartz_block", "minecraft:quartz_block");
        for (String quartz : List.of("blue", "rose", "smoky", "dark", "ruby")) {
            String base = "thematic:" + quartz + "_quartz";
            if (!quartz.equals("dark") && !quartz.equals("ruby")) {
                extras.remove(base, base + "_block");
            }
            for (String shape : List.of("_brick_slab", "_brick_stairs", "_brick_wall", "_bricks", "_pillar", "_pillar_wall", "_slab",
                    "_stairs", "_tile_slab", "_tile_stairs", "_tile_wall", "_tiles", "_wall")) {
                extras.remove(base + shape);
            }
            String smooth = "thematic:smooth_" + quartz + "_quartz";
            extras.remove(smooth, smooth + "_slab", smooth + "_stairs", smooth + "_wall", "thematic:chiseled_" + quartz + "_quartz_block");
        }
        extras.remove("thematic:dark_quartz_wall_light", "thematic:dark_quartz_wall_light_four");

        extras.replace("thematic:gold_plate", "minecraft:gold_ingot");
        extras.replace("thematic:iron_plate", "minecraft:iron_ingot");
        extras.replace("thematic:amazonium_plate", "minecraft:gold_ingot");
        extras.replace("thematic:crimson_iron_plate", "minecraft:copper_ingot");
        extras.replace("thematic:tin_plate", "minecraft:copper_ingot");
        extras.replace("thematic:dark_iron_plate", "minecraft:iron_ingot");
        extras.replace("thematic:adamantium_plate", "marvel:adamantium_ingot");
        extras.replace("thematic:gold_titanium_alloy_plate", "marvel:gold_titanium_ingot");
        extras.replace("thematic:silver_plate", ModCompat.ICE_AND_FIRE ? "iceandfire:silver_ingot" : "minecraft:iron_ingot");
        if (ModCompat.ANTARCHY) {
            for (String id : List.of("nth_metal", "nth_metal_plate", "nth_metal_block", "raw_nth", "raw_nth_block")) {
                extras.replace("thematic:" + id, "antarchy:antimetal");
            }
            extras.remove("thematic:nth_metal_nugget", "thematic:nth_ore", "thematic:deepslate_nth_ore");
        } else {
            extras.replace("thematic:nth_metal", "minecraft:iron_ingot");
            extras.replace("thematic:nth_metal_plate", "minecraft:iron_ingot");
            extras.replace("thematic:nth_metal_nugget", "minecraft:iron_nugget");
            extras.replace("thematic:nth_metal_block", "minecraft:iron_block");
            extras.replace("thematic:raw_nth", "minecraft:raw_iron");
            extras.replace("thematic:raw_nth_block", "minecraft:raw_iron_block");
            extras.replace("thematic:nth_ore", "minecraft:iron_ore");
            extras.replace("thematic:deepslate_nth_ore", "minecraft:deepslate_iron_ore");
        }
        extras.feature("thematic:ore_nth");
        if (ModCompat.NETHER_EXP) {
            extras.replace("thematic:infernal_fossil", "netherexp:fossil_fuel");
            extras.replace("thematic:infernal_fossil_ore", "netherexp:fossil_fuel_ore");
        } else {
            extras.replace("thematic:infernal_fossil", "minecraft:blaze_powder");
            extras.remove("thematic:infernal_fossil_ore");
        }
        extras.feature("thematic:ore_infernal_fossil");
        extras.retire("thematic:plate_press", "thematic:alloy_smelter", "thematic:centrifuge");

        extras.replace("thematic:steel_ingot", "minecraft:iron_ingot");
        extras.replace("thematic:steel_block", "minecraft:iron_block");

        extras.replace("thematic:dyeable_fiber", "minecraft:string");
        for (String color : colors()) {
            extras.replace("thematic:" + color + "_fiber", "minecraft:string");
        }
        extras.remove("thematic:fiber_cluster", "thematic:small_fiber_cluster", "thematic:medium_fiber_cluster", "thematic:large_fiber_cluster");
        extras.feature("thematic:ore_fiber_cluster");
        for (String stone : List.of("limestone", "slate")) {
            extras.feature("thematic:ore_" + stone + "_lower");
            extras.feature("thematic:ore_" + stone + "_upper");
        }
        stoneSet(extras, "thematic:limestone", "minecraft:diorite", "minecraft:diorite_slab", "minecraft:diorite_stairs", "minecraft:diorite_wall");
        stoneSet(extras, "thematic:polished_limestone", "minecraft:polished_diorite", "minecraft:polished_diorite_slab",
                "minecraft:polished_diorite_stairs", "minecraft:diorite_wall");
        for (String bricks : List.of("polished_limestone", "large_limestone")) {
            extras.replace("thematic:" + bricks + "_bricks", "minecraft:polished_diorite");
            extras.replace("thematic:" + bricks + "_brick_slab", "minecraft:polished_diorite_slab");
            extras.replace("thematic:" + bricks + "_brick_stairs", "minecraft:polished_diorite_stairs");
            extras.replace("thematic:" + bricks + "_brick_wall", "minecraft:diorite_wall");
        }
        extras.replace("thematic:limestone_pillar", "minecraft:polished_diorite");
        extras.replace("thematic:limestone_pillar_wall", "minecraft:diorite_wall");
        stoneSet(extras, "thematic:slate", "minecraft:deepslate", "minecraft:cobbled_deepslate_slab", "minecraft:cobbled_deepslate_stairs",
                "minecraft:cobbled_deepslate_wall");
        stoneSet(extras, "thematic:polished_slate", "minecraft:polished_deepslate", "minecraft:polished_deepslate_slab",
                "minecraft:polished_deepslate_stairs", "minecraft:polished_deepslate_wall");
        extras.replace("thematic:slate_pillar", "minecraft:chiseled_deepslate");
        extras.replace("thematic:slate_pillar_wall", "minecraft:cobbled_deepslate_wall");

        extras.replace("thematic:future_tech", "thematic:advanced_technology");

        extras.replaceDyed("thematic:leather_jacket", "minecraft:leather_chestplate", 0x1D1D21);
        extras.replace("thematic:dog_tags", "minecraft:name_tag");
        extras.replace("thematic:magnifying_glass", "minecraft:spyglass");
        extras.replace("thematic:batnoculars", "minecraft:spyglass");
        extras.replace("thematic:voice_changer", "minecraft:note_block");
        extras.replace("thematic:blueprint", "minecraft:paper");
        extras.replace("thematic:alien_blood_vial", "integrated_superheroes:alien_blood");
        extras.replace("thematic:alien_blood_splatter", "integrated_superheroes:alien_blood");
        extras.remove("thematic:diamond_cube", "thematic:bat_wing", "thematic:mysterious_acid_vial", "thematic:gamma_sample",
                "thematic:ruby_quartz_visor", "thematic:cyclops_visor", "thematic:ancient_relic", "thematic:plasma_ball",
                "thematic:lobster_thermidor", "thematic:cosmic_debris");
        for (String shard : List.of("compassion", "death", "greed", "hope", "life", "love", "rage", "water")) {
            extras.remove("thematic:" + shard + "_shard");
        }
        extras.dropRecipe("thematic:compound_v");

        for (String material : List.of("tin", "crimson_iron", "magnesium", "dark_iron", "amazonium", "promethium", "white_dwarf_star_alloy",
                "peridot", "scarlet_emerald", "blood_diamond", "sapphire", "ruby", "thunder_infused", "krypt_steel", "steel", "nth_metal")) {
            removeShapes(extras, material);
        }
    }

    private static void thematicPackSwaps(Group swaps) {
        if (ModCompat.ALEXS_CAVES) {
            swaps.replace("thematic:kryptonite", "alexscaves:uranium");
            swaps.remove("thematic:kryptonite_ore", "thematic:deepslate_kryptonite_ore");
            swaps.feature("thematic:ore_krypt");
            swaps.replace("thematic:filled_ooze_canister", "alexscaves:toxic_paste");
            swaps.remove("thematic:ooze_canister");
            swaps.feature("thematic:lake_ooze");
            swaps.replace("thematic:radioactive_barrel", "alexscaves:waste_drum");
        }
        if (ModCompat.MTS_OFFICIAL_PACK) {
            swaps.replace("thematic:car_wheel", "mts:mtsofficialpack.wheelmedium");
        }
        if (ModCompat.ICE_AND_FIRE) {
            swaps.replace("thematic:atlantean_plate", "iceandfire:sea_serpent_scales_blue");
            swaps.replace("thematic:nth_feather", "iceandfire:stymphalian_bird_feather");
        }
        if (ModCompat.PASTEL) {
            swaps.replace("thematic:cosmic_debris", "pastel:star_fragment");
            swaps.replace("thematic:cosmic_debris_block", "pastel:stardust_block");
        }
        swaps.remove("thematic:chili_pot", "thematic:terminal_link", "thematic:construction_projector");
        for (String size : List.of("1k", "4k", "16k", "64k")) {
            swaps.remove("thematic:data_core_" + size);
        }
        for (String card : List.of("air_place", "build_to_me", "bulk", "capacity", "grid", "network", "parallel", "range", "speed", "surface",
                "tag_filter")) {
            swaps.remove("thematic:" + card + "_card");
        }
    }

    private static void thematicDecor(Group decor) {
        for (String id : List.of("tuff_bricks", "tuff_brick_slab", "tuff_brick_stairs", "tuff_brick_wall", "chiseled_tuff_bricks", "tuff_slab",
                "tuff_stairs", "tuff_wall", "polished_tuff", "polished_tuff_slab", "polished_tuff_stairs", "polished_tuff_wall", "chiseled_tuff")) {
            decor.replace("thematic:" + id, "minecraft:" + id);
        }
        if (ModCompat.PASTEL) {
            for (String id : List.of("basalt_bricks", "basalt_brick_slab", "basalt_brick_stairs", "basalt_brick_wall", "basalt_tiles",
                    "basalt_tile_slab", "basalt_tile_stairs", "basalt_tile_wall", "polished_basalt_slab", "polished_basalt_stairs",
                    "polished_basalt_wall", "calcite_bricks", "calcite_brick_slab", "calcite_brick_stairs", "calcite_brick_wall",
                    "polished_calcite", "polished_calcite_slab", "polished_calcite_stairs", "polished_calcite_wall")) {
                decor.replace("thematic:" + id, "pastel:" + id);
            }
            decor.replace("thematic:calcite_pillar", "pastel:polished_calcite_pillar");
        }
        if (ModCompat.NETHER_EXP) {
            decor.replace("thematic:nether_brick_pillar", "netherexp:nether_brick_pillar");
            decor.replace("thematic:netherite_slab", "netherexp:cut_netherite_slab");
            decor.replace("thematic:netherite_stairs", "netherexp:cut_netherite_stairs");
            for (String shape : List.of("", "_slab", "_stairs", "_wall")) {
                decor.replace("thematic:soul_sandstone" + shape, "netherexp:soul_slate" + shape);
                decor.replace("thematic:smooth_soul_sandstone" + shape, "netherexp:soul_slate_tile" + (shape.isEmpty() ? "s" : shape));
            }
            decor.replace("thematic:soul_sandstone_bricks", "netherexp:soul_slate_bricks");
            decor.replace("thematic:soul_sandstone_brick_slab", "netherexp:soul_slate_brick_slab");
            decor.replace("thematic:soul_sandstone_brick_stairs", "netherexp:soul_slate_brick_stairs");
            decor.replace("thematic:soul_sandstone_brick_wall", "netherexp:soul_slate_brick_wall");
            decor.replace("thematic:cut_soul_sandstone", "netherexp:soul_slate_bricks");
            decor.replace("thematic:cut_soul_sandstone_slab", "netherexp:soul_slate_brick_slab");
            decor.replace("thematic:chiseled_soul_sandstone", "netherexp:chiseled_soul_slate_bricks");
        } else {
            for (String shape : List.of("", "_slab", "_stairs", "_wall")) {
                decor.remove("thematic:soul_sandstone" + shape, "thematic:smooth_soul_sandstone" + shape);
            }
            decor.remove("thematic:soul_sandstone_bricks", "thematic:soul_sandstone_brick_slab", "thematic:soul_sandstone_brick_stairs",
                    "thematic:soul_sandstone_brick_wall", "thematic:cut_soul_sandstone", "thematic:cut_soul_sandstone_slab",
                    "thematic:chiseled_soul_sandstone");
        }
        decor.remove("thematic:soul_brazier", "thematic:soul_lamp_post", "thematic:creeping_schist", "thematic:ammo_bench", "thematic:arcane_workbench");
        for (String palette : List.of("", "bleached_", "bleached_sandy_", "dark_", "dark_sandy_", "sandy_", "vibrant_", "vibrant_sandy_",
                "weathered_", "weathered_sandy_")) {
            String base = palette.contains("sandy") ? "minecraft:mud_brick" : "minecraft:brick";
            String full = base + "s";
            String tone = palette.isEmpty() ? "" : palette.substring(0, palette.length() - 1);
            if (!palette.isEmpty()) {
                decor.replace("thematic:" + palette + "bricks", full);
                decor.replace("thematic:" + palette + "brick_slab", base + "_slab");
                decor.replace("thematic:" + palette + "brick_stairs", base + "_stairs");
                decor.replace("thematic:" + palette + "brick_wall", base + "_wall");
            }
            decor.replace("thematic:" + palette + "layered_bricks", full);
            if (!palette.startsWith("weathered")) {
                decor.replace("thematic:" + palette + "corner_bricks", full);
                decor.replace("thematic:" + palette + "large_corner_bricks", full);
                decor.replace("thematic:large_" + (tone.isEmpty() ? "" : tone + "_") + "bricks", full);
                decor.replace("thematic:stacked_" + (tone.isEmpty() ? "" : tone + "_") + "bricks", full);
            }
        }
        for (String stone : List.of("obsidian", "crying_obsidian", "dripstone", "cement")) {
            decor.replace("thematic:" + stone + "_bricks", "thematic:polished_" + stone);
            for (String shape : List.of("_slab", "_stairs", "_wall")) {
                decor.replace("thematic:" + stone + "_brick" + shape, "thematic:polished_" + stone + shape);
            }
        }
        decor.replace("thematic:schist_bricks", "thematic:schist");
        decor.replace("thematic:snow_bricks", "thematic:packed_snow");
        for (String shape : List.of("_slab", "_stairs", "_wall")) {
            decor.replace("thematic:schist_brick" + shape, "thematic:schist" + shape);
            decor.replace("thematic:snow_brick" + shape, "thematic:packed_snow" + shape);
        }
        for (String sand : List.of("sandstone", "red_sandstone")) {
            decor.replace("thematic:" + sand + "_bricks", "minecraft:cut_" + sand);
            decor.replace("thematic:" + sand + "_brick_slab", "minecraft:cut_" + sand + "_slab");
            decor.replace("thematic:" + sand + "_brick_stairs", "minecraft:smooth_" + sand + "_stairs");
            decor.replace("thematic:" + sand + "_brick_wall", "minecraft:" + sand + "_wall");
        }
        decor.replace("thematic:nth_metal_bars", "minecraft:iron_bars");
        decor.replace("thematic:nth_metal_chain", "minecraft:chain");
        decor.replace("thematic:nth_metal_door", "minecraft:iron_door");
        decor.replace("thematic:nth_metal_trapdoor", "minecraft:iron_trapdoor");
        decor.replace("thematic:nth_metal_cell_door", "thematic:cell_door");
        decor.replace("thematic:nth_metal_cross", "thematic:iron_cross");
        decor.replace("thematic:nth_metal_support_beam", "thematic:steel_support_beam");
        decor.replace("thematic:nth_metal_ladder", "thematic:metal_ladder");
        for (String size : List.of("", "mini_", "small_")) {
            decor.replace("thematic:" + size + "nth_metal_cage_block", "thematic:" + size + "cage_block");
            decor.replace("thematic:" + size + "nth_cage_scaffold", "thematic:" + size + "cage_scaffold");
            decor.replace("thematic:" + size + "nth_cage_scaffold_stairs", "thematic:" + size + "cage_scaffold_stairs");
        }
        for (String color : List.of("blue", "orange", "red", "yellow")) {
            decor.replace("thematic:" + color + "_safety_ladder", "thematic:metal_ladder");
        }
        if (ModCompat.ANTARCHY) {
            decor.replace("thematic:titanium_bars", "antarchy:titanium_bars");
        }
        decor.replace("thematic:obsidian_pillar", "thematic:polished_obsidian");
        decor.replace("thematic:obsidian_pillar_wall", "thematic:polished_obsidian_wall");
        decor.replace("thematic:crying_obsidian_pillar", "thematic:polished_crying_obsidian");
        decor.replace("thematic:crying_obsidian_pillar_wall", "thematic:polished_crying_obsidian_wall");
        decor.replace("thematic:blackstone_pillar", "minecraft:polished_blackstone");
        decor.replace("thematic:blackstone_pillar_wall", "minecraft:polished_blackstone_wall");
        decor.replace("thematic:nether_brick_pillar_wall", "minecraft:nether_brick_wall");
        decor.replace("thematic:quartz_pillar_wall", "thematic:quartz_wall");
        decor.replace("thematic:cement_pillar", "thematic:cement");
        decor.replace("thematic:cement_pillar_wall", "thematic:cement_wall");
        decor.replace("thematic:cement_panel", "thematic:cement_slab");
        decor.replace("thematic:polished_cement_pillar", "thematic:polished_cement");
        decor.replace("thematic:polished_cement_pillar_wall", "thematic:polished_cement_wall");
        decor.replace("thematic:quartz_brick_slab", "minecraft:quartz_slab");
        decor.replace("thematic:quartz_brick_stairs", "minecraft:quartz_stairs");
        decor.replace("thematic:quartz_brick_wall", "thematic:quartz_wall");
        decor.replace("thematic:quartz_tiles", "minecraft:quartz_bricks");
        decor.replace("thematic:quartz_tile_slab", "minecraft:quartz_slab");
        decor.replace("thematic:quartz_tile_stairs", "minecraft:quartz_stairs");
        decor.replace("thematic:quartz_tile_wall", "thematic:quartz_wall");
        decor.replace("thematic:stone_path", "minecraft:cobblestone");
        decor.replace("thematic:blue_tile_block", "thematic:tile_block");
        decor.replace("thematic:blue_tile_slab", "thematic:tile_slab");
        decor.replace("thematic:blue_tile_stairs", "thematic:tile_stairs");
        decor.replace("thematic:blue_tile_wall", "thematic:tile_wall");
        decor.replace("thematic:rose_tile_block", "thematic:tile_block");
        decor.replace("thematic:rose_tile_slab", "thematic:tile_slab");
        decor.replace("thematic:rose_tile_stairs", "thematic:tile_stairs");
        decor.replace("thematic:rose_tile_wall", "thematic:tile_wall");
        decor.replace("thematic:smoky_tile_block", "thematic:tile_block");
        decor.replace("thematic:smoky_tile_slab", "thematic:tile_slab");
        decor.replace("thematic:smoky_tile_stairs", "thematic:tile_stairs");
        decor.replace("thematic:smoky_tile_wall", "thematic:tile_wall");
        decor.replace("thematic:snow_block_slab", "thematic:packed_snow_slab");
        decor.replace("thematic:snow_block_stairs", "thematic:packed_snow_stairs");
        decor.replace("thematic:snow_block_wall", "thematic:packed_snow_wall");
        decor.replace("thematic:netherite_bars", "minecraft:iron_bars");
        decor.replace("thematic:circular_frosted_glass_slab", "thematic:frosted_glass_slab");
        decor.replace("thematic:circular_frosted_glass_stairs", "thematic:frosted_glass_stairs");
        decor.replace("thematic:plum_frosted_glass_slab", "thematic:frosted_glass_slab");
        decor.replace("thematic:plum_frosted_glass_stairs", "thematic:frosted_glass_stairs");
        decor.replace("thematic:neon_cyan_frosted_glass_slab", "thematic:frosted_glass_slab");
        decor.replace("thematic:neon_cyan_frosted_glass_stairs", "thematic:frosted_glass_stairs");
        decor.replace("thematic:neon_light_blue_frosted_glass_slab", "thematic:frosted_glass_slab");
        decor.replace("thematic:neon_light_blue_frosted_glass_stairs", "thematic:frosted_glass_stairs");
        decor.replace("thematic:neon_lime_frosted_glass_slab", "thematic:frosted_glass_slab");
        decor.replace("thematic:neon_lime_frosted_glass_stairs", "thematic:frosted_glass_stairs");
        decor.replace("thematic:neon_purple_frosted_glass_slab", "thematic:frosted_glass_slab");
        decor.replace("thematic:neon_purple_frosted_glass_stairs", "thematic:frosted_glass_stairs");
        decor.replace("thematic:glass_pillar", "minecraft:glass");
        decor.replace("thematic:black_stained_glass_door", "thematic:glass_door");
        decor.replace("thematic:black_stained_glass_trapdoor", "thematic:glass_trapdoor");
        decor.replace("thematic:backless_office_bench", "thematic:park_bench");
        decor.replace("thematic:modern_office_bench", "thematic:park_bench");
        decor.replace("thematic:modern_bench", "thematic:park_bench");
        decor.replace("thematic:metal_bench", "thematic:park_bench");
        decor.replace("thematic:glass_bench", "thematic:park_bench");
        decor.replace("thematic:modern_office_chair", "thematic:office_chair");
        decor.replace("thematic:large_bookshelf", "minecraft:bookshelf");
        decor.replace("thematic:large_bookshelf_b", "minecraft:bookshelf");
        decor.replace("thematic:wall_light_four", "thematic:wall_light");
        decor.replace("thematic:dark_wall_light", "thematic:wall_light");
        decor.replace("thematic:dark_wall_light_four", "thematic:wall_light");
        decor.replace("thematic:red_wall_light_four", "thematic:red_wall_light");
        decor.replace("thematic:theater_light", "thematic:spotlight");
        decor.replace("thematic:moon_light", "minecraft:pearlescent_froglight");
        decor.replace("thematic:squidlight", "minecraft:sea_lantern");
        decor.replace("thematic:aged_manhole_cover", "thematic:manhole_cover");
        decor.replace("thematic:red_phone_booth", "thematic:phone_booth");
        decor.replace("thematic:blue_guard_rail", "thematic:guard_rail");
        decor.replace("thematic:orange_guard_rail", "thematic:guard_rail");
        decor.replace("thematic:red_guard_rail", "thematic:guard_rail");
        decor.replace("thematic:yellow_guard_rail", "thematic:guard_rail");
        decor.replace("thematic:green_park_trash_can", "thematic:park_trash_can");
        decor.replace("thematic:screen_door", "minecraft:iron_door");
        decor.replace("thematic:screen_trapdoor", "thematic:vent_trapdoor");
        decor.replace("thematic:wall_fan", "thematic:vent");
        decor.replace("thematic:papers_black", "thematic:papers");
        decor.replace("thematic:papers_blue", "thematic:papers");
        decor.replace("thematic:green_folders", "thematic:papers");
        decor.replace("thematic:orange_folders", "thematic:papers");
        decor.replace("thematic:envelopes", "thematic:papers");
        decor.replace("thematic:sticky_notes", "thematic:papers");
        decor.replace("thematic:green_filing_cabinet", "thematic:filing_cabinet");
        decor.replace("thematic:yellow_filing_cabinet", "thematic:filing_cabinet");
        decor.replace("thematic:book", "minecraft:book");
        decor.replace("thematic:hay_stack", "minecraft:hay_block");
        decor.replace("thematic:spiral_carpet", "minecraft:red_carpet");
        decor.replace("thematic:sticky_cobweb", "minecraft:cobweb");
        decor.replace("thematic:retro_jukebox", "minecraft:jukebox");
        decor.replace("thematic:vintage_radio", "minecraft:jukebox");
        decor.replace("thematic:black_guitar", "thematic:guitar");
        decor.replace("thematic:red_guitar", "thematic:guitar");
        decor.replace("thematic:microphone_stand", "thematic:microphone");
        decor.replace("thematic:retro_microphone", "thematic:microphone");
        decor.replace("thematic:telescope", "minecraft:spyglass");
        decor.replace("thematic:mannequin", "minecraft:armor_stand");
        decor.replace("thematic:framed_picture", "minecraft:painting");
        decor.replace("thematic:framed_picture_b", "minecraft:painting");
        decor.replace("thematic:hanging_pictures", "minecraft:painting");
        decor.replace("thematic:hanging_pictures_b", "minecraft:painting");
        decor.replace("thematic:hanging_pictures_c", "minecraft:painting");
        decor.replace("thematic:strange_man_painting", "minecraft:painting");
        decor.replace("thematic:antique_scale", "thematic:scale");
        if (ModCompat.PASTEL) {
            decor.replace("thematic:calcite_pillar_wall", "pastel:polished_calcite_wall");
        } else {
            decor.remove("thematic:calcite_pillar_wall");
        }
        if (ModCompat.DIM_DOORS) {
            decor.replace("thematic:stone_wall", "dimdoors:stone_wall");
        } else {
            decor.remove("thematic:stone_wall");
        }
        if (ModCompat.ABYSSAL_DECOR) {
            decor.replace("thematic:circular_frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:circular_frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:plum_frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:plum_frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:neon_cyan_frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:neon_cyan_frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:neon_light_blue_frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:neon_light_blue_frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:neon_lime_frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:neon_lime_frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:neon_purple_frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:neon_purple_frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:aqua_wallpaper", "abyssal_decor:wallpaper_cyan");
            decor.replace("thematic:arcade_wallpaper", "abyssal_decor:wallpaper_blue");
            decor.replace("thematic:beige_wallpaper", "abyssal_decor:brown_wallpaper");
            decor.replace("thematic:blue_bamboo_wallpaper", "abyssal_decor:wallpaper_blue");
            decor.replace("thematic:checkered_wallpaper", "abyssal_decor:red_wallpaper");
            decor.replace("thematic:gray_checkered_wallpaper", "abyssal_decor:gray_wallpaper");
            decor.replace("thematic:red_checkered_wallpaper", "abyssal_decor:red_wallpaper");
            decor.replace("thematic:yellow_checkered_wallpaper", "abyssal_decor:yellow_wallpaper");
            decor.replace("thematic:christmas_wallpaper", "abyssal_decor:red_wallpaper");
            decor.replace("thematic:halloween_wallpaper", "abyssal_decor:orange_wallpaper");
            decor.replace("thematic:city_night_wallpaper", "abyssal_decor:black_wallpaper");
            decor.replace("thematic:cloud_wallpaper", "abyssal_decor:wallpaper_light_blue");
            decor.replace("thematic:diamond_wallpaper", "abyssal_decor:wallpaper_blue");
            decor.replace("thematic:dotted_wallpaper", "abyssal_decor:black_wallpaper");
            decor.replace("thematic:flower_wallpaper", "abyssal_decor:lime_wallpaper");
            decor.replace("thematic:hexagon_wallpaper", "abyssal_decor:orange_wallpaper");
            decor.replace("thematic:leaf_wallpaper", "abyssal_decor:green_wallpaper");
            decor.replace("thematic:mosaic_wallpaper", "abyssal_decor:orange_wallpaper");
            decor.replace("thematic:night_sky_wallpaper", "abyssal_decor:wallpaper_blue");
            decor.replace("thematic:olive_wallpaper", "abyssal_decor:green_wallpaper");
            decor.replace("thematic:rose_wallpaper", "abyssal_decor:white_wallpaper");
            decor.replace("thematic:starry_night_wallpaper", "abyssal_decor:wallpaper_blue");
            decor.replace("thematic:modern_picket_fence", "abyssal_decor:whitewood_picket_fence");
            decor.replace("thematic:toilet_paper", "abyssal_decor:bog_roll");
        } else {
            decor.remove("thematic:circular_frosted_glass", "thematic:circular_frosted_glass_pane", "thematic:plum_frosted_glass", "thematic:plum_frosted_glass_pane", "thematic:neon_cyan_frosted_glass", "thematic:neon_cyan_frosted_glass_pane", "thematic:neon_light_blue_frosted_glass", "thematic:neon_light_blue_frosted_glass_pane", "thematic:neon_lime_frosted_glass", "thematic:neon_lime_frosted_glass_pane", "thematic:neon_purple_frosted_glass", "thematic:neon_purple_frosted_glass_pane", "thematic:aqua_wallpaper", "thematic:arcade_wallpaper", "thematic:beige_wallpaper", "thematic:blue_bamboo_wallpaper", "thematic:checkered_wallpaper", "thematic:gray_checkered_wallpaper", "thematic:red_checkered_wallpaper", "thematic:yellow_checkered_wallpaper", "thematic:christmas_wallpaper", "thematic:halloween_wallpaper", "thematic:city_night_wallpaper", "thematic:cloud_wallpaper", "thematic:diamond_wallpaper", "thematic:dotted_wallpaper", "thematic:flower_wallpaper", "thematic:hexagon_wallpaper", "thematic:leaf_wallpaper", "thematic:mosaic_wallpaper", "thematic:night_sky_wallpaper", "thematic:olive_wallpaper", "thematic:rose_wallpaper", "thematic:starry_night_wallpaper", "thematic:modern_picket_fence", "thematic:toilet_paper");
        }
        if (ModCompat.MALISIS_DOORS) {
            decor.replace("thematic:paper_door", "malisisdoorsreborn:shoji_door");
        } else {
            decor.remove("thematic:paper_door");
        }
        if (ModCompat.ALEXS_CAVES) {
            decor.replace("thematic:red_candy_cane_block", "alexscaves:candy_cane_block");
        } else {
            decor.remove("thematic:red_candy_cane_block");
        }
        if (ModCompat.REFURBISHED_FURNITURE) {
            decor.replace("thematic:modern_chair", "refurbished_furniture:oak_chair");
            decor.replace("thematic:metal_chair", "refurbished_furniture:oak_chair");
            decor.replace("thematic:glass_chair", "refurbished_furniture:oak_chair");
            decor.replace("thematic:modern_table", "refurbished_furniture:oak_table");
            decor.replace("thematic:metal_table", "refurbished_furniture:oak_table");
            decor.replace("thematic:glass_table", "refurbished_furniture:oak_table");
            decor.replace("thematic:vintage_desk_lamp", "refurbished_furniture:white_lamp");
            decor.replace("thematic:plate_stack", "refurbished_furniture:plate");
            decor.replace("thematic:ground_bathtub", "refurbished_furniture:white_bath");
            decor.replace("thematic:desk_fan", "refurbished_furniture:oak_light_ceiling_fan");
        } else {
            decor.remove("thematic:modern_chair", "thematic:metal_chair", "thematic:glass_chair", "thematic:modern_table", "thematic:metal_table", "thematic:glass_table", "thematic:vintage_desk_lamp", "thematic:plate_stack", "thematic:ground_bathtub", "thematic:desk_fan");
        }
        if (ModCompat.SUPPLEMENTARIES) {
            decor.replace("thematic:chalkboard", "supplementaries:blackboard");
            decor.replace("thematic:alarm_clock", "supplementaries:clock_block");
            decor.replace("thematic:grandfather_clock", "supplementaries:clock_block");
            decor.replace("thematic:grandfather_clock_stackable", "supplementaries:clock_block");
            decor.replace("thematic:wooden_clock", "supplementaries:clock_block");
            decor.replace("thematic:knight_statue", "supplementaries:statue");
            decor.replace("thematic:paladin_statue", "supplementaries:statue");
            decor.replace("thematic:legionnaire_statue", "supplementaries:statue");
        } else {
            decor.remove("thematic:chalkboard", "thematic:alarm_clock", "thematic:grandfather_clock", "thematic:grandfather_clock_stackable", "thematic:wooden_clock", "thematic:knight_statue", "thematic:paladin_statue", "thematic:legionnaire_statue");
        }
        if (ModCompat.ANTOS) {
            decor.replace("thematic:vintage_computer", "antos:computer");
        } else {
            decor.remove("thematic:vintage_computer");
        }
        if (ModCompat.VISTA) {
            decor.replace("thematic:retro_tv", "vista:television");
            decor.replace("thematic:vintage_tv", "vista:television");
        } else {
            decor.remove("thematic:retro_tv", "thematic:vintage_tv");
        }
        decor.remove("thematic:diamond_slab", "thematic:diamond_stairs", "thematic:diamond_wall", "thematic:emerald_slab");
        decor.remove("thematic:emerald_stairs", "thematic:emerald_wall", "thematic:gold_slab", "thematic:gold_stairs");
        decor.remove("thematic:gold_wall", "thematic:iron_slab", "thematic:iron_stairs", "thematic:iron_wall");
        decor.remove("thematic:lapis_slab", "thematic:lapis_stairs", "thematic:lapis_wall", "thematic:coal_slab");
        decor.remove("thematic:coal_stairs", "thematic:coal_wall", "thematic:netherite_wall", "thematic:packed_ice_slab");
        decor.remove("thematic:packed_ice_stairs", "thematic:packed_ice_wall", "thematic:blue_ice_slab", "thematic:blue_ice_stairs");
        decor.remove("thematic:blue_ice_wall", "thematic:paper_wall", "thematic:smooth_paper_wall", "thematic:petal_paper_wall");
        decor.remove("thematic:christmas_tree", "thematic:christmas_star", "thematic:christmas_garland", "thematic:wreath");
        decor.remove("thematic:mistletoe", "thematic:christmas_lights", "thematic:colorful_christmas_lights", "thematic:white_christmas_lights");
        decor.remove("thematic:halloween_lights", "thematic:orange_halloween_lights", "thematic:red_candy_cane_block_slab", "thematic:red_candy_cane_block_stairs");
        decor.remove("thematic:red_candy_cane_block_wall", "thematic:green_candy_cane_block", "thematic:green_candy_cane_block_slab", "thematic:green_candy_cane_block_stairs");
        decor.remove("thematic:green_candy_cane_block_wall", "thematic:velvet_rope_fence", "thematic:derry_billboard", "thematic:raccoon_city_billboard");
        decor.remove("thematic:typewriter", "thematic:calendar", "thematic:paper_holder", "thematic:blender");
        decor.remove("thematic:coffee_maker", "thematic:condiment_bottles", "thematic:cookie_jar", "thematic:knife_block");
        decor.remove("thematic:radiator", "thematic:water_cooler", "thematic:shower", "thematic:birdhouse");
        decor.remove("thematic:broom", "thematic:oak_broom", "thematic:boards", "thematic:stackable_mugs");
        decor.remove("thematic:wine_rack", "thematic:filthy_stain", "thematic:arcade_machine", "thematic:pool_table");
        decor.remove("thematic:modern_piano", "thematic:gold_unicorn", "thematic:funko_display", "thematic:sword_block");
        decor.remove("thematic:sword_display", "thematic:corpse_pyre", "thematic:corpse_pyre2", "thematic:lament_configuration");
        decor.remove("thematic:dark_horse_logo", "thematic:image_comics_logo", "thematic:timely_comics_logo", "thematic:wildstorm_comics_logo");
        if (ModCompat.DIM_DOORS) {
            decor.replace("thematic:terracotta_slab", "dimdoors:terracotta_slab");
            decor.replace("thematic:terracotta_stairs", "dimdoors:terracotta_stairs");
            for (String color : colors()) {
                if (!color.equals("light_gray")) {
                    decor.replace("thematic:" + color + "_terracotta_slab", "dimdoors:" + color + "_terracotta_slab");
                    decor.replace("thematic:" + color + "_terracotta_stairs", "dimdoors:" + color + "_terracotta_stairs");
                }
            }
            for (String id : List.of("button", "door", "fence", "log", "planks", "slab", "stairs", "trapdoor", "wood")) {
                decor.replace("thematic:driftwood_" + id, "dimdoors:driftwood_" + id);
            }
            decor.replace("thematic:driftwood_fence_gate", "dimdoors:driftwood_gate");
        }
        if (ModCompat.MALISIS_DOORS) {
            decor.replace("thematic:steel_door", "malisisdoorsreborn:metal_door");
            decor.replace("thematic:steel_trapdoor", "malisisdoorsreborn:iron_sliding_trapdoor");
        }
        if (ModCompat.ABYSSAL_DECOR) {
            decor.replace("thematic:barbed_wire", "abyssal_decor:barb_barrier_wires");
            decor.replace("thematic:frosted_glass", "abyssal_decor:frosted_glass");
            decor.replace("thematic:frosted_glass_pane", "abyssal_decor:frosted_glass_pane");
            decor.replace("thematic:dirty_glass", "abyssal_decor:grime_glass");
            decor.replace("thematic:dirty_glass_pane", "abyssal_decor:grime_glass_pane");
            decor.replace("thematic:desk_bell", "abyssal_decor:desk_bell");
            decor.replace("thematic:life_preserver", "abyssal_decor:life_preserver");
            decor.replace("thematic:fox_wallpaper", "abyssal_decor:wallpaper_light_blue");
            for (String color : colors()) {
                String target = switch (color) {
                    case "blue", "cyan", "light_blue", "magenta", "purple" -> "wallpaper_" + color;
                    default -> color + "_wallpaper";
                };
                decor.replace("thematic:" + color + "_wallpaper", "abyssal_decor:" + target);
            }
        }
        if (ModCompat.SUPPLEMENTARIES) {
            for (String id : List.of("gold_candelabra", "gold_candlestick", "iron_candelabra", "iron_candlestick", "nth_metal_candelabra",
                    "nth_metal_candlestick")) {
                decor.replace("thematic:" + id, "supplementaries:candle_holder");
            }
            decor.replace("thematic:blackboard", "supplementaries:blackboard");
            decor.replace("thematic:globe", "supplementaries:globe");
            decor.replace("thematic:clock", "supplementaries:clock_block");
        }
        if (ModCompat.REFURBISHED_FURNITURE) {
            for (String id : List.of("lamp", "table_lamp", "lamp_stackable")) {
                decor.replace("thematic:" + id, "refurbished_furniture:white_lamp");
            }
            decor.replace("thematic:ceiling_light", "refurbished_furniture:light_ceiling_light");
            decor.replace("thematic:hanging_ceiling_light", "refurbished_furniture:dark_ceiling_light");
            decor.replace("thematic:fridge", "refurbished_furniture:light_fridge");
            decor.replace("thematic:stainless_steel_fridge", "refurbished_furniture:light_fridge");
            decor.replace("thematic:fridge_black", "refurbished_furniture:dark_fridge");
            decor.replace("thematic:oven", "refurbished_furniture:light_stove");
            decor.replace("thematic:stainless_steel_oven", "refurbished_furniture:dark_stove");
            decor.replace("thematic:microwave", "refurbished_furniture:light_microwave");
            decor.replace("thematic:toaster", "refurbished_furniture:light_toaster");
            for (String id : List.of("counter", "counter_corner", "gray_counter", "gray_counter_corner")) {
                decor.replace("thematic:" + id, "refurbished_furniture:white_kitchen_cabinetry");
            }
            decor.replace("thematic:counter_sink", "refurbished_furniture:white_kitchen_sink");
            decor.replace("thematic:gray_counter_sink", "refurbished_furniture:white_kitchen_sink");
            decor.replace("thematic:bathroom_sink", "refurbished_furniture:white_basin");
            decor.replace("thematic:bathtub", "refurbished_furniture:white_bath");
            decor.replace("thematic:toilet", "refurbished_furniture:white_toilet");
            decor.replace("thematic:toilet_no_tp", "refurbished_furniture:white_toilet");
            decor.replace("thematic:ceiling_fan", "refurbished_furniture:oak_light_ceiling_fan");
            decor.replace("thematic:cutting_board", "refurbished_furniture:oak_cutting_board");
            decor.replace("thematic:plate", "refurbished_furniture:plate");
            decor.replace("thematic:mailbox", "refurbished_furniture:oak_mail_box");
            decor.replace("thematic:doorbell", "refurbished_furniture:doorbell");
            decor.replace("thematic:cabinet", "refurbished_furniture:oak_storage_cabinet");
            decor.replace("thematic:nightstand", "refurbished_furniture:oak_drawer");
            for (String color : colors()) {
                decor.replace("thematic:" + color + "_couch", "refurbished_furniture:" + color + "_sofa");
            }
            for (String wood : List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "mangrove", "cherry", "crimson", "warped")) {
                for (String kind : List.of("chair", "table", "crate")) {
                    decor.replace("thematic:" + wood + "_" + kind, "refurbished_furniture:" + wood + "_" + kind);
                }
            }
        }
        if (ModCompat.VISTA) {
            decor.replace("thematic:tv", "vista:television");
            decor.replace("thematic:tv_large", "vista:television");
        }
        if (ModCompat.NATURES_SPIRIT) {
            for (String id : List.of("button", "door", "fence", "fence_gate", "hanging_sign", "leaves", "log", "planks", "pressure_plate", "sapling",
                    "sign", "slab", "stairs", "trapdoor", "vines", "wood")) {
                decor.replace("thematic:willow_" + id, "natures_spirit:willow_" + id);
            }
            decor.replace("thematic:stripped_willow_log", "natures_spirit:stripped_willow_log");
            decor.replace("thematic:stripped_willow_wood", "natures_spirit:stripped_willow_wood");
        }
        if (ModCompat.VANILLA_BACKPORT) {
            for (String color : colors()) {
                for (String kind : List.of("concrete_slab", "concrete_stairs", "wool_slab", "wool_stairs")) {
                    decor.replace("thematic:" + color + "_" + kind, "minecraft:" + color + "_" + kind);
                }
            }
        }

        List<String> weapons = List.of("auto_rifle", "crowbar", "grenade_launcher", "grenade", "knife", "lmg", "molotov", "pistol", "rifle",
                "rocket_launcher", "shotgun", "smg", "sniper");
        decor.hide("thematic:criminal_spawn_egg", "thematic:criminal_strong_spawn_egg", "thematic:penguin_criminal_spawn_egg",
                "thematic:penguin_strong_spawn_egg", "thematic:penguin_heavy_spawn_egg", "thematic:penguin_heavy_lmg_spawn_egg",
                "thematic:penguin_heavy_shotgun_spawn_egg", "thematic:twoface_criminal_spawn_egg", "thematic:twoface_strong_spawn_egg",
                "thematic:twoface_heavy_spawn_egg", "thematic:twoface_heavy_lmg_spawn_egg", "thematic:twoface_heavy_shotgun_spawn_egg");
        for (String weapon : weapons) {
            decor.hide("thematic:" + weapon + "_criminal_spawn_egg", "thematic:criminal_strong_" + weapon + "_spawn_egg",
                    "thematic:penguin_criminal_" + weapon + "_spawn_egg", "thematic:penguin_strong_" + weapon + "_spawn_egg",
                    "thematic:twoface_criminal_" + weapon + "_spawn_egg", "thematic:twoface_strong_" + weapon + "_spawn_egg");
        }
        for (String prisoner : List.of("prisoner", "prisoner_strong")) {
            decor.hide("thematic:" + prisoner + "_spawn_egg");
            for (String weapon : List.of("crowbar", "grenade", "knife", "molotov")) {
                decor.hide("thematic:" + prisoner + "_" + weapon + "_spawn_egg");
            }
        }
        for (String id : List.of("arm_wrap", "base", "bola_armor", "camera", "cig", "comms_device", "dinnerplate", "embrace_fear", "fantastic_ult",
                "gold_lasso", "silver_lasso", "tea_cup", "wilfred", "icy_block", "hologram", "wall_hologram", "wave_spawner", "engraved_lodestone",
                "ooze", "speed_force_block", "speed_force_floor", "quantum_electron", "quantum_membrane_inner", "quantum_membrane_outer",
                "quantum_nucleon", "quantum_nucleus", "suspicious_soul_soil")) {
            decor.hide("thematic:" + id);
        }
        for (String glass : List.of("amber", "blue", "cyan", "lime", "magenta", "red", "violet", "white")) {
            decor.hide("thematic:quantum_glass_" + glass);
        }
    }

    private static void stoneSet(Group group, String from, String block, String slab, String stairs, String wall) {
        group.replace(from, block);
        group.replace(from + "_slab", slab);
        group.replace(from + "_stairs", stairs);
        group.replace(from + "_wall", wall);
    }

    private static void metal(Group group, String from, String to, String nugget) {
        group.replace("thematic:" + from + "_ingot", "minecraft:" + to + "_ingot");
        group.replace("thematic:raw_" + from, "minecraft:raw_" + to);
        group.replace("thematic:" + from + "_block", "minecraft:" + to + "_block");
        group.replace("thematic:raw_" + from + "_block", "minecraft:raw_" + to + "_block");
        group.replace("thematic:" + from + "_ore", "minecraft:" + to + "_ore");
        group.replace("thematic:deepslate_" + from + "_ore", "minecraft:deepslate_" + to + "_ore");
        if (nugget == null) {
            group.remove("thematic:" + from + "_nugget");
        } else {
            group.replace("thematic:" + from + "_nugget", nugget);
        }
    }

    private static void gem(Group group, String from, String gem, String block, String ore, String deepslateOre) {
        group.replace("thematic:" + from, gem);
        group.replace("thematic:" + from + "_block", block);
        group.replace("thematic:" + from + "_ore", ore);
        group.replace("thematic:deepslate_" + from + "_ore", deepslateOre);
    }

    private static List<String> colors() {
        return List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    }

    private static void removeShapes(Group group, String material) {
        group.remove("thematic:" + material + "_slab", "thematic:" + material + "_stairs", "thematic:" + material + "_wall");
    }

    private Cleanup() {
    }

    private static Group group(ModConfigSpec.BooleanValue toggle) {
        Group group = new Group(toggle);
        GROUPS.add(group);
        return group;
    }

    private static Active active() {
        Active current = active;
        if (current == null) {
            current = new Active();
            for (Group group : GROUPS) {
                if (group.toggle == null || CleanupConfig.on(group.toggle)) {
                    current.removals.removeAll(group.replacements.keySet());
                    current.replacements.keySet().removeAll(group.removals);
                    current.dyes.keySet().removeAll(group.removals);
                    current.dyes.keySet().removeAll(group.replacements.keySet());
                    current.replacements.putAll(group.replacements);
                    current.dyes.putAll(group.dyes);
                    current.removals.addAll(group.removals);
                    current.retired.addAll(group.retired);
                    current.hidden.addAll(group.hidden);
                    current.tags.putAll(group.tags);
                    current.features.addAll(group.features);
                    current.renames.putAll(group.renames);
                    current.droppedRecipes.addAll(group.droppedRecipes);
                    current.droppedTypes.addAll(group.droppedTypes);
                }
            }
            if (CleanupConfig.SPEC.isLoaded()) {
                active = current;
            }
        }
        return current;
    }

    public static Map<ResourceLocation, ResourceLocation> replacements() {
        return active().replacements;
    }

    public static Set<ResourceLocation> removals() {
        return active().removals;
    }

    public static Map<ResourceLocation, Integer> dyes() {
        return active().dyes;
    }

    public static Map<String, String> tagSwaps() {
        return active().tags;
    }

    public static Set<ResourceLocation> removedFeatures() {
        return active().features;
    }

    public static boolean dropsRecipe(ResourceLocation id) {
        return active().droppedRecipes.contains(id);
    }

    public static boolean dropsRecipeType(String type) {
        return type != null && active().droppedTypes.contains(type);
    }

    public static boolean isGone(ResourceLocation id) {
        Active current = active();
        return current.replacements.containsKey(id) || current.removals.contains(id);
    }

    public static boolean dropsRecipesFor(ResourceLocation id) {
        return isGone(id) || active().retired.contains(id);
    }

    public static Set<ResourceLocation> randomExclusions() {
        Active current = active();
        Set<ResourceLocation> ids = new HashSet<>(current.removals);
        ids.addAll(current.replacements.keySet());
        ids.addAll(current.retired);
        ids.addAll(current.hidden);
        return ids;
    }

    public static boolean isHidden(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        return dropsRecipesFor(id) || active().hidden.contains(id);
    }

    public static Optional<String> rename(Item item) {
        return Optional.ofNullable(active().renames.get(BuiltInRegistries.ITEM.getKey(item)));
    }

    public static ItemStack convert(ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        Active current = active();
        if (current.removals.contains(id)) {
            return ItemStack.EMPTY;
        }
        ResourceLocation target = current.replacements.get(id);
        if (target == null) {
            return stack;
        }
        Optional<Item> item = BuiltInRegistries.ITEM.getOptional(target);
        if (item.isEmpty()) {
            return stack;
        }
        ItemStack converted = new ItemStack(item.get(), stack.getCount());
        Integer dye = current.dyes.get(id);
        if (dye != null) {
            converted.set(DataComponents.DYED_COLOR, new DyedItemColor(dye, true));
        }
        return converted;
    }

    public static boolean repairsAs(Ingredient ingredient, ItemStack repair) {
        if (repair.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(repair.getItem());
        for (Map.Entry<ResourceLocation, ResourceLocation> entry : active().replacements.entrySet()) {
            if (entry.getValue().equals(id)) {
                Optional<Item> original = BuiltInRegistries.ITEM.getOptional(entry.getKey());
                if (original.isPresent() && ingredient.test(new ItemStack(original.get()))) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean needsConversion(ItemStack stack) {
        return !stack.isEmpty() && isGone(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static final class Active {
        final Map<ResourceLocation, ResourceLocation> replacements = new HashMap<>();
        final Map<ResourceLocation, Integer> dyes = new HashMap<>();
        final Set<ResourceLocation> removals = new HashSet<>();
        final Set<ResourceLocation> retired = new HashSet<>();
        final Set<ResourceLocation> hidden = new HashSet<>();
        final Map<String, String> tags = new HashMap<>();
        final Set<ResourceLocation> features = new HashSet<>();
        final Map<ResourceLocation, String> renames = new HashMap<>();
        final Set<ResourceLocation> droppedRecipes = new HashSet<>();
        final Set<String> droppedTypes = new HashSet<>();
    }

    private static final class Group {
        final ModConfigSpec.BooleanValue toggle;
        final Map<ResourceLocation, ResourceLocation> replacements = new HashMap<>();
        final Map<ResourceLocation, Integer> dyes = new HashMap<>();
        final Set<ResourceLocation> removals = new HashSet<>();
        final Set<ResourceLocation> retired = new HashSet<>();
        final Set<ResourceLocation> hidden = new HashSet<>();
        final Map<String, String> tags = new HashMap<>();
        final Set<ResourceLocation> features = new HashSet<>();
        final Map<ResourceLocation, String> renames = new HashMap<>();
        final Set<ResourceLocation> droppedRecipes = new HashSet<>();
        final Set<String> droppedTypes = new HashSet<>();

        Group(ModConfigSpec.BooleanValue toggle) {
            this.toggle = toggle;
        }

        void replace(String from, String to) {
            this.replacements.put(ResourceLocation.parse(from), ResourceLocation.parse(to));
        }

        void replaceDyed(String from, String to, int rgb) {
            this.replace(from, to);
            this.dyes.put(ResourceLocation.parse(from), rgb);
        }

        void remove(String... ids) {
            for (String id : ids) {
                this.removals.add(ResourceLocation.parse(id));
            }
        }

        void retire(String... ids) {
            for (String id : ids) {
                this.retired.add(ResourceLocation.parse(id));
            }
        }

        void hide(String... ids) {
            for (String id : ids) {
                this.hidden.add(ResourceLocation.parse(id));
            }
        }

        void tag(String from, String to) {
            this.tags.put(from, to);
        }

        void feature(String id) {
            this.features.add(ResourceLocation.parse(id));
        }

        void dropRecipe(String id) {
            this.droppedRecipes.add(ResourceLocation.parse(id));
        }

        void dropType(String type) {
            this.droppedTypes.add(type);
        }

        void rename(String id, String key) {
            this.renames.put(ResourceLocation.parse(id), key);
        }
    }
}
