package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.trait.WearerTraits;
import com.craisinlord.integrated_superheroes.hero.skin.MarvelSerumLooks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.content.legacy.mixin.client.ClientTraitCache", remap = false)
public abstract class ThematicClientTraitCacheMixin {
    @Inject(method = "get", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$dontCacheLooks(LivingEntity living, String trait, CallbackInfoReturnable<Object> cir) {
        if (MarvelSerumLooks.overriding(living)) {
            cir.setReturnValue(WearerTraits.get(living, trait));
        }
    }
}
