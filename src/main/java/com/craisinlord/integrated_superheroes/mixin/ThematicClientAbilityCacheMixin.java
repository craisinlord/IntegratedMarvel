package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.util.ClientAbilityCache", remap = false)
public abstract class ThematicClientAbilityCacheMixin {
    @Inject(method = "isActive(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$unsuitedNotActive(LivingEntity entity, String abilityId, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && !(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CharacterItem)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hasAbility(Lnet/minecraft/world/entity/LivingEntity;Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$unsuitedHasNone(LivingEntity entity, String abilityId, CallbackInfoReturnable<Boolean> cir) {
        if (entity != null && !(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CharacterItem)) {
            cir.setReturnValue(false);
        }
    }
}
