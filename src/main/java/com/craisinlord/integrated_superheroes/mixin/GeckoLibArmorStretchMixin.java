package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoArmorRenderer;

@Mixin(value = GeoArmorRenderer.class, remap = false)
public abstract class GeckoLibArmorStretchMixin {
    @Shadow
    protected GeoBone head;
    @Shadow
    protected GeoBone body;
    @Shadow
    protected GeoBone rightArm;
    @Shadow
    protected GeoBone leftArm;
    @Shadow
    protected GeoBone rightLeg;
    @Shadow
    protected GeoBone leftLeg;
    @Shadow
    protected GeoBone rightBoot;
    @Shadow
    protected GeoBone leftBoot;

    @Inject(method = "applyBaseTransformations", at = @At("TAIL"))
    private void integratedSuperheroes$copyStretch(HumanoidModel<?> baseModel, CallbackInfo ci) {
        integratedSuperheroes$scale(head, baseModel.head);
        integratedSuperheroes$scale(body, baseModel.body);
        integratedSuperheroes$scale(rightArm, baseModel.rightArm);
        integratedSuperheroes$scale(leftArm, baseModel.leftArm);
        integratedSuperheroes$scale(rightLeg, baseModel.rightLeg);
        integratedSuperheroes$scale(leftLeg, baseModel.leftLeg);
        integratedSuperheroes$scale(rightBoot, baseModel.rightLeg);
        integratedSuperheroes$scale(leftBoot, baseModel.leftLeg);
    }

    private static void integratedSuperheroes$scale(GeoBone bone, ModelPart part) {
        if (bone != null) {
            bone.updateScale(part.xScale, part.yScale, part.zScale);
        }
    }
}
