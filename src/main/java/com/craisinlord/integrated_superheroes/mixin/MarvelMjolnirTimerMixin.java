package com.craisinlord.integrated_superheroes.mixin;

import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.timers.TimerQueue;
import net.tintankgames.marvel.world.level.timers.ResetCallMjolnirCallback;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ResetCallMjolnirCallback.class, remap = false)
public abstract class MarvelMjolnirTimerMixin {
    @Shadow
    @Final
    private UUID player;

    @Inject(method = "handle(Lnet/minecraft/server/MinecraftServer;Lnet/minecraft/world/level/timers/TimerQueue;J)V", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$playerLeft(MinecraftServer server, TimerQueue<MinecraftServer> timers, long time, CallbackInfo ci) {
        if (server.getPlayerList().getPlayer(this.player) == null) ci.cancel();
    }
}
