package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.HeroPowers;
import com.craisinlord.integrated_marvel.hero.StormPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import weather2.weathersystem.storm.TornadoHelper;

@Mixin(value = TornadoHelper.class, remap = false)
public abstract class Weather2StormRiderMixin {
    @Inject(method = {"canGrabEntity", "canGrabEntityClient"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedMarvel$stormIsNeverGrabbed(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof Player player && StormPower.ID.equals(HeroPowers.data(player).getPrimaryPower())) {
            callback.setReturnValue(false);
        }
    }
}
