package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.ActivePowerList;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SuitAbilities {
    private static final Map<String, SuitAbility> ABILITIES = new LinkedHashMap<>();
    private static final Map<String, List<String>> LOADOUTS = new HashMap<>();

    public static final String AFTERBURNER = "suit_afterburner";
    public static final String MICRO_MISSILES = "suit_micro_missiles";
    public static final String LANDING = "suit_superhero_landing";
    public static final String FLARES = "suit_flares";
    public static final String SHOULDER_ROCKET = "suit_shoulder_rocket";
    public static final String LASER_RING = "suit_laser_ring";
    public static final String STEALTH = "suit_stealth";
    public static final String OVERCHARGE = "suit_overcharge";
    public static final String BRACE = "suit_brace";
    public static final String HYDRAULIC_PUNCH = "suit_hydraulic_punch";
    public static final String GRAPPLE = "suit_grapple";
    public static final String HEAVY_THROW = "suit_heavy_throw";
    public static final String STARBOOST = "suit_starboost";
    public static final String ROCKET_PUNCH = "suit_rocket_punch";
    public static final String PROTON_CANNON = "suit_proton_cannon";
    public static final String GRENADE_LAUNCHER = "suit_grenade_launcher";
    public static final String HELLFIRE_BOMB = "suit_hellfire_bomb";
    public static final String ORDNANCE_DROP = "suit_ordnance_drop";
    public static final String RALLY = "suit_rally";
    public static final String NUCLEAR_MISSILE = "suit_nuclear_missile";
    public static final String CRYO_CHARGE = "suit_cryo_charge";
    public static final String FIRE_CHARGE = "suit_fire_charge";
    public static final String ARC_CHARGE = "suit_arc_charge";
    public static final String DEPTH_CHARGE = "suit_depth_charge";
    public static final String MAGNET_PULSE = "suit_magnet_pulse";

    static {
        register(new SuitAbility(AFTERBURNER, "Afterburner", () -> SuitAbilityConfig.AFTERBURNER, SuitMovementAbilities::afterburner));
        register(new SuitAbility(LANDING, "Superhero Landing", () -> SuitAbilityConfig.LANDING, SuitMovementAbilities::superheroLanding));
        register(new SuitAbility(STARBOOST, "Starboost", () -> SuitAbilityConfig.STARBOOST, SuitMovementAbilities::starboost));
        register(new SuitAbility(MICRO_MISSILES, "Micro-Missiles", () -> SuitAbilityConfig.MICRO_MISSILES, SuitMissileAbilities::microMissiles));
        register(new SuitAbility(SHOULDER_ROCKET, "Shoulder Rocket", () -> SuitAbilityConfig.SHOULDER_ROCKET, SuitMissileAbilities::shoulderRocket));
        register(new SuitAbility(ORDNANCE_DROP, "Ordnance Drop", () -> SuitAbilityConfig.ORDNANCE_DROP, SuitMissileAbilities::ordnanceDrop));
        register(new SuitAbility(ROCKET_PUNCH, "Rocket Punch", () -> SuitAbilityConfig.ROCKET_PUNCH, SuitMissileAbilities::rocketPunch));
        register(new SuitAbility(NUCLEAR_MISSILE, "Nuclear Missile", () -> SuitAbilityConfig.NUCLEAR_MISSILE, SuitMissileAbilities::nuclearMissile));
        register(new SuitAbility(FLARES, "Flares & Flashbang", () -> SuitAbilityConfig.FLARES, SuitCombatAbilities::flares));
        register(new SuitAbility(LASER_RING, "Laser Ring", () -> SuitAbilityConfig.LASER_RING, SuitCombatAbilities::laserRing));
        register(new SuitAbility(STEALTH, "Stealth", () -> SuitAbilityConfig.STEALTH, SuitCombatAbilities::stealth));
        register(new SuitAbility(OVERCHARGE, "Overcharge", () -> SuitAbilityConfig.OVERCHARGE, SuitCombatAbilities::overcharge));
        register(new SuitAbility(BRACE, "Brace", () -> SuitAbilityConfig.BRACE, SuitCombatAbilities::brace));
        register(new SuitAbility(HYDRAULIC_PUNCH, "Hydraulic Punch", () -> SuitAbilityConfig.HYDRAULIC_PUNCH, SuitCombatAbilities::hydraulicPunch));
        register(new SuitAbility(GRAPPLE, "Grapple", () -> SuitAbilityConfig.GRAPPLE, SuitCombatAbilities::grapple));
        register(new SuitAbility(HEAVY_THROW, "Heavy Throw", () -> SuitAbilityConfig.HEAVY_THROW, SuitCombatAbilities::heavyThrow));
        register(new SuitAbility(RALLY, "Rally", () -> SuitAbilityConfig.RALLY, SuitCombatAbilities::rally));
        register(new SuitAbility(PROTON_CANNON, "Proton Cannon", () -> SuitAbilityConfig.PROTON_CANNON, SuitCombatAbilities::protonCannon));
        register(new SuitAbility(GRENADE_LAUNCHER, "Grenade Launcher", () -> SuitAbilityConfig.GRENADE_LAUNCHER, SuitGrenadeAbilities::grenadeLauncher));
        register(new SuitAbility(HELLFIRE_BOMB, "Hellfire Bomb", () -> SuitAbilityConfig.HELLFIRE_BOMB, SuitGrenadeAbilities::hellfireBomb));
        register(new SuitAbility(CRYO_CHARGE, "Cryo Charge", () -> SuitAbilityConfig.CRYO_CHARGE, SuitIntegrationAbilities::cryoCharge));
        register(new SuitAbility(FIRE_CHARGE, "Fire Charge", () -> SuitAbilityConfig.FIRE_CHARGE, SuitIntegrationAbilities::fireCharge));
        register(new SuitAbility(ARC_CHARGE, "Arc Charge", () -> SuitAbilityConfig.ARC_CHARGE, SuitIntegrationAbilities::arcCharge));
        register(new SuitAbility(DEPTH_CHARGE, "Depth Charge", () -> SuitAbilityConfig.DEPTH_CHARGE, SuitIntegrationAbilities::depthCharge));
        register(new SuitAbility(MAGNET_PULSE, "Magnet Pulse", () -> SuitAbilityConfig.MAGNET_PULSE, SuitIntegrationAbilities::magnetPulse));

        List<String> core = List.of(AFTERBURNER, MICRO_MISSILES, LANDING);
        loadout("iron_man_mark_1", SHOULDER_ROCKET);
        loadout("iron_man_mark_2", AFTERBURNER);
        loadout("iron_man_mark_3", AFTERBURNER, FLARES);
        loadout("iron_man_mark_5", AFTERBURNER, FLARES);
        loadout("iron_man_mark_6", AFTERBURNER, FLARES, LASER_RING);
        for (String mark : List.of("7", "11", "19", "20", "22", "28")) {
            LOADOUTS.put("iron_man_mark_" + mark, core);
        }
        loadout("iron_man_mark_15", AFTERBURNER, MICRO_MISSILES, STEALTH);
        loadout("iron_man_mark_17", AFTERBURNER, MICRO_MISSILES, OVERCHARGE);
        loadout("iron_man_mark_21", AFTERBURNER, CRYO_CHARGE, LANDING);
        loadout("iron_man_mark_23", AFTERBURNER, FIRE_CHARGE, HELLFIRE_BOMB);
        loadout("iron_man_mark_24", BRACE, MICRO_MISSILES, LANDING);
        loadout("iron_man_mark_25", AFTERBURNER, HYDRAULIC_PUNCH, LANDING);
        loadout("iron_man_mark_30", AFTERBURNER, ARC_CHARGE, LANDING);
        loadout("iron_man_mark_33", AFTERBURNER, MICRO_MISSILES, MAGNET_PULSE);
        loadout("iron_man_mark_35", AFTERBURNER, MICRO_MISSILES, GRAPPLE);
        loadout("iron_man_mark_37", DEPTH_CHARGE, MICRO_MISSILES, LANDING);
        loadout("iron_man_mark_38", HEAVY_THROW, MICRO_MISSILES, LANDING);
        loadout("iron_man_mark_39", STARBOOST, MICRO_MISSILES, LANDING);
        loadout("iron_man_mark_42", AFTERBURNER, MICRO_MISSILES, ROCKET_PUNCH);
        loadout("iron_man_mark_43", AFTERBURNER, MICRO_MISSILES, PROTON_CANNON);
        loadout("war_machine_mark_1", AFTERBURNER, ORDNANCE_DROP, GRENADE_LAUNCHER);
        loadout("war_machine_mark_2", MICRO_MISSILES, ORDNANCE_DROP, NUCLEAR_MISSILE);
        loadout("iron_patriot", MICRO_MISSILES, ORDNANCE_DROP, RALLY);
    }

    private SuitAbilities() {
    }

    private static void register(SuitAbility ability) {
        ABILITIES.put(ability.name(), ability);
    }

    private static void loadout(String suitId, String... abilities) {
        LOADOUTS.put(suitId, List.of(abilities));
    }

    public static SuitAbility get(String name) {
        return ABILITIES.get(name);
    }

    public static boolean isSuitAbility(String name) {
        return name != null && ABILITIES.containsKey(name);
    }

    public static List<SuitAbility> loadout(String suitId) {
        List<SuitAbility> abilities = new ArrayList<>();
        if (suitId == null || !IronManSuits.isLoaded() || !SuitAbilityConfig.ENABLED.get()) {
            return abilities;
        }
        for (String name : LOADOUTS.getOrDefault(suitId, List.of())) {
            SuitAbility ability = ABILITIES.get(name);
            if (ability != null) {
                abilities.add(ability);
            }
        }
        return abilities;
    }

    public static SuitContext context(ServerPlayer player) {
        String suitId = IronManSuits.fullSuitId(player);
        IronManSuits.Stats stats = suitId == null ? null : IronManSuits.get(suitId);
        return stats == null ? null : new SuitContext(player, suitId, stats);
    }

    public static void registerLegacyPowerEntries() {
        ABILITIES.values().forEach(ability -> ActivePowerList.activePowerList.put(ability.name(), newPower(ability, 0.0D)));
    }

    static Power newPower(SuitAbility ability, double cooldown) {
        return new Power(ability.name(), ability.displayName(), 2, cooldown, false, false);
    }

    public static boolean handle(ServerPlayer player, String name, int slot, String inputType) {
        SuitAbility ability = ABILITIES.get(name);
        if (ability == null) {
            return false;
        }
        if (!"press".equals(inputType)) {
            return true;
        }
        SuitContext context = context(player);
        if (context == null || !loadout(context.suitId()).contains(ability)) {
            return true;
        }
        PlayerData data = HeroPowers.data(player);
        if (slot < 0 || slot >= data.getActivePowers().size()) {
            return true;
        }
        Power slotPower = data.getActivePower(slot);
        if (slotPower == null || !name.equals(slotPower.getName()) || slotPower.getCooldown() > 0.0D) {
            return true;
        }
        SuitAbilityConfig.Settings settings = ability.settings().get();
        double cost = settings.energyCost().get();
        if (!SuitStates.isMidAbility(player, name) && !SuitEnergy.has(context.chest(), cost)) {
            player.displayClientMessage(Component.literal("§cInsufficient suit power"), true);
            return true;
        }
        SuitAbility.Result result = ability.handler().press(context);
        if (result.spendEnergy) {
            SuitEnergy.spend(context.chest(), cost);
        }
        if (result.startCooldown) {
            slotPower.setCooldown(settings.cooldownSeconds().get());
            PlayerData.sync(player);
        }
        return true;
    }
}
