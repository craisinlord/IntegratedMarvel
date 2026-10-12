package com.craisinlord.integrated_marvel.suit;

import com.craisinlord.integrated_marvel.hero.IronManSuits;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

public record SuitContext(ServerPlayer player, String suitId, IronManSuits.Stats stats) {
    private static final String FIRST_MISSILE_SUIT = "iron_man_mark_7";
    private static final String TOP_SUIT = "iron_man_mark_43";

    public ItemStack chest() {
        return player.getItemBySlot(EquipmentSlot.CHEST);
    }

    public double repulsor() {
        return stats.repulsorDamage().get();
    }

    public double unibeam() {
        return stats.unibeamDamage().get();
    }

    public float damage(SuitAbilityConfig.Settings settings) {
        return (float) (repulsor() * settings.damageMultiplier().get());
    }

    public double tier() {
        IronManSuits.Stats low = IronManSuits.get(FIRST_MISSILE_SUIT);
        IronManSuits.Stats high = IronManSuits.get(TOP_SUIT);
        if (low == null || high == null) {
            return 0.0D;
        }
        double from = low.repulsorDamage().get();
        double to = high.repulsorDamage().get();
        return to <= from ? 1.0D : Mth.clamp((repulsor() - from) / (to - from), 0.0D, 1.0D);
    }
}
