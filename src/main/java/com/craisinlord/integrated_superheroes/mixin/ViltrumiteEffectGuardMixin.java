package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class ViltrumiteEffectGuardMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.LivingEntityStatsMixin", name = "rejectDebuffs")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$allowBypassEffects(MobEffectInstance effect, CallbackInfoReturnable<Boolean> original, CallbackInfo ci) {
        if (ViltrumiteGuards.bypassesEffectImmunity(effect)) {
            ci.cancel();
        }
    }
}
