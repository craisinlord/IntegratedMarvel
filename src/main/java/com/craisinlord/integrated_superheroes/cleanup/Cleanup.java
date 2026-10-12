package com.craisinlord.integrated_superheroes.cleanup;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
        extras.remove("infinity_gauntlet:cosmi_rod", "infinity_gauntlet:the_sceptre", "infinity_gauntlet:infinity_stone_tracker",
                "infinity_gauntlet:infinity_stone_case");
        for (String stone : List.of("power", "space", "reality", "soul", "time", "mind")) {
            extras.remove("infinity_gauntlet:cosmi_rod_" + stone);
        }
        extras.feature("infinity_gauntlet:mjolnir_structure_feature");

        Group titanium = group(CleanupConfig.TITANIUM);
        for (String id : List.of("titanium_ingot", "raw_titanium", "titanium_nugget", "titanium_block", "raw_titanium_block",
                "titanium_ore", "deepslate_titanium_ore")) {
            titanium.replace("marvel:" + id, "antarchy:" + id);
        }
        titanium.feature("marvel:ore_titanium");

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
                    current.replacements.putAll(group.replacements);
                    current.removals.addAll(group.removals);
                    current.retired.addAll(group.retired);
                    current.tags.putAll(group.tags);
                    current.features.addAll(group.features);
                    current.renames.putAll(group.renames);
                    current.droppedRecipes.addAll(group.droppedRecipes);
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

    public static Map<String, String> tagSwaps() {
        return active().tags;
    }

    public static Set<ResourceLocation> removedFeatures() {
        return active().features;
    }

    public static boolean dropsRecipe(ResourceLocation id) {
        return active().droppedRecipes.contains(id);
    }

    public static boolean isGone(ResourceLocation id) {
        Active current = active();
        return current.replacements.containsKey(id) || current.removals.contains(id);
    }

    public static boolean dropsRecipesFor(ResourceLocation id) {
        return isGone(id) || active().retired.contains(id);
    }

    public static boolean isHidden(Item item) {
        return dropsRecipesFor(BuiltInRegistries.ITEM.getKey(item));
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
        return item.map(value -> new ItemStack(value, stack.getCount())).orElse(stack);
    }

    public static boolean needsConversion(ItemStack stack) {
        return !stack.isEmpty() && isGone(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    private static final class Active {
        final Map<ResourceLocation, ResourceLocation> replacements = new HashMap<>();
        final Set<ResourceLocation> removals = new HashSet<>();
        final Set<ResourceLocation> retired = new HashSet<>();
        final Map<String, String> tags = new HashMap<>();
        final Set<ResourceLocation> features = new HashSet<>();
        final Map<ResourceLocation, String> renames = new HashMap<>();
        final Set<ResourceLocation> droppedRecipes = new HashSet<>();
    }

    private static final class Group {
        final ModConfigSpec.BooleanValue toggle;
        final Map<ResourceLocation, ResourceLocation> replacements = new HashMap<>();
        final Set<ResourceLocation> removals = new HashSet<>();
        final Set<ResourceLocation> retired = new HashSet<>();
        final Map<String, String> tags = new HashMap<>();
        final Set<ResourceLocation> features = new HashSet<>();
        final Map<ResourceLocation, String> renames = new HashMap<>();
        final Set<ResourceLocation> droppedRecipes = new HashSet<>();

        Group(ModConfigSpec.BooleanValue toggle) {
            this.toggle = toggle;
        }

        void replace(String from, String to) {
            this.replacements.put(ResourceLocation.parse(from), ResourceLocation.parse(to));
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

        void tag(String from, String to) {
            this.tags.put(from, to);
        }

        void feature(String id) {
            this.features.add(ResourceLocation.parse(id));
        }

        void dropRecipe(String id) {
            this.droppedRecipes.add(ResourceLocation.parse(id));
        }

        void rename(String id, String key) {
            this.renames.put(ResourceLocation.parse(id), key);
        }
    }
}
