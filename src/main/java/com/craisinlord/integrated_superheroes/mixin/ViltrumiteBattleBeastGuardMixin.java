package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.baranhan.viltrumitecore.entity.battlebeast.BattleBeastEntity;
import dev.baranhan.viltrumitecore.entity.battlebeast.BattleBeastImpactManager;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BattleBeastImpactManager.class, remap = false)
public abstract class ViltrumiteBattleBeastGuardMixin {
    @WrapMethod(method = "launch")
    private static void integratedSuperheroes$skipBossSlam(LivingEntity victim, Vec3 velocity, BattleBeastEntity source, Operation<Void> original) {
        if (!ViltrumiteGuards.resistsForce(victim)) {
            original.call(victim, velocity, source);
        }
    }
}
