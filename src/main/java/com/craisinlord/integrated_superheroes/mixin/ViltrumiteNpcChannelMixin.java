package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = NetworkRegistry.class, remap = false)
public abstract class ViltrumiteNpcChannelMixin {
    @Inject(method = "hasChannel(Lnet/minecraft/network/Connection;Lnet/minecraft/network/ConnectionProtocol;Lnet/minecraft/resources/ResourceLocation;)Z", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$noChannelForNpcs(Connection connection, ConnectionProtocol protocol, ResourceLocation id, CallbackInfoReturnable<Boolean> cir) {
        if (connection == null || connection.channel() == null) {
            cir.setReturnValue(false);
        }
    }
}
