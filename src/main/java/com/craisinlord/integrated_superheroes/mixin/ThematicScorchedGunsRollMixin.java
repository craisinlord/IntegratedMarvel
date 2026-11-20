package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.criminal.DataCriminalEntity;
import bond.thematic.api.content.criminal.GunnerStats;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicScorchedGuns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DataCriminalEntity.class, remap = false)
public abstract class ThematicScorchedGunsRollMixin {
    @Inject(method = "rollGun", at = @At("RETURN"), cancellable = true, remap = false)
    private void integratedSuperheroes$scorchedGun(GunnerStats stats, CallbackInfoReturnable<String> cir) {
        DataCriminalEntity self = (DataCriminalEntity) (Object) this;
        String gun = ThematicScorchedGuns.gunFor(self.getType(), cir.getReturnValue());
        if (gun != null && !gun.equals(cir.getReturnValue())) {
            cir.setReturnValue(gun);
        }
    }
}
