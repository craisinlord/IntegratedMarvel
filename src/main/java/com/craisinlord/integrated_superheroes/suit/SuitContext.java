package com.craisinlord.integrated_superheroes.suit;

import com.craisinlord.integrated_superheroes.hero.HeroCaster;
import com.craisinlord.integrated_superheroes.hero.IronManSuits;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public record SuitContext(LivingEntity caster, String suitId, IronManSuits.Stats stats) {
    private static final String FIRST_MISSILE_SUIT = "iron_man_mark_7";
    private static final String TOP_SUIT = "iron_man_mark_43";

    public static SuitContext of(LivingEntity caster, String suitId) {
        IronManSuits.Stats stats = suitId == null ? null : IronManSuits.get(suitId);
        return stats == null ? null : new SuitContext(caster, suitId, stats);
    }

    public ServerPlayer player() {
        return HeroCaster.player(caster);
    }

    public ServerLevel level() {
        return HeroCaster.level(caster);
    }

    public ItemStack chest() {
        return caster.getItemBySlot(EquipmentSlot.CHEST);
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
