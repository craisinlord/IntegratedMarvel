package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicFixes;
import com.craisinlord.integrated_superheroes.compat.thematic.ThematicOptions;
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Explosion.class, priority = 1500)
public abstract class ThematicExplosionKnockbackMixin {
    @TargetHandler(mixin = "bond.thematic.api.mixin.core.ExplosionKnockbackMixin", name = "thematic$reduceExplosionKnockback")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$suitedOnly(CallbackInfo outer, CallbackInfo ci) {
        if (!ThematicOptions.on(ThematicOptions.VANILLA_EXPLOSION_KNOCKBACK)) {
            return;
        }
        Explosion explosion = (Explosion) (Object) this;
        if (!ThematicFixes.suited(explosion.getDirectSourceEntity()) && !ThematicFixes.suited(explosion.getIndirectSourceEntity())) {
            ci.cancel();
        }
    }
}
