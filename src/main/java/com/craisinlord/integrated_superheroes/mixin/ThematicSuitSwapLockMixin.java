package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.util.AbilityUtils", remap = false)
public abstract class ThematicSuitSwapLockMixin {
    @Inject(method = "hasPressAbilityBusy(Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$freeSuitSwaps(Player player, CallbackInfoReturnable<Boolean> cir) {
        if (ThematicOptions.on(ThematicOptions.FREE_SUIT_SWAPS)) {
            cir.setReturnValue(false);
        }
    }
}
