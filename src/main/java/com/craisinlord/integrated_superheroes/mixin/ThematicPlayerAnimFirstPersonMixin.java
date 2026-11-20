package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import dev.kosmx.playerAnim.api.firstPerson.FirstPersonMode;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.client.SuitModelProxies", remap = false)
public abstract class ThematicPlayerAnimFirstPersonMixin {
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$skipSuitInFirstPersonPass(LivingEntity entity, CallbackInfoReturnable<CharacterItem> cir) {
        if (entity != null && FirstPersonMode.isFirstPersonPass() && entity == Minecraft.getInstance().getCameraEntity()) {
            cir.setReturnValue(null);
        }
    }
}
