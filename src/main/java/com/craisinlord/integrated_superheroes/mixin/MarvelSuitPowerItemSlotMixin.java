package com.craisinlord.integrated_superheroes.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.tintankgames.marvel.world.item.SuitItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.List;

/**
 * Marvel's SuitItem.inventoryTick checks Inventory.contains/countItem (all slots) but looks the power
 * item up with findSlotMatchingItem over the 36 main slots only. A power item in the offhand gives -1
 * and Inventory.getItem(-1) crashes the server. Search the whole inventory and never pass a negative slot.
 */
@Mixin(value = SuitItem.class, remap = false)
public abstract class MarvelSuitPowerItemSlotMixin {

    @WrapOperation(
            method = "inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/tintankgames/marvel/world/item/SuitItem;findSlotMatchingItem(Ljava/util/List;Lnet/minecraft/world/item/Item;)I"),
            remap = false)
    private int integratedSuperheroes$findAnySlot(List<ItemStack> items, Item item, Operation<Integer> original,
                                                  @Local(argsOnly = true) Entity entity) {
        int slot = original.call(items, item);
        if (slot >= 0 || !(entity instanceof Player player)) {
            return slot;
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    @WrapOperation(
            method = "inventoryTick(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;IZ)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;getItem(I)Lnet/minecraft/world/item/ItemStack;"),
            remap = false)
    private ItemStack integratedSuperheroes$guardGetItem(Inventory inv, int slot, Operation<ItemStack> original) {
        if (slot < 0 || slot >= inv.getContainerSize()) {
            return ItemStack.EMPTY;
        }
        return original.call(inv, slot);
    }
}
