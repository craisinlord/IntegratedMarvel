package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = Player.class, priority = 1500)
public abstract class ViltrumiteHeroFlightMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.PlayerEntityCoreMixin", name = "onTick")
    @ModifyExpressionValue(method = "@MixinSquared:Handler", at = @At(value = "INVOKE", target = "Ljava/util/Set;contains(Ljava/lang/Object;)Z"), remap = false)
    private boolean integratedSuperheroes$keepHeroFlight(boolean exempt) {
        if (exempt) {
            return true;
        }
        Player player = (Player) (Object) this;
        return !player.getAbilities().mayfly && !player.getAbilities().flying || player.getAttributeValue(NeoForgeMod.CREATIVE_FLIGHT) > 0.0D;
    }
}
