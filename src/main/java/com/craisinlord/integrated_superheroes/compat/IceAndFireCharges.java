package com.craisinlord.integrated_superheroes.compat;

import com.iafenvoy.iceandfire.entity.FireDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.IceDragonChargeEntity;
import com.iafenvoy.iceandfire.entity.LightningDragonChargeEntity;
import com.iafenvoy.iceandfire.registry.IafEntities;
import com.iafenvoy.iceandfire.registry.IafMobEffects;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.level.Level;

public final class IceAndFireCharges {
    private IceAndFireCharges() {
    }

    public static AbstractHurtingProjectile ice(Level level) {
        return new IceDragonChargeEntity(IafEntities.ICE_DRAGON_CHARGE.get(), level, 0, 0, 0, 0, 0, 0);
    }

    public static AbstractHurtingProjectile fire(Level level) {
        return new FireDragonChargeEntity(IafEntities.FIRE_DRAGON_CHARGE.get(), level, 0, 0, 0, 0, 0, 0);
    }

    public static AbstractHurtingProjectile lightning(Level level) {
        return new LightningDragonChargeEntity(IafEntities.LIGHTNING_DRAGON_CHARGE.get(), level, 0, 0, 0, 0, 0, 0);
    }

    public static Holder<MobEffect> frozen() {
        return IafMobEffects.FROZEN;
    }

    public static Holder<MobEffect> sirenCharm() {
        return IafMobEffects.SIREN_CHARM;
    }
}
