package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FireworkRocketEntity.class)
public interface FireworkRocketAccessor {
    @Accessor("lifetime")
    int integratedSuperheroes$getLifetime();

    @Accessor("lifetime")
    void integratedSuperheroes$setLifetime(int lifetime);

    @Accessor("life")
    int integratedSuperheroes$getLife();

    @Accessor("attachedToEntity")
    LivingEntity integratedSuperheroes$getAttachedToEntity();

    @Invoker("explode")
    void integratedSuperheroes$explode();
}
