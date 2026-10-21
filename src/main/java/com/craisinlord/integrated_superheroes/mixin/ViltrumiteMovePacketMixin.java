package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import java.util.Set;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerGamePacketListenerImpl.class, priority = 1500)
public abstract class ViltrumiteMovePacketMixin {
    @Shadow
    public ServerPlayer player;

    @WrapMethod(method = "handleMovePlayer")
    private void integratedSuperheroes$markMoveHandling(ServerboundMovePlayerPacket packet, Operation<Void> original) {
        ViltrumiteGuards.enterMove();
        try {
            original.call(packet);
        } finally {
            ViltrumiteGuards.exitMove();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.AntiCheatBypassMixin", name = "bypassGrabRubberband")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$skipUngrabbed(ServerboundMovePlayerPacket packet, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteGuards.performance()) {
            return;
        }
        if (!ViltrumiteCompat.active() || player == null || !player.getTags().contains(ViltrumiteGuards.GRAB_TAG)) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.AntiCheatBypassMixin", name = "bypassFlightRubberband")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$onlyMovementCorrections(double x, double y, double z, float yaw, float pitch, Set<?> relative,
                                                             CallbackInfo original, CallbackInfo ci) {
        if (ViltrumiteGuards.teleports() && !ViltrumiteGuards.handlingMove()) {
            ci.cancel();
        }
    }
}
