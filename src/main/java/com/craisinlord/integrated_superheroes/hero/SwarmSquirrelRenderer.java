package com.craisinlord.integrated_superheroes.hero;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class SwarmSquirrelRenderer extends GeoEntityRenderer<SwarmSquirrel> {
    public SwarmSquirrelRenderer(EntityRendererProvider.Context context) {
        super(context, new SwarmSquirrelModel());
        this.shadowRadius = 0.3F;
    }

    @Override
    protected void applyRotations(SwarmSquirrel animatable, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick, float nativeScale) {
        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, nativeScale);
        if (animatable.isOnShoulder()) {
            poseStack.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot())));
        }
    }
}
