package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.model.client.armor.ArmorBones;
import bond.thematic.api.model.client.armor.ArmorTransforms;
import bond.thematic.api.model.client.armor.ModelArmorRenderer;
import bond.thematic.api.model.pose.PoseState;
import com.craisinlord.integrated_superheroes.compat.thematic.SuitPieces;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.registries.armors.armor.client.CharacterRenderer", remap = false)
public abstract class ThematicSuitPartsMixin {
    @Inject(method = "applyBoneVisibilityBySlot", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$splitParts(EquipmentSlot slot, CallbackInfo ci) {
        if (slot != EquipmentSlot.CHEST) {
            return;
        }
        ModelArmorRenderer<?> renderer = (ModelArmorRenderer<?>) (Object) this;
        if (!(renderer.getCurrentEntity() instanceof Player player)) {
            return;
        }
        SuitPieces.Parts parts = SuitPieces.parts(player, renderer.getCurrentStack());
        PoseState pose = renderer.currentPose();
        ArmorBones.Resolved bones = renderer.resolvedBones();
        if (parts == null || pose == null || bones == null) {
            return;
        }
        if (!parts.head()) {
            hide(pose, bones.head());
        }
        if (!parts.body()) {
            hide(pose, bones.body());
            hide(pose, bones.rightArm());
            hide(pose, bones.leftArm());
        }
        if (!parts.legs()) {
            hide(pose, bones.rightLeg());
            hide(pose, bones.leftLeg());
        }
        if (!parts.feet()) {
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
