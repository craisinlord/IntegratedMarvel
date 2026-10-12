package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.MagnetoPower;
import com.github.alexmodguy.alexscaves.server.entity.util.MagnetUtil;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagnetUtil.class, remap = false)
public abstract class MagnetoWalkMixin {
    @Inject(method = "isPulledByMagnets", at = @At("RETURN"), cancellable = true)
    private static void integratedMarvel$magnetoPulled(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && MagnetoPower.magneticWalker(entity)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "attachesToMagnets", at = @At("RETURN"), cancellable = true)
    private static void integratedMarvel$magnetoAttaches(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && MagnetoPower.magneticWalker(entity)) {
            cir.setReturnValue(true);
        }
    }

    @WrapOperation(method = {"getStandingOnMagnetSurface", "calculateClosestDirection"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"))
    private static boolean integratedMarvel$metalWalls(BlockState state, TagKey<Block> tag, Operation<Boolean> original,
                                                   @Local(argsOnly = true) Entity entity) {
        return original.call(state, tag) || MagnetoPower.magneticWalker(entity) && MagnetoPower.metalBlock(state);
    }

    @Inject(method = "tickMagnetism", at = @At("HEAD"))
    private static void integratedMarvel$selfPull(Entity entity, CallbackInfo ci) {
        if (MagnetoPower.magneticWalker(entity)) {
            MagnetoPower.selfPull(entity);
        }
    }
}
