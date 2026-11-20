package com.craisinlord.integrated_superheroes.mixin;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = LivingEntity.class, priority = 1500)
public abstract class ThematicSuitPieceSlotMixin {
    @TargetHandler(mixin = "bond.thematic.api.mixin.core.ArmorSlotMixin", name = "thematic$tick")
    @WrapOperation(method = "@MixinSquared:Handler", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/entity/LivingEntity;spawnAtLocation(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity integratedSuperheroes$keepArmorDrop(LivingEntity living, ItemStack stack, Operation<ItemEntity> original) {
        return null;
    }

    @TargetHandler(mixin = "bond.thematic.api.mixin.core.ArmorSlotMixin", name = "thematic$tick")
    @WrapOperation(method = "@MixinSquared:Handler", remap = false, at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraft/world/entity/LivingEntity;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"))
    private void integratedSuperheroes$keepArmorSlot(LivingEntity living, EquipmentSlot slot, ItemStack stack, Operation<Void> original) {
    }
}
