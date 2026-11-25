package com.craisinlord.integrated_superheroes.mixin;

import com.google.common.collect.MapMaker;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "bond.thematic.api.util.ScaleStore", remap = false)
public abstract class ThematicScaleStoreMixin {
    @ModifyExpressionValue(method = "<clinit>", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Ljava/util/Collections;synchronizedMap(Ljava/util/Map;)Ljava/util/Map;"))
    private static Map<Object, Object> integratedSuperheroes$keyByObject(Map<Object, Object> original) {
        return new MapMaker().weakKeys().makeMap();
    }
}
