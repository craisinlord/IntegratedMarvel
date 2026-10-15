package com.craisinlord.integrated_superheroes.gauntlet;

import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.craisinlord.integrated_superheroes.gauntlet.GauntletAbility.Scroll;
import static com.craisinlord.integrated_superheroes.gauntlet.GauntletAbility.charge;
import static com.craisinlord.integrated_superheroes.gauntlet.GauntletAbility.hold;
import static com.craisinlord.integrated_superheroes.gauntlet.GauntletAbility.tap;

public final class GauntletAbilities {
    public static final List<String> STONES = List.of("power", "space", "reality", "soul", "time", "mind", "snap");
    public static final int[] NUMERAL_COLORS = {0x55FFFF, 0xAA55FF, 0xFFAA00, 0x55FF55, 0xFF5555};
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V"};
    private static final Map<String, List<GauntletAbility>> BY_STONE = new LinkedHashMap<>();

    static {
        stone("power",
                tap("overcharge", 1, 400, PowerStone::overcharge).ownCooldown(),
                charge("annihilationBeam", 3, 100, cast -> GauntletAbilityController.beginBeamCharge(cast, GauntletAbilityController.BeamKind.POWER)).crouch(),
                tap("moonToss", 2, 600, PowerStone::moonToss));
        stone("space",
                tap("spaceFold", 4, 100, SpaceStone::fold).crouch().ownCooldown(),
                tap("dimensionalRift", 6, 0, SpaceStone::rift).crouch(),
                tap("singularity", 5, 600, SpaceStone::singularity),
                tap("banish", 18, 400, SpaceStone::banish).crouch());
        stone("reality",
                tap("transmute", 8, 20, RealityStone::transmute).crouch().scroll(Scroll.ANY, RealityStone::transmuteRadius),
                tap("gravityField", 9, 200, RealityStone::gravityField).crouch(),
                tap("sizeShift", 7, 0, RealityStone::sizeShiftActivate).crouch().scroll(Scroll.ANY, RealityStone::sizeShift),
                tap("realityClipboard", -8, 0, RealityClipboard::activate).crouch().scroll(Scroll.ANY, RealityClipboard::scroll));
        stone("soul",
                tap("soulExchange", 11, 100, SoulStone::exchange).crouch(),
                tap("soulSever", 12, 240, SoulStone::sever),
                tap("reapRestore", 10, 200, SoulStone::reapRestore).crouch().ownCooldown(),
                tap("soulSight", 1011, 200, MindStone::sight));
        stone("time",
                hold("chronostasis", 20, 0, TimeStone::chronostasis),
                tap("temporalDilation", 13, 100, TimeStone::age).crouch().scroll(Scroll.ENTITY, TimeStone::dilate),
                tap("timeField", -13, 600, TimeStone::timeField).crouch().scroll(Scroll.ANY, TimeStone::fieldRate),
                tap("timeSurge", -20, 600, TimeStone::surge),
                tap("rewind", 1013, 900, TimeStone::rewind).crouch());
        stone("mind",
                tap("command", 15, 300, MindStone::command),
                charge("mindBeam", 14, 100, cast -> GauntletAbilityController.beginBeamCharge(cast, GauntletAbilityController.BeamKind.MIND)),
                tap("frenzy", -15, 400, MindStone::frenzy).crouch(),
                tap("mindLink", -14, 400, MindLinkManager::activate).ownCooldown());
        stone("snap",
                charge("infinityBeam", 19, 200, cast -> GauntletAbilityController.beginBeamCharge(cast, GauntletAbilityController.BeamKind.INFINITY)),
                tap("lifeHalvingSnap", 16, 1200, SnapStone::halve),
                tap("restoreSnap", -16, 1200, SnapStone::restore),
                tap("selectiveSnap", 17, 1200, SnapStone::selective).crouch());
    }

    private GauntletAbilities() {
    }

    private static void stone(String stone, GauntletAbility.Builder... builders) {
        List<GauntletAbility> abilities = new ArrayList<>();
        for (int index = 0; index < builders.length; index++) abilities.add(builders[index].build(stone, index));
        BY_STONE.put(stone, List.copyOf(abilities));
    }

    public static List<GauntletAbility> forStone(String stone) {
        return BY_STONE.getOrDefault(stone, List.of());
    }

    public static int count(String stone) {
        return forStone(stone).size();
    }

    public static GauntletAbility get(String stone, int index) {
        List<GauntletAbility> abilities = forStone(stone);
        return index >= 0 && index < abilities.size() ? abilities.get(index) : null;
    }

    public static boolean unlocked(ItemStack gauntlet, String stone) {
        if (gauntlet.isEmpty()) return false;
        if (stone.equals("snap")) {
            return InfinityGauntletStones.isFullGauntlet(gauntlet) && InfinityGauntletStones.hasAllStones(gauntlet);
        }
        return InfinityGauntletStones.hasStone(gauntlet, stone);
    }

    public static GauntletAbility selected(ItemStack gauntlet) {
        if (gauntlet.isEmpty()) return null;
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        if (!unlocked(gauntlet, stone)) return null;
        GauntletAbility ability = get(stone, InfinityGauntletStones.selectedAbility(gauntlet, stone));
        return ability != null && ability.enabled() ? ability : null;
    }

    public static boolean isSelected(ItemStack gauntlet, String stone, String id) {
        GauntletAbility ability = selected(gauntlet);
        return ability != null && ability.stone().equals(stone) && ability.id().equals(id);
    }

    public static boolean infinity(ItemStack gauntlet) {
        return GauntletConfig.infinityTierEnabled() && InfinityGauntletStones.isFullGauntlet(gauntlet)
                && InfinityGauntletStones.hasAllStones(gauntlet);
    }

    public static String numeral(int index) {
        return NUMERALS[Math.clamp(index, 0, NUMERALS.length - 1)];
    }

    public static int numeralColor(int index) {
        return NUMERAL_COLORS[Math.clamp(index, 0, NUMERAL_COLORS.length - 1)];
    }

    public static int stoneColor(String stone) {
        return switch (stone) {
            case "power" -> 0xB040FF;
            case "space" -> 0x3070FF;
            case "reality" -> 0xE01828;
            case "soul" -> 0xFF8C1A;
            case "time" -> 0x28E050;
            case "mind" -> 0xFFE030;
            default -> 0xFFD700;
        };
    }
}
