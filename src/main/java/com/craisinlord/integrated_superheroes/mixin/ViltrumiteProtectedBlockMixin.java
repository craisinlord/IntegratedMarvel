package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.ViltrumiteInvasions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class ViltrumiteProtectedBlockMixin {
    @Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$unbreakableToNpcs(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
        if (ViltrumiteInvasions.protects((BlockBehaviour.BlockStateBase) (Object) this)) {
            cir.setReturnValue(-1.0F);
        }
    }
}
