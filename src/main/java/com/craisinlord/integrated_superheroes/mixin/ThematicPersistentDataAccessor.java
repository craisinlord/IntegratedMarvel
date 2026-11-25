package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.data.ability.PersistentDataComponent;
import java.util.LinkedHashMap;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = PersistentDataComponent.class, remap = false)
public interface ThematicPersistentDataAccessor {
    @Accessor(value = "persistentData", remap = false)
    LinkedHashMap<String, Tag> integratedSuperheroes$persistentData();
}
