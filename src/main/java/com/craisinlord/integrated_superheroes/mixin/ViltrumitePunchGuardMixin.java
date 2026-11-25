package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteBridge;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.baranhan.viltrumitecore.util.PunchImpactManager;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PunchImpactManager.class, remap = false)
public abstract class ViltrumitePunchGuardMixin {
    @WrapOperation(method = "tickMeteorPhysics", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"))
    private static Iterator<Map.Entry<Entity, ?>> integratedSuperheroes$meteorsInLevel(Set<Map.Entry<Entity, ?>> launched,
            Operation<Iterator<Map.Entry<Entity, ?>>> original, @Local(argsOnly = true) ServerLevel level) {
        launched.removeIf(entry -> ViltrumiteGuards.stranded(entry.getKey()));
        return ViltrumiteGuards.inLevel(original.call(launched), Map.Entry::getKey, level);
    }

    @WrapWithCondition(method = "punchCone", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static boolean integratedSuperheroes$keepBossStill(LivingEntity target, Vec3 velocity) {
        return !ViltrumiteGuards.resistsForce(target);
    }

    @WrapOperation(method = "punchCone", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object integratedSuperheroes$skipBossLaunch(Map<Object, Object> launched, Object target, Object data, Operation<Object> original) {
        return target instanceof Entity entity && ViltrumiteGuards.resistsForce(entity) ? null : original.call(launched, target, data);
    }

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
