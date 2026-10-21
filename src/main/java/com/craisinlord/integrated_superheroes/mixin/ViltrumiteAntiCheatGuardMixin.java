package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import net.minecraft.server.network.ServerCommonPacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ServerCommonPacketListenerImpl.class, priority = 1500)
public abstract class ViltrumiteAntiCheatGuardMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.mixin.FlightAntiCheatBypassMixin", name = "bypassMovementChecks")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$onlyDuringMovement(CallbackInfoReturnable<Boolean> original, CallbackInfo ci) {
        if (ViltrumiteGuards.teleports() && !ViltrumiteGuards.handlingMove()) {
            ci.cancel();
        }
    }
}
