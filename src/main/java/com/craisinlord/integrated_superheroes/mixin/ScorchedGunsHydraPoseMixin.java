package com.craisinlord.integrated_superheroes.mixin;

import net.tintankgames.marvel.world.entity.HydraAgent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.item.GunItem;

@Mixin(value = HydraAgent.class, remap = false)
public abstract class ScorchedGunsHydraPoseMixin {
    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$gunPose(CallbackInfoReturnable<HydraAgent.ArmPose> cir) {
        HydraAgent self = (HydraAgent) (Object) this;
        if (self.getMainHandItem().getItem() instanceof GunItem) {
            cir.setReturnValue(HydraAgent.ArmPose.CROSSBOW_HOLD);
        }
    }
}
