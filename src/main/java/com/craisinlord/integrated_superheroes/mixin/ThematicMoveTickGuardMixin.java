package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class ThematicMoveTickGuardMixin {
    @TargetHandler(mixin = "bond.thematic.api.mixin.core.LivingEntityMixin", name = "thematic$moveTick")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$skipIdleMover(CallbackInfo outer, CallbackInfo ci) {
        if (ThematicIdle.movementQuiet((LivingEntity) (Object) this)) {
            ci.cancel();
        }
    }
}
