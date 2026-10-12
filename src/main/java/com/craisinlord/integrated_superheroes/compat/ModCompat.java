package com.craisinlord.integrated_superheroes.compat;

import net.neoforged.fml.ModList;

public final class ModCompat {
    public static final boolean ALEXS_CAVES = ModList.get().isLoaded("alexscaves");
    public static final boolean SCORCHED_GUNS = ModList.get().isLoaded("scguns");
    public static final boolean WEATHER2 = ModList.get().isLoaded("weather2");
    public static final boolean ICE_AND_FIRE = ModList.get().isLoaded("iceandfire");
    public static final boolean MUTANT_MONSTERS = ModList.get().isLoaded("mutantmonsters");
    public static final boolean BETTER_COMBAT = ModList.get().isLoaded("bettercombat");
    public static final boolean CELESTISYNTH = ModList.get().isLoaded("celestisynth");

    private ModCompat() {
    }
}
