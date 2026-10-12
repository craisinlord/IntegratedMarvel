package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.antarchy.content.entity.flying_squirrel.FlyingSquirrelEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = FlyingSquirrelEntity.class, remap = false)
public interface FlyingSquirrelInvoker {
    @Invoker("startShoulderMountSequence")
    void integratedSuperheroes$mountShoulder(Player player);

    @Invoker("dismountShoulder")
    void integratedSuperheroes$dismountShoulder();

    @Invoker("startGliding")
    void integratedSuperheroes$startGliding(BlockPos target, String reason);

    @Invoker("setTextureVariant")
    void integratedSuperheroes$setTextureVariant(int variant);
}
