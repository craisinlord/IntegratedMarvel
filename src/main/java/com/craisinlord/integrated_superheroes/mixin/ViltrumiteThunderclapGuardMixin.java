package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.baranhan.viltrumitecore.util.ThunderClapManager;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ThunderClapManager.class, remap = false)
public abstract class ViltrumiteThunderclapGuardMixin {
    @WrapMethod(method = "tickThunderclaps")
    private static void integratedSuperheroes$guardNpcClaps(ServerLevel level, Operation<Void> original) {
        boolean entered = ViltrumiteInvasions.enterGuard(ViltrumiteInvasions.npcClapsActive());
        try {
            original.call(level);
        } finally {
            ViltrumiteInvasions.exitGuard(entered);
        }
    }
}
