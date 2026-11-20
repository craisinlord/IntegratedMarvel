package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class HeroHudClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Corner> CORNER;
    public static final ModConfigSpec.IntValue OFFSET_X;
    public static final ModConfigSpec.IntValue OFFSET_Y;
    public static final ModConfigSpec.BooleanValue AVOID_AMMO_HUD;
    public static final ModConfigSpec.BooleanValue UNIFIED_SUIT_ABILITIES;
    public static final ModConfigSpec.BooleanValue SUIT_GADGETS_IN_HUD;
    public static final ModConfigSpec.BooleanValue HIDE_SUIT_EQUIP_PREVIEW;

    public enum Corner { BOTTOM_RIGHT, BOTTOM_LEFT, TOP_RIGHT, TOP_LEFT }

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("abilities");
        CORNER = builder.defineEnum("corner", Corner.BOTTOM_RIGHT);
        OFFSET_X = builder.defineInRange("offsetX", 0, -2000, 2000);
        OFFSET_Y = builder.defineInRange("offsetY", 0, -2000, 2000);
        AVOID_AMMO_HUD = builder.define("avoidAmmoHud", true);
        UNIFIED_SUIT_ABILITIES = builder.define("unifiedSuitAbilities", true);
        SUIT_GADGETS_IN_HUD = builder.define("suitGadgetsInHud", true);
        HIDE_SUIT_EQUIP_PREVIEW = builder.define("hideSuitEquipPreview", true);
        builder.pop();
        SPEC = builder.build();
    }

    private HeroHudClientConfig() {
    }

    public static Corner corner() {
        return SPEC.isLoaded() ? CORNER.get() : Corner.BOTTOM_RIGHT;
    }

    public static int offsetX() {
        return SPEC.isLoaded() ? OFFSET_X.get() : 0;
    }

    public static int offsetY() {
        return SPEC.isLoaded() ? OFFSET_Y.get() : 0;
    }

    public static boolean avoidAmmoHud() {
        return !SPEC.isLoaded() || AVOID_AMMO_HUD.get();
    }

    public static boolean unifiedSuitAbilities() {
        return !SPEC.isLoaded() || UNIFIED_SUIT_ABILITIES.get();
    }

    public static boolean suitGadgetsInHud() {
        return unifiedSuitAbilities() && (!SPEC.isLoaded() || SUIT_GADGETS_IN_HUD.get());
    }

    public static boolean hideSuitEquipPreview() {
        return !SPEC.isLoaded() || HIDE_SUIT_EQUIP_PREVIEW.get();
    }
}
