package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.baranhan.viltrumiteflight.client.FlightInputHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FlightInputHandler.class, remap = false)
public abstract class ViltrumiteFlightInputMixin {
    @ModifyExpressionValue(method = "onClientTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z"))
    private static boolean integratedSuperheroes$onlyViltrumiteTakeoff(boolean mayfly) {
        LocalPlayer player = Minecraft.getInstance().player;
        return (mayfly || player != null && player.mayFly()) && ViltrumiteCompat.mayUseFlight(player);
    }
}
