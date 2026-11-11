package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Player.class, priority = 1500)
public abstract class ViltrumiteSupersonicMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.mixin.PlayerEntityMixin", name = "onTick")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/lang/Math;min(FF)F"), remap = false)
    private float integratedSuperheroes$subsonicUnlessToggled(float throttle) {
        return ViltrumiteCompat.capThrottle((Player) (Object) this, throttle);
    }
}
