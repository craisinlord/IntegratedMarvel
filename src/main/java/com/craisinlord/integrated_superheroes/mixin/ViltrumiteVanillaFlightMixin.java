package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Player.class, priority = 1500)
public abstract class ViltrumiteVanillaFlightMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumiteflight.mixin.PlayerEntityMixin", name = "onTick")
    @WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "stopFlight()V", ordinal = 0), remap = false)
    private boolean integratedSuperheroes$keepVanillaFlight(Player player) {
        return player.getAbilities().flying && ViltrumiteBridge.isViltrumite(player);
    }
}
