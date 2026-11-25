package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobPersistenceAccessor {
    @Accessor("persistenceRequired")
    void integratedSuperheroes$setPersistenceRequired(boolean required);
}
