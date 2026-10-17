package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.baranhan.viltrumiteflight.network.packet.FlightToggleC2SPacket;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FlightToggleC2SPacket.class, remap = false)
public abstract class ViltrumiteFlightTakeoffMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$onlyViltrumiteTakeoff(FlightToggleC2SPacket packet, IPayloadContext context, CallbackInfo ci) {
        if (!ViltrumiteCompat.mayUseFlight(context.player())) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "handle", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z"))
    private static boolean integratedSuperheroes$allowHeroFlight(boolean mayfly, FlightToggleC2SPacket packet, IPayloadContext context) {
        Player player = context.player();
        return mayfly || player != null && player.mayFly();
    }
}
