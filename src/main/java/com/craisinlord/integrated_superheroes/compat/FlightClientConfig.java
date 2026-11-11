package com.craisinlord.integrated_superheroes.compat;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class FlightClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SCREEN_SHAKE;
    public static final ModConfigSpec.DoubleValue SCREEN_SHAKE_SCALE;
    public static final ModConfigSpec.BooleanValue FOV_EFFECT;
    public static final ModConfigSpec.DoubleValue FOV_EFFECT_SCALE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("Viltrumite flight visuals, used by Omni-Man and Captain Marvel.").push("flight");
        SCREEN_SHAKE = builder.comment("Shake the screen on takeoff and at high speed.")
                .define("screenShake", true);
        SCREEN_SHAKE_SCALE = builder.comment("How hard the screen shakes. 1 is the default, 0 is none, 2 is double.")
                .defineInRange("screenShakeScale", 1.0D, 0.0D, 2.0D);
        FOV_EFFECT = builder.comment("Widen the field of view as you speed up.")
                .define("fovEffect", true);
        FOV_EFFECT_SCALE = builder.comment("How much the field of view widens. 1 is the default, 0 is none, 2 is double.")
                .defineInRange("fovEffectScale", 1.0D, 0.0D, 2.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private FlightClientConfig() {
    }

    public static float shakeScale() {
        if (!SPEC.isLoaded()) {
            return 1.0F;
        }
        return SCREEN_SHAKE.get() ? SCREEN_SHAKE_SCALE.get().floatValue() : 0.0F;
    }

    public static boolean fovEffect() {
        return !SPEC.isLoaded() || FOV_EFFECT.get();
    }

    public static float fovScale() {
        return SPEC.isLoaded() ? FOV_EFFECT_SCALE.get().floatValue() : 1.0F;
    }
}
