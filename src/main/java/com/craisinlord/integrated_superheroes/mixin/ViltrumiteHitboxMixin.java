package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.baranhan.viltrumiteflight.util.FlightState;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Player.class, priority = 1500)
public abstract class ViltrumiteHitboxMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.mixin.PlayerEntityMixin", name = "updateFlightHitbox")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;noCollision(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/AABB;)Z"), remap = false)
    private boolean integratedSuperheroes$restoreOutsideCruise(boolean fits) {
        FlightState state = ((ViltrumiteFlightPlayer) this).getFlightState();
        return fits || state == FlightState.NONE || state == FlightState.HOVER;
    }
}
