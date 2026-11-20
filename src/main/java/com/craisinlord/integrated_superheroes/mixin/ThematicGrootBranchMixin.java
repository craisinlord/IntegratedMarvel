package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.model.client.armor.ModelArmorRenderer;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicGrootPose;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.registries.armors.armor.client.CharacterRenderer", remap = false)
public abstract class ThematicGrootBranchMixin {
    @Inject(method = "applyBoneVisibilityBySlot", at = @At("RETURN"), remap = false)
    private void integratedSuperheroes$stretchBranch(EquipmentSlot slot, CallbackInfo ci) {
        ModelArmorRenderer<?> renderer = (ModelArmorRenderer<?>) (Object) this;
        if (renderer.getCurrentEntity() instanceof Player player) {
            ThematicGrootPose.stretch(player, renderer.currentPose());
        }
    }
}
