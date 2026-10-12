package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.IronManEnergy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.tintankgames.marvel.attachment.MarvelAttachmentTypes;
import net.tintankgames.marvel.attachment.VeronicaData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = VeronicaData.class, remap = false)
public abstract class VeronicaSuitChargeMixin {
    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private static void integratedMarvel$chargeStoredSuits(PlayerTickEvent.Post event, CallbackInfo ci) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        int rate = IronManEnergy.veronicaRate();
        if (rate <= 0) {
            return;
        }
        VeronicaData veronica = player.getData(MarvelAttachmentTypes.VERONICA);
        for (VeronicaData.Suit suit : veronica.getSuits()) {
            for (ItemStack piece : suit.armor()) {
                IronManEnergy.addFE(piece, rate);
            }
        }
    }
}
