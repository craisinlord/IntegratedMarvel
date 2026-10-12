package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.HumanTorchPower;
import com.craisinlord.integrated_marvel.hero.InvisibleWomanPower;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class FantasticEntityMixin {
    @Inject(method = "displayFireAnimation", at = @At("RETURN"), cancellable = true)
    private void integratedMarvel$flameOn(CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && HumanTorchPower.showsFlames((Entity) (Object) this)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "canSpawnSprintParticle", at = @At("RETURN"), cancellable = true)
    private void integratedMarvel$silentSprint(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && (Object) this instanceof Player player && InvisibleWomanPower.isInvisible(player)) {
            cir.setReturnValue(false);
        }
    }
}
