package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.server.level.ServerPlayer;

public final class CheatDeath {
    private static final String READY = "integrated_superheroes_cheat_death_ready";

    private CheatDeath() {
    }

    public static boolean ready(ServerPlayer player) {
        return player.getPersistentData().getLong(READY) <= now(player);
    }

    public static void use(ServerPlayer player) {
        long ticks = (long) (MarvelBalanceConfig.CHEAT_DEATH_COOLDOWN.get() * 20.0D);
        player.getPersistentData().putLong(READY, now(player) + ticks);
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }
}
