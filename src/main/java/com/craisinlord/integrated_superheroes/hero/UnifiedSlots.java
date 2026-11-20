package com.craisinlord.integrated_superheroes.hero;

import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.entity.player.Player;

import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;

public final class UnifiedSlots {
    public static final int SLOTS = 12;
    private static final boolean[] SUIT_OWNED = new boolean[SLOTS];
    private static SuitRows suitRows = (player, keys) -> List.of();
    private static BooleanSupplier hostedElsewhere = () -> false;

    public interface SuitRows {
        List<AbilityRow> rows(Player player, KeyMapping[] keys);
    }

    private UnifiedSlots() {
    }

    public static int firstFree(PlayerData data) {
        int base = ViltrumitePower.ID.equals(data.getPrimaryPower()) ? HeroSlots.VILTRUMITE_OFFSET : 0;
        List<Power> active = data.getActivePowers();
        for (int i = 0; i < active.size() && i < HeroSlots.SLOTS; i++) {
            Power power = active.get(i);
            if (power != null && !"none".equals(power.getName())) {
                base = Math.max(base, i + 1);
            }
        }
        return base;
    }

    public static boolean suitOwned(int slot) {
        return slot >= 0 && slot < SLOTS && SUIT_OWNED[slot];
    }

    public static void setSuitOwned(boolean[] owned) {
        for (int i = 0; i < SLOTS; i++) {
            SUIT_OWNED[i] = i < owned.length && owned[i];
        }
    }

    public static void clearSuitOwned() {
        Arrays.fill(SUIT_OWNED, false);
    }

    public static void setSuitRows(SuitRows rows) {
        suitRows = rows;
    }

    public static List<AbilityRow> suitRows(Player player, KeyMapping[] keys) {
        return suitRows.rows(player, keys);
    }

    public static void setHostedElsewhere(BooleanSupplier supplier) {
        hostedElsewhere = supplier;
    }

    public static boolean hostedElsewhere() {
        return hostedElsewhere.getAsBoolean();
    }
}
