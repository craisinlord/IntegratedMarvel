package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import dev.baranhan.viltrumiteflight.client.FlightInputHandler;
import dev.baranhan.viltrumiteflight.util.ViltrumiteFlightPlayer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = FlightInputHandler.class, remap = false)
public abstract class ViltrumiteFlightInputMixin {
    @ModifyExpressionValue(method = "onClientTick", at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/player/Abilities;mayfly:Z"))
    private static boolean integratedSuperheroes$onlyViltrumiteTakeoff(boolean mayfly) {
        LocalPlayer player = Minecraft.getInstance().player;
        return (mayfly || player != null && (player.mayFly() || ViltrumiteCompat.binaryFlight(player))) && ViltrumiteCompat.mayUseFlight(player);
    }

    @WrapWithCondition(method = "onClientTick", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumiteflight/util/ViltrumiteFlightPlayer;setTakeoffTicks(I)V", ordinal = 0))
    private static boolean integratedSuperheroes$noGroundedTakeoffBoost(ViltrumiteFlightPlayer flight, int ticks) {
        return !integratedSuperheroes$blockedTakeoff();
    }

    @WrapWithCondition(method = "onClientTick", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumiteflight/network/ModMessages;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V", ordinal = 0))
    private static boolean integratedSuperheroes$noGroundedTakeoff(CustomPacketPayload payload) {
        return !integratedSuperheroes$blockedTakeoff();
    }

    @Unique
    private static boolean integratedSuperheroes$blockedTakeoff() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || ViltrumiteBridge.inFlight(player)) {
            return false;
        }
        return player.onGround() || !player.level().noCollision(player, player.getBoundingBox().expandTowards(0.0D, 0.5D, 0.0D));
    }
}
