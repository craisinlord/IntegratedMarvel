package com.craisinlord.integrated_marvel.mixin;

import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface DisplayAccessor {
    @Invoker("setPosRotInterpolationDuration")
    void integratedMarvel$setPosRotInterpolationDuration(int duration);
}
