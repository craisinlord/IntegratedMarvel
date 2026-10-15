package com.craisinlord.integrated_superheroes.gauntlet;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class GauntletClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<CardMode> ABILITY_CARD;

    public enum CardMode { FULL, CONTROLS, OFF }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("hud");
        ABILITY_CARD = builder.defineEnum("abilityCard", CardMode.CONTROLS);
        builder.pop();
        SPEC = builder.build();
    }

    private GauntletClientConfig() {
    }

    public static CardMode cardMode() {
        return SPEC.isLoaded() ? ABILITY_CARD.get() : CardMode.CONTROLS;
    }
}
