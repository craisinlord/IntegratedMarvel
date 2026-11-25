package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.data.ability.AbilityDataComponent;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicIdle;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.LinkedHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = AbilityDataComponent.class, remap = false)
public abstract class ThematicAbilityDataWriteMixin {
    @WrapOperation(method = {"set(Ljava/lang/String;Lnet/minecraft/nbt/Tag;Z)V", "read(Lbond/thematic/api/component/core/DataIn;)V"},
            remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/LinkedHashMap;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object integratedSuperheroes$noteHeldWindow(LinkedHashMap<Object, Object> map, Object key, Object value, Operation<Object> original) {
        ThematicIdle.abilityDataPut(key, value);
        return original.call(map, key, value);
    }
}
