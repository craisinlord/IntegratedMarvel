package com.craisinlord.integrated_superheroes.boss.command;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class StormCommandBlockConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ALLOW_SURVIVAL_EDITING;
    public static final ModConfigSpec.IntValue PERMISSION_LEVEL;
    public static final ModConfigSpec.BooleanValue OWNER_ONLY;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.push("stormCommandBlock");
        ENABLED = builder
                .comment("If false, Storm Command Blocks do nothing when powered and cannot be edited.")
                .define("enabled", true);
        ALLOW_SURVIVAL_EDITING = builder
                .comment("If false, only opped players in creative mode can edit them, like vanilla command blocks.")
                .define("allowSurvivalEditing", true);
        PERMISSION_LEVEL = builder
                .comment("Permission level the stored command runs at. Vanilla command blocks use 2.")
                .defineInRange("permissionLevel", 2, 0, 4);
        OWNER_ONLY = builder
                .comment("If true, only the player who placed the block (or an opped creative player) can edit or break it.")
                .define("ownerOnly", false);
        builder.pop();
        SPEC = builder.build();
    }

    private StormCommandBlockConfig() {
    }

    static boolean enabled() {
        return SPEC.isLoaded() ? ENABLED.get() : ENABLED.getDefault();
    }

    static boolean allowSurvivalEditing() {
        return SPEC.isLoaded() ? ALLOW_SURVIVAL_EDITING.get() : ALLOW_SURVIVAL_EDITING.getDefault();
    }

    static int permissionLevel() {
        return SPEC.isLoaded() ? PERMISSION_LEVEL.get() : PERMISSION_LEVEL.getDefault();
    }

    static boolean ownerOnly() {
        return SPEC.isLoaded() ? OWNER_ONLY.get() : OWNER_ONLY.getDefault();
    }
}
