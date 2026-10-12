package com.craisinlord.integrated_marvel.suit;

import com.craisinlord.integrated_marvel.hero.IronManEnergy;
import net.minecraft.world.item.ItemStack;
import net.tintankgames.marvel.world.item.EnergySuitItem;

public final class SuitEnergy {
    private SuitEnergy() {
    }

    public static boolean has(ItemStack chest, double amount) {
        return amount <= 0.0D || !(chest.getItem() instanceof EnergySuitItem)
                || EnergySuitItem.getEnergy(chest) >= IronManEnergy.scaledPercent(chest, (float) amount);
    }

    public static void spend(ItemStack chest, double amount) {
        if (amount > 0.0D && chest.getItem() instanceof EnergySuitItem) {
            EnergySuitItem.removeEnergy(chest, (float) amount);
        }
    }

    public static double drainAll(ItemStack chest) {
        if (!(chest.getItem() instanceof EnergySuitItem)) {
            return 100.0D;
        }
        double percent = EnergySuitItem.getEnergy(chest);
        IronManEnergy.setFE(chest, 0.0D);
        return percent;
    }
}
