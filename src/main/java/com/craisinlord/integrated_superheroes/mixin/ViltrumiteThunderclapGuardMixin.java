package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.baranhan.viltrumitecore.util.ThunderClapManager;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ThunderClapManager.class, remap = false)
public abstract class ViltrumiteThunderclapGuardMixin {
    @WrapOperation(method = "tickThunderclaps", at = @At(value = "INVOKE", target = "Ljava/util/List;iterator()Ljava/util/Iterator;", ordinal = 0))
    private static Iterator<ThunderClapManager.ActiveClap> integratedSuperheroes$clapsInLevel(List<ThunderClapManager.ActiveClap> claps,
            Operation<Iterator<ThunderClapManager.ActiveClap>> original, @Local(argsOnly = true) ServerLevel level) {
        claps.removeIf(clap -> ViltrumiteGuards.stranded(clap.attacker));
        return ViltrumiteGuards.inLevel(original.call(claps), clap -> clap.attacker, level);
    }

    @WrapWithCondition(method = "tickThunderclaps", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(DDD)V"))
    private static boolean integratedSuperheroes$keepBossStill(LivingEntity target, double x, double y, double z) {
        return !ViltrumiteGuards.resistsForce(target);
    }

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
