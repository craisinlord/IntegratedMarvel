package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntityRenderer.class, priority = 1500)
public abstract class ViltrumiteHandSyncMixin {
    @Unique
    private static int integratedSuperheroes$lastSyncTick = Integer.MIN_VALUE;

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.client.mixin.HandPositionTrackerMixin", name = "extractHandPosition")
    @WrapWithCondition(method = "@MixinSquared:Handler", at = @At(value = "INVOKE",
            target = "Ldev/baranhan/viltrumitecore/network/CoreMessages;sendToServer(Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"), remap = false)
    private boolean integratedSuperheroes$syncOncePerTick(CustomPacketPayload payload) {
        if (!ViltrumiteGuards.performance()) {
            return true;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.tickCount == integratedSuperheroes$lastSyncTick) {
            return false;
        }
        integratedSuperheroes$lastSyncTick = player.tickCount;
        return true;
    }
}
