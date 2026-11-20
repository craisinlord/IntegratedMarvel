package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.model.client.armor.ArmorBones;
import bond.thematic.api.model.pose.PoseState;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.model.client.armor.ModelArmorRenderer", remap = false)
public abstract class ThematicSuitStretchMixin {
    @Inject(method = "syncBase(Lbond/thematic/api/model/pose/PoseState;Lbond/thematic/api/model/client/armor/ArmorBones$Resolved;Lnet/minecraft/client/model/HumanoidModel;)V",
            at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$copyStretch(PoseState pose, ArmorBones.Resolved bones, HumanoidModel<?> base, CallbackInfo ci) {
        if (pose == null || bones == null || base == null) {
            return;
        }
        integratedSuperheroes$scale(pose, bones.head(), base.head);
        integratedSuperheroes$scale(pose, bones.body(), base.body);
        integratedSuperheroes$scale(pose, bones.rightArm(), base.rightArm);
        integratedSuperheroes$scale(pose, bones.leftArm(), base.leftArm);
        integratedSuperheroes$scale(pose, bones.rightLeg(), base.rightLeg);
        integratedSuperheroes$scale(pose, bones.leftLeg(), base.leftLeg);
        integratedSuperheroes$scale(pose, bones.rightBoot(), base.rightLeg);
        integratedSuperheroes$scale(pose, bones.leftBoot(), base.leftLeg);
    }

    private static void integratedSuperheroes$scale(PoseState pose, int bone, ModelPart part) {
        if (bone >= 0) {
            pose.setScale(bone, part.xScale, part.yScale, part.zScale);
        }
    }
}
