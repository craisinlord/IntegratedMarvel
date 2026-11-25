package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import com.craisinlord.integrated_superheroes.voidrealm.variant.moves.RealMoves;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.List;

public final class SuitVariantMoves {
    public static final String REPULSOR = RealMoves.SUIT + "repulsor";
    public static final String UNIBEAM = RealMoves.SUIT + "unibeam";
    public static final String FLAMETHROWER = RealMoves.SUIT + "flamethrower";
    public static final String SHOULDER_TURRET = RealMoves.SUIT + "shoulder_turret";
    public static final String WASP_STING = RealMoves.SUIT + "wasp_sting";
    public static final String MJOLNIR_THROW = RealMoves.SUIT + "mjolnir_throw";
    public static final String KINETIC_BLAST = RealMoves.SUIT + "kinetic_blast";
    public static final String CALL_COLONY = RealMoves.SUIT + "call_colony";
    public static final String CALL_HIVE = RealMoves.SUIT + "call_hive";
    public static final String SIC_EM = RealMoves.SUIT + "sic_em";
    public static final String OVERRUN = RealMoves.SUIT + "overrun";
    public static final String STING_STORM = RealMoves.SUIT + "sting_storm";
    private static final List<String> MARKS = List.of("1", "2", "3", "5", "6", "7", "11", "15", "17", "19", "20", "21", "22", "23", "24", "25",
            "28", "30", "33", "35", "37", "38", "39", "42", "43");
    private static final List<String> KINETIC_SUITS = List.of("killmonger", "kinetic_black_panther", "black_panther_shuri");
    private static boolean registered;

    private SuitVariantMoves() {
    }

    public static String id(String abilityName) {
        return RealMoves.SUIT + abilityName.replaceFirst("^suit_", "").replaceFirst("^pym_", "");
    }

    static void register() {
        if (registered) {
            return;
        }
        registered = true;
        SuitMobSupport.register();
        SuitBasePowers.register();
        HeroCaster.onTick(PymColony::tickMob);
        HeroCaster.onCleanup(PymColony::cleanupMob);

        for (SuitAbility ability : SuitAbilities.all()) {
            RealMoves.source(id(ability.name()), (level, mob, target) -> press(mob, ability));
        }
        RealMoves.source(REPULSOR, SuitBasePowers::repulsor);
        RealMoves.source(UNIBEAM, SuitBasePowers::unibeam);
        RealMoves.source(FLAMETHROWER, SuitBasePowers::flamethrower);
        RealMoves.source(SHOULDER_TURRET, SuitBasePowers::shoulderTurret);
        RealMoves.source(WASP_STING, SuitBasePowers::waspSting);
        RealMoves.source(MJOLNIR_THROW, SuitBasePowers::mjolnir);
        RealMoves.source(KINETIC_BLAST, SuitBasePowers::kineticBlast);
        RealMoves.source(CALL_COLONY, SuitVariantMoves::callColony);
        RealMoves.source(CALL_HIVE, SuitVariantMoves::callColony);
        RealMoves.source(SIC_EM, SuitVariantMoves::sicEm);
        RealMoves.source(OVERRUN, (level, mob, target) -> PymColony.kindOf(mob) == PymHelmets.Kind.ANT ? PymColony.overrun(mob) : 0.0D);
        RealMoves.source(STING_STORM, (level, mob, target) -> PymColony.kindOf(mob) == PymHelmets.Kind.WASP ? PymColony.stingStorm(mob) : 0.0D);

        for (String mark : MARKS) {
            ironKit("iron_man_mark_" + mark);
        }
        ironKit("war_machine_mark_1");
        ironKit("war_machine_mark_2");
        ironKit("iron_patriot");
        RealMoves.kit("thor", List.of(MJOLNIR_THROW));
        RealMoves.kit("ant_man", List.of(CALL_COLONY, SIC_EM, OVERRUN));
        RealMoves.kit("ant_man_upgraded", List.of(CALL_COLONY, SIC_EM, OVERRUN));
        RealMoves.kit("wasp", List.of(WASP_STING, CALL_HIVE, SIC_EM, STING_STORM));
        for (String suit : KINETIC_SUITS) {
            RealMoves.kit(suit, List.of(KINETIC_BLAST));
        }
    }

    private static void ironKit(String suit) {
        List<String> kit = new ArrayList<>(basePowers(suit));
        for (String ability : SuitAbilities.loadoutNames(suit)) {
            String id = id(ability);
            if (!kit.contains(id)) {
                kit.add(id);
            }
        }
        RealMoves.kit(suit, kit);
    }

    static List<String> basePowers(String suit) {
        if (suit.equals("iron_man_mark_1")) {
            return List.of(FLAMETHROWER);
        }
        if (suit.equals("iron_man_mark_2")) {
            return List.of(REPULSOR);
        }
        if (suit.startsWith("war_machine") || suit.equals("iron_patriot")) {
            return List.of(REPULSOR, UNIBEAM, SHOULDER_TURRET);
        }
        return List.of(REPULSOR, UNIBEAM);
    }

    private static double press(Mob mob, SuitAbility ability) {
        if (!IronManSuits.isLoaded() || !SuitAbilityConfig.ENABLED.get()) {
            return 0.0D;
        }
        SuitContext context = SuitContext.of(mob, IronManSuits.fullSuitId(mob));
        if (context == null) {
            return 0.0D;
        }
        SuitAbility.Result result = ability.handler().press(context);
        return result == SuitAbility.Result.FAILED ? 0.0D : ability.settings().get().cooldownSeconds().get();
    }

    private static double callColony(ServerLevel level, Mob mob, LivingEntity target) {
        PymHelmets.Kind kind = PymColony.kindOf(mob);
        return kind == null ? 0.0D : PymColony.callColony(mob, kind);
    }

    private static double sicEm(ServerLevel level, Mob mob, LivingEntity target) {
        PymHelmets.Kind kind = PymColony.kindOf(mob);
        return kind == null ? 0.0D : PymColony.sicEm(mob, kind);
    }
}
