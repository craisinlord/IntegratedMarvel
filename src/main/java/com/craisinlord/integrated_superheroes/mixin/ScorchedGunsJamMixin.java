package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.compat.GunJam;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.ribs.scguns.entity.weapon.ScGunsWeapon;

@Mixin(value = ScGunsWeapon.class, remap = false)
public abstract class ScorchedGunsJamMixin {
    @Inject(method = "performRangedAttackIWeapon", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedSuperheroes$jammed(Mob shooter, double x, double y, double z, float speed, CallbackInfo ci) {
        if (GunJam.blocksShot(shooter)) {
            ci.cancel();
        }
    }
}
