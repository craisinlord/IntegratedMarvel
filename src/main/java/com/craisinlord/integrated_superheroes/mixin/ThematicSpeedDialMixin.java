package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.LanternRings;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.content.movement.MovementModes", remap = false)
public abstract class ThematicSpeedDialMixin {
    @Inject(method = "adjust(Lnet/minecraft/server/level/ServerPlayer;Ljava/lang/String;I)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$lockSpeedDial(ServerPlayer player, String key, int delta, CallbackInfo ci) {
        if ("speed".equals(key) && LanternRings.locksSpeedDial(player)) {
            ci.cancel();
        }
    }
}
