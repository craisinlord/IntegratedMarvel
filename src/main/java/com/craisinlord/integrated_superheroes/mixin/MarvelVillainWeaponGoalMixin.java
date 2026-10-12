package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.villain.Villains;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = {
        "net.tintankgames.marvel.world.entity.WinterSoldier",
        "net.tintankgames.marvel.world.entity.RedSkull",
        "net.tintankgames.marvel.world.entity.HydraAgent"
}, remap = false)
public abstract class MarvelVillainWeaponGoalMixin {
    @Inject(method = "reassessWeaponGoal", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void integratedSuperheroes$keepVillainGoals(CallbackInfo ci) {
        if (Villains.blockWeaponGoal((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
