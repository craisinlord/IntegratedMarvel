package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.hero.skin.MarvelSerumLooks;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class MarvelSerumLookSlotMixin {
    @Inject(method = "getItemBySlot", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$serumLook(EquipmentSlot slot, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack look = MarvelSerumLooks.override((Player) (Object) this, slot);
        if (look != null) {
            cir.setReturnValue(look);
        }
    }
}
