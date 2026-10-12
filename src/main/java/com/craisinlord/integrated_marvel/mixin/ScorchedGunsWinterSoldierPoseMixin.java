package com.craisinlord.integrated_marvel.mixin;

import net.tintankgames.marvel.world.entity.WinterSoldier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import top.ribs.scguns.item.GunItem;

@Mixin(value = WinterSoldier.class, remap = false)
public abstract class ScorchedGunsWinterSoldierPoseMixin {
    @Inject(method = "getArmPose", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void integratedMarvel$gunPose(CallbackInfoReturnable<WinterSoldier.ArmPose> cir) {
        WinterSoldier self = (WinterSoldier) (Object) this;
        if (self.getMainHandItem().getItem() instanceof GunItem) {
            cir.setReturnValue(WinterSoldier.ArmPose.CROSSBOW_HOLD);
        }
    }
}
