package com.craisinlord.integrated_superheroes.gauntlet;

import com.craisinlord.integrated_superheroes.gauntlet.InfinityGauntletStones;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class GauntletCooldownClientState {
    private static final Map<String, ClientCooldown> COOLDOWNS = new ConcurrentHashMap<>();

    private GauntletCooldownClientState() {}

    public static void set(GauntletCooldownPayload payload) {
        COOLDOWNS.put(key(payload.stone(), payload.ability()),
                new ClientCooldown(System.currentTimeMillis() + payload.duration() * 50L, payload.duration()));
    }

    public static float percent(Player player) {
        ClientCooldown cooldown = current(player);
        if (cooldown == null) return 0.0F;
        long remaining = cooldown.endsAtMillis() - System.currentTimeMillis();
        return Math.clamp((float) remaining / (cooldown.duration() * 50.0F), 0.0F, 1.0F);
    }

    public static long remainingMillis(Player player) {
        ClientCooldown cooldown = current(player);
        return cooldown == null ? 0L : Math.max(0L, cooldown.endsAtMillis() - System.currentTimeMillis());
    }

    private static ClientCooldown current(Player player) {
        var gauntlet = InfinityGauntletStones.equippedGauntlet(player);
        if (gauntlet.isEmpty()) return null;
        String stone = InfinityGauntletStones.selectedStone(gauntlet);
        int ability = InfinityGauntletStones.selectedAbility(gauntlet, stone) + 1;
        ClientCooldown cooldown = COOLDOWNS.get(key(stone, ability));
        if (cooldown == null) return null;
        if (cooldown.endsAtMillis() <= System.currentTimeMillis()) {
            COOLDOWNS.remove(key(stone, ability), cooldown);
            return null;
        }
        return cooldown;
    }

    private static String key(String stone, int ability) { return stone + ":" + ability; }
    private record ClientCooldown(long endsAtMillis, int duration) {}
}
