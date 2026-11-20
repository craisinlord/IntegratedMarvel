package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.criminal.DataCriminalEntity;
import bond.thematic.api.content.criminal.GunnerBrain;
import bond.thematic.api.content.criminal.GunnerStats;
import com.craisinlord.integrated_superheroes.compat.GunJam;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunnerBrain.class, remap = false, priority = 900)
public abstract class ThematicGunJamMixin {
    @Shadow
    @Final
    private DataCriminalEntity entity;

    @Inject(method = "fireVolleyAt", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$jammed(GunnerStats stats, LivingEntity target, float spread, CallbackInfo ci) {
        if (GunJam.blocksShot(entity)) {
            ci.cancel();
        }
    }
}
