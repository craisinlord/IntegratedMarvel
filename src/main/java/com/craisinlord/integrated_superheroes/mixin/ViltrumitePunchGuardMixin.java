package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.baranhan.viltrumitecore.util.PunchImpactManager;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = PunchImpactManager.class, remap = false)
public abstract class ViltrumitePunchGuardMixin {
    @WrapMethod(method = "executePunch")
    private static void integratedSuperheroes$trackNpcLaunches(ServerPlayer player, Operation<Void> original) {
        if (!ViltrumiteBridge.isNpc(player)) {
            original.call(player);
            return;
        }
        Set<Entity> before = ViltrumiteInvasions.launchedSnapshot();
        boolean entered = ViltrumiteInvasions.enterGuard(true);
        try {
            original.call(player);
        } finally {
            ViltrumiteInvasions.exitGuard(entered);
            ViltrumiteInvasions.markLaunched(before);
        }
    }

    @WrapMethod(method = "tickMeteorPhysics")
    private static void integratedSuperheroes$guardNpcMeteors(ServerLevel level, Operation<Void> original) {
        boolean entered = ViltrumiteInvasions.enterGuard(ViltrumiteInvasions.npcMeteorsActive());
        try {
            original.call(level);
        } finally {
            ViltrumiteInvasions.exitGuard(entered);
        }
    }
}
