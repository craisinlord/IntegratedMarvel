package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.data.ability.AbilityDataComponent;
import java.util.LinkedHashMap;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AbilityDataComponent.class, remap = false)
public interface ThematicAbilityDataAccessor {
    @Accessor(value = "syncedData", remap = false)
    LinkedHashMap<String, Tag> integratedSuperheroes$syncedData();
}
