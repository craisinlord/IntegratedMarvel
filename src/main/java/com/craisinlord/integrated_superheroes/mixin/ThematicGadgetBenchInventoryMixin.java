package com.craisinlord.integrated_superheroes.mixin;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = {"bond.thematic.mod.block.entity.BlockEntityGadgetBench", "bond.thematic.mod.block.entity.BlockEntityArrowBench"}, remap = false)
public abstract class ThematicGadgetBenchInventoryMixin {
    @Inject(method = "getItems", at = @At("RETURN"), cancellable = true, remap = false)
    private void integratedSuperheroes$nonNullInventory(CallbackInfoReturnable<NonNullList<ItemStack>> cir) {
        if (cir.getReturnValue() == null) cir.setReturnValue(NonNullList.create());
    }
}
