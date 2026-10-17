package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HumanoidModel.class)
public abstract class HulkArmorScaleMixin {
    @Inject(method = "copyPropertiesTo", at = @At("TAIL"))
    private void integratedSuperheroes$copyScale(HumanoidModel<?> target, CallbackInfo ci) {
        HumanoidModel<?> self = (HumanoidModel<?>) (Object) this;
        integratedSuperheroes$scale(self.head, target.head);
        integratedSuperheroes$scale(self.hat, target.hat);
        integratedSuperheroes$scale(self.body, target.body);
        integratedSuperheroes$scale(self.rightArm, target.rightArm);
        integratedSuperheroes$scale(self.leftArm, target.leftArm);
        integratedSuperheroes$scale(self.rightLeg, target.rightLeg);
        integratedSuperheroes$scale(self.leftLeg, target.leftLeg);
    }

    private static void integratedSuperheroes$scale(ModelPart from, ModelPart to) {
        to.xScale = from.xScale;
        to.yScale = from.yScale;
        to.zScale = from.zScale;
    }
}
