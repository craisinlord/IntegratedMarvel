package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.HeroSlots;
import com.stxllarplanes.marvellegacy.data.PlayerData;
import com.stxllarplanes.marvellegacy.power.Power;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = PlayerData.class, remap = false)
public abstract class LegacyTenSlotsMixin {
    @Shadow
    private List<Power> activePowers;

    @Inject(method = "getNewActivePowers", at = @At("RETURN"))
    private void integratedMarvel$tenNewSlots(CallbackInfoReturnable<List<Power>> cir) {
        HeroSlots.pad(cir.getReturnValue());
    }

    @Inject(method = "retainActivePowerSize", at = @At("TAIL"))
    private void integratedMarvel$tenSlots(CallbackInfo ci) {
        HeroSlots.pad(this.activePowers);
    }
}
