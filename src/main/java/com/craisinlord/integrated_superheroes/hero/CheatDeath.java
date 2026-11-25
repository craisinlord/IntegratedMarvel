package com.craisinlord.integrated_superheroes.hero;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

public final class CheatDeath {
    private static final String READY = "integrated_superheroes_cheat_death_ready";

    private CheatDeath() {
    }

    public static boolean ready(ServerPlayer player) {
        return player.getPersistentData().getCompound(Player.PERSISTED_NBT_TAG).getLong(READY) <= now(player);
    }

    public static void use(ServerPlayer player) {
        long ticks = (long) (MarvelBalanceConfig.CHEAT_DEATH_COOLDOWN.get() * 20.0D);
        CompoundTag data = player.getPersistentData();
        CompoundTag kept = data.getCompound(Player.PERSISTED_NBT_TAG);
        kept.putLong(READY, now(player) + ticks);
        data.put(Player.PERSISTED_NBT_TAG, kept);
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }
}
