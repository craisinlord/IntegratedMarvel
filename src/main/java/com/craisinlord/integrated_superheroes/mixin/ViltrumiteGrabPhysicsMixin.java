package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.craisinlord.integrated_superheroes.compat.ViltrumiteCompat;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Entity.class, priority = 1500)
public abstract class ViltrumiteGrabPhysicsMixin {
    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.SolidEntityBypassMixin", name = "disableSolidCollision")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idleSolid(CallbackInfoReturnable<Boolean> cir, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.SolidEntityBypassMixin", name = "disablePushable")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idlePushable(CallbackInfoReturnable<Boolean> cir, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.GrabbedEntityPhysicsMixin", name = "onPushEntity")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idlePushEntity(Entity other, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }

    @TargetHandler(mixin = "dev.baranhan.viltrumitecore.mixin.GrabbedEntityPhysicsMixin", name = "onPushVector")
    @Inject(method = "@MixinSquared:Handler", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$idlePushVector(double x, double y, double z, CallbackInfo original, CallbackInfo ci) {
        if (!ViltrumiteCompat.active()) {
            ci.cancel();
        }
    }
}
