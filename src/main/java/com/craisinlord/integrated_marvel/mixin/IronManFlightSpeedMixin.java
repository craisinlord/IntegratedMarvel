package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManSuits;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class IronManFlightSpeedMixin {
    @Inject(method = "getFlyingSpeed", at = @At("RETURN"), cancellable = true)
    private void integratedMarvel$markFlightSpeed(CallbackInfoReturnable<Float> cir) {
        Player player = (Player) (Object) this;
        if (!player.getAbilities().flying || player.isPassenger() || !IronManSuits.isLoaded()) {
            return;
        }
        String suit = IronManSuits.fullSuitId(player);
        IronManSuits.Stats stats = suit == null ? null : IronManSuits.get(suit);
        if (stats != null) {
            cir.setReturnValue(cir.getReturnValue() * stats.flightSpeed().get().floatValue());
        }
    }
}
