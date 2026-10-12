package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.gauntlet.MindControlManager;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MindControlledPlayerPacketMixin {
    @Shadow public net.minecraft.server.level.ServerPlayer player;

    @Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$blockMindControlledAttack(ServerboundInteractPacket packet, CallbackInfo callback) {
        if (MindControlManager.isControlled(this.player.getUUID())) {
            callback.cancel();
        }
    }

    @Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$blockMindControlledUse(ServerboundUseItemPacket packet, CallbackInfo callback) {
        if (MindControlManager.isControlled(this.player.getUUID())) {
            callback.cancel();
        }
    }

    @Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$blockMindControlledUseOn(ServerboundUseItemOnPacket packet, CallbackInfo callback) {
        if (MindControlManager.isControlled(this.player.getUUID())) {
            callback.cancel();
        }
    }
}
