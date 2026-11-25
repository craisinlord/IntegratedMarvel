package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.player.Player;
import net.redgitreds.infinitygauntlet.network.GauntletPowersMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GauntletPowersMessage.class, remap = false)
public abstract class InfinityGauntletLegacyPowersMixin {
    @Inject(method = "pressAction", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$ignoreLegacyPowers(Player player, int type, int pressedMs, CallbackInfo ci) {
        ci.cancel();
    }
}
