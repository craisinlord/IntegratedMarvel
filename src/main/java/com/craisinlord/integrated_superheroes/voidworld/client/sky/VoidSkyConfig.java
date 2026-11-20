package com.craisinlord.integrated_superheroes.voidworld.client.sky;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class VoidSkyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue STRANDS;
    public static final ModConfigSpec.DoubleValue STRAND_BRIGHTNESS;
    public static final ModConfigSpec.BooleanValue PRUNE_SNAPS;
    public static final ModConfigSpec.BooleanValue LIGHTNING;
    public static final ModConfigSpec.DoubleValue LIGHTNING_BRIGHTNESS;
    public static final ModConfigSpec.BooleanValue CLOUDS;
    public static final ModConfigSpec.DoubleValue CLOUD_OPACITY;
    public static final ModConfigSpec.BooleanValue CLOUD_LIGHTNING;
    public static final ModConfigSpec.BooleanValue CLOUD_RING_GLOW;
    public static final ModConfigSpec.BooleanValue STARS;
    public static final ModConfigSpec.DoubleValue STAR_BRIGHTNESS;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("timeline");
        STRANDS = builder
                .comment("Draw the glowing timeline ring around the Void sky.")
                .define("strands", true);
        STRAND_BRIGHTNESS = builder
                .comment("Brightness of the timeline strands.")
                .defineInRange("strandBrightness", 1.0D, 0.0D, 2.0D);
        PRUNE_SNAPS = builder
                .comment("A strand flashes and snaps in the sky where a pruned structure is about to fall.")
                .define("pruneSnaps", true);
        builder.pop();
        builder.push("lightning");
        LIGHTNING = builder
                .comment("Silent sheet lightning flickering far behind the Void fog.")
                .define("sheetLightning", true);
        LIGHTNING_BRIGHTNESS = builder
                .comment("Brightness of the sheet lightning flashes.")
                .defineInRange("lightningBrightness", 1.0D, 0.0D, 2.0D);
        builder.pop();
        builder.push("clouds");
        CLOUDS = builder
                .comment("Draw the storm clouds around the Void horizon. Follows the vanilla Clouds setting: Fast draws one layer, Fancy draws three.")
                .define("clouds", true);
        CLOUD_OPACITY = builder
                .comment("Opacity of the Void clouds.")
                .defineInRange("cloudOpacity", 0.85D, 0.0D, 1.0D);
        CLOUD_LIGHTNING = builder
                .comment("Lightning lights the clouds from inside, with forked bolts between the layers.")
                .define("cloudLightning", true);
        CLOUD_RING_GLOW = builder
                .comment("Cloud tops under the timeline ring pick up its glow.")
                .define("cloudRingGlow", true);
        builder.pop();
        builder.push("stars");
        STARS = builder
                .comment("Dim stars in the open sky inside the timeline ring. They vanish when a Witherstorm closes the clouds over it.")
                .define("stars", false);
        STAR_BRIGHTNESS = builder
                .comment("Brightness of the Void stars.")
                .defineInRange("starBrightness", 1.0D, 0.0D, 2.0D);
        builder.pop();
        SPEC = builder.build();
    }

    private VoidSkyConfig() {
    }

    static boolean strands() {
        return !SPEC.isLoaded() || STRANDS.get();
    }

    static float strandBrightness() {
        return SPEC.isLoaded() ? STRAND_BRIGHTNESS.get().floatValue() : 1.0F;
    }

    static boolean pruneSnaps() {
        return !SPEC.isLoaded() || PRUNE_SNAPS.get();
    }

    static boolean lightning() {
        return !SPEC.isLoaded() || LIGHTNING.get();
    }

    static float lightningBrightness() {
        return SPEC.isLoaded() ? LIGHTNING_BRIGHTNESS.get().floatValue() : 1.0F;
    }

    static boolean clouds() {
        return !SPEC.isLoaded() || CLOUDS.get();
    }

    static float cloudOpacity() {
        return SPEC.isLoaded() ? CLOUD_OPACITY.get().floatValue() : 0.85F;
    }

    static boolean cloudLightning() {
        return lightning() && (!SPEC.isLoaded() || CLOUD_LIGHTNING.get());
    }

    static boolean stars() {
        return SPEC.isLoaded() && STARS.get();
    }

    static float starBrightness() {
        return SPEC.isLoaded() ? STAR_BRIGHTNESS.get().floatValue() : 1.0F;
    }

    static boolean cloudRingGlow() {
        return !SPEC.isLoaded() || CLOUD_RING_GLOW.get();
    }
}
