package com.craisinlord.integrated_superheroes.voidrealm.variant.moves;

import com.craisinlord.integrated_superheroes.hero.HeroAbilities;
import com.craisinlord.integrated_superheroes.hero.HeroFx;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.craisinlord.integrated_superheroes.voidrealm.variant.VariantProfile;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.SerumMoves;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.StreetMoves;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.SuitMoves;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.VariantBody;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.VariantBody.Flight;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HeroMoves {
    private static final Map<String, VariantMoves.Move> MOVES = new HashMap<>();
    private static final Map<String, List<String>> SERUM_KITS = new HashMap<>();
    private static final Map<String, Flight> SERUM_FLIGHT = new HashMap<>();
    private static final Pattern MARK = Pattern.compile("mark_(\\d+)");

    static {
        SerumMoves.register(MOVES);
        SuitMoves.register(MOVES);
        StreetMoves.register(MOVES);
        com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.MarvelKitMoves.register(MOVES);
        com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.DcKitMoves.register(MOVES);
        com.craisinlord.integrated_superheroes.voidrealm.variant.moves.hero.IconKitMoves.register(MOVES);
        serum("kryptonian_clark_kent", Flight.SWOOP, "heat_vision", "freeze_breath", "flying_punch", "thunder_clap");
        serum("compound_v_homelander", Flight.SWOOP, "heat_vision", "flying_punch", "thunder_clap");
        serum("martian_jonn_jonzz", Flight.SWOOP, "heat_vision", "psychic_blast", "flying_punch");
        serum("shazam_billy_batson", Flight.SWOOP, "thunder_call", "lightning_storm", "flying_punch");
        serum("viltrumite_blood_omni_man", Flight.SWOOP, "flying_punch", "dive", "thunder_clap");
        serum("absorption_anna_marie", Flight.SWOOP, "life_drain", "flying_punch");
        serum("winged_mutation_warren_worthington", Flight.SWOOP, "storm_gust", "dive");
        serum("binary_carol_danvers", Flight.HOVER, "photon_blast", "binary_nova", "flying_punch");
        serum("weather_manipulation_ororo_munroe", Flight.HOVER, "thunder_call", "lightning_storm", "storm_gust");
        serum("flame_on_johnny_storm", Flight.HOVER, "fireball_volley", "nova_burst");
        serum("power_cosmic_norrin_radd", Flight.HOVER, "cosmic_blast", "flying_punch");
        serum("magnetism_erik_lehnsherr", Flight.LEVITATE, "magnet_pull", "magnetic_crush");
        serum("telekinesis_jean_grey", Flight.LEVITATE, "telekinetic_throw", "phoenix_burst");
        serum("chaos_magic_wanda_maximoff", Flight.LEVITATE, "chaos_hex", "hex_storm");
        serum("optic_blasts_scott_summers", Flight.NONE, "optic_blast", "optic_sweep", "lunge");
        serum("cryokinesis_bobby_drake", Flight.NONE, "freeze_breath", "ice_prison", "lunge");
        serum("teleportation_kurt_wagner", Flight.NONE, "bamf_strike", "bamf_ballet");
        serum("super_speed_pietro_maximoff", Flight.NONE, "blitz", "speed_dash");
        serum("gamma_mutation_bruce_banner", Flight.NONE, "hulk_smash", "gamma_leap", "thunder_clap", "hulk_roar");
        serum("rock_hide_ben_grimm", Flight.NONE, "ground_pound", "thunder_clap", "lunge");
        serum("organic_steel_piotr_rasputin", Flight.NONE, "ground_pound", "lunge");
        serum("cyttorak_avatar_cain_marko", Flight.NONE, "unstoppable_charge", "ground_pound");
        serum("super_soldier_steve_rogers", Flight.NONE, "shield_throw", "shield_bash", "lunge");
        serum("heart_shaped_herb_tchalla", Flight.NONE, "combo_flurry", "kinetic_burst", "lunge");
        serum("weapon_x_wade_wilson", Flight.NONE, "combo_flurry", "pistol_shots", "smoke");
        serum("weapon_x_logan", Flight.NONE, "claw_flurry", "berserker_pounce");
        serum("spider_powers_peter_parker", Flight.NONE, "web_shot", "web_yank", "lunge");
        serum("kinetic_charge_remy_lebeau", Flight.NONE, "kinetic_cards", "lunge");
        serum("sonic_dinah_lance", Flight.NONE, "sonic_scream", "lunge");
        serum("telepathy_charles_xavier", Flight.NONE, "psychic_blast", "psychic_wave");
        serum("invisibility_sue_storm", Flight.NONE, "force_wave", "vanish");
        serum("elasticity_reed_richards", Flight.NONE, "stretch_punch", "stretch_grab");
        serum("phasing_kitty_pryde", Flight.NONE, "phase_strike", "lunge");
        serum("pyrotechnics_jubilation_lee", Flight.NONE, "plasmoids", "flash");
        serum("atlantean_arthur_curry", Flight.NONE, "tidal_surge", "lunge");
        serum("amazon_diana_prince", Flight.NONE, "lasso_pull", "slam", "lunge");
        serum("spirit_of_vengeance_johnny_blaze", Flight.NONE, "hellfire", "penance_chain");
        serum("flora_colossus_groot", Flight.NONE, "root_snare", "slam");
        serum("avatar_of_the_green_alec_holland", Flight.NONE, "vine_snare", "slam");
        serum("acrobatics_hank_mccoy", Flight.NONE, "combo_flurry", "lunge", "slam");
        serum("radar_sense_matt_murdock", Flight.NONE, "billy_club_throw", "grapple_takedown", "takedown", "staff_sweep", "radar_dodge");
        serum("squirrel_powers_doreen_green", Flight.NONE, "call_swarm", "sic_em", "squirrel_pile", "lunge");
    }

    private HeroMoves() {
    }

    private static void serum(String family, Flight flight, String... moves) {
        SERUM_KITS.put(family, List.of(moves));
        SERUM_FLIGHT.put(family, flight);
    }

    public static VariantMoves.Move get(String id) {
        return MOVES.get(id);
    }

    public static List<String> kitFor(VariantProfile profile) {
        if (profile == null) {
            return List.of();
        }
        return switch (profile.kind()) {
            case SERUM, VILTRUMITE -> SERUM_KITS.getOrDefault(profile.family(), List.of());
            case THEMATIC -> ThematicKits.kitFor(profile.family(), profile.look());
            case MARVEL -> suitKit(profile.family());
            default -> List.of();
        };
    }

    private static int mark(String set) {
        Matcher matcher = MARK.matcher(set);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : 0;
    }

    private static boolean ironSuit(String set) {
        return set.startsWith("iron_man") || set.startsWith("war_machine") || set.startsWith("iron_patriot") || IronManSuits.get(set) != null;
    }

    private static List<String> suitKit(String set) {
        if (set.startsWith("war_machine") || set.startsWith("iron_patriot")) {
            return List.of("repulsor", "shoulder_barrage", "micro_missiles");
        }
        if (set.equals("iron_man_mark_1")) {
            return List.of("flamethrower", "slam", "lunge");
        }
        if (ironSuit(set)) {
            return mark(set) >= 7 ? List.of("repulsor", "unibeam", "micro_missiles", "flying_punch") : List.of("repulsor", "unibeam", "flying_punch");
        }
        if (set.equals("thor")) {
            return List.of("mjolnir_throw", "thunder_call", "dive");
        }
        if (set.startsWith("ant_man")) {
            return List.of("pym_shrink_strike", "giant_stomp", "lunge");
        }
        if (set.equals("wasp")) {
            return List.of("wasp_sting", "pym_shrink_strike");
        }
        if (set.equals("killmonger") || set.contains("black_panther")) {
            return List.of("combo_flurry", "kinetic_burst", "lunge");
        }
        return List.of();
    }

    private static Flight suitFlight(String set) {
        if (set.equals("iron_man_mark_1")) {
            return Flight.NONE;
        }
        if (ironSuit(set)) {
            return Flight.HOVER;
        }
        return set.equals("thor") ? Flight.SWOOP : Flight.NONE;
    }

    public static void tick(ServerLevel level, Mob mob, VariantProfile profile, LivingEntity target) {
        if (profile == null || !mob.isAlive()) {
            return;
        }
        boolean engaged = target != null && target.isAlive();
        String family = profile.family();
        switch (profile.kind()) {
            case SERUM, VILTRUMITE -> {
                VariantBody.fly(mob, target, SERUM_FLIGHT.getOrDefault(family, Flight.NONE), exhaust(family));
                if (family.equals("super_speed_pietro_maximoff")) {
                    VariantBody.haste(mob, engaged ? 0.6D : 0.0D);
                    if (engaged && mob.getDeltaMovement().horizontalDistanceSqr() > 0.01D) {
                        level.sendParticles(HeroFx.particle("marvel:quicksilver", ParticleTypes.CLOUD), mob.getX(), mob.getY() + 1.0D, mob.getZ(),
                                2, 0.2D, 0.4D, 0.2D, 0.0D);
                    }
                }
            }
            case MARVEL -> {
                if (family.startsWith("ant_man")) {
                    pym(mob, engaged, (mob.tickCount + mob.getId() * 37) / 100 % 3 == 1);
                } else if (family.equals("wasp")) {
                    boolean small = engaged && (mob.tickCount + mob.getId() * 37) / 120 % 4 != 3;
                    pym(mob, engaged, small);
                    VariantBody.fly(mob, small ? target : null, Flight.HOVER, null);
                } else {
                    Flight flight = suitFlight(family);
                    VariantBody.fly(mob, target, flight, flight == Flight.HOVER ? HeroFx.particle("marvel:iron_man_flame", ParticleTypes.FLAME) : null);
                }
            }
            default -> {
            }
        }
    }

    private static void pym(Mob mob, boolean engaged, boolean small) {
        if (VariantBody.sizeLocked(mob)) {
            return;
        }
        boolean shrink = engaged && small;
        VariantBody.size(mob, shrink ? -0.6D : 0.0D);
        VariantBody.haste(mob, shrink ? 0.3D : 0.0D);
    }

    private static ParticleOptions exhaust(String family) {
        return switch (family) {
            case "flame_on_johnny_storm" -> ParticleTypes.FLAME;
            case "weather_manipulation_ororo_munroe" -> ParticleTypes.CLOUD;
            case "binary_carol_danvers" -> HeroAbilities.emissive(0xFFC040, 0.6F);
            case "power_cosmic_norrin_radd" -> ParticleTypes.END_ROD;
            default -> null;
        };
    }
}
