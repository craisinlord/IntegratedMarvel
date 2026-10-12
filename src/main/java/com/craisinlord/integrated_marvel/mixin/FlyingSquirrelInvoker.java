package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.antarchy.content.entity.flying_squirrel.FlyingSquirrelEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = FlyingSquirrelEntity.class, remap = false)
public interface FlyingSquirrelInvoker {
    @Invoker("startShoulderMountSequence")
    void integratedMarvel$mountShoulder(Player player);

    @Invoker("dismountShoulder")
    void integratedMarvel$dismountShoulder();

    @Invoker("startGliding")
    void integratedMarvel$startGliding(BlockPos target, String reason);

    @Invoker("setTextureVariant")
    void integratedMarvel$setTextureVariant(int variant);
}
