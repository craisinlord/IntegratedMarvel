package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {"bond.thematic.mod.abilities.ArmWrapAbility", "bond.thematic.mod.abilities.ConstructClawAbility"}, remap = false)
public abstract class ThematicIdleListenersMixin {
    @Inject(method = {"lambda$serverEvents$0(Lnet/minecraft/world/entity/LivingEntity;)V", "lambda$serverEvents$1(Lnet/minecraft/world/entity/LivingEntity;)V"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$skipUnequipped(LivingEntity entity, CallbackInfo ci) {
        if (ThematicIdle.wearsNothing(entity)) {
            ci.cancel();
        }
    }
}
