package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.IntegratedSuperheroes;
import com.craisinlord.integrated_superheroes.compat.ModCompat;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicLookGroups;
import com.craisinlord.integrated_superheroes.compat.thematic.PowerOwnership;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SerumLooks {
    public record Look(String id, String power, String skin, String costume) {
        public Component name() {
            return SerumLooks.name(this.costume);
        }
    }

    private static final Map<String, List<String>> RELATED = Map.of(
            "kryptonian_clark_kent", List.of("thematic:supergirl", "thematic:superboy", "thematic:jon_kent", "thematic:bizarro"),
            "shazam_billy_batson", List.of("thematic:mary_shazam", "thematic:darla_shazam", "thematic:eugene_shazam",
                    "thematic:freddy_shazam", "thematic:pedro_shazam", "thematic:black_adam"),
            "weapon_x_logan", List.of("thematic:x23"),
            "spider_powers_peter_parker", List.of("thematic:spiderman_2099", "thematic:spiderwoman", "thematic:ghost_spider"),
            "martian_jonn_jonzz", List.of("thematic:miss_martian"),
            "amazon_diana_prince", List.of("thematic:wonder_girl", "thematic:barda", "thematic:cheetah"),
            "compound_v_homelander", List.of("thematic:maeve"),
            "sonic_dinah_lance", List.of("thematic:silver_banshee"),
            "super_soldier_steve_rogers", List.of("thematic:soldier_boy"),
            "gamma_mutation_bruce_banner", List.of("thematic:thanos")
    );

    private record Alias(String set, Set<String> skip) {
    }

    private static final String FANTASTIC_FOUR = "integrated_superheroes:fantastic_four";
    private static final Map<String, Alias> ALIASES = Map.of(
            FANTASTIC_FOUR, new Alias("marvellegacy:spiderman_bagman", Set.of("helmet"))
    );

    private static final Map<String, List<String>> MARVEL_SETS = Map.ofEntries(
            Map.entry("spider_powers_peter_parker", List.of("marvellegacy:spiderman_classic", "marvellegacy:spiderman_original",
                    "marvellegacy:spiderman_tasmii", "marvel:spider_man", "marvel:spider_man_mcu",
                    "marvel:spider_man_insomniac", "marvel:miles_morales_suit", "marvel:spider_gwen")),
            Map.entry("weapon_x_logan", List.of("marvellegacy:wolverine_classic", "marvellegacy:wolverine_original", "marvel:wolverine",
                    "marvel:wolverine_brown", "marvel:wolverine_x_force")),
            Map.entry("weapon_x_wade_wilson", List.of("marvel:deadpool", "marvel:deadpool_x_force")),
            Map.entry("optic_blasts_scott_summers", List.of("marvel:cyclops", "marvel:cyclops_astonishing")),
            Map.entry("super_soldier_steve_rogers", List.of("marvel:captain_america", "marvel:captain_america_stealth", "marvel:captain_carter",
                    "marvel:red_guardian", "marvel:us_agent")),
            Map.entry("binary_carol_danvers", List.of("marvel:captain_marvel", "marvel:captain_marvel_838")),
            Map.entry("heart_shaped_herb_tchalla", List.of("marvel:black_panther")),
            Map.entry("super_speed_pietro_maximoff", List.of("marvel:quicksilver", "marvel:quicksilver_mcu")),
            Map.entry("elasticity_reed_richards", List.of(FANTASTIC_FOUR)),
            Map.entry("flame_on_johnny_storm", List.of(FANTASTIC_FOUR)),
            Map.entry("invisibility_sue_storm", List.of(FANTASTIC_FOUR)),
            Map.entry("rock_hide_ben_grimm", List.of(FANTASTIC_FOUR))
    );

    private static final String[] PIECES = {"chestplate", "helmet", "leggings", "boots"};
    private static final Map<String, List<Look>> CACHE = new ConcurrentHashMap<>();

    private SerumLooks() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener((TagsUpdatedEvent event) -> CACHE.clear());
    }

    public static List<Look> forPower(String power) {
        if (power == null || power.isEmpty()) {
            return List.of();
        }
        return CACHE.computeIfAbsent(power, SerumLooks::build);
    }

    public static Optional<Look> byId(String power, String id) {
        return forPower(power).stream().filter(look -> look.id().equals(id)).findFirst();
    }

    public static Optional<Look> byCostume(String power, String skin, String costume) {
        if (costume == null || costume.isEmpty()) {
            return Optional.empty();
        }
        return forPower(power).stream().filter(look -> look.costume().equals(costume) && look.skin().equals(skin)).findFirst();
    }

    public static Optional<Look> anywhere(String id) {
        Set<String> powers = new LinkedHashSet<>(SerumCostumes.powers());
        SerumSkins.all().forEach(skin -> powers.add(skin.powerId()));
        for (String power : powers) {
            Optional<Look> look = byId(power, id);
            if (look.isPresent()) {
                return look;
            }
        }
        return Optional.empty();
    }

    public static boolean marvelSet(String costume) {
        ResourceLocation id = ResourceLocation.tryParse(costume);
        return id != null && !BuiltInRegistries.ITEM.containsKey(id) && piece(costume, "chestplate") != null;
    }

    public static Item piece(String set, String slot) {
        Alias alias = ALIASES.get(set);
        if (alias != null) {
            if (alias.skip().contains(slot)) {
                return null;
            }
            set = alias.set();
        }
        ResourceLocation id = ResourceLocation.tryParse(set + "_" + slot);
        return id != null && BuiltInRegistries.ITEM.containsKey(id) ? BuiltInRegistries.ITEM.get(id) : null;
    }

    public static Set<Item> costumeItems() {
        Set<Item> items = new HashSet<>();
        MARVEL_SETS.values().forEach(sets -> sets.forEach(set -> {
            for (String slot : PIECES) {
                Item item = piece(set, slot);
                if (item != null) {
                    items.add(item);
                }
            }
            Item sleeveless = piece(set + "_sleeveless", "chestplate");
            if (sleeveless != null) {
                items.add(sleeveless);
            }
        }));
        BuiltInRegistries.ITEM.getTag(TagKey.create(Registries.ITEM, IntegratedSuperheroes.id("thematic_costume")))
                .ifPresent(tag -> tag.forEach(holder -> items.add(holder.value())));
        return items;
    }

    static Component name(String costume) {
        ResourceLocation id = ResourceLocation.tryParse(costume);
        if (id != null && BuiltInRegistries.ITEM.containsKey(id)) {
            return new ItemStack(BuiltInRegistries.ITEM.get(id)).getHoverName();
        }
        Item chest = piece(costume, "chestplate");
        return chest == null ? Component.literal(costume) : new ItemStack(chest).getHoverName();
    }

    public static String preferred(String power) {
        List<String> sets = MARVEL_SETS.getOrDefault(power, List.of());
        for (String namespace : List.of("marvellegacy:", "marvel:")) {
            for (String set : sets) {
                Alias alias = ALIASES.get(set);
                String real = alias == null ? set : alias.set();
                if (real.startsWith(namespace) && marvelSet(set)) {
                    return set;
                }
            }
        }
        return SerumCostumes.forPower(power);
    }

    private static List<Look> build(String power) {
        List<Look> looks = new ArrayList<>();
        Set<String> taken = new HashSet<>(Set.of(SkinData.DEFAULT, SkinData.OFF));
        SerumSkins.all().forEach(skin -> taken.add(skin.id()));
        Set<String> seen = new HashSet<>();
        String base = SerumCostumes.forPower(power);
        reserve(preferred(power), taken, seen);
        List<SerumSkin> skins = SerumSkins.forPower(power);
        skins.forEach(skin -> reserve(skin.costume(), taken, seen));
        if (!base.isEmpty() && thematicSuit(base) && seen.add(base)) {
            looks.add(new Look(unique(path(base), taken), power, "", base));
        }
        thematic(looks, power, "", base, taken, seen);
        Set<String> relatedLooks = new LinkedHashSet<>(RELATED.getOrDefault(power, List.of()));
        relatedLooks.addAll(PowerOwnership.looks(power));
        for (String related : relatedLooks) {
            if (thematicSuit(related) && seen.add(related)) {
                looks.add(new Look(unique(path(related), taken), power, "", related));
            }
            if (!com.craisinlord.integrated_superheroes.cleanup.CosmeticMerges.serumCostume(related)) {
                thematic(looks, power, "", related, taken, seen);
            }
        }
        for (String set : MARVEL_SETS.getOrDefault(power, List.of())) {
            if (marvelSet(set) && seen.add(set)) {
                looks.add(new Look(unique(path(set).replaceAll("_suit$", ""), taken), power, "", set));
            }
        }
        for (SerumSkin skin : skins) {
            thematic(looks, power, skin.id(), skin.costume(), taken, seen);
        }
        return List.copyOf(looks);
    }

    private static void thematic(List<Look> looks, String power, String skin, String base, Set<String> taken, Set<String> seen) {
        if (base == null || base.isEmpty() || !ModCompat.THEMATIC) {
            return;
        }
        for (String member : ThematicLookGroups.members(base)) {
            if (seen.add(member)) {
                looks.add(new Look(unique(path(member), taken), power, skin, member));
            }
        }
    }

    private static boolean thematicSuit(String id) {
        return ModCompat.THEMATIC && ThematicLookGroups.isSuit(id);
    }

    private static void reserve(String costume, Set<String> taken, Set<String> seen) {
        if (costume != null && !costume.isEmpty()) {
            taken.add(path(costume));
            seen.add(costume);
        }
    }

    private static String path(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    private static String unique(String name, Set<String> taken) {
        String result = name;
        while (!taken.add(result)) {
            result = result + "_alt";
        }
        return result;
    }
}
