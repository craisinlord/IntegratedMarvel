package com.craisinlord.integrated_superheroes.compat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

public final class JammedMobEffect extends MobEffect {
    public JammedMobEffect() {
        super(MobEffectCategory.HARMFUL, 0x3FA9FF);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return duration % 8 == 0;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.level() instanceof ServerLevel level) {
            double y = entity.getY() + entity.getBbHeight() * 0.6D;
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, entity.getX(), y, entity.getZ(), 3, entity.getBbWidth() * 0.4D, 0.25D, entity.getBbWidth() * 0.4D, 0.02D);
            if (entity.getRandom().nextInt(3) == 0) {
                level.sendParticles(ParticleTypes.SMOKE, entity.getX(), y, entity.getZ(), 1, 0.15D, 0.1D, 0.15D, 0.01D);
            }
        }
        return true;
    }
}
