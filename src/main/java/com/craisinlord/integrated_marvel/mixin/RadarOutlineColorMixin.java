package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.DaredevilClient;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class RadarOutlineColorMixin {
    @Inject(method = "getTeamColor", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$radarColor(CallbackInfoReturnable<Integer> cir) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && DaredevilClient.highlights(self)) {
            cir.setReturnValue(DaredevilClient.OUTLINE);
        }
    }
}
