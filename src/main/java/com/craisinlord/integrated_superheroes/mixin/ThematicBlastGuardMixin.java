package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import bond.thematic.api.registries.data.EntityComponents;
import bond.thematic.api.registries.data.ability.AbilityDataComponent;
import bond.thematic.api.rendering.seam.ModelDrawTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.content.ability.client.visuals.BlastRenderer", remap = false)
public abstract class ThematicBlastGuardMixin {
    @Inject(method = "drawInWorld", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipIdleEntities(Minecraft client, LivingEntity entity, PoseStack pose, ModelDrawTarget consumers,
                                                              float tickDelta, Camera camera, boolean firstPerson, CallbackInfo ci) {
        if (entity != client.player && !(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CharacterItem)
                && entity.getActiveEffectsMap().isEmpty() && integratedSuperheroes$noAbilityData(entity)) {
            ci.cancel();
        }
    }

    @Unique
    private static boolean integratedSuperheroes$noAbilityData(LivingEntity entity) {
        if (EntityComponents.ABILITY_DATA == null) {
            return true;
        }
        AbilityDataComponent data = EntityComponents.ABILITY_DATA.getNullable(entity);
        return data == null || ((ThematicAbilityDataAccessor) data).integratedSuperheroes$syncedData().isEmpty();
    }
}
