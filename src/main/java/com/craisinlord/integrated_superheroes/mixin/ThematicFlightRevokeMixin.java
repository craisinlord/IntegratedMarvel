package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.neoforged.neoforge.common.NeoForgeMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "bond.thematic.api.event.ServerHandler", remap = false)
public abstract class ThematicFlightRevokeMixin {
    @Inject(method = "revokeFlight", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$keepOtherFlight(ServerPlayer player, boolean applyDownwardImpulse, CallbackInfo ci) {
        if (!applyDownwardImpulse || !ThematicOptions.on(ThematicOptions.KEEP_OTHER_FLIGHT)) {
            return;
        }
        AttributeInstance flight = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (player.getAbilities().mayfly || (flight != null && flight.getValue() > 0.0D)) {
            ci.cancel();
        }
    }
}
