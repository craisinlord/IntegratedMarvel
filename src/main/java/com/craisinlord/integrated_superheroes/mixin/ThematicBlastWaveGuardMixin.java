package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.data.EntityComponents;
import bond.thematic.api.registries.data.ability.AbilityDataComponent;
import bond.thematic.api.rendering.seam.ModelDrawTarget;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.content.ability.client.visuals.BlastWaveGeometry", remap = false)
public abstract class ThematicBlastWaveGuardMixin {
    @Inject(method = "renderFor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipEmptyData(LivingEntity entity, String abilityId, PoseStack pose, ModelDrawTarget target,
                                                           float partialTick, Camera camera, CallbackInfo ci) {
        if (EntityComponents.ABILITY_DATA == null) {
            return;
        }
        AbilityDataComponent data = EntityComponents.ABILITY_DATA.getNullable(entity);
        if (data == null || ((ThematicAbilityDataAccessor) data).integratedSuperheroes$syncedData().isEmpty()) {
            ci.cancel();
        }
    }
}
