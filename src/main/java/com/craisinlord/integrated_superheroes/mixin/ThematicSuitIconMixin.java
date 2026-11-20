package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.client.appearance.MannequinPoseContext;
import bond.thematic.api.model.client.armor.ArmorBones;
import bond.thematic.api.model.client.armor.ArmorTransforms;
import bond.thematic.api.model.geo.BakedModel;
import bond.thematic.api.model.pose.PoseState;
import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieceIcons;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import net.minecraft.world.entity.EquipmentSlot;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.registries.armors.armor.client.CharacterItemRenderer", remap = false)
public abstract class ThematicSuitIconMixin {
    @Unique
    private int integratedSuperheroes$zoomed;

    @Inject(method = "renderByItem", at = @At("HEAD"), remap = false)
    private void integratedSuperheroes$zoomIn(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                              CallbackInfo ci) {
        if (!(stack.getItem() instanceof CharacterItem suit) || MannequinPoseContext.current() != null) {
            integratedSuperheroes$zoomed = 0;
            return;
        }
        EquipmentSlot piece = SuitPieceIcons.drawing();
        float zoom;
        float focus;
        if (piece == EquipmentSlot.HEAD) {
            zoom = 2.5F;
            focus = 2.25F;
        } else if (piece == EquipmentSlot.LEGS) {
            zoom = 2.0F;
            focus = 0.906F;
        } else if (piece == EquipmentSlot.FEET) {
            zoom = 3.0F;
            focus = 0.656F;
        } else if (SuitPieces.helmetless(suit)) {
            zoom = 1.5F;
            focus = 1.8F;
        } else {
            zoom = 1.75F;
            focus = 1.625F;
        }
        pose.pushPose();
        pose.translate(0.5F, 1.5F, 0.5F);
        pose.scale(zoom, zoom, zoom);
        pose.translate(-0.5F, -focus, -0.5F);
        integratedSuperheroes$zoomed = 1;
    }

    @Inject(method = "renderByItem", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$zoomOut(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay,
                                               CallbackInfo ci) {
        if (integratedSuperheroes$zoomed == 1) {
            integratedSuperheroes$zoomed = 0;
            pose.popPose();
        }
    }

    @Inject(method = "posePose", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$chestOnly(CharacterItem suit, BakedModel model, PoseState pose, CallbackInfo ci) {
        if (suit == null || model == null || pose == null || MannequinPoseContext.current() != null) {
            return;
        }
        ArmorBones.Resolved bones = ArmorBones.DEFAULT.resolve(model);
        if (bones == null) {
            return;
        }
        EquipmentSlot piece = SuitPieceIcons.drawing();
        boolean head = piece == null ? SuitPieces.helmetless(suit) : piece == EquipmentSlot.HEAD;
        boolean body = piece == null;
        boolean legs = piece == EquipmentSlot.LEGS;
        boolean feet = piece == EquipmentSlot.FEET;
        if (!head) {
            hide(pose, bones.head());
        }
        if (!body) {
            hide(pose, bones.body());
            hide(pose, bones.rightArm());
            hide(pose, bones.leftArm());
        }
        if (!legs) {
            hide(pose, bones.rightLeg());
            hide(pose, bones.leftLeg());
        }
        if (!feet) {
            hide(pose, bones.rightBoot());
            hide(pose, bones.leftBoot());
        }
    }

    private static void hide(PoseState pose, int bone) {
        if (bone >= 0) {
            ArmorTransforms.setVisible(pose, bone, false);
        }
    }
}
