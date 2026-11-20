package com.craisinlord.integrated_superheroes.hero.skin;

import com.craisinlord.integrated_superheroes.hero.HulkPower;
import com.craisinlord.integrated_superheroes.hero.ShazamPower;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public final class SerumCostumes {
    private static final Map<String, String> BY_POWER = new ConcurrentHashMap<>(Map.ofEntries(
            Map.entry("spider_powers_peter_parker", "thematic:spiderman"),
            Map.entry("weapon_x_logan", "thematic:wolverine"),
            Map.entry("super_soldier_steve_rogers", "thematic:captain_america"),
            Map.entry("heart_shaped_herb_tchalla", "thematic:black_panther"),
            Map.entry("weapon_x_wade_wilson", "thematic:deadpool"),
            Map.entry("super_speed_pietro_maximoff", "thematic:quicksilver"),
            Map.entry("optic_blasts_scott_summers", "thematic:cyclops"),
            Map.entry("binary_carol_danvers", "thematic:captain_marvel"),
            Map.entry("gamma_mutation_bruce_banner", "thematic:hulk"),
            Map.entry("magnetism_erik_lehnsherr", "thematic:magneto"),
            Map.entry("weather_manipulation_ororo_munroe", "thematic:storm"),
            Map.entry("cryokinesis_bobby_drake", "thematic:iceman"),
            Map.entry("chaos_magic_wanda_maximoff", "thematic:scarlet_witch"),
            Map.entry("organic_steel_piotr_rasputin", "thematic:colossus"),
            Map.entry("radar_sense_matt_murdock", "thematic:daredevil"),
            Map.entry("phasing_kitty_pryde", "thematic:kitty_pryde"),
            Map.entry("squirrel_powers_doreen_green", "thematic:squirrel_girl"),
            Map.entry("flame_on_johnny_storm", "thematic:human_torch"),
            Map.entry("invisibility_sue_storm", "thematic:invisible_woman"),
            Map.entry("elasticity_reed_richards", "thematic:mr_fantastic"),
            Map.entry("rock_hide_ben_grimm", "thematic:the_thing"),
            Map.entry("kinetic_charge_remy_lebeau", "thematic:gambit"),
            Map.entry("absorption_anna_marie", "thematic:rogue"),
            Map.entry("pyrotechnics_jubilation_lee", "thematic:jubilee"),
            Map.entry("kryptonian_clark_kent", "thematic:superman"),
            Map.entry("martian_jonn_jonzz", "thematic:martian_manhunter"),
            Map.entry("shazam_billy_batson", "thematic:shazam"),
            Map.entry("atlantean_arthur_curry", "thematic:aquaman"),
            Map.entry("amazon_diana_prince", "thematic:wonder_woman"),
            Map.entry("sonic_dinah_lance", "thematic:black_canary"),
            Map.entry("teleportation_kurt_wagner", "thematic:nightcrawler"),
            Map.entry("compound_v_homelander", "thematic:homelander")
    ));

    private static final Map<String, Predicate<Player>> SHOWN_WHEN = new ConcurrentHashMap<>(Map.of(
            HulkPower.ID, HulkPower::isHulk,
            ShazamPower.ID, ShazamPower::isPowered
    ));

    private SerumCostumes() {
    }

    public static void showWhen(String powerId, Predicate<Player> condition) {
        SHOWN_WHEN.put(powerId, condition);
    }

    public static boolean shows(String powerId, Player player) {
        Predicate<Player> condition = SHOWN_WHEN.get(powerId);
        return condition == null || condition.test(player);
    }

    public static void register(String powerId, String costume) {
        if (costume == null || costume.isEmpty()) {
            BY_POWER.remove(powerId);
        } else {
            BY_POWER.put(powerId, costume);
        }
    }

    public static java.util.Set<String> powers() {
        return java.util.Set.copyOf(BY_POWER.keySet());
    }

    public static String forPower(String powerId) {
        return powerId == null ? "" : BY_POWER.getOrDefault(powerId, "");
    }

    public static String forSkin(String skinId) {
        String costume = ExistingSerumSkins.COSTUMES.get(skinId);
        if (costume == null) {
            costume = NewSerumSkins.COSTUMES.get(skinId);
        }
        return costume == null ? "" : costume;
    }
}
