package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteGuards;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.baranhan.viltrumitecore.util.ChargePunchManager;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ChargePunchManager.class, remap = false)
public abstract class ViltrumiteChargePunchGuardMixin {
    @ModifyReturnValue(method = "isValidTarget", at = @At("RETURN"))
    private static boolean integratedSuperheroes$skipBossPull(boolean valid, ServerPlayer player, LivingEntity entity) {
        return valid && !ViltrumiteGuards.resistsForce(entity);
    }

    @WrapWithCondition(method = "executeSuperPunch", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;setDeltaMovement(Lnet/minecraft/world/phys/Vec3;)V"))
    private static boolean integratedSuperheroes$keepBossStill(LivingEntity target, Vec3 velocity) {
        return !ViltrumiteGuards.resistsForce(target);
    }

    @WrapOperation(method = "executeSuperPunch", at = @At(value = "INVOKE",
            target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private static Object integratedSuperheroes$skipBossLaunch(Map<Object, Object> launched, Object target, Object data, Operation<Object> original) {
        return target instanceof Entity entity && ViltrumiteGuards.resistsForce(entity) ? null : original.call(launched, target, data);
    }
}
