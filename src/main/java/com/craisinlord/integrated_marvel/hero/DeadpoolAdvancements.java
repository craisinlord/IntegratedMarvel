package com.craisinlord.integrated_marvel.hero;

import com.craisinlord.integrated_marvel.IntegratedMarvel;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class DeadpoolAdvancements {
    public static final String ROOT = "maximum_effort";
    public static final String UNALIVE = "unalive_unsuccessful";
    public static final String NAH = "nah";
    public static final String REGENERATING_DEGENERATE = "regenerating_degenerate";
    public static final String OKAY_FINE = "okay_fine";
    public static final String OOPS = "oops";
    public static final String SELF_INFLICTED_COMBO = "self_inflicted_combo";
    public static final String THAT_NEVER_HAPPENED = "that_never_happened";
    public static final String MONTAGE_SKIPPED = "montage_skipped";
    public static final String UNPAUSE = "unpause";
    public static final String I_READ_THE_WIKI = "i_read_the_wiki";
    public static final String GUN_KATA = "gun_kata";
    public static final String FIFTH_WALL = "fourth_wall_fifth_wall";
    public static final String NOT_THE_HERO = "not_the_hero_we_deserve";

    private DeadpoolAdvancements() {
    }

    public static void award(ServerPlayer player, String name) {
        AdvancementHolder advancement = player.server.getAdvancements().get(ResourceLocation.fromNamespaceAndPath(IntegratedMarvel.MODID, "deadpool/" + name));
        if (advancement == null) {
            return;
        }
        for (String criterion : player.getAdvancements().getOrStartProgress(advancement).getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }
}
