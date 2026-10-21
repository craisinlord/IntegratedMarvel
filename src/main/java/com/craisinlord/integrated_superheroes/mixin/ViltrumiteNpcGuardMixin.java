package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.baranhan.viltrumitecore.entity.ViltrumiteFakePlayer;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ViltrumiteFakePlayer.class, remap = false)
public abstract class ViltrumiteNpcGuardMixin {
    @WrapMethod(method = "tick")
    private void integratedSuperheroes$guardBlockEntities(Operation<Void> original) {
        boolean entered = ViltrumiteInvasions.enterGuard(true);
        try {
            original.call();
        } finally {
            ViltrumiteInvasions.exitGuard(entered);
        }
    }
}
