package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.HeroSlotsClient;
import net.neoforged.neoforge.client.event.InputEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.stxllarplanes.marvellegacy.init.ModKeyMappings$KeyHandler", remap = false)
public abstract class LegacyAbilityKeysMixin {
    @Inject(method = "onKeyInput", at = @At("HEAD"), cancellable = true)
    private static void integratedSuperheroes$extraSlots(InputEvent.Key event, CallbackInfo ci) {
        if (HeroSlotsClient.handleKey(event)) {
            ci.cancel();
        }
    }
}
