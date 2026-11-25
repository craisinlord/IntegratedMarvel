package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.ability.runtime.StateFlags;
import com.craisinlord.integrated_superheroes.hero.skin.MarvelSerumLooks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.content.ability.client.StateFlagsClient", remap = false)
public abstract class ThematicStateFlagsClientMixin {
    @Inject(method = "of", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$dontCacheLooks(LivingEntity living, CallbackInfoReturnable<StateFlags.Flags> cir) {
        if (MarvelSerumLooks.overriding(living)) {
            cir.setReturnValue(StateFlags.compute(living));
        }
    }
}
