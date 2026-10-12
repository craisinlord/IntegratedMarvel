package com.craisinlord.integrated_marvel.mixin;

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
    private void integratedMarvel$copyStretch(HumanoidModel<?> baseModel, CallbackInfo ci) {
        integratedMarvel$scale(head, baseModel.head);
        integratedMarvel$scale(body, baseModel.body);
        integratedMarvel$scale(rightArm, baseModel.rightArm);
        integratedMarvel$scale(leftArm, baseModel.leftArm);
        integratedMarvel$scale(rightLeg, baseModel.rightLeg);
        integratedMarvel$scale(leftLeg, baseModel.leftLeg);
        integratedMarvel$scale(rightBoot, baseModel.rightLeg);
        integratedMarvel$scale(leftBoot, baseModel.leftLeg);
    }

    private static void integratedMarvel$scale(GeoBone bone, ModelPart part) {
        if (bone != null) {
            bone.updateScale(part.xScale, part.yScale, part.zScale);
        }
    }
}
