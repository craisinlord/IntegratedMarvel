package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroHits;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(LivingEntity.class)
public abstract class HeroHitsMixin implements HeroHits.Invulnerability {
    @Shadow
    protected float lastHurt;

    @WrapMethod(method = "hurt")
    private boolean integratedSuperheroes$landAbilityHits(DamageSource source, float amount, Operation<Boolean> original) {
        return HeroHits.land((LivingEntity) (Object) this, source, amount, scaled -> original.call(source, scaled));
    }

    @Override
    public float integratedSuperheroes$lastHurt() {
        return this.lastHurt;
    }

    @Override
    public void integratedSuperheroes$setLastHurt(float amount) {
        this.lastHurt = amount;
    }
}
