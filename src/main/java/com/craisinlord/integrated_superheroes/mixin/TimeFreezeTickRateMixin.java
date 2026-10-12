package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.TimeFreezeState;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TickRateManager.class)
public abstract class TimeFreezeTickRateMixin {
    @Inject(method = "isEntityFrozen", at = @At("RETURN"), cancellable = true)
    private void integratedSuperheroes$gauntletTimeFreeze(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && TimeFreezeState.isFrozen(entity)) cir.setReturnValue(true);
    }
}
