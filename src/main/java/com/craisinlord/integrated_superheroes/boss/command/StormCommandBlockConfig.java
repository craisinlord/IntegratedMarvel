package com.craisinlord.integrated_superheroes.boss.command;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class StormCommandBlockConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ALLOW_SURVIVAL_EDITING;
    public static final ModConfigSpec.IntValue PERMISSION_LEVEL;
    public static final ModConfigSpec.BooleanValue OWNER_ONLY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_COMMANDS;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> BLOCKED_TEXT;
    private static final List<String> DEFAULT_BLOCKED_COMMANDS = List.of("gamemode", "defaultgamemode");
    private static final List<String> DEFAULT_BLOCKED_TEXT = List.of("command_block", "storm_command_block", "run_command", "creative", "debug");
    private static final Cache COMMANDS = new Cache();
    private static final Cache TEXT = new Cache();

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
        BLOCKED_COMMANDS = builder
                .comment("Commands a Storm Command Block refuses to run; the block is kept. \"execute ... run\" is followed to the command it runs, and execute subcommands (like summon) or if/unless tests (like function) named here are refused too.")
                .defineListAllowEmpty("blockedCommands", DEFAULT_BLOCKED_COMMANDS, () -> "", o -> o instanceof String);
        BLOCKED_TEXT = builder
                .comment("A command containing any of these anywhere is refused. The defaults stop placing, giving or summoning command blocks (Storm ones included) and command block minecarts, signs or items with click commands, which would run commands without this filter, plus creative-only and debug items.")
                .defineListAllowEmpty("blockedText", DEFAULT_BLOCKED_TEXT, () -> "", o -> o instanceof String);
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

    static Set<String> blockedCommands() {
        return COMMANDS.get(SPEC.isLoaded() ? BLOCKED_COMMANDS.get() : DEFAULT_BLOCKED_COMMANDS, true);
    }

    static Set<String> blockedText() {
        return TEXT.get(SPEC.isLoaded() ? BLOCKED_TEXT.get() : DEFAULT_BLOCKED_TEXT, false);
    }

    static boolean ownerOnly() {
        return SPEC.isLoaded() ? OWNER_ONLY.get() : OWNER_ONLY.getDefault();
    }

    private static final class Cache {
        private List<? extends String> source;
        private Set<String> values = Set.of();

        Set<String> get(List<? extends String> list, boolean stripSlash) {
            if (list != this.source) {
                Set<String> set = new HashSet<>();
                for (String entry : list) {
                    String trimmed = entry.strip().toLowerCase(Locale.ROOT);
                    if (stripSlash && trimmed.startsWith("/")) {
                        trimmed = trimmed.substring(1);
                    }
                    if (!trimmed.isEmpty()) {
                        set.add(trimmed);
                    }
                }
                this.values = Set.copyOf(set);
                this.source = list;
            }
            return this.values;
        }
    }
}
