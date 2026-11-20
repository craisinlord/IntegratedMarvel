package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.content.ability.client.visuals.SilhouetteRenderer", remap = false)
public abstract class ThematicSilhouetteGuardMixin {
    @Inject(method = "silhouetteFor(Lnet/minecraft/world/entity/Entity;)Lbond/thematic/api/content/ability/state/visuals/SilhouetteVisual$Settings;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$noEffectsNoSilhouette(Entity entity, CallbackInfoReturnable<Object> cir) {
        if (!(entity instanceof LivingEntity living) || living.getActiveEffectsMap().isEmpty()) {
            cir.setReturnValue(null);
        }
    }
}
