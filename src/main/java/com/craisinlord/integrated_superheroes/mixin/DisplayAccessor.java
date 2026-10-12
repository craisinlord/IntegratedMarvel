package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setPosRotInterpolationDuration")
    void integratedSuperheroes$setPosRotInterpolationDuration(int duration);
}
