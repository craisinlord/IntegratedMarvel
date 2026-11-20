package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.LanternRings;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.util.ThematicHelper", remap = false)
public abstract class ThematicRingActiveMixin {
    @Inject(method = "isRingActive(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$wornRing(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (LanternRings.wearsRing(player)) {
            cir.setReturnValue(true);
        }
    }
}
