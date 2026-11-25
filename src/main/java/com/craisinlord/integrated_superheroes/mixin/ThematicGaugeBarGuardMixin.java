package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.content.ability.client.visuals.GaugeBarRenderer", remap = false)
public abstract class ThematicGaugeBarGuardMixin {
    @Shadow(remap = false)
    private static String shownBy;

    @Inject(method = "tick(Lnet/minecraft/client/Minecraft;)V", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipUnsuited(Minecraft client, CallbackInfo ci) {
        Player player = client.player;
        if (player != null && shownBy == null && player.getInventory() != null
                && ThematicIdle.unsuited(player) && ThematicIdle.noHeldDownFlags(player)) {
            ci.cancel();
        }
    }
}
