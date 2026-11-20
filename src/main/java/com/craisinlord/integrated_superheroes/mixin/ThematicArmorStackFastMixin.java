package com.craisinlord.integrated_superheroes.mixin;

import bond.thematic.api.registries.armors.armor.CharacterItem;
import com.craisinlord.integrated_superheroes.compat.thematic.PowerOwnership;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "bond.thematic.api.util.ThematicHelper", remap = false)
public abstract class ThematicArmorStackFastMixin {
    @Inject(method = "getArmorStack(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), cancellable = true, remap = false)
    private static void integratedSuperheroes$chestOnly(LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (entity == null || entity instanceof Player player && player.getInventory() == null) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack chest = entity.getItemBySlot(EquipmentSlot.CHEST);
        boolean powered = !(entity instanceof Player)
                || !PowerOwnership.costume(BuiltInRegistries.ITEM.getKey(chest.getItem()).toString());
        cir.setReturnValue(powered && !chest.isEmpty() && chest.getItem() instanceof CharacterItem ? chest : ItemStack.EMPTY);
    }
}
