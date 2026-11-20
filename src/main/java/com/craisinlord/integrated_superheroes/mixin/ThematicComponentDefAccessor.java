package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.content.ability.part.ComponentDef;
import bond.thematic.api.content.ability.part.MobPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ComponentDef.class, remap = false)
public interface ThematicComponentDefAccessor {
    @Mutable
    @Accessor(value = "mob", remap = false)
    void integratedSuperheroes$setMob(MobPart mob);
}
