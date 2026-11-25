package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.ability.runtime.StateFlags;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = StateFlags.class, remap = false)
public abstract class ThematicStateFlagsMixin {
    @WrapOperation(method = "of", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lbond/thematic/api/content/ability/runtime/StateFlags;compute(Lnet/minecraft/world/entity/LivingEntity;)Lbond/thematic/api/content/ability/runtime/StateFlags$Flags;"))
    private static StateFlags.Flags integratedSuperheroes$oncePerTick(LivingEntity living, Operation<StateFlags.Flags> original) {
        if (!(living instanceof ServerPlayer player)) {
            return original.call(living);
        }
        return ThematicIdle.flags(player, () -> original.call(living));
    }
}
