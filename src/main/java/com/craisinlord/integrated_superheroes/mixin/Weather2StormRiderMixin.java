package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.hero.StormPower;
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
    private void integratedSuperheroes$stormIsNeverGrabbed(Entity entity, CallbackInfoReturnable<Boolean> callback) {
        if (entity instanceof Player player && StormPower.ID.equals(HeroPowers.data(player).getPrimaryPower())) {
            callback.setReturnValue(false);
        }
    }
}
