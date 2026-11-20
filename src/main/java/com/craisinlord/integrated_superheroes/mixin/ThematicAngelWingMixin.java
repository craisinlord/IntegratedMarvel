package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.model.client.armor.ModelArmorRenderer;
import bond.thematic.api.model.pose.BoneHandle;
import bond.thematic.api.model.pose.PoseState;
import com.craisinlord.integrated_superheroes.hero.AngelWingLayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.registries.armors.armor.client.CharacterRenderer", remap = false)
public abstract class ThematicAngelWingMixin {
    @Inject(method = "applyBoneVisibilityBySlot", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$hideAngelWings(EquipmentSlot slot, CallbackInfo ci) {
        if (slot != EquipmentSlot.CHEST) {
            return;
        }
        ModelArmorRenderer<?> renderer = (ModelArmorRenderer<?>) (Object) this;
        if (!(renderer.getCurrentEntity() instanceof Player player) || !AngelWingLayer.replacesThematicWings(player)) {
            return;
        }
        PoseState pose = renderer.currentPose();
        if (pose == null || pose.model() == null) {
            return;
        }
        for (String name : AngelWingLayer.replacedWingBones(player)) {
            if (pose.model().bone(name) == null) {
                continue;
            }
            BoneHandle bone = pose.bone(name);
            if (bone != null) {
                bone.setHidden(true);
                bone.setChildrenHidden(true);
            }
        }
    }
}
