package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.villain.Villains;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.tintankgames.marvel.world.entity.WinterSoldier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = WinterSoldier.class, remap = false)
public abstract class WinterSoldierVillainMixin {
    @Redirect(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/tintankgames/marvel/world/entity/WinterSoldier;setItemSlot(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;)V"),
            remap = false, require = 0)
    private void integratedSuperheroes$noClockSwap(WinterSoldier self, EquipmentSlot slot, ItemStack stack) {
        if (!Villains.managed(self)) {
            self.setItemSlot(slot, stack);
        }
    }
}
