package com.craisinlord.integrated_superheroes.mixin;

import com.craisinlord.integrated_superheroes.cleanup.Cleanup;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public abstract class ItemNameCleanupMixin {
    @Inject(method = "getName", at = @At("HEAD"), cancellable = true)
    private void integratedSuperheroes$rename(ItemStack stack, CallbackInfoReturnable<Component> cir) {
        Cleanup.rename((Item) (Object) this).ifPresent(key -> cir.setReturnValue(Component.translatable(key)));
    }
}
