package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.ThingPower;
import com.craisinlord.integrated_marvel.hero.ThingSkin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayer.class)
public abstract class ThingSkinMixin {
    @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
    private void integratedMarvel$rockSkin(CallbackInfoReturnable<PlayerSkin> cir) {
        if (ThingPower.isThing((AbstractClientPlayer) (Object) this)) {
            cir.setReturnValue(ThingSkin.apply(cir.getReturnValue()));
        }
    }
}
