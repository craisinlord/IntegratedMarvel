package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.StormZones;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Biome.class)
public abstract class StormBiomePrecipitationMixin {
    @Inject(method = "getPrecipitationAt", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$stormPrecipitation(BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> callback) {
        Boolean snow = StormZones.forcedAt(pos.getX() + 0.5D, pos.getZ() + 0.5D);
        if (snow != null) {
            callback.setReturnValue(snow ? Biome.Precipitation.SNOW : Biome.Precipitation.RAIN);
        }
    }

    @Inject(method = "warmEnoughToRain", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$stormTemperature(BlockPos pos, CallbackInfoReturnable<Boolean> callback) {
        Boolean snow = StormZones.forcedAt(pos.getX() + 0.5D, pos.getZ() + 0.5D);
        if (snow != null) {
            callback.setReturnValue(!snow);
        }
    }

    @Inject(method = "hasPrecipitation", at = @At("HEAD"), cancellable = true)
    private void integratedMarvel$stormHasPrecipitation(CallbackInfoReturnable<Boolean> callback) {
        if (StormZones.focusForced()) {
            callback.setReturnValue(true);
        }
    }
}
