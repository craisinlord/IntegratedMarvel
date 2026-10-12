package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.TimeFreezeState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TimeFreezeTurnMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$lockFrozenCamera(double yRot, double xRot, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && TimeFreezeState.isFrozenPlayer(self)) ci.cancel();
    }
}
