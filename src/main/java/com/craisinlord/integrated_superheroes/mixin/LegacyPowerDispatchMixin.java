package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroPowers;
import com.craisinlord.integrated_superheroes.suit.SuitAbilities;
import com.stxllarplanes.marvellegacy.network.packet.PowerHandlerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = PowerHandlerPacket.class, remap = false)
public abstract class LegacyPowerDispatchMixin {
    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$dispatchHeroPowers(PowerHandlerPacket packet, IPayloadContext context, CallbackInfo ci) {
        if (context.player() instanceof ServerPlayer suitPlayer
                && SuitAbilities.handle(suitPlayer, packet.ability(), packet.slot(), packet.inputType())) {
            ci.cancel();
            return;
        }
        if ("press".equals(packet.inputType())
                && context.player() instanceof ServerPlayer player
                && HeroPowers.handleAbility(player, packet.ability(), packet.slot())) {
            ci.cancel();
        }
    }
}
