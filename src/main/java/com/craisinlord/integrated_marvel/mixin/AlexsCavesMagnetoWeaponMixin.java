package com.craisinlord.integrated_marvel.mixin;

import com.craisinlord.integrated_marvel.hero.MagnetoPower;
import com.github.alexmodguy.alexscaves.server.entity.item.MagneticWeaponEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagneticWeaponEntity.class, remap = false)
public abstract class AlexsCavesMagnetoWeaponMixin {
    @Inject(method = "isOwnerWearingGauntlet", at = @At("HEAD"), cancellable = true, remap = false)
    private void integratedMarvel$magnetoControls(CallbackInfoReturnable<Boolean> callback) {
        if (((MagneticWeaponEntity) (Object) this).getController() instanceof Player player && MagnetoPower.isSwarming(player)) {
            callback.setReturnValue(true);
        }
    }
}
