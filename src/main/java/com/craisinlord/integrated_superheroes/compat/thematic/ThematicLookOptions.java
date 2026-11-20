package com.craisinlord.integrated_superheroes.compat.thematic;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class ThematicLookOptions {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_OWN;
    public static final ModConfigSpec.BooleanValue SHOW_OTHERS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Hero costumes drawn on players who have a serum in them. These only change what you see.").push("serumLooks");
        SHOW_OWN = builder.comment("Draw your own serum costume.")
                .define("showOwn", true);
        SHOW_OTHERS = builder.comment("Draw other players' serum costumes.")
                .define("showOthers", true);
        builder.pop();
        SPEC = builder.build();
    }

    private ThematicLookOptions() {
    }

    public static boolean showOwn() {
        return !SPEC.isLoaded() || SHOW_OWN.get();
    }

    public static boolean showOthers() {
        return !SPEC.isLoaded() || SHOW_OTHERS.get();
    }
}
