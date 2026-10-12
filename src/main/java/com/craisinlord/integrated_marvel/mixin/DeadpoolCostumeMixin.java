package com.craisinlord.integrated_marvel.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.common.EffectCure;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(targets = "net.tintankgames.marvel.world.item.DeadpoolSuitItem", remap = false)
public abstract class DeadpoolCostumeMixin {
    @Redirect(
            method = "livingTick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;heal(F)V", remap = false),
            remap = false
    )
    private static void integratedMarvel$disableCostumeHealing(LivingEntity living, float amount) {
    }

    @Redirect(
            method = "livingTick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;removeEffectsCuredBy(Lnet/neoforged/neoforge/common/EffectCure;)Z",
                    remap = false
            ),
            remap = false
    )
    private static boolean integratedMarvel$disableCostumeCures(LivingEntity living, EffectCure cure) {
        return false;
    }
}
