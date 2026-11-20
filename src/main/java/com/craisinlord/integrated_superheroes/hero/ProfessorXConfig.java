package com.craisinlord.integrated_superheroes.hero;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;

public final class ProfessorXConfig {
    public static MarvelBalanceConfig.HeroStats STATS;
    public static ModConfigSpec.DoubleValue LINK_RANGE;
    public static ModConfigSpec.IntValue LINK_CHANNEL_TICKS;
    public static ModConfigSpec.IntValue LINK_TICKS;
    public static ModConfigSpec.IntValue MAX_LINKS;
    public static ModConfigSpec.DoubleValue LINK_COOLDOWN;
    public static ModConfigSpec.DoubleValue LINK_LEASH;
    public static ModConfigSpec.DoubleValue LINK_TARGET_RANGE;
    public static ModConfigSpec.DoubleValue LINK_DEFEND_RADIUS;
    public static ModConfigSpec.IntValue LINK_MEMORY_TICKS;
    public static ModConfigSpec.DoubleValue CHANNEL_BREAK_DAMAGE;
    public static ModConfigSpec.DoubleValue CEREBRO_RADIUS;
    public static ModConfigSpec.IntValue CEREBRO_GLOW_TICKS;
    public static ModConfigSpec.DoubleValue CEREBRO_COOLDOWN;
    public static ModConfigSpec.IntValue ASTRAL_TICKS;
    public static ModConfigSpec.DoubleValue ASTRAL_RANGE;
    public static ModConfigSpec.DoubleValue ASTRAL_COOLDOWN;
    public static ModConfigSpec.DoubleValue ROGUE_LINK_RANGE;
    public static ModConfigSpec.IntValue ROGUE_LINK_TICKS;
    public static ModConfigSpec.DoubleValue ROGUE_LINK_COOLDOWN;

    private ProfessorXConfig() {
    }

    static void define(ModConfigSpec.Builder builder) {
        builder.push("professorX");
        STATS = new MarvelBalanceConfig.HeroStats(builder, 12.0D, 4.0D, 0.0D, 0.0D, 0.0D, 2.8D, 0, List.of());
        LINK_RANGE = builder.comment("How far away a creature can be when you start a Link.")
                .defineInRange("linkRange", 12.0D, 2.0D, 48.0D);
        LINK_CHANNEL_TICKS = builder.comment("How long you have to keep your eyes on a creature before the Link takes hold.")
                .defineInRange("linkChannelTicks", 20, 1, 200);
        LINK_TICKS = builder.comment("How long a linked creature fights for you.")
                .defineInRange("linkTicks", 200, 20, 2400);
        MAX_LINKS = builder.comment("How many creatures you can hold at once. Linking past this lets go of the one with the least time left.")
                .defineInRange("maxLinks", 3, 1, 4);
        LINK_COOLDOWN = builder.comment("Cooldown after a Link takes hold. Failed or cancelled Links cost nothing.")
                .defineInRange("linkCooldownSeconds", 6.0D, 0.0D, 600.0D);
        LINK_LEASH = builder.comment("A linked creature that ends up farther than this from you is let go.")
                .defineInRange("linkLeash", 40.0D, 8.0D, 128.0D);
        LINK_TARGET_RANGE = builder.comment("Linked creatures only go after targets within this distance of you.")
                .defineInRange("linkTargetRange", 20.0D, 4.0D, 64.0D);
        LINK_DEFEND_RADIUS = builder.comment("Linked creatures go after anything this close to you that is targeting you.")
                .defineInRange("linkDefendRadius", 10.0D, 0.0D, 32.0D);
        LINK_MEMORY_TICKS = builder.comment("How long linked creatures remember what you hit, or what hit you.")
                .defineInRange("linkMemoryTicks", 200, 20, 1200);
        CHANNEL_BREAK_DAMAGE = builder.comment("A single hit at least this strong breaks your concentration on Link.")
                .defineInRange("channelBreakDamage", 4.0D, 0.0D, 1000.0D);
        CEREBRO_RADIUS = builder.comment("Cerebro reveals every creature and player within this distance.")
                .defineInRange("cerebroRadius", 64.0D, 8.0D, 128.0D);
        CEREBRO_GLOW_TICKS = builder.defineInRange("cerebroGlowTicks", 200, 20, 1200);
        CEREBRO_COOLDOWN = builder.defineInRange("cerebroCooldownSeconds", 20.0D, 0.0D, 600.0D);
        ASTRAL_TICKS = builder.comment("Longest an Astral Projection lasts before you snap back to your body.")
                .defineInRange("astralProjectionTicks", 400, 20, 6000);
        ASTRAL_RANGE = builder.comment("Drifting farther than this from your body snaps you back.")
                .defineInRange("astralProjectionRange", 48.0D, 4.0D, 256.0D);
        ASTRAL_COOLDOWN = builder.defineInRange("astralProjectionCooldownSeconds", 30.0D, 0.0D, 3600.0D);
        ROGUE_LINK_RANGE = builder.comment("Range of the Link that Rogue borrows. Rogue's Link is instant, holds one creature and can't be refreshed.")
                .defineInRange("rogueLinkRange", 10.0D, 2.0D, 48.0D);
        ROGUE_LINK_TICKS = builder.defineInRange("rogueLinkTicks", 120, 20, 1200);
        ROGUE_LINK_COOLDOWN = builder.defineInRange("rogueLinkCooldownSeconds", 20.0D, 0.0D, 600.0D);
        builder.pop();
    }
}
