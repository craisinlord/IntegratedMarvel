package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityEffectInvoker {
    @Invoker("onEffectRemoved")
    void integratedSuperheroes$onEffectRemoved(MobEffectInstance effect);
}
