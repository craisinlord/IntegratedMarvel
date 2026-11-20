package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.bridge.BridgeHeldItems;
import bond.thematic.api.content.criminal.AbstractCriminalEntity;
import bond.thematic.api.content.criminal.GunWielder;
import bond.thematic.api.content.criminal.client.CriminalHeldItemLayer;
import bond.thematic.api.rendering.seam.ModelDrawTarget;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicScorchedGuns;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CriminalHeldItemLayer.class, remap = false)
public abstract class ThematicScorchedGunsHeldMixin {
    @Inject(method = "renderGun", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$drawScorchedGun(PoseStack poseStack, AbstractCriminalEntity animatable, ModelDrawTarget target, int light, int overlay, CallbackInfo ci) {
        if (!(animatable instanceof GunWielder wielder)) {
            return;
        }
        ItemStack stack = wielder.getEquippedGun();
        if (stack == null || !ThematicScorchedGuns.isGun(stack)) {
            return;
        }
        ci.cancel();
        poseStack.pushPose();
        try {
            poseStack.translate(0.375D, 0.74D, -0.15D);
            poseStack.mulPose(Axis.XP.rotation(4.75F));
            BridgeHeldItems.draw(target, stack, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, animatable, light, overlay, poseStack);
        } finally {
            poseStack.popPose();
        }
    }
}
